package com.example.kgpath.dto;

import lombok.Data;

@Data
public class RelationDTO {
    private String source;
    private String target;
    private String sourceName;
    private String targetName;
    private String note;
}