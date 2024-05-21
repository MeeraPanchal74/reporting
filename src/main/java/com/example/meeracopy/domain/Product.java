package com.example.meeracopy.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown=true)
@Document(indexName = "products")
public class Product {
    @Id
    private String id;

    @Field(type=FieldType.Text, name = "name")
    private String name;

    @Field(type=FieldType.Text, name = "category")
    private String category;

    @Field(type=FieldType.Double, name = "name")
    private Double price;

    @Field(type=FieldType.Boolean, name = "name")
    private boolean inStock;

    @Field(type = FieldType.Object, name = "entity")
    private Entity entity;

    @Field(type = FieldType.Keyword, name = "tags")
    private List<String> tags;

    @Field(type = FieldType.Date, name = "createdAt")
    private Date createdAt;

}

