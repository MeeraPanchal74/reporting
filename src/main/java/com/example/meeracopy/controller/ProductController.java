package com.example.meeracopy.controller;




import com.example.meeracopy.domain.Product;
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
import org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.elasticsearch.search.aggregations.metrics.SumAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ValueCount;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;



import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        searchSourceBuilder.query(QueryBuilders.matchAllQuery()).size(0);


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
        searchSourceBuilder.query(QueryBuilders.matchAllQuery()).size(0);


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
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("inStock", true));


        searchSourceBuilder.query(boolQueryBuilder);

        SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder);


        SearchResponse response = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
        long count = response.getHits().getTotalHits().value;

        return count;
    }

    @GetMapping("/searchByFieldInList")
    public long searchByFieldInList(@RequestParam String fieldName, @RequestParam String value) throws IOException {
        // Build the search source
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // Construct the terms query to search for the value in the specified field inside the list
        searchSourceBuilder.query(QueryBuilders.termsQuery(fieldName, value));

        // Create the search request
        SearchRequest searchRequest = new SearchRequest("products"); // Replace "index_name" with your Elasticsearch index name
        searchRequest.source(searchSourceBuilder);

        // Execute the search request and return the response

        SearchResponse response= getRestClient().search(searchRequest, RequestOptions.DEFAULT);
        long count = response.getHits().getTotalHits().value;
        return count;
    }

    public static RestHighLevelClient getRestClient() {
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")));
    }

}







