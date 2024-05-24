package com.example.meeracopy.controller;

import com.example.meeracopy.domain.*;
import com.example.meeracopy.domain.globaldata.GlobalBody;
import com.example.meeracopy.domain.globaldata.ReturnGlobal;
import com.example.meeracopy.filters.Filter;
import com.example.meeracopy.myAggregations.MyAggregations;
import com.example.meeracopy.repo.ProductRepo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
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
import org.elasticsearch.search.aggregations.bucket.range.Range;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.*;

@RestController
@Slf4j
@RequestMapping("api/products")
public class ProductController {


    @Autowired
    private ProductRepo elasticSearchQuery;

    @PostMapping("/insert")
    public ResponseEntity<String> createOrUpdateDocument(@RequestBody Product product) throws IOException {
        String response = elasticSearchQuery.createOrUpdateDocument(product);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/bulk")
    public ResponseEntity<String> bulk(@RequestBody List<Product> product) throws IOException {
        String response = elasticSearchQuery.bulkSave(product);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/bulkGlobal")
    public ResponseEntity<String> bulkGlobal(@RequestBody List<UnifiedModel> unifiedModels) throws IOException {
        String response = elasticSearchQuery.bulkSaveGlobal(unifiedModels);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getDocumentById(@PathVariable String productId) throws IOException {
        Product product = elasticSearchQuery.findDocById(productId);
        log.info("Product Document has been successfully retrieved.");
        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<String> deleteDocumentById(@PathVariable String productId) throws IOException {
        String message = elasticSearchQuery.deleteDocById(productId);
        log.info("Product Document has been successfully deleted. Message: {}", message);
        return new ResponseEntity<>(message, HttpStatus.NO_CONTENT);
    }

    @GetMapping("/findAll")
    public ResponseEntity<List<Product>> findAll() throws IOException {
        List<Product> products = elasticSearchQuery.findAll();
        log.info("No of Product Documents has been successfully retrieved: {}", products.size());
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/findAllGlobal")
    public ResponseEntity<List<UnifiedModel>> findAllGlobal() throws IOException {
        List<UnifiedModel> unifiedModels = elasticSearchQuery.findAllGlobal();
        log.info("No of Product Documents has been successfully retrieved: {}", unifiedModels.size());
        return new ResponseEntity<>(unifiedModels, HttpStatus.OK);
    }

    @GetMapping("/productsCountByCategory")
    public Map<String, Map<Object, Long>> getProductsCountByCategory() throws IOException {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(matchAllQuery()).size(0);
        TermsAggregationBuilder groupByCategory = AggregationBuilders.terms("group_by_category").field("category.keyword").size(2);
        searchSourceBuilder.aggregation(groupByCategory);
        SearchRequest searchRequest = new SearchRequest("products");
        searchRequest.source(searchSourceBuilder);
        SearchResponse response = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
        Aggregations aggregation = response.getAggregations();
        Map<Object, Long> countsPerCategory = ((ParsedStringTerms) aggregation.get("group_by_category")).getBuckets().stream()
                .collect(Collectors.toMap(Terms.Bucket::getKey, Terms.Bucket::getDocCount));

        return Collections.singletonMap("counts_by_category", countsPerCategory);
    }

    @GetMapping("/totalPrice")
    public Map<String, Double> getTotalMarketCapacity() throws IOException {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(matchAllQuery()).size(0);
        SumAggregationBuilder sumAggregation = AggregationBuilders.sum("total_price").field("price");
        searchSourceBuilder.aggregation(sumAggregation);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);
        SearchResponse response = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
        Aggregations aggregation = response.getAggregations();
        double totalMarketCapacity = ((ParsedSum) aggregation.get("total_price")).getValue();
        return Collections.singletonMap("total_price", totalMarketCapacity);
    }

    @GetMapping("/countInStock") // for isSensitive field
    public long countSensitiveDocuments() throws IOException {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = boolQuery()
                .must(termQuery("inStock", true));
        searchSourceBuilder.query(boolQueryBuilder);
        SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder);
        SearchResponse response = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
        long count = response.getHits().getTotalHits().value;

        return count;
    }

    @GetMapping("/searchByFieldInList")     //GET /searchByFieldInList?fieldName=tags&value=weapon
    public long searchByFieldInList(@RequestParam String fieldName, @RequestParam String value) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(termsQuery(fieldName, value));
        SearchRequest searchRequest = new SearchRequest("products");
        searchRequest.source(searchSourceBuilder);
        SearchResponse response = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
        long count = response.getHits().getTotalHits().value;
        return count;
    }

    @GetMapping("/searchByFieldInNestedObject")
    public long searchByFieldInNestedObject(@RequestParam String nestedFieldName, @RequestParam String nestedFieldvalue) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(termsQuery("entity." + nestedFieldName, nestedFieldvalue));
        SearchRequest searchRequest = new SearchRequest("products"); // Replace "index_name" with your Elasticsearch index name
        searchRequest.source(searchSourceBuilder);
        SearchResponse response = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
        return response.getHits().getTotalHits().value;
    }

    @GetMapping("/countProductsByCategory")
    public long countProductsByCategory(@RequestParam String term) throws IOException {
        SearchRequest searchRequest = new SearchRequest("products");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(matchQuery("category", term));
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
        return searchResponse.getHits().getTotalHits().value;
    }


    @PostMapping("/generalQuery")
    public ReturnObj generalQuery(@RequestBody ObjectBody obj) throws IOException {
        String filt = obj.filter;
        List<String> aggr = obj.aggregations;

        long count1 = 0;
        ReturnObj object = new ReturnObj();
        System.out.println(filt);

        if (Objects.equals(filt, "inStock")) {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQueryBuilder = boolQuery()
                    .must(termQuery("inStock", true));
            searchSourceBuilder.query(boolQueryBuilder);
            SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder);
            SearchResponse response = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
            count1 = response.getHits().getTotalHits().value;

        }

        object.filtcount = count1;


        for (String str : aggr) {
            if (Objects.equals(str, "byCategory")) {

                SearchSourceBuilder searchSourceBuilder2 = new SearchSourceBuilder();
                searchSourceBuilder2.query(matchAllQuery()).size(0);
                TermsAggregationBuilder groupByCategory = AggregationBuilders.terms("group_by_category").field("category.keyword").size(2);
                searchSourceBuilder2.aggregation(groupByCategory);
                SearchRequest searchRequest = new SearchRequest("products");
                searchRequest.source(searchSourceBuilder2);
                SearchResponse response2 = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
                Aggregations aggregation = response2.getAggregations();
                Map<Object, Long> countsPerCategory = ((ParsedStringTerms) aggregation.get("group_by_category")).getBuckets().stream()
                        .collect(Collectors.toMap(Terms.Bucket::getKey, Terms.Bucket::getDocCount));
                object.countByCate = Collections.singletonMap("counts_by_category", countsPerCategory);

            }

            if (Objects.equals(str, "totalPrice")) {
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(matchAllQuery()).size(0);
                SumAggregationBuilder sumAggregation = AggregationBuilders.sum("total_price").field("price");
                searchSourceBuilder.aggregation(sumAggregation);
                SearchRequest searchRequest = new SearchRequest();
                searchRequest.source(searchSourceBuilder);
                SearchResponse response = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
                Aggregations aggregation = response.getAggregations();
                double totalMarketCapacity = ((ParsedSum) aggregation.get("total_price")).getValue();
                object.totalPrice = Collections.singletonMap("total_price", totalMarketCapacity);
            }
        }
        for (String str : obj.sort) {
            if (Objects.equals(str, "minPrice")) {
                SearchRequest searchRequest = new SearchRequest("products");
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(matchAllQuery());
                searchSourceBuilder.sort(SortBuilders.fieldSort("price").order(SortOrder.ASC));
                searchSourceBuilder.size(1);
                searchRequest.source(searchSourceBuilder);
                SearchResponse searchResponse = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
                if (searchResponse.getHits().getTotalHits().value > 0) {
                    Map<String, Object> sourceAsMap = searchResponse.getHits().getHits()[0].getSourceAsMap();
                    Object localMinPrice = sourceAsMap.get("price");
                    if (localMinPrice != null && localMinPrice instanceof Double) {
                        object.minPrice = (Double) localMinPrice;
                    } else {
                        object.minPrice = null; // or handle appropriately if no documents found
                    }
                }
            }
            if (Objects.equals(str, "maxPrice")) {
                SearchRequest searchRequest = new SearchRequest("products");
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(matchAllQuery());
                searchSourceBuilder.sort(SortBuilders.fieldSort("price").order(SortOrder.DESC));
                searchSourceBuilder.size(1);
                searchRequest.source(searchSourceBuilder);
                SearchResponse searchResponse = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
                if (searchResponse.getHits().getTotalHits().value > 0) {
                    Map<String, Object> sourceAsMap = searchResponse.getHits().getHits()[0].getSourceAsMap();
                    Object localMaxPrice = sourceAsMap.get("price");
                    if (localMaxPrice != null && localMaxPrice instanceof Double) {
                        object.maxPrice = (Double) localMaxPrice;
                    } else {
                        object.maxPrice = null; // or handle appropriately if no documents found
                    }
                }
            }
        }

        if (obj.range.onPrice != null) {
            Double minRange = obj.range.onPrice.get(0);
            Double maxRange = obj.range.onPrice.get(1);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(matchAllQuery());
            searchSourceBuilder.sort(SortBuilders.fieldSort("price").order(SortOrder.ASC));
            searchSourceBuilder.aggregation(
                    AggregationBuilders.range("price_range")
                            .field("price")
                            .addRange(minRange, maxRange)
            );

            SearchRequest searchRequest = new SearchRequest("products");
            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
            Range rangeAgg = searchResponse.getAggregations().get("price_range");
            long rangeTotalCount = 0;
            for (Range.Bucket bucket : rangeAgg.getBuckets()) {
                rangeTotalCount += bucket.getDocCount();
            }

            object.rangeCount = rangeTotalCount;
        }

        return object;

    }

