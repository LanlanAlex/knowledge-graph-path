-- Neo4j 连通性自检
-- 光标停在语句上，按 Ctrl + Enter 或右键 Run Cypher 执行

RETURN 1 AS ok;

CALL dbms.components() YIELD name, versions, edition
RETURN name, versions, edition;

MATCH (n) RETURN count(n) AS nodes;


