package com.example.meeracopy.controller;

import com.example.meeracopy.domain.ObjectBody;
import com.example.meeracopy.domain.Product;
import com.example.meeracopy.domain.ReturnObj;
import com.example.meeracopy.repo.ProductRepo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.elasticsearch.search.aggregations.metrics.SumAggregationBuilder;
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
    public ResponseEntity<String> createOrUpdateDocument( @RequestBody Product product) throws IOException {
        String response = elasticSearchQuery.createOrUpdateDocument(product);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/bulk")
    public ResponseEntity<String> bulk( @RequestBody List<Product> product) throws IOException {
        String response = elasticSearchQuery.bulkSave(product);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @GetMapping("/{productId}")
    public ResponseEntity<Product> getDocumentById( @PathVariable String productId) throws IOException {
        Product product =elasticSearchQuery.findDocById(productId);
        log.info("Product Document has been successfully retrieved.");
        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<String> deleteDocumentById( @PathVariable String productId) throws IOException {
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
        SearchResponse response= getRestClient().search(searchRequest, RequestOptions.DEFAULT);
        long count = response.getHits().getTotalHits().value;
        return count;
    }

    @GetMapping("/searchByFieldInNestedObject")
    public long searchByFieldInNestedObject(@RequestParam String nestedFieldName, @RequestParam String nestedFieldvalue) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query( termsQuery("entity."+nestedFieldName , nestedFieldvalue));
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
        SearchResponse searchResponse =  getRestClient() .search(searchRequest, RequestOptions.DEFAULT);
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
       for(String str:obj.sort){
            if(Objects.equals(str, "minPrice")) {
                SearchRequest searchRequest = new SearchRequest("products");
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(QueryBuilders.matchAllQuery());
                searchSourceBuilder.sort(SortBuilders.fieldSort("price").order(SortOrder.ASC));
                searchSourceBuilder.size(1);
                searchRequest.source(searchSourceBuilder);
                SearchResponse searchResponse = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
                if (searchResponse.getHits().getTotalHits().value > 0) {
                    Map<String, Object> sourceAsMap = searchResponse.getHits().getHits()[0].getSourceAsMap();
                    Object localMinPrice = sourceAsMap.get("price");
                    if (localMinPrice != null && localMinPrice instanceof Double) {
                            object.minPrice = (Double) localMinPrice;
                        }
                        else {
                            object.minPrice = null; // or handle appropriately if no documents found
                        }
                }
            }
           if(Objects.equals(str, "maxPrice")) {
               SearchRequest searchRequest = new SearchRequest("products");
               SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
               searchSourceBuilder.query(QueryBuilders.matchAllQuery());
               searchSourceBuilder.sort(SortBuilders.fieldSort("price").order(SortOrder.DESC));
               searchSourceBuilder.size(1);
               searchRequest.source(searchSourceBuilder);
               SearchResponse searchResponse = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
               if (searchResponse.getHits().getTotalHits().value > 0) {
                   Map<String, Object> sourceAsMap = searchResponse.getHits().getHits()[0].getSourceAsMap();
                   Object localMaxPrice = sourceAsMap.get("price");
                   if (localMaxPrice != null && localMaxPrice instanceof Double) {
                       object.maxPrice = (Double) localMaxPrice;
                   }
                   else {
                       object.maxPrice = null; // or handle appropriately if no documents found
                   }
               }
           }
       }

       if(obj.range.onPrice!= null){
            Double minRange= obj.range.onPrice.get(0);
            Double maxRange = obj.range.onPrice.get(1);
           SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
           searchSourceBuilder.query(QueryBuilders.matchAllQuery());
           searchSourceBuilder.sort(SortBuilders.fieldSort("price").order(SortOrder.ASC));

           // Create a range aggregation to count products within the specified range
           searchSourceBuilder.aggregation(
                   AggregationBuilders.range("price_range")
                           .field("price")
                           .addRange(minRange, maxRange)
           );

           SearchRequest searchRequest = new SearchRequest("products");
           searchRequest.source(searchSourceBuilder);

           SearchResponse searchResponse =  getRestClient().search(searchRequest, RequestOptions.DEFAULT);

           // Get the range aggregation
           Range rangeAgg = searchResponse.getAggregations().get("price_range");

           // Get the count of products within the specified range
           long rangeTotalCount = 0;
           for (Range.Bucket bucket : rangeAgg.getBuckets()) {
               rangeTotalCount += bucket.getDocCount();
           }

          object.rangeCount = rangeTotalCount;
       }

       return object;

    }

    public static RestHighLevelClient getRestClient() {
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")));
    }

}







