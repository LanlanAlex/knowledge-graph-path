package com.example.kgpath.repository;

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
}