    @PostMapping("/generalQuery2")
    public ReturnObj2 generalQuery2(@RequestBody ObjectBody2 userObj) throws IOException {
        ReturnObj2 newInstance = new ReturnObj2();


        if (userObj.groupBy.category != null) {
            String value = userObj.groupBy.category.value;
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            SearchRequest searchRequest = new SearchRequest("products");

            searchSourceBuilder.query(termQuery("category.keyword", value));


            if (userObj.groupBy.category.filters != null) {

                for (String str : userObj.groupBy.category.filters) {
                    if (Objects.equals(str, "minValue")) {
                        searchSourceBuilder.aggregation(
                                AggregationBuilders.min("min_price").field("price")
                        );
                        searchRequest.source(searchSourceBuilder);
                        SearchResponse searchResponse = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
                        Min minAgg = searchResponse.getAggregations().get("min_price");
                        newInstance.minValue = minAgg.getValue();
                    } else if (Objects.equals(str, "maxValue")) {
                        searchSourceBuilder.aggregation(
                                AggregationBuilders.max("max_price").field("price")
                        );
                        searchRequest.source(searchSourceBuilder);
                        SearchResponse searchResponse = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
                        Max maxAgg = searchResponse.getAggregations().get("max_price");
                        newInstance.maxValue = maxAgg.getValue();
                    } else if (Objects.equals(str, "totalValue")) {
                        searchSourceBuilder.aggregation(
                                AggregationBuilders.sum("total_price").field("price")
                        );
                        searchRequest.source(searchSourceBuilder);
                        SearchResponse searchResponse = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
                        Sum sumAgg = searchResponse.getAggregations().get("total_price");
                        newInstance.totalValue = sumAgg.getValue();
                    }

                }
            }
            newInstance.value = userObj.groupBy.category.value;
        }

        return newInstance;
    }


