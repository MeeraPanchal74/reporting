package com.example.meeracopy.domain;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class ObjectBody {
    public String filter;  //value -> category
    public String aggregations;
}
