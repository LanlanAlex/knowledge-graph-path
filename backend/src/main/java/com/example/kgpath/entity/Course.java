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
@Node("Course")
public class Course {
    @Id
    private String id; // 例如 C-01

    private String name; // 数据结构（C语言版 第2版）
    private Integer credit; // 4
    private Integer hours; // 64

    // 正向映射：包含哪些章节（HAS_CHAPTER）
    // 排除 toString/equals，防止 Course <-> Chapter 双向互指导致无限递归
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Relationship(type = "HAS_CHAPTER", direction = Relationship.Direction.OUTGOING)
    private List<Chapter> chapters;
}