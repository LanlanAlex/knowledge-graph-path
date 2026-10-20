package com.example.kgpath.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Data
@Node("Chapter")
public class Chapter {
    @Id
    private String id; // 例如 CH-01

    private String name; // 第1章 绪论
    private Integer order; // 1

    // 反向映射：属于哪个课程（HAS_CHAPTER）
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Relationship(type = "HAS_CHAPTER", direction = Relationship.Direction.INCOMING)
    private Course course;

    // 正向映射：包含哪些知识点（CONTAINS）
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    private List<KnowledgePoint> knowledgePoints;
}