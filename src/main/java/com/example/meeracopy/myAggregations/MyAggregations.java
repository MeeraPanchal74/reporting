package com.example.meeracopy.myAggregations;

import com.example.meeracopy.domain.globaldata.Proj;

import java.util.List;
import java.util.Map;

public class MyAggregations {
    public String field;
    public String value;
    public String subaggregation;
    public List<Map<String, List<String>>> projection;
    public Long size;
    public String sort;
}
