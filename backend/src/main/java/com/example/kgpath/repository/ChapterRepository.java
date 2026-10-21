package com.example.kgpath.repository;

import com.example.kgpath.dto.ChapterDTO;
import com.example.kgpath.entity.Chapter;
import com.example.kgpath.entity.KnowledgePoint;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChapterRepository extends Neo4jRepository<Chapter, String> {

    // 查询某个章节下的所有知识点（用于左侧目录树点击加载）
    // 注意：查询返回的是 KnowledgePoint 节点，返回类型必须用 List<KnowledgePoint>
    @Query("MATCH (ch:Chapter {id: $chapterId})-[:CONTAINS]->(k:KnowledgePoint) RETURN k ORDER BY k.id")
    List<KnowledgePoint> findKnowledgePointsByChapterId(@Param("chapterId") String chapterId);

    // 按课程查章节列表（多教材支持的核心方法）
    // 返回 value=KP.chapter 短名（前端分类映射用）+ label=完整章节名（菜单显示用），按章节 order 排序
    @Query("MATCH (c:Course {id: $courseId})-[:HAS_CHAPTER]->(ch:Chapter)-[:CONTAINS]->(k:KnowledgePoint) " +
            "RETURN DISTINCT k.chapter AS value, min(ch.name) AS label, min(ch.order) AS ord ORDER BY ord")
    List<ChapterDTO> findChaptersByCourseId(@Param("courseId") String courseId);
}