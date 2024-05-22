package com.example.meeracopy.domain;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class ObjectBody {
    public String filter;  //value -> category
    public List<String> aggregations;
}
