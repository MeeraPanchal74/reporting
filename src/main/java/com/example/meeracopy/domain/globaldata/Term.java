package com.example.meeracopy.domain.globaldata;

import com.example.meeracopy.filters.Filter;
import com.example.meeracopy.myAggregations.MyAggregations;

import java.util.List;

public class Term {
    public String value;
    public List<Filter> filters;
    public List <MyAggregations> Aggregations;


   // public SortClass sort;
}
