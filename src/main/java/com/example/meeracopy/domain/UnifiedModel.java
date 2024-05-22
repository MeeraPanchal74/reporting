package com.example.meeracopy.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "globaldata1")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnifiedModel {
    @Id
    @Field(type = FieldType.Text)
    private String id;

    @Field(type = FieldType.Long)
    private long createdAt;

    @Field(type = FieldType.Long)
    private long postingTime;

    @Field(type = FieldType.Text)
    private String languageCode;

    @Field(type = FieldType.Text)
    private String text;

    @Field(type = FieldType.Text)
    private String source;

    @Field(type = FieldType.Boolean)
    private boolean isComment;

    @Field(type = FieldType.Text)
    private String parentId;

    @Field(type = FieldType.Long)
    private long score;

    @Field(type = FieldType.Text)
    private String urlOfPost;

    @Field(type = FieldType.Text)
    private String titleOfpPost;

    @Field(type = FieldType.Object)
    private User user;

    private List<String> mediaUrls;
}
