package com.example.kgpath.dto;

import lombok.Data;

import java.util.List;

@Data
public class GraphDataDTO {
    private List<GraphNodeDTO> nodes;
    private List<GraphLinkDTO> links;
    // 图例分类名（跨学科视图传课程名；按章节上色的普通视图留空，由前端用章节列表）
    private List<String> categories;
}
