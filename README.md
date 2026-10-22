# knowledge-graph-path

基于 Neo4j 知识图谱的高校课程知识点可视化与学习路径推荐系统。

以教材为基本单位管理知识点及其先修/相关关系，前端力导向图交互展示，支持多教材切换、章节筛选、跨学科关联视图和学习路径查询。

## 功能

- 📚 **多教材管理**：数据结构、计算机网络技术基础、数字逻辑、计算机组成原理（可持续扩展）
- 🕸️ **知识图谱可视化**：ECharts 力导向图，按章节配色，难度映射节点大小
- 🔍 **章节筛选**：点击章节高亮对应知识点
- 🌐 **跨学科关联视图**：展示不同课程之间的先修/相关关系（如 数字逻辑 → 计算机组成原理）
- ⭐ **节点详情**：难度星级、所属章节、类型、描述
- 📱 **移动端适配**：响应式布局，支持触摸拖动/缩放，放大自动显示标签
- 🛤️ **学习路径**：查询目标知识点的先修链（/api/graph/path）

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite + Element Plus + ECharts 6 + Axios |
| 后端 | Spring Boot 4 + Spring Data Neo4j |
| 数据库 | Neo4j 5.26 Community（Bolt 7687 / HTTP 7474） |
| 构建 | Maven (mvnw) + npm |

## 目录结构

```
knowledge-graph-path/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/example/kgpath/
│   │   ├── entity/             # Course / Chapter / KnowledgePoint (@Node)
│   │   ├── repository/         # Neo4jRepository + 自定义 Cypher
│   │   ├── service/            # 图谱组装、路径查询
│   │   ├── controller/         # REST API
│   │   └── dto/                # GraphNode/GraphLink/GraphData 等
│   ├── src/main/resources/application.yml
│   ├── neo4j-import-all.cypher # 数据导入脚本（幂等，可重复执行）
│   └── pom.xml
└── kg-frontend/                # Vue 3 前端
    └── src/views/GraphView.vue # 图谱主页面
```

## 运行步骤

### 1. 启动 Neo4j

需要 Neo4j 5.x（JDK 17/21）。确认 7687 端口监听，账号 `neo4j` / 密码 `password`（可在 `backend/src/main/resources/application.yml` 修改）。

### 2. 导入数据（首次）

将 4 个 CSV（courses / chapters / nodes / relationships）放入 Neo4j 的 `import` 目录，然后在 Neo4j Browser 或 IDE 图数据库插件中**按段顺序执行** `backend/neo4j-import-all.cypher`（每段一条语句，全部 MERGE 幂等）。

### 3. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

服务地址：`http://localhost:8080`

### 4. 启动前端

```bash
cd kg-frontend
npm install
npm run dev
```

访问 Vite 输出的地址（默认 `http://localhost:5173`）。

## API 一览

| 接口 | 说明 |
|---|---|
| `GET /api/graph/courses` | 教材列表（下拉框数据源） |
| `GET /api/graph/chapters?courseId=C-01` | 某教材的章节列表 |
| `GET /api/graph/global?courseId=C-01` | 某教材完整图谱 |
| `GET /api/graph/cross` | 跨学科关联视图 |
| `GET /api/graph/path?targetId=KP-08-06` | 目标知识点的先修路径 |

## 新增一本教材

1. 在 4 个 CSV 中追加数据（知识点 ID 使用新前缀，如 `OS-`；章节 `Chapter` 列与章节名保持前缀一致）
2. 在 `neo4j-import-all.cypher` 的验证段之前复制一段 CONTAINS，修改课程号与 ID 前缀
3. 从头按段执行整个脚本（幂等，不会污染已有数据）
4. 执行脚本末尾的验证查询，确认新课程知识点数非 0
5. 刷新前端页面即可，前后端代码均无需改动

## 数据模型

```
(:Course)-[:HAS_CHAPTER]->(:Chapter)-[:CONTAINS]->(:KnowledgePoint)
(:KnowledgePoint)-[:PREREQUISITE_OF {note}]->(:KnowledgePoint)
(:KnowledgePoint)-[:RELATED_TO {note}]->(:KnowledgePoint)
```
