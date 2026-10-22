package com.example.kgpath.dto;

import lombok.Data;

@Data
public class ChapterDTO {
    private String value; // 与 KnowledgePoint.chapter 一致的短名（如 "第1章"），用于前端 category 映射
    private String label; // 完整章节名（如 "第1章 绪论"），用于菜单展示
}