    public static RestHighLevelClient getRestClient() {
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")));
    }


    @PostMapping("/generalGlobalQuery")
    public ReturnGlobal generalGlobalQuery(@RequestBody GlobalBody getObj) throws IOException, ParseException {
        ReturnGlobal putObj = new ReturnGlobal();

        List<Filter> filters = getObj.term.filters;
        List<MyAggregations> myAggregations = getObj.term.myAggregations;

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

                    //String myVal = filter.value.toLowerCase();
                    case "CONTAINS":

                        boolQuery.filter(termsQuery(filter.field,lowerCaseValues));
                        break;

                    case "range":
                        System.out.println(lowerCaseValues.get(0));
                        boolQuery.filter(rangeQuery(filter.field).gte(lowerCaseValues.get(0)).lte(lowerCaseValues.get(1)));
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
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(boolQuery);


        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse1 = getRestClient().search(searchRequest, RequestOptions.DEFAULT);

        putObj.no_of_posts = searchResponse1.getHits().getTotalHits().value;



        if(myAggregations!=null) {
            for (MyAggregations term : myAggregations) {

                switch (term.field) {
                    case "language":
                        if (!Objects.equals(term.subaggregation, "none")) {
                            searchSourceBuilder.aggregation(
                                    AggregationBuilders.terms("group_by_language").field("languageCode.keyword").subAggregation(AggregationBuilders.terms("group_by_source").field("source.keyword"))
                            );
                        } else {
                            searchSourceBuilder.aggregation(
                                    AggregationBuilders.terms("group_by_language").field("languageCode.keyword"));
                        }

                        searchRequest.source(searchSourceBuilder);

                        SearchResponse searchResponse = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
                        // System.out.println(searchResponse);
                        Terms languageCount = searchResponse.getAggregations().get("group_by_language");

                        if (!Objects.equals("subaggregation", "none")) {
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
                        } else {

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
                            for(Map<String, List<String>> myProj: term.projection){
                        System.out.println(myProj.get("score"));
                        if (myProj.containsKey("score")) {
                            if (myProj.get("score") != null) {

                                List<String> scoreProjections = new ArrayList<String>();

                                for (String myStr : myProj.get("score")) {
                                    scoreProjections.add(myStr);

                                }


                                searchSourceBuilder.aggregation(
                                        AggregationBuilders.terms("group_by_source")
                                                .field("source.keyword")
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

        System.out.println(searchRequest.source());
        putObj.searchRequest = searchRequest.source().toString();
        return putObj;
    }

}




