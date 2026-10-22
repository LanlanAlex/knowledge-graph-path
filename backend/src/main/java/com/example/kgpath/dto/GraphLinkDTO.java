package com.example.kgpath.dto;

import lombok.Data;

@Data
public class GraphLinkDTO {
    private String source; // 起点ID
    private String target; // 终点ID
    private String label;  // 连线上的文字（如：先修）
}
