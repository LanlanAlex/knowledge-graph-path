// ============================================================
// 合并导入脚本：数据结构(C-01) + 计算机网络技术基础(C-02) + 数字逻辑(C-03)
// 前提：import 目录下 4 个 CSV 已包含三本书数据（courses/chapters/nodes/relationships）
// 全部 MERGE + 已有唯一约束，重复执行安全
// 每段一条语句，IDE 插件里光标放分号上 Ctrl+Enter 逐段执行
// ============================================================

// ========= 1. 导入课程（3 门） =========
LOAD CSV WITH HEADERS FROM 'file:///courses.csv' AS row
MERGE (c:Course {id: row.ID})
SET c.name = row.Name,
    c.credit = toFloat(row.Credit),
    c.hours = toInteger(row.Hours);

// ========= 2. 导入章节（24 章）并挂到课程 =========
LOAD CSV WITH HEADERS FROM 'file:///chapters.csv' AS row
MERGE (ch:Chapter {id: row.ID})
SET ch.name = row.Name,
    ch.order = toInteger(row.Order);

// ========= 3. HAS_CHAPTER（Course -> Chapter，按 CSV 的 Course 列） =========
LOAD CSV WITH HEADERS FROM 'file:///chapters.csv' AS row
MATCH (c:Course {id: row.Course})
MATCH (ch:Chapter {id: row.ID})
MERGE (c)-[:HAS_CHAPTER]->(ch);

// ========= 4. 导入知识点（204 个：93 KP + 72 CN + 39 DL） =========
LOAD CSV WITH HEADERS FROM 'file:///nodes.csv' AS row
MERGE (k:KnowledgePoint {id: row.ID})
SET k.name = row.Name,
    k.type = row.Type,
    k.chapter = row.Chapter,
    k.page = toInteger(row.Page),
    k.difficulty = toInteger(row.Difficulty),
    k.description = row.Description;

// ========= 5. PREREQUISITE_OF（含跨课边，双向端点都在才挂） =========
LOAD CSV WITH HEADERS FROM 'file:///relationships.csv' AS row
WITH row WHERE row.Relation = 'PREREQUISITE_OF'
MATCH (a:KnowledgePoint {id: row.From_ID})
MATCH (b:KnowledgePoint {id: row.To_ID})
MERGE (a)-[r:PREREQUISITE_OF]->(b)
SET r.note = row["依据/备注"];

// ========= 6. RELATED_TO =========
LOAD CSV WITH HEADERS FROM 'file:///relationships.csv' AS row
WITH row WHERE row.Relation = 'RELATED_TO'
MATCH (a:KnowledgePoint {id: row.From_ID})
MATCH (b:KnowledgePoint {id: row.To_ID})
MERGE (a)-[r:RELATED_TO]->(b)
SET r.note = row["依据/备注"];

// ========= 7. CONTAINS：C-01 部分（KP- 前缀，按章节号匹配，限 C-01 链路） =========
MATCH (c:Course {id: 'C-01'})-[:HAS_CHAPTER]->(ch:Chapter), (k:KnowledgePoint)
WHERE k.id STARTS WITH 'KP-'
  AND split(ch.name, ' ')[0] = split(k.chapter, ' ')[0]
MERGE (ch)-[:CONTAINS]->(k);

// ========= 8. CONTAINS：C-02 部分（CN- 前缀，按章节号匹配，限 C-02 链路） =========
MATCH (c:Course {id: 'C-02'})-[:HAS_CHAPTER]->(ch:Chapter), (k:KnowledgePoint)
WHERE k.id STARTS WITH 'CN-'
  AND split(ch.name, ' ')[0] = split(k.chapter, ' ')[0]
MERGE (ch)-[:CONTAINS]->(k);

// ========= 9. CONTAINS：C-03 部分（DL- 前缀，按章节号匹配，限 C-03 链路） =========
MATCH (c:Course {id: 'C-03'})-[:HAS_CHAPTER]->(ch:Chapter), (k:KnowledgePoint)
WHERE k.id STARTS WITH 'DL-'
  AND split(ch.name, ' ')[0] = split(k.chapter, ' ')[0]
MERGE (ch)-[:CONTAINS]->(k);

// ========= 验证 A：节点分布（期望 KnowledgePoint=204, Chapter=24, Course=3） =========
MATCH (n) RETURN labels(n)[0] AS 类型, count(n) AS 数量 ORDER BY 数量 DESC;

// ========= 验证 B：各课程隔离统计（C-01: 8章/93点, C-02: 9章/72点, C-03: 7章/39点） =========
MATCH (c:Course)-[:HAS_CHAPTER]->(ch:Chapter)
OPTIONAL MATCH (ch)-[:CONTAINS]->(k:KnowledgePoint)
RETURN c.id AS 课程, count(DISTINCT ch) AS 章节数, count(DISTINCT k) AS 知识点数 ORDER BY 课程;

// ========= 验证 C：关系统计（CONTAINS=204，PREREQUISITE_OF 略增，含 4 条跨课边） =========
MATCH ()-[r]->() RETURN type(r) AS 关系类型, count(r) AS 数量 ORDER BY 数量 DESC;

// ========= 验证 D：跨书污染检查（任何 KP 不应同时挂两本书的章节，期望 0 行） =========
MATCH (k:KnowledgePoint)<-[:CONTAINS]-(ch:Chapter)<-[:HAS_CHAPTER]-(c:Course)
WITH k, count(DISTINCT c) AS courses WHERE courses > 1
RETURN k.id AS 跨书节点, courses;
