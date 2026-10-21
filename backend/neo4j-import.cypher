// ============================================================
// Neo4j 知识图谱导入脚本
// 数据源：C:\Users\Administrator\neo4j\import\
// 执行方式：光标移到某条语句末尾分号上 → Ctrl + Enter
// 注意：Graph Database 插件每次只能跑一条语句，多条必须空行分段
// ============================================================

// ========= 0. 清库（首次导入跳过；重复导入前执行一次） =========
// MATCH (n) DETACH DELETE n;

// ========= 1. Course 唯一约束 =========
CREATE CONSTRAINT IF NOT EXISTS FOR (c:Course) REQUIRE c.id IS UNIQUE;

// ========= 2. Chapter 唯一约束 =========
CREATE CONSTRAINT IF NOT EXISTS FOR (ch:Chapter) REQUIRE ch.id IS UNIQUE;

// ========= 3. KnowledgePoint 唯一约束 =========
CREATE CONSTRAINT IF NOT EXISTS FOR (k:KnowledgePoint) REQUIRE k.id IS UNIQUE;

// ========= 4. 导入课程节点（courses.csv） =========
LOAD CSV WITH HEADERS FROM 'file:///courses.csv' AS row
MERGE (c:Course {id: row.ID})
SET c.name   = row.Name,
    c.credit = toInteger(row.Credit),
    c.hours  = toInteger(row.Hours);

// ========= 5. 导入章节节点 + HAS_CHAPTER（chapters.csv） =========
LOAD CSV WITH HEADERS FROM 'file:///chapters.csv' AS row
MERGE (ch:Chapter {id: row.ID})
SET ch.name      = row.Name,
    ch.course_id = row.Course,
    ch.order     = toInteger(row.Order)
WITH ch, row
MATCH (c:Course {id: row.Course})
MERGE (c)-[:HAS_CHAPTER {order: toInteger(row.Order)}]->(ch);

// ========= 6. 导入知识点节点（nodes.csv） =========
LOAD CSV WITH HEADERS FROM 'file:///nodes.csv' AS row
MERGE (k:KnowledgePoint {id: row.ID})
SET k.name        = row.Name,
    k.type        = row.Type,
    k.chapter     = row.Chapter,
    k.page        = toInteger(row.Page),
    k.difficulty  = toInteger(row.Difficulty),
    k.description = row.Description;

// ========= 7. PREREQUISITE_OF 关系（relationships.csv 过滤后 MERGE） =========
LOAD CSV WITH HEADERS FROM 'file:///relationships.csv' AS row
WITH row WHERE row.Relation = 'PREREQUISITE_OF'
MATCH (a:KnowledgePoint {id: row.From_ID})
MATCH (b:KnowledgePoint {id: row.To_ID})
MERGE (a)-[r:PREREQUISITE_OF]->(b)
SET r.note = row["依据/备注"];

// ========= 8. RELATED_TO 关系 =========
LOAD CSV WITH HEADERS FROM 'file:///relationships.csv' AS row
WITH row WHERE row.Relation = 'RELATED_TO'
MATCH (a:KnowledgePoint {id: row.From_ID})
MATCH (b:KnowledgePoint {id: row.To_ID})
MERGE (a)-[r:RELATED_TO]->(b)
SET r.note = row["依据/备注"];

// ========= 9. 补 CONTAINS（Chapter → KnowledgePoint） =========
// 走 Course 链路限定作用域：多教材共存时重跑本脚本也不会把边挂到别的书上
MATCH (c:Course)-[:HAS_CHAPTER]->(ch:Chapter), (k:KnowledgePoint)
WHERE ch.name STARTS WITH k.chapter
MERGE (ch)-[:CONTAINS]->(k);

// ========= 验证 A：节点分布 =========
MATCH (n) RETURN labels(n)[0] AS 类型, count(n) AS 数量 ORDER BY 数量 DESC;

// ========= 验证 B：关系统计 =========
MATCH ()-[r]->() RETURN type(r) AS 关系类型, count(r) AS 数量 ORDER BY 数量 DESC;

// ========= 验证 C：知识点样例 =========
MATCH (k:KnowledgePoint) RETURN k.id, k.name, k.chapter, k.difficulty LIMIT 10;

// ========= 验证 D：课程-章节结构 =========
MATCH (c:Course)-[:HAS_CHAPTER]->(ch:Chapter) RETURN c.name AS 课程, ch.name AS 章节 ORDER BY ch.order;

// ========= 验证 E：先修关系统计 =========
MATCH ()-[r:PREREQUISITE_OF]->() RETURN count(r) AS 先修关系总数;

// ========= 验证 F：图谱 schema（可视化） =========
CALL db.schema.visualization;
