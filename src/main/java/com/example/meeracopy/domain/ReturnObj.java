package com.example.meeracopy.domain;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
public class ReturnObj {
    public long filtcount;
    public Map<String, Map<Object, Long>> countByCate;
    public Map<String, Double> totalPrice;
    public Double minPrice;
    public Double maxPrice;
    public long rangeCount;
}
