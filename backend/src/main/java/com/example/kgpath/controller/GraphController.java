package com.example.kgpath.controller;

import com.example.kgpath.dto.ChapterDTO;
import com.example.kgpath.dto.GraphDataDTO;
import com.example.kgpath.entity.Course;
import com.example.kgpath.repository.CourseRepository;
import com.example.kgpath.service.GraphService;
import com.example.kgpath.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/graph")
@CrossOrigin // 允许跨域请求
public class GraphController {

    @Autowired
    private GraphService graphService;

    @Autowired
    private RecommendService recommendService;

    @Autowired
    private CourseRepository courseRepository;

    // 获取全局图谱（按课程隔离，默认数据结构 C-01）
    // 访问方式：http://localhost:8080/api/graph/global?courseId=C-01
    @GetMapping("/global")
    public GraphDataDTO getGlobalGraph(@RequestParam(defaultValue = "C-01") String courseId) {
        return graphService.getGlobalGraph(courseId);
    }

    // 获取某门课程的章节列表（前端左侧目录 + category 映射基准）
    // 访问方式：http://localhost:8080/api/graph/chapters?courseId=C-01
    @GetMapping("/chapters")
    public List<ChapterDTO> getChapters(@RequestParam(defaultValue = "C-01") String courseId) {
        return graphService.getChapters(courseId);
    }

    // 获取所有课程（前端教材下拉框用）
    @GetMapping("/courses")
    public List<Course> getCourses() {
        return courseRepository.findAll();
    }

    // 跨学科关联视图：只返回两端分属不同课程的边（如 数据结构 -> 计算机网络）
    // 访问方式：http://localhost:8080/api/graph/cross
    @GetMapping("/cross")
    public GraphDataDTO getCrossCourseGraph() {
        return graphService.getCrossCourseGraph();
    }

    // 获取某个知识点的推荐学习路径
    // 访问方式：http://localhost:8080/api/graph/path?targetId=KP-08-06
    @GetMapping("/path")
    public GraphDataDTO getLearningPath(@RequestParam String targetId) {
        return recommendService.generateLearningPath(targetId);
    }
}
