package com.example.kgpath.service;

import com.example.kgpath.dto.GraphDataDTO;
import com.example.kgpath.dto.GraphLinkDTO;
import com.example.kgpath.dto.GraphNodeDTO;
import com.example.kgpath.dto.RelationDTO;
import com.example.kgpath.entity.KnowledgePoint;
import com.example.kgpath.repository.KnowledgePointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RecommendService {

    @Autowired
    private KnowledgePointRepository kpRepository;

    /**
     * 核心功能：生成学习路径
     * 输入：目标知识点ID（比如 "KP-08-06" 快速排序）
     * 输出：一条连贯的先修子图（从最基础的知识点一直指到目标）
     */
    public GraphDataDTO generateLearningPath(String targetId) {
        KnowledgePoint targetNode = kpRepository.findById(targetId).orElse(null);
        if (targetNode == null) {
            // 查无此点返回 404，而不是 200 空响应体，前端才能区分"查无此点"和"无路径"
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "知识点不存在: " + targetId);
        }

        // 1. 查询目标节点的所有前置依赖（向上溯源，最大深度5），并把目标自身加入集合
        List<KnowledgePoint> allPrereqs = kpRepository.findAllPrerequisitesUpToDepth(targetId);
        allPrereqs.add(targetNode);

        // 2. 排序策略：按章节、难度排序，让前端画出来的路径有层次感
        // 章节是 "第1章".."第8章"（单数字），字符串排序即自然章节顺序
        allPrereqs.sort(Comparator.comparing(KnowledgePoint::getChapter)
                .thenComparing(KnowledgePoint::getDifficulty));

        // 3. 组装节点（category 语义与全图保持一致：0-概念, 1-结构, 2-算法；
        //    难度只体现在 symbolSize 上，避免和颜色分类冲突）
        GraphDataDTO graphData = new GraphDataDTO();
        List<GraphNodeDTO> nodes = new ArrayList<>();
        List<String> chapterOrder = List.of("第1章", "第2章", "第3章", "第4章", "第5章", "第6章", "第7章", "第8章");
        Set<String> addedNodeIds = new HashSet<>();
        for (KnowledgePoint kp : allPrereqs) {
            if (addedNodeIds.add(kp.getId())) {
                GraphNodeDTO node = new GraphNodeDTO();
                node.setId(kp.getId());
                node.setName(kp.getName());
                node.setChapter(kp.getChapter());
                node.setType(kp.getType());
                node.setDifficulty(kp.getDifficulty());
                node.setDescription(kp.getDescription());
                int categoryIdx = chapterOrder.indexOf(kp.getChapter());
                node.setCategory(categoryIdx >= 0 ? categoryIdx : 0);
                node.setSymbolSize(20 + kp.getDifficulty() * 5);
                nodes.add(node);
            }
        }

        // 4. 组装连线：一条 Cypher 查出集合内部的所有先修边，避免逐点 N+1 查询
        List<GraphLinkDTO> links = new ArrayList<>();
        for (RelationDTO rel : kpRepository.findPrerequisiteRelationsAmong(addedNodeIds)) {
            GraphLinkDTO link = new GraphLinkDTO();
            link.setSource(rel.getSource());
            link.setTarget(rel.getTarget());
            link.setLabel("先修");
            links.add(link);
        }

        graphData.setNodes(nodes);
        graphData.setLinks(links);
        return graphData;
    }
}
