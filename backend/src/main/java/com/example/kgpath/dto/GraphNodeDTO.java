package com.example.kgpath.dto;

import lombok.Data;

@Data
public class GraphNodeDTO {
    private String id;
    private String name;
    private String chapter;   // "第1章".."第8章"，用于前端筛选、分类上色
    private String courseId;  // 所属课程（跨学科视图中区分两本书）
    private String courseName; // 所属课程名（跨学科视图图例使用）
    private String type;      // 概念/结构/算法
    private Integer difficulty; // 1~5，前端用星级显示
    private String description; // 节点描述，详情面板展示
    private Integer category;   // 前端 legend 分类下标（对应 8 个章节，0~7）
    private Integer symbolSize; // 节点大小，按难度映射
}
