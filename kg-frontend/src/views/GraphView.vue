<template>
  <el-container class="app-layout">
    <!-- 移动端顶部 Header -->
    <el-header class="mobile-header" height="60px">
      <el-button text @click="leftDrawerVisible = true">
        <el-icon size="24"><Menu /></el-icon>
      </el-button>
      <span style="font-weight: bold;">知识图谱系统</span>
      <el-button text @click="rightDrawerVisible = true" v-if="selectedNode">
        <el-icon size="24"><InfoFilled /></el-icon>
      </el-button>
    </el-header>

    <el-container class="main-container">
      <!-- 左侧栏：桌面端显示 Aside，移动端显示 Drawer -->
      <el-aside width="280px" class="desktop-aside">
        <el-card style="height: 100%; border-radius: 0;">
          <template #header>
            <div style="font-weight: bold; color: #E0BBE4;">📚 当前教材</div>
          </template>
          <el-select v-model="currentBook" style="width: 100%; margin-bottom: 20px;">
            <el-option label="数据结构（C语言版）" value="ds-c" />
            <el-option label="计算机网络（后续扩展）" value="network" disabled />
          </el-select>

          <div style="font-weight: bold; margin-bottom: 15px;">📑 章节目录</div>
          <el-menu :default-active="selectedChapter || 'ALL'" @select="handleSelectChapter">
            <el-menu-item index="ALL">全部章节</el-menu-item>
            <el-menu-item v-for="chap in chapterList" :key="chap" :index="chap">
              {{ chap }}
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-aside>

      <!-- 中间：主图表区 -->
      <el-main class="chart-main">
        <div id="chart-container"></div>
        <el-button
            class="reset-btn"
            type="success"
            round
            @click="loadGraph"
            :icon="Refresh"
        >
          重置视图
        </el-button>
      </el-main>

      <!-- 右侧栏：详情与推荐 -->
      <el-aside width="320px" class="desktop-aside right-aside">
        <el-card style="height: 100%; border-radius: 0;">
          <template #header>
            <div style="font-weight: bold; color: #BAE1FF;">📌 节点详情</div>
          </template>

          <div v-if="selectedNode">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="名称">{{ selectedNode.name }}</el-descriptions-item>
              <el-descriptions-item label="章节">{{ selectedNode.chapter }}</el-descriptions-item>
              <el-descriptions-item label="类型">{{ selectedNode.type }}</el-descriptions-item>
              <el-descriptions-item label="难度">
                <el-rate v-model="selectedNode.difficulty" disabled />
              </el-descriptions-item>
            </el-descriptions>
            <p style="font-size: 13px; color: #666; margin-top: 15px; line-height: 1.6;">
              {{ selectedNode.description }}
            </p>
            <el-button
                type="primary"
                style="width: 100%; margin-top: 20px; background: var(--macaron-purple); border: none;"
                @click="loadRecommendPath(selectedNode.id)"
            >
              ✨ 生成学习路径
            </el-button>
          </div>
          <el-empty v-else description="点击图上的节点查看详情" />
        </el-card>
      </el-aside>
    </el-container>

    <!-- 移动端左侧抽屉 (章节选择) -->
    <el-drawer v-model="leftDrawerVisible" title="章节目录" direction="ltr" size="80%">
      <el-menu :default-active="selectedChapter || 'ALL'" @select="handleSelectChapter">
        <el-menu-item index="ALL">全部章节</el-menu-item>
        <el-menu-item v-for="chap in chapterList" :key="chap" :index="chap">
          {{ chap }}
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <!-- 移动端右侧抽屉 (详情推荐) -->
    <el-drawer v-model="rightDrawerVisible" title="节点详情" direction="rtl" size="80%">
      <div v-if="selectedNode">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="名称">{{ selectedNode.name }}</el-descriptions-item>
          <el-descriptions-item label="章节">{{ selectedNode.chapter }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ selectedNode.type }}</el-descriptions-item>
          <el-descriptions-item label="难度">
            <el-rate v-model="selectedNode.difficulty" disabled />
          </el-descriptions-item>
        </el-descriptions>
        <p style="font-size: 13px; color: #666; margin-top: 15px; line-height: 1.6;">
          {{ selectedNode.description }}
        </p>
        <el-button
            type="primary"
            style="width: 100%; margin-top: 20px; background: var(--macaron-purple); border: none;"
            @click="loadRecommendPath(selectedNode.id)"
        >
          ✨ 生成学习路径
        </el-button>
      </div>
      <el-empty v-else description="点击图上的节点查看详情" />
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import axios from 'axios';
import { Menu, InfoFilled, Refresh } from '@element-plus/icons-vue';

