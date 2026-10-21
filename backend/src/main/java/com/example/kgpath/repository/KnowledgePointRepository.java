package com.example.kgpath.repository;

import com.example.kgpath.entity.KnowledgePoint;
import com.example.kgpath.dto.KpCourseDTO;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.kgpath.dto.RelationDTO;

import java.util.Collection;
import java.util.List;

@Repository
public interface KnowledgePointRepository extends Neo4jRepository<KnowledgePoint, String> {

    // ==========================================
    // 1. 基础查询
    // ==========================================

    // 根据名称模糊搜索知识点（用于前端搜索框）
    @Query("MATCH (k:KnowledgePoint) WHERE k.name CONTAINS $name RETURN k")
    List<KnowledgePoint> findByNameContaining(@Param("name") String name);

    // 根据章节名称查询该章节下的所有知识点
    @Query("MATCH (k:KnowledgePoint {chapter: $chapter}) RETURN k")
    List<KnowledgePoint> findByChapter(@Param("chapter") String chapter);


    // ==========================================
    // 2. 先修关系查询 (用于生成局部图谱)
    // ==========================================

    // 查询某个知识点的【直接前置节点】（学它之前需要先掌握哪些知识点）
    @Query("MATCH (k:KnowledgePoint {id: $id})<-[:PREREQUISITE_OF]-(prev) RETURN prev")
    List<KnowledgePoint> findDirectPrevNodes(@Param("id") String id);

    // 查询某个知识点的【直接后继节点】（学完它能学什么）
    @Query("MATCH (k:KnowledgePoint {id: $id})-[:PREREQUISITE_OF]->(next) RETURN next")
    List<KnowledgePoint> findDirectNextNodes(@Param("id") String id);


    // ==========================================
    // 3. 路径推荐核心查询 (多跳查询)
    // ==========================================

    // 【核心亮点】查出目标知识点的所有前置知识链（向上溯源，最大深度5）
    // 比如查"动态规划"，会把它依赖的"递归"、"数组"等全查出来
    @Query("MATCH path = (target:KnowledgePoint {id: $targetId})<-[:PREREQUISITE_OF*1..5]-(prev:KnowledgePoint) " +
            "RETURN DISTINCT prev")
    List<KnowledgePoint> findAllPrerequisitesUpToDepth(@Param("targetId") String targetId);

    // 查出两个知识点之间的所有先修路径（比如 A 到 B 有多条路，用于对比推荐）
    // 返回 List<KnowledgePoint> 其实只能拿到节点，如果想拿完整路径需要特殊处理，这步留到 Service 层做，这里先查出所有关联节点
    @Query("MATCH path = (start:KnowledgePoint {id: $startId})-[:PREREQUISITE_OF*1..6]->(end:KnowledgePoint {id: $endId}) " +
            "RETURN DISTINCT nodes(path) as pathNodes")
    List<List<KnowledgePoint>> findPathsBetween(@Param("startId") String startId, @Param("endId") String endId);

    // 记得在头部 import com.example.kgpath.dto.RelationDTO;

    @Query("MATCH (a:KnowledgePoint)-[r:PREREQUISITE_OF]->(b:KnowledgePoint) " +
            "RETURN a.id as source, a.name as sourceName, b.id as target, b.name as targetName, r.note as note")
    List<RelationDTO> findAllPrerequisiteRelationsForGraph();

    // 返回所有的 RELATED_TO 关系数据（12 条），配合上面的先修边一起交给前端画全图
    @Query("MATCH (a:KnowledgePoint)-[r:RELATED_TO]->(b:KnowledgePoint) " +
            "RETURN a.id as source, a.name as sourceName, b.id as target, b.name as targetName, r.note as note")
    List<RelationDTO> findAllRelatedRelationsForGraph();

    // 只返回给定 ID 集合内部存在的 PREREQUISITE_OF 边（推荐路径局部子图专用，一条查询替代 N+1）
    @Query("MATCH (a:KnowledgePoint)-[r:PREREQUISITE_OF]->(b:KnowledgePoint) " +
            "WHERE a.id IN $ids AND b.id IN $ids " +
            "RETURN a.id as source, a.name as sourceName, b.id as target, b.name as targetName, r.note as note")
    List<RelationDTO> findPrerequisiteRelationsAmong(@Param("ids") Collection<String> ids);

    // ==========================================
    // 4. 多教材支持（按课程隔离）
    // ==========================================

    // 查某门课程下的所有知识点（只走该课程 Course->Chapter->KP 链路，防止两本书数据混淆）
    @Query("MATCH (c:Course {id: $courseId})-[:HAS_CHAPTER]->(:Chapter)-[:CONTAINS]->(k:KnowledgePoint) RETURN k")
    List<KnowledgePoint> findAllByCourseId(@Param("courseId") String courseId);

    // 反查知识点所属课程 ID（推荐路径用它动态确定章节顺序，替代硬编码）
    @Query("MATCH (c:Course)-[:HAS_CHAPTER]->(:Chapter)-[:CONTAINS]->(k:KnowledgePoint {id: $kpId}) RETURN c.id LIMIT 1")
    String findCourseIdByKnowledgePointId(@Param("kpId") String kpId);

    // 集合内部的 RELATED_TO 边（课程隔离版）
    @Query("MATCH (a:KnowledgePoint)-[r:RELATED_TO]->(b:KnowledgePoint) " +
            "WHERE a.id IN $ids AND b.id IN $ids " +
            "RETURN a.id as source, a.name as sourceName, b.id as target, b.name as targetName, r.note as note")
    List<RelationDTO> findRelatedRelationsAmong(@Param("ids") Collection<String> ids);

    // ==========================================
    // 5. 跨学科关联视图（两门课程之间的边）
    // ==========================================

    // 跨课程先修边：两端知识点分属不同课程
    @Query("MATCH (a:KnowledgePoint)-[r:PREREQUISITE_OF]->(b:KnowledgePoint) " +
            "MATCH (ca:Course)-[:HAS_CHAPTER]->(:Chapter)-[:CONTAINS]->(a) " +
            "MATCH (cb:Course)-[:HAS_CHAPTER]->(:Chapter)-[:CONTAINS]->(b) " +
            "WHERE ca.id <> cb.id " +
            "RETURN DISTINCT a.id as source, a.name as sourceName, b.id as target, b.name as targetName, r.note as note")
    List<RelationDTO> findCrossCoursePrerequisiteRelations();

    // 跨课程相关边：两端知识点分属不同课程
    @Query("MATCH (a:KnowledgePoint)-[r:RELATED_TO]->(b:KnowledgePoint) " +
            "MATCH (ca:Course)-[:HAS_CHAPTER]->(:Chapter)-[:CONTAINS]->(a) " +
            "MATCH (cb:Course)-[:HAS_CHAPTER]->(:Chapter)-[:CONTAINS]->(b) " +
            "WHERE ca.id <> cb.id " +
            "RETURN DISTINCT a.id as source, a.name as sourceName, b.id as target, b.name as targetName, r.note as note")
    List<RelationDTO> findCrossCourseRelatedRelations();

    // 批量查知识点所属课程（跨学科视图给节点标注课程归属）
    @Query("MATCH (c:Course)-[:HAS_CHAPTER]->(:Chapter)-[:CONTAINS]->(k:KnowledgePoint) " +
            "WHERE k.id IN $ids " +
            "RETURN DISTINCT k.id AS kpId, c.id AS courseId, c.name AS courseName")
    List<KpCourseDTO> findCourseMemberships(@Param("ids") Collection<String> ids);
}