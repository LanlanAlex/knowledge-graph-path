package com.example.kgpath;

import com.example.kgpath.dto.RelationDTO;
import com.example.kgpath.entity.KnowledgePoint;
import com.example.kgpath.repository.KnowledgePointRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class RepositoryTest {

    @Autowired
    private KnowledgePointRepository repository;

    @Test
    public void testFind() {
        // 找一下 "动态规划" (或者你图里有的任何一个知识点ID)
        // 假设 "栈" 的 ID 是 KP-03-01
        List<KnowledgePoint> nexts = repository.findDirectNextNodes("KP-03-01");
        System.out.println("=== 栈的后继节点 ===");
        for (KnowledgePoint k : nexts) {
            System.out.println(k.getName() + " - 难度:" + k.getDifficulty());
        }

        // 测试全局关系查询
        System.out.println("=== 全局关系图数据 ===");
        List<RelationDTO> relations = repository.findAllPrerequisiteRelationsForGraph();
        relations.stream().limit(5).forEach(System.out::println);
    }
}