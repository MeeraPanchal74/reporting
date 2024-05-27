/*package com.example.meeracopy.controller;

import com.example.meeracopy.domain.*;
import com.example.meeracopy.domain.globaldata.GlobalBody;
import com.example.meeracopy.domain.globaldata.ReturnGlobal;
import com.example.meeracopy.filters.Filter;
import com.example.meeracopy.myAggregations.MyAggregations;
import com.example.meeracopy.repo.ProductRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.aggregations.BucketOrder;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.*;

@RestController
@Slf4j
@RequestMapping("api/products")
public class ProductController {


    @Autowired
    private ProductRepo elasticSearchQuery;


    @PostMapping("/bulkGlobal")
    public ResponseEntity<String> bulkGlobal(@RequestBody List<UnifiedModel> unifiedModels) throws IOException {
        String response = elasticSearchQuery.bulkSaveGlobal(unifiedModels);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }




    @GetMapping("/findAllGlobal")
    public ResponseEntity<List<UnifiedModel>> findAllGlobal() throws IOException {
        List<UnifiedModel> unifiedModels = elasticSearchQuery.findAllGlobal();
        log.info("No of Product Documents has been successfully retrieved: {}", unifiedModels.size());
        return new ResponseEntity<>(unifiedModels, HttpStatus.OK);
    }

    public static Map<String, Object> convertBoolQueryToMap(BoolQueryBuilder boolQuery) {
        Map<String, Object> queryMap = new HashMap<>();

        List<Map<String, Object>> filterList = new ArrayList<>();
        List<Map<String, Object>> mustNotList = new ArrayList<>();

        // Iterate over filter clauses
        for (QueryBuilder filter : boolQuery.filter()) {
            filterList.add(buildQueryMap(filter));
        }

        // Iterate over must_not clauses
        for (QueryBuilder mustNot : boolQuery.mustNot()) {
            mustNotList.add(buildQueryMap(mustNot));
        }

        // Put filter and must_not clauses into the query map
        queryMap.put("filter", filterList);
        queryMap.put("must_not", mustNotList);

        // Handle adjust_pure_negative and boost if needed



        return Map.of("bool", queryMap);
    }

    // Helper method to build a map for a single query clause
    private static Map<String, Object> buildQueryMap(QueryBuilder queryBuilder) {
        Map<String, Object> queryMap = new HashMap<>();

        if (queryBuilder instanceof TermsQueryBuilder) {
            TermsQueryBuilder termsQuery = (TermsQueryBuilder) queryBuilder;
            queryMap.put("terms", Map.of(
                    termsQuery.fieldName(), termsQuery.values()
            ));
        } else if (queryBuilder instanceof RangeQueryBuilder) {
            RangeQueryBuilder rangeQuery = (RangeQueryBuilder) queryBuilder;
            Map<String, Object> rangeDetails = new HashMap<>();
            rangeDetails.put("from", rangeQuery.from());
            rangeDetails.put("to", rangeQuery.to());
            rangeDetails.put("include_lower", rangeQuery.includeLower());
            rangeDetails.put("include_upper", rangeQuery.includeUpper());
            rangeDetails.put("boost", rangeQuery.boost());

            queryMap.put("range", Map.of(rangeQuery.fieldName(), rangeDetails));
        }
        // Add other conditions for different types of queries if needed

        return queryMap;
    }


    public static RestHighLevelClient getRestClient() {
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")));
    }



    @PostMapping("/generalGlobalQuery")
    public ReturnGlobal generalGlobalQuery(@RequestBody GlobalBody getObj) throws IOException, ParseException {
        ReturnGlobal putObj = new ReturnGlobal();

        List<Filter> filters = getObj.query.filters;
        List<MyAggregations> myAggregations = getObj.query.Aggregations;
        Map<String, Object> queryMap = new HashMap<>();
        BoolQueryBuilder boolQuery = boolQuery();


        if(filters!=null) {

            for (Filter filter : filters) {
                List<Object> lowerCaseValues = new ArrayList<>();


                for (Object value : filter.value) {
                    if (value instanceof String && !Objects.equals(filter.field, "postingTime")) {
                        lowerCaseValues.add(((String) value).toLowerCase());
                    }

                    else if(!Objects.equals(filter.field, "postingTime")){
                        lowerCaseValues.add(value);
                    }
                }

                if(Objects.equals(filter.field, "postingTime")){
                    for(String value: filter.value){
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                        LocalDate localDate = LocalDate.parse(value, formatter);
                        Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

                        lowerCaseValues.add(date.getTime());
                    }
                }

                switch (filter.type) {

                    case "CONTAINS":

                        boolQuery.filter(termsQuery(filter.field,lowerCaseValues));

                        Object queryBuilder = null;
                        TermsQueryBuilder termsQuery = (TermsQueryBuilder) queryBuilder;
                        queryMap.put("type", "terms");
                        queryMap.put("field", termsQuery.fieldName());
                        List<Object> values = new ArrayList<>();
                        for (Object value : termsQuery.values()) {

                            values.add(value);
                        }
                        queryMap.put("values", values);
                        break;

                    case "range":
                        ;
                        boolQuery.filter(rangeQuery(filter.field).from(lowerCaseValues.get(0)).to(lowerCaseValues.get(1)));
                        break;

                    case "IN":
                        boolQuery.filter(termsQuery(filter.field,lowerCaseValues));
                        break;

                    case "NIN":
                        boolQuery.mustNot(termsQuery(filter.field, lowerCaseValues));
                        break;

                    default:
                        break;
                }
            }
        }



        Map<String, Object> jsonMap;
        Map<String, Object> mp = convertBoolQueryToMap(boolQuery);
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = "";
        try {
            // Serialize map to JSON string
            jsonString = mapper.writeValueAsString(mp);

            // Deserialize JSON string back to map
            jsonMap = mapper.readValue(jsonString, Map.class);

            // Print the map to verify its structure

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }







        SearchModule sm = new SearchModule(Settings.EMPTY, false, Collections.emptyList());
        XContentParser parser = XContentFactory.xContent(XContentType.JSON)
                .createParser(new NamedXContentRegistry(sm.getNamedXContents()),
                        LoggingDeprecationHandler.INSTANCE,
                        jsonString);



        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.wrapperQuery(jsonString));



        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse1 = getRestClient().search(searchRequest, RequestOptions.DEFAULT);

        putObj.no_of_posts = searchResponse1.getHits().getTotalHits().value;



        if(myAggregations!=null) {
            for (MyAggregations term : myAggregations) {

                switch (term.field) {
                    case "language":
                        long num=10;
                        boolean sortVal = false;
                        if(Objects.equals(term.sort, "asc")){
                            sortVal = true;
                        }
                        if (term.size != null && term.size<=20) {
                            num = term.size;
                        } else {
                            num = 10;
                        }


                        if (!Objects.equals(term.subaggregation, "none")) {
                            searchSourceBuilder.aggregation(
                                    AggregationBuilders.terms("group_by_language")

                                            .field("languageCode.keyword")
                                            .subAggregation(AggregationBuilders.terms("group_by_source")
                                                    .field("source.keyword"))
                                            .size((int) num)
                                            .order(BucketOrder.count(sortVal))



                            );
                        }
                        else {
                            searchSourceBuilder.aggregation(
                                    AggregationBuilders.terms("group_by_language").field("languageCode.keyword")
                                            .size((int)num)
                                            .order(BucketOrder.count(sortVal)));
                        }



                        searchRequest.source(searchSourceBuilder);

                        SearchResponse searchResponse = getRestClient().search(searchRequest, RequestOptions.DEFAULT);

                        Terms languageCount = searchResponse.getAggregations().get("group_by_language");

                        if (!term.subaggregation.isEmpty()  && !term.subaggregation.equals("none")) {

                            Map<String, List<Map<String, Object>>> languageSourceCounts = languageCount.getBuckets().stream()
                                    .collect(Collectors.toMap(
                                            Terms.Bucket::getKeyAsString,
                                            languageBucket -> {
                                                Terms sourceCount = languageBucket.getAggregations().get("group_by_source");
                                                Map<String, Long> sourceCountsMap = sourceCount.getBuckets().stream()
                                                        .collect(Collectors.toMap(
                                                                Terms.Bucket::getKeyAsString,
                                                                Terms.Bucket::getDocCount,
                                                                (oldValue, newValue) -> oldValue,
                                                                LinkedHashMap::new
                                                        ));
                                                List<Map<String, Object>> resultList = new ArrayList<>();
                                                resultList.add(new HashMap<>(sourceCountsMap));
                                                Map<String, Object> totalCountMap = new HashMap<>();
                                                totalCountMap.put("totalCount", languageBucket.getDocCount());
                                                resultList.add(totalCountMap);
                                                return resultList;
                                            },
                                            (oldValue, newValue) -> oldValue,
                                            LinkedHashMap::new
                                    ));
                            putObj.languageCode = languageSourceCounts;
                        }


                        else {

                            Map<String, Long> languageSourceCounts = languageCount.getBuckets().stream()
                                    .collect(Collectors.toMap(
                                            Terms.Bucket::getKeyAsString,
                                            Terms.Bucket::getDocCount,
                                            (oldValue, newValue) -> oldValue,
                                            LinkedHashMap::new
                                    ));
                            putObj.languageCount = languageSourceCounts;
                        }
                        break;

                    case "source":
                        boolean sortVal1 = false;
                        long num1 = 10;
                        if (term.size != null && term.size<=20) {
                            num1 = term.size;
                        } else {
                            num1 = 10;
                        }
                        if(Objects.equals(term.sort, "asc")){
                            sortVal = true;
                        }
                        for(Map<String, List<String>> myProj: term.projection){

                            if (myProj.containsKey("score")) {
                                if (myProj.get("score") != null) {

                                    List<String> scoreProjections = new ArrayList<String>();

                                    for (String myStr : myProj.get("score")) {
                                        scoreProjections.add(myStr);

                                    }


                                    searchSourceBuilder.aggregation(
                                            AggregationBuilders.terms("group_by_source")
                                                    .field("source.keyword")
                                                    .size((int) num1)
                                                    .order(BucketOrder.count(sortVal1))
                                                    .subAggregation(
                                                            AggregationBuilders.sum("total_score").field("score")
                                                    )
                                                    .subAggregation(
                                                            AggregationBuilders.min("min_score").field("score")
                                                    )
                                                    .subAggregation(
                                                            AggregationBuilders.max("max_score").field("score")
                                                    )
                                    );
                                    searchRequest.source(searchSourceBuilder);

                                    SearchResponse searchResponse2 = getRestClient().search(searchRequest, RequestOptions.DEFAULT);

                                    Terms sourceCount = searchResponse2.getAggregations().get("group_by_source");


                                    Map<String, List<Map<String, Object>>> sourceAggregationsSource = sourceCount.getBuckets().stream()
                                            .collect(Collectors.toMap(
                                                    Terms.Bucket::getKeyAsString,
                                                    sourceBucket -> {
                                                        List<Map<String, Object>> resultList = new ArrayList<>();
                                                        Sum totalScore = sourceBucket.getAggregations().get("total_score");
                                                        Min minScore = sourceBucket.getAggregations().get("min_score");
                                                        Max maxScore = sourceBucket.getAggregations().get("max_score");

                                                        Map<String, Object> postCountMap = new HashMap<>();
                                                        postCountMap.put("No_of_Posts", sourceBucket.getDocCount());
                                                        resultList.add(postCountMap);

                                                        // Only add the fields that are in the projection
                                                        if (scoreProjections.contains("total") && totalScore != null) {
                                                            //  System.out.print("inside totalscore");
                                                            Map<String, Object> totalScoreMap = new HashMap<>();
                                                            totalScoreMap.put("totalScore", totalScore.getValue());
                                                            resultList.add(totalScoreMap);
                                                        }

                                                        if (scoreProjections.contains("min") && minScore != null) {
                                                            Map<String, Object> minScoreMap = new HashMap<>();
                                                            minScoreMap.put("minScore", minScore.getValue());
                                                            resultList.add(minScoreMap);
                                                        }

                                                        if (scoreProjections.contains("max") && maxScore != null) {
                                                            Map<String, Object> maxScoreMap = new HashMap<>();
                                                            maxScoreMap.put("maxScore", maxScore.getValue());
                                                            resultList.add(maxScoreMap);
                                                        }

                                                        return resultList;
                                                    },
                                                    (oldValue, newValue) -> oldValue,
                                                    LinkedHashMap::new
                                            ));

                                    putObj.source = sourceAggregationsSource;
                                }
                            } else {

                                searchSourceBuilder.aggregation(
                                        AggregationBuilders.terms("group_by_source")
                                                .field("source.keyword"));

                                searchRequest.source(searchSourceBuilder);
                                SearchResponse searchResponse2 = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
                                Terms sourceCount = searchResponse2.getAggregations().get("group_by_source");
                                Map<String, List<Map<String, Object>>> sourceAggregationsSource = sourceCount.getBuckets().stream()
                                        .collect(Collectors.toMap(
                                                Terms.Bucket::getKeyAsString,
                                                sourceBucket -> {
                                                    List<Map<String, Object>> resultList = new ArrayList<>();


                                                    Map<String, Object> postCountMap = new HashMap<>();
                                                    postCountMap.put("No_of_Posts", sourceBucket.getDocCount());
                                                    resultList.add(postCountMap);
                                                    return resultList;

                                                },
                                                (oldValue, newValue) -> oldValue,
                                                LinkedHashMap::new
                                        ));

                                putObj.source = sourceAggregationsSource;
                            }
                        }
                    default:

                }

            }


            searchSourceBuilder.aggregation(
                    AggregationBuilders.min("min_score").field("score")
            );
            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse3 = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
            Min minAgg = searchResponse3.getAggregations().get("min_score");
            if(putObj.no_of_posts==0){
                putObj.minScore = 0;
            }
            else {
                putObj.minScore = (long) minAgg.getValue();
            }

            searchSourceBuilder.aggregation(
                    AggregationBuilders.max("max_score").field("score"));
            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse4 = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
            Max maxAgg = searchResponse4.getAggregations().get("max_score");
            if(putObj.no_of_posts==0) {
                putObj.maxScore =0;
            }
            else{
                putObj.maxScore = (long) maxAgg.getValue();
            }


            searchSourceBuilder.aggregation(
                    AggregationBuilders.sum("total_score").field("score")
            );
            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse5 = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
            Sum sumAgg = searchResponse5.getAggregations().get("total_score");
            putObj.totalScore = (long) sumAgg.getValue();


        }

        // System.out.println(searchRequest.source());
        putObj.searchRequest = searchSourceBuilder.toString();
        return putObj;

    }

}*/