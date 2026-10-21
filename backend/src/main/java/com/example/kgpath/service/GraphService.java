package com.example.kgpath.service;

import com.example.kgpath.dto.ChapterDTO;
import com.example.kgpath.dto.GraphDataDTO;
import com.example.kgpath.dto.GraphLinkDTO;
import com.example.kgpath.dto.GraphNodeDTO;
import com.example.kgpath.dto.KpCourseDTO;
import com.example.kgpath.dto.RelationDTO;
import com.example.kgpath.entity.KnowledgePoint;
import com.example.kgpath.repository.ChapterRepository;
import com.example.kgpath.repository.KnowledgePointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GraphService {

    @Autowired
    private KnowledgePointRepository kpRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    /**
     * 获取某门课程的全局知识图谱（多教材隔离：节点、边都只限该课程链路内）
     */
    public GraphDataDTO getGlobalGraph(String courseId) {
        GraphDataDTO graphData = new GraphDataDTO();
        List<GraphNodeDTO> nodes = new ArrayList<>();
        List<GraphLinkDTO> links = new ArrayList<>();

        // 1. 先取该课程的章节顺序，作为 category 的映射基准（不同教材章节数不同）
        List<String> chapterOrder = getChapterOrder(courseId);

        // 2. 只查该课程下的知识点（防止两本书的 KP 混在一张图）
        List<KnowledgePoint> allKps = kpRepository.findAllByCourseId(courseId);
        List<String> ids = new ArrayList<>();
        for (KnowledgePoint kp : allKps) {
            GraphNodeDTO node = new GraphNodeDTO();
            node.setId(kp.getId());
            node.setName(kp.getName());
            node.setChapter(kp.getChapter());
            node.setType(kp.getType());
            node.setDifficulty(kp.getDifficulty());
            node.setDescription(kp.getDescription());
            int categoryIdx = chapterOrder.indexOf(kp.getChapter());
            node.setCategory(categoryIdx >= 0 ? categoryIdx : 0);
            // 节点大小根据难度稍微放大一点（难度1为20，难度5为40）
            node.setSymbolSize(20 + kp.getDifficulty() * 4);
            nodes.add(node);
            ids.add(kp.getId());
        }

        // 3. 边也限制在该课程节点集合内部（PREREQUISITE_OF + RELATED_TO）
        if (!ids.isEmpty()) {
            for (RelationDTO rel : kpRepository.findPrerequisiteRelationsAmong(ids)) {
                GraphLinkDTO link = new GraphLinkDTO();
                link.setSource(rel.getSource());
                link.setTarget(rel.getTarget());
                link.setLabel("先修");
                links.add(link);
            }
            for (RelationDTO rel : kpRepository.findRelatedRelationsAmong(ids)) {
                GraphLinkDTO link = new GraphLinkDTO();
                link.setSource(rel.getSource());
                link.setTarget(rel.getTarget());
                link.setLabel("相关");
                links.add(link);
            }
        }

        graphData.setNodes(nodes);
        graphData.setLinks(links);
        return graphData;
    }

    /**
     * 某门课程的章节列表（前端左侧目录用）
     */
    public List<ChapterDTO> getChapters(String courseId) {
        return chapterRepository.findChaptersByCourseId(courseId);
    }

    /**
     * 跨学科关联视图：只保留两端分属不同课程的边，以及这些边的端点节点
     * 用于展示两本教材之间的知识迁移关系（如 循环队列 -> ARQ协议）
     */
    public GraphDataDTO getCrossCourseGraph() {
        List<RelationDTO> prereqEdges = kpRepository.findCrossCoursePrerequisiteRelations();
        List<RelationDTO> relatedEdges = kpRepository.findCrossCourseRelatedRelations();

        GraphDataDTO graphData = new GraphDataDTO();
        List<GraphNodeDTO> nodes = new ArrayList<>();
        List<GraphLinkDTO> links = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        graphData.setNodes(nodes);
        graphData.setLinks(links);
        graphData.setCategories(categories);
        if (prereqEdges.isEmpty() && relatedEdges.isEmpty()) {
            return graphData;
        }

        // 1. 收集所有端点 ID
        Set<String> ids = new LinkedHashSet<>();
        for (RelationDTO e : prereqEdges) {
            ids.add(e.getSource());
            ids.add(e.getTarget());
        }
        for (RelationDTO e : relatedEdges) {
            ids.add(e.getSource());
            ids.add(e.getTarget());
        }

        // 2. 批量取节点属性 + 课程归属
        Map<String, KnowledgePoint> kpMap = new HashMap<>();
        for (KnowledgePoint kp : kpRepository.findAllById(ids)) {
            kpMap.put(kp.getId(), kp);
        }
        Map<String, String[]> courseMap = new HashMap<>(); // kpId -> [courseId, courseName]
        Set<String> courseIds = new LinkedHashSet<>();
        for (KpCourseDTO row : kpRepository.findCourseMemberships(ids)) {
            courseMap.putIfAbsent(row.getKpId(), new String[]{row.getCourseId(), row.getCourseName()});
            courseIds.add(row.getCourseId());
        }

        // 3. category 用课程下标（跨学科视图的图例是课程名，不是章节名）
        List<String> sortedCourseIds = new ArrayList<>(courseIds);
        sortedCourseIds.sort(null);
        Map<String, Integer> courseIndex = new HashMap<>();
        for (String cid : sortedCourseIds) {
            courseIndex.put(cid, courseIndex.size());
        }
        Map<String, String> courseNameById = new HashMap<>();
        for (String[] v : courseMap.values()) {
            courseNameById.put(v[0], v[1]);
        }
        for (String cid : sortedCourseIds) {
            categories.add(courseNameById.getOrDefault(cid, cid));
        }

        // 4. 组装节点
        for (String id : ids) {
            KnowledgePoint kp = kpMap.get(id);
            if (kp == null) continue;
            GraphNodeDTO node = new GraphNodeDTO();
            node.setId(kp.getId());
            node.setName(kp.getName());
            node.setChapter(kp.getChapter());
            node.setType(kp.getType());
            node.setDifficulty(kp.getDifficulty());
            node.setDescription(kp.getDescription());
            String[] membership = courseMap.get(id);
            if (membership != null) {
                node.setCourseId(membership[0]);
                node.setCourseName(membership[1]);
                node.setCategory(courseIndex.getOrDefault(membership[0], 0));
            } else {
                node.setCategory(0);
            }
            node.setSymbolSize(20 + kp.getDifficulty() * 4);
            nodes.add(node);
        }

        // 5. 组装边
        for (RelationDTO e : prereqEdges) {
            links.add(toLink(e, "跨课先修"));
        }
        for (RelationDTO e : relatedEdges) {
            links.add(toLink(e, "跨课相关"));
        }
        return graphData;
    }

    private GraphLinkDTO toLink(RelationDTO e, String label) {
        GraphLinkDTO link = new GraphLinkDTO();
        link.setSource(e.getSource());
        link.setTarget(e.getTarget());
        link.setLabel(label);
        return link;
    }

    private List<String> getChapterOrder(String courseId) {
        return chapterRepository.findChaptersByCourseId(courseId).stream()
                .map(ChapterDTO::getValue)
                .toList();
    }
}
