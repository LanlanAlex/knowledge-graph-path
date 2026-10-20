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
@Node("KnowledgePoint")
public class KnowledgePoint {
    @Id
    private String id;
    private String name;
    private String type;
    private String chapter;
    private Integer page;
    private Integer difficulty;
    private String description;

    // @JsonIgnore 防止 JSON 死循环；@ToString/@EqualsAndHashCode.Exclude 防止 next <-> prev 递归栈溢出
    //
    // ⚠️ save() 警告：SDN 保存实体时会"先删后建"本字段映射的关系。
    // 若 nextPoints/prevPoints 为 null（如只做了 findAll 后直接改属性再 save），
    // 该知识点的 PREREQUISITE_OF 出边/入边会被全部删掉！
    // 写库请用自定义 Cypher（@Query + SET/MERGE），或先查全关系列表再 save。
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Relationship(type = "PREREQUISITE_OF", direction = Relationship.Direction.OUTGOING)
    private List<KnowledgePoint> nextPoints;

    // ⚠️ 同上：save() 时为 null 会清空全部入边
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Relationship(type = "PREREQUISITE_OF", direction = Relationship.Direction.INCOMING)
    private List<KnowledgePoint> prevPoints;
}