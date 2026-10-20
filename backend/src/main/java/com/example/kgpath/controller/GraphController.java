package com.example.kgpath.controller;

import com.example.kgpath.dto.GraphDataDTO;
import com.example.kgpath.service.GraphService;
import com.example.kgpath.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/graph")
@CrossOrigin // 允许跨域请求
public class GraphController {

    @Autowired
    private GraphService graphService;

    @Autowired
    private RecommendService recommendService;

    // 获取全局图谱
    @GetMapping("/global")
    public GraphDataDTO getGlobalGraph() {
        return graphService.getGlobalGraph();
    }

    // 获取某个知识点的推荐学习路径
    // 访问方式：http://localhost:8080/api/graph/path?targetId=KP-08-06
    @GetMapping("/path")
    public GraphDataDTO getLearningPath(@RequestParam String targetId) {
        return recommendService.generateLearningPath(targetId);
    }
}