const leftDrawerVisible = ref(false);
const rightDrawerVisible = ref(false);
const currentBook = ref('ds-c');
const selectedChapter = ref('');
const chapterList = ref(['第1章', '第2章', '第3章', '第4章', '第5章', '第6章', '第7章', '第8章']);
const selectedNode = ref(null);

let myChart = null;
let resizeObserver = null;
// 缓存最近一次渲染的原始节点/连线，供章节高亮复用（避免重新布局导致视图跳动）
let lastGraphData = null;

const macaronColors = ['#FFB3BA', '#FFDFBA', '#FFFFBA', '#BAFFC9', '#BAE1FF', '#E0BBE4', '#FFC8DD', '#A2D2FF'];

// 选中章节：高亮该章节节点，其余节点淡出；选"全部章节"恢复
const handleSelectChapter = (index) => {
  selectedChapter.value = index === 'ALL' ? '' : index;
  leftDrawerVisible.value = false; // 移动端选中后关闭抽屉
  applyChapterHighlight();
};

const applyChapterHighlight = () => {
  if (!myChart || !lastGraphData) return;
  const chapter = selectedChapter.value;

  // 只更新 data，不重建 series，保证 force 布局和当前缩放/平移不被重置
  const nodes = lastGraphData.nodes.map(node => {
    const dimmed = chapter && node.chapter !== chapter;
    return {
      ...node,
      itemStyle: { opacity: dimmed ? 0.12 : 1 },
      label: { show: !dimmed, opacity: dimmed ? 0.15 : 1 }
    };
  });

  myChart.setOption({ series: [{ data: nodes }] });
};

const loadGraph = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/graph/global');
    selectedChapter.value = '';
    selectedNode.value = null;
    renderChart(res.data, '全局知识图谱');
  } catch (error) {
    console.error('获取数据失败', error);
  }
};

const loadRecommendPath = async (targetId) => {
  try {
    const res = await axios.get(`http://localhost:8080/api/graph/path?targetId=${targetId}`);
    selectedChapter.value = '';
    renderChart(res.data, '推荐学习路径');
    rightDrawerVisible.value = false; // 移动端关闭抽屉
  } catch (error) {
    console.error(error);
  }
};

