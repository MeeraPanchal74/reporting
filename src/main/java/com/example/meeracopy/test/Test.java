/*package com.example.meeracopy.test;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHits;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;


public class Test {
    @Mock
    private RestHighLevelClient restClientMock;

    @Test
    public void testCountPostsByFilter() throws IOException {
        // Initialize Mockito annotations
        MockitoAnnotations.initMocks(this);

        // Mock the response from Elasticsearch
        SearchHits searchHitsMock = mock(SearchHits.class);
        when(searchHitsMock.getTotalHits()).thenReturn(10L);

        SearchResponse searchResponseMock = mock(SearchResponse.class);
        when(searchResponseMock.getHits()).thenReturn(searchHitsMock);

        // Mock your service
        YourService yourService = new YourService(restClientMock);

        // Prepare test data
        Filter filter1 = new Filter("source", Arrays.asList("twitter", "reddit"), "CONTAINS");
        Filter filter2 = new Filter("score", Arrays.asList(10, 20), "range");
        Filter filter3 = new Filter("language", Arrays.asList("EN", "CN"), "NIN"); // Not in filter
        List<Filter> filters = Arrays.asList(filter1, filter2, filter3);

        // Mock the Elasticsearch client response
        when(restClientMock.search(any(), any())).thenReturn(searchResponseMock);

        // Call the method to test
        ReturnGlobal putObj = yourService.countPostsByFilter(filters);

        // Verify the method behavior
        assertNotNull(putObj);
        assertEquals(10L, putObj.no_of_posts);
    }
}
}*/
