package com.example.kgpath.dto;

import lombok.Data;

import java.util.List;

@Data
public class GraphDataDTO {
    private List<GraphNodeDTO> nodes;
    private List<GraphLinkDTO> links;
}
