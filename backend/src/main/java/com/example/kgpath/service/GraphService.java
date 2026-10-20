package com.example.kgpath.service;

import com.example.kgpath.dto.GraphDataDTO;
import com.example.kgpath.dto.GraphLinkDTO;
import com.example.kgpath.dto.GraphNodeDTO;
import com.example.kgpath.dto.RelationDTO;
import com.example.kgpath.entity.KnowledgePoint;
import com.example.kgpath.repository.KnowledgePointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GraphService {

    @Autowired
    private KnowledgePointRepository kpRepository;

    /**
     * 获取全局知识图谱（用于首页展示）
     */
    public GraphDataDTO getGlobalGraph() {
        GraphDataDTO graphData = new GraphDataDTO();
        List<GraphNodeDTO> nodes = new ArrayList<>();
        List<GraphLinkDTO> links = new ArrayList<>();

        // 1. 查出所有知识点，转换成 NodeDTO
        // category 按章节索引 0~7 赋值，用于前端按章节上色和图例展示（用户需求）
        List<String> chapterOrder = List.of("第1章", "第2章", "第3章", "第4章", "第5章", "第6章", "第7章", "第8章");
        List<KnowledgePoint> allKps = kpRepository.findAll();
        for (KnowledgePoint kp : allKps) {
            GraphNodeDTO node = new GraphNodeDTO();
            node.setId(kp.getId());
            node.setName(kp.getName());
            node.setChapter(kp.getChapter());
            node.setType(kp.getType());
            node.setDifficulty(kp.getDifficulty());
            node.setDescription(kp.getDescription());
            // 根据章节索引赋予 category（用于前端按章节上色）
            int categoryIdx = chapterOrder.indexOf(kp.getChapter());
            node.setCategory(categoryIdx >= 0 ? categoryIdx : 0);

            // 节点大小根据难度稍微放大一点（难度1为20，难度5为40）
            node.setSymbolSize(20 + kp.getDifficulty() * 4);
            nodes.add(node);
        }

        // 2. 查出所有先修关系，转换成 LinkDTO
        List<RelationDTO> relations = kpRepository.findAllPrerequisiteRelationsForGraph();
        for (RelationDTO rel : relations) {
            GraphLinkDTO link = new GraphLinkDTO();
            link.setSource(rel.getSource());
            link.setTarget(rel.getTarget());
            link.setLabel("先修");
            links.add(link);
        }

        // 3. 查出所有 RELATED_TO 关系（相关知识点），前端按 label 用不同样式区分
        List<RelationDTO> related = kpRepository.findAllRelatedRelationsForGraph();
        for (RelationDTO rel : related) {
            GraphLinkDTO link = new GraphLinkDTO();
            link.setSource(rel.getSource());
            link.setTarget(rel.getTarget());
            link.setLabel("相关");
            links.add(link);
        }

        graphData.setNodes(nodes);
        graphData.setLinks(links);
        return graphData;
    }
}