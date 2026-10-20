package com.example.kgpath.repository;

import com.example.kgpath.entity.Course;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

public interface CourseRepository extends Neo4jRepository<Course, String> {
    // 查询所有课程，并且连带查出它们包含的章节（利用实体类里的 @Relationship 自动映射）
    @Query("MATCH (c:Course)-[:HAS_CHAPTER]->(ch:Chapter) RETURN c, collect(ch) as chapters")
    List<Course> findAllWithChapters();
}