const renderChart = (data, title) => {
  if (!myChart) return;

  const isMobile = window.innerWidth <= 768;

  const categories = chapterList.value.map((name, index) => ({
    name: name,
    itemStyle: { color: macaronColors[index % macaronColors.length] }
  }));

  const nodes = data.nodes.map(node => {
    const chapterIdx = chapterList.value.indexOf(node.chapter);
    return {
      id: node.id,
      name: node.name,
      chapter: node.chapter,
      type: node.type,
      difficulty: node.difficulty,
      category: chapterIdx >= 0 ? chapterIdx : 0,
      symbolSize: node.symbolSize || 30,
      rawData: node // 详情面板直接读这个对象
    };
  });

  const links = data.links.map(link => ({
    source: link.source,
    target: link.target,
    lineStyle: {
      color: '#ccc',
      curveness: 0.1,
      // 相关关系用虚线+更淡，和先修关系区分
      type: link.label === '相关' ? 'dashed' : 'solid',
      opacity: link.label === '相关' ? 0.5 : 0.8
    }
  }));

  const option = {
    title: { text: title, left: 'center', top: 10, textStyle: { color: '#555', fontSize: 16 } },
    tooltip: {
      formatter: (x) => {
        const d = x.data && x.data.rawData;
        if (!d) return x.name;
        return `${d.name}<br/>${d.chapter || ''} · ${d.type || ''}<br/>难度：${d.difficulty || '-'}`;
      }
    },
    legend: [{
      data: categories.map(a => a.name),
      left: 'left',
      bottom: 10,
      orient: 'vertical',
      textStyle: { fontSize: isMobile ? 10 : 12 }
    }],
    series: [{
      type: 'graph',
      layout: 'force',
      data: nodes,
      links: links,
      categories: categories,
      roam: true,
      // 限制缩放范围，避免移动端手势把图谱缩得看不见或放到视野外
      scaleLimit: { min: 0.25, max: 4 },
      // 移动端初始缩小一点，先看到整体再放大
      zoom: isMobile ? 0.7 : 1,
      label: { show: true, position: 'right', formatter: '{b}', fontSize: isMobile ? 10 : 12 },
      force: isMobile
          ? { repulsion: 120, edgeLength: [40, 90], gravity: 0.25 }
          : { repulsion: 300, edgeLength: [60, 150], gravity: 0.1 },
      emphasis: { focus: 'adjacency', lineStyle: { width: 3, color: '#FFB3BA' } }
    }]
  };

  myChart.setOption(option, true);
  lastGraphData = { nodes, links };

  myChart.off('click');
  myChart.on('click', function (params) {
    if (params.dataType === 'node') {
      selectedNode.value = params.data.rawData;
      // 小屏幕自动弹出详情
      if (window.innerWidth <= 768) {
        rightDrawerVisible.value = true;
      }
    }
  });

  // 保持当前的章节筛选状态（例如生成路径后仍停留在某章）
  if (selectedChapter.value) applyChapterHighlight();
};

onMounted(() => {
  const el = document.getElementById('chart-container');
  myChart = echarts.init(el);
  // ResizeObserver 能覆盖窗口缩放、侧栏折叠、移动端地址栏收起等所有尺寸变化
  resizeObserver = new ResizeObserver(() => {
    if (myChart) myChart.resize();
  });
  resizeObserver.observe(el);
  loadGraph();
});

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect();
    resizeObserver = null;
  }
  if (myChart) {
    myChart.dispose();
    myChart = null;
  }
});
</script>

<style scoped>
.app-layout {
  height: 100vh;
}
.main-container {
  /* 桌面端没有顶部 Header，占满整屏 */
  height: 100vh;
}
.desktop-aside {
  background-color: #fff;
  border-right: 1px solid #ebeef5;
}
.right-aside {
  border-right: none;
  border-left: 1px solid #ebeef5;
}
.chart-main {
  padding: 0;
  position: relative;
  overflow: hidden; /* 防止图表撑出容器，导致页面出现滚动条 */
  background-color: var(--macaron-bg);
}
/* 关键：触摸事件交给 ECharts 处理，否则移动端拖动会被浏览器当成页面滚动 */
#chart-container {
  width: 100%;
  height: 100%;
  min-height: 0;
  touch-action: none;
  overscroll-behavior: contain;
}
.reset-btn {
  position: absolute;
  bottom: 20px;
  right: 20px;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.mobile-header {
  display: none;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  align-items: center;
  justify-content: space-between;
}
:deep(.el-menu) {
  border-right: none;
}
/* 难度星星：主题主色是浅马卡龙蓝，默认星星几乎看不见，这里单独指定暖色 */
:deep(.el-rate) {
  --el-rate-fill-color: #FFB347;
  --el-rate-void-color: #E4E7ED;
}

/* 响应式：小屏幕隐藏桌面侧边栏，显示头部 */
@media (max-width: 768px) {
  .desktop-aside {
    display: none;
  }
  .mobile-header {
    display: flex;
  }
  .main-container {
    /* 100dvh 跟随移动端地址栏变化，避免底部被裁掉 */
    height: calc(100vh - 60px);
    height: calc(100dvh - 60px);
  }
}
</style>