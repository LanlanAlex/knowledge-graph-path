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
          <el-select v-model="currentCourseId" style="width: 100%; margin-bottom: 20px;" @change="handleBookChange">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>

          <div style="font-weight: bold; margin-bottom: 15px;">📑 章节目录</div>
          <el-menu :default-active="menuActive" @select="handleMenuSelect">
            <el-menu-item index="CROSS">🌐 跨学科关联</el-menu-item>
            <el-menu-item index="ALL">全部章节</el-menu-item>
            <el-menu-item v-for="chap in chapterOptions" :key="chap.value" :index="chap.value">
              {{ chap.label }}
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
            @click="reloadCurrentView"
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
              <el-descriptions-item label="课程" v-if="selectedNode.courseName">{{ selectedNode.courseName }}</el-descriptions-item>
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

    <!-- 移动端左侧抽屉 (教材 + 章节选择) -->
    <el-drawer v-model="leftDrawerVisible" title="目录" direction="ltr" size="80%">
      <div style="font-weight: bold; color: #E0BBE4; margin-bottom: 8px;">📚 当前教材</div>
      <el-select v-model="currentCourseId" style="width: 100%; margin-bottom: 20px;" @change="handleBookChange">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>

      <div style="font-weight: bold; margin-bottom: 8px;">📑 章节目录</div>
      <el-menu :default-active="menuActive" @select="handleMenuSelect">
        <el-menu-item index="CROSS">🌐 跨学科关联</el-menu-item>
        <el-menu-item index="ALL">全部章节</el-menu-item>
        <el-menu-item v-for="chap in chapterOptions" :key="chap.value" :index="chap.value">
          {{ chap.label }}
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <!-- 移动端右侧抽屉 (详情推荐) -->
    <el-drawer v-model="rightDrawerVisible" title="节点详情" direction="rtl" size="80%">
      <div v-if="selectedNode">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="名称">{{ selectedNode.name }}</el-descriptions-item>
          <el-descriptions-item label="课程" v-if="selectedNode.courseName">{{ selectedNode.courseName }}</el-descriptions-item>
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
import { ref, computed, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import axios from 'axios';
import { Menu, InfoFilled, Refresh } from '@element-plus/icons-vue';

const leftDrawerVisible = ref(false);
const rightDrawerVisible = ref(false);
const courses = ref([]);               // 教材下拉框数据（来自 /api/graph/courses）
const currentCourseId = ref('C-01');   // 默认数据结构
const selectedChapter = ref('');
const chapterList = ref([]);           // 章节短名数组（与 KP.chapter 对齐，用于 category 映射）
const chapterOptions = ref([]);        // [{value, label}] 菜单显示用
const selectedNode = ref(null);
// 视图模式：course=单本教材（按章节上色），cross=跨学科关联（按课程上色）
const viewMode = ref('course');

// 左侧菜单当前高亮项：跨学科视图高亮 CROSS，否则高亮章节/全部
const menuActive = computed(() => {
  if (viewMode.value === 'cross') return 'CROSS';
  return selectedChapter.value || 'ALL';
});

let myChart = null;
let resizeObserver = null;
let resizeRaf = null;
// 缓存最近一次渲染的原始节点/连线，供章节高亮复用（避免重新布局导致视图跳动）
let lastGraphData = null;
// 原始后端数据 + 标题 + 渲染时尺寸：跨断点/大幅变化时据此整体重绘（只 resize 会让 ECharts 内部坐标系失配）
let lastRawData = null;
let lastTitle = '';
let lastRenderSize = { w: 0, h: 0 };
// 移动端标签显隐状态与防抖定时器（缩放时高频触发，必须防抖后再 setOption）
let labelsVisible = false;
let labelToggleTimer = null;
// graphRoam 里的 params.zoom 是“每次增量因子”（如 1.1 / 0.909），需自行累乘得到当前倍率
let liveZoom = 1;

const macaronColors = [
  '#FFB3BA', '#FFDFBA', '#FFFFBA',
  '#BAFFC9', '#BAE1FF', '#E0BBE4',
  '#FFC8DD', '#A2D2FF', '#C4E8C2' // 第9种：支持9章的教材
];

// 切换教材：重置筛选状态 -> 拉章节 -> 拉图谱
const handleBookChange = async (courseId) => {
  viewMode.value = 'course';
  selectedChapter.value = '';
  selectedNode.value = null;
  leftDrawerVisible.value = false; // 移动端选完教材后关闭抽屉，否则会挡住图谱
  await loadChapters(courseId);
  await loadGraph(courseId);
};

// 左侧菜单统一入口：CROSS=跨学科关联，其它=章节筛选
const handleMenuSelect = async (index) => {
  leftDrawerVisible.value = false; // 移动端选中后关闭抽屉
  if (index === 'CROSS') {
    await loadCrossGraph();
    return;
  }
  // 从跨学科视图切回章节视图时，先恢复单本教材图谱再高亮
  if (viewMode.value === 'cross') {
    viewMode.value = 'course';
    selectedChapter.value = '';
    await loadGraph(currentCourseId.value);
  }
  handleSelectChapter(index);
};

// 跨学科关联视图：两本教材之间的先修/相关边
const loadCrossGraph = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/graph/cross');
    viewMode.value = 'cross';
    selectedChapter.value = '';
    selectedNode.value = null;
    if (!res.data.nodes || res.data.nodes.length === 0) {
      renderEmptyCross();
      return;
    }
    renderChart(res.data, '跨学科关联视图');
  } catch (error) {
    console.error('获取跨学科关联数据失败', error);
  }
};

// 没有跨课边时给个明确提示，避免用户误以为图坏了
const renderEmptyCross = () => {
  if (!myChart) return;
  myChart.clear();
  myChart.setOption({
    title: {
      text: '暂无跨学科关联数据',
      subtext: '当前两本教材之间还没有登记跨课程先修/相关关系',
      left: 'center',
      top: 'middle',
      textStyle: { color: '#888', fontSize: 18 }
    }
  });
  lastGraphData = { nodes: [], links: [] };
  myChart.off('click');
};

// 重置按钮：按当前视图重新加载
const reloadCurrentView = () => {
  if (viewMode.value === 'cross') {
    loadCrossGraph();
  } else {
    loadGraph(currentCourseId.value);
  }
};

const loadChapters = async (courseId) => {
  try {
    const res = await axios.get(`http://localhost:8080/api/graph/chapters?courseId=${courseId}`);
    chapterOptions.value = res.data;
    chapterList.value = res.data.map(c => c.value);
  } catch (error) {
    console.error('获取章节列表失败', error);
  }
};

const loadCourses = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/graph/courses');
    courses.value = res.data;
  } catch (error) {
    console.error('获取课程列表失败', error);
  }
};

const currentCourseName = () => {
  const c = courses.value.find(c => c.id === currentCourseId.value);
  return c ? c.name : '知识图谱';
};

// 选中章节：高亮该章节节点，其余节点淡出；选"全部章节"恢复
const handleSelectChapter = (index) => {
  selectedChapter.value = index === 'ALL' ? '' : index;
  leftDrawerVisible.value = false; // 移动端选中后关闭抽屉
  applyChapterHighlight();
};

const applyChapterHighlight = () => {
  if (!myChart || !lastGraphData) return;
  if (viewMode.value === 'cross') return; // 跨学科视图按课程上色，章节筛选不适用
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

const loadGraph = async (courseId) => {
  try {
    const res = await axios.get(`http://localhost:8080/api/graph/global?courseId=${courseId}`);
    viewMode.value = 'course';
    selectedChapter.value = '';
    selectedNode.value = null;
    renderChart(res.data, `${currentCourseName()} · 知识图谱`);
  } catch (error) {
    console.error('获取数据失败', error);
  }
};

const loadRecommendPath = async (targetId) => {
  try {
    const res = await axios.get(`http://localhost:8080/api/graph/path?targetId=${targetId}`);
    viewMode.value = 'course';
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

  // 优先使用后端给的图例分类（跨学科视图=课程名）；否则退回本地章节列表
  const backendCategories = Array.isArray(data.categories) && data.categories.length > 0
      ? data.categories
      : null;
  const categoryNames = backendCategories || chapterList.value;

  const categories = categoryNames.map((name, index) => ({
    name: name,
    itemStyle: { color: macaronColors[index % macaronColors.length] }
  }));

  const nodes = data.nodes.map(node => {
    // 后端给了 category 就按课程的分类下标走，否则按章节名映射
    let category;
    if (backendCategories) {
      category = typeof node.category === 'number' ? node.category : 0;
    } else {
      const chapterIdx = chapterList.value.indexOf(node.chapter);
      category = chapterIdx >= 0 ? chapterIdx : 0;
    }
    return {
      id: node.id,
      name: node.name,
      chapter: node.chapter,
      type: node.type,
      difficulty: node.difficulty,
      courseName: node.courseName,
      category: category,
      symbolSize: node.symbolSize || 30,
      rawData: node // 详情面板直接读这个对象
    };
  });

  const links = data.links.map(link => {
    const label = link.label || '';
    const isRelated = label.includes('相关');
    const isCross = label.startsWith('跨课');
    return {
      source: link.source,
      target: link.target,
      lineStyle: {
        // 跨学科边用紫色加粗，和同课程内部的灰色边区分开
        color: isCross ? '#C5A3E0' : '#ccc',
        width: isCross ? 2 : 1,
        curveness: 0.1,
        // 相关关系用虚线+更淡，和先修关系区分
        type: isRelated ? 'dashed' : 'solid',
        opacity: isRelated ? 0.55 : 0.8
      }
    };
  });

  const option = {
    title: { text: title, left: 'center', top: 10, textStyle: { color: '#555', fontSize: 16 } },
    tooltip: {
      formatter: (x) => {
        const d = x.data && x.data.rawData;
        if (!d) return x.name;
        // 跨学科视图额外显示所属课程，避免两本书同名章节混淆
        const courseLine = d.courseName ? `${d.courseName}<br/>` : '';
        return `${d.name}<br/>${courseLine}${d.chapter || ''} · ${d.type || ''}<br/>难度：${d.difficulty || '-'}`;
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
      label: { show: !isMobile, position: 'right', formatter: '{b}', fontSize: isMobile ? 10 : 12 },
      // 标签重叠时自动隐藏，避免密集区域文字糊成一片（桌面/移动端都生效）
      labelLayout: { hideOverlap: true },
      force: isMobile
          ? { repulsion: 160, edgeLength: [45, 95], gravity: 0.2 }
          : { repulsion: 300, edgeLength: [60, 150], gravity: 0.1 },
      emphasis: {
        focus: 'adjacency',
        // 移动端默认不显示标签，点中节点时把该节点及其邻接节点的名字弹出来
        label: { show: true, fontSize: 11 },
        lineStyle: { width: 3, color: '#FFB3BA' }
      }
    }]
  };

  myChart.setOption(option, true);
  lastGraphData = { nodes, links };
  lastRawData = data;
  lastTitle = title;
  lastRenderSize = { w: myChart.getDom().clientWidth, h: myChart.getDom().clientHeight };

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

  // 移动端：放大到一定倍率后自动显示标签，缩回则隐藏（桌面端标签始终可见，不走这条逻辑）
  labelsVisible = !isMobile;
  liveZoom = isMobile ? 0.7 : 1; // 与 series.zoom 初始值保持一致
  clearTimeout(labelToggleTimer);
  myChart.off('graphRoam');
  myChart.on('graphRoam', function (params) {
    if (!isMobile) return;
    // params.zoom 是本次操作的增量倍率，累乘得到当前缩放级别（初始 0.7）
    liveZoom = Math.min(4, Math.max(0.25, liveZoom * (params.zoom || 1)));
    // 双阈值防抖：放大到 1.0 显示，缩到 0.85 以下才隐藏，避免临界值反复闪烁
    const want = labelsVisible ? liveZoom > 0.85 : liveZoom >= 1.0;
    if (want === labelsVisible) return;
    // 捏合缩放会连续触发，防抖后再改配置，避免卡顿（改配置只合并 label，不重建布局）
    clearTimeout(labelToggleTimer);
    labelToggleTimer = setTimeout(function () {
      if (!myChart || myChart.isDisposed()) return;
      if (want === labelsVisible) return;
      labelsVisible = want;
      myChart.setOption({ series: [{ label: { show: want } }] });
    }, 200);
  });

  // 保持当前的章节筛选状态（例如生成路径后仍停留在某章）
  if (selectedChapter.value) applyChapterHighlight();
};

onMounted(() => {
  const el = document.getElementById('chart-container');
  myChart = echarts.init(el);
  // ResizeObserver 能覆盖窗口缩放、侧栏折叠、移动端地址栏收起等所有尺寸变化
  resizeObserver = new ResizeObserver(() => {
    // 合并同一帧内的多次尺寸变化，避免在力导向布局动画中反复 resize 导致 ECharts 内部状态异常
    if (resizeRaf) cancelAnimationFrame(resizeRaf);
    resizeRaf = requestAnimationFrame(() => {
      resizeRaf = null;
      if (!myChart || myChart.isDisposed()) return;
      // 容器不可见（宽高为 0，如抽屉/标签页切换瞬间）时跳过，ECharts 此时布局数据为 null
      if (!el.clientWidth || !el.clientHeight) return;
      const w = el.clientWidth;
      const h = el.clientHeight;
      // 跨越 768 断点或尺寸大幅变化（如 DevTools 切换设备、横竖屏切换）时整体重绘：
      // 只调用 resize() 会让已收敛的力导向坐标系与新画布尺寸失配，表现为“画布正常但拖不动”
      const crossedBreakpoint = (w <= 768) !== (lastRenderSize.w <= 768);
      const bigChange = Math.abs(w - lastRenderSize.w) > 120 || Math.abs(h - lastRenderSize.h) > 120;
      if (lastRawData && (crossedBreakpoint || bigChange)) {
        // 关键：必须按新尺寸重建画布。只重绘数据不重建 canvas，画布仍保留旧宽度（如桌面 840px），
        // 图谱会以旧坐标系居中，表现为整体偏右、右侧被裁切、拖动范围异常
        if (myChart && !myChart.isDisposed()) {
          myChart.dispose();
        }
        myChart = echarts.init(el);
        renderChart(lastRawData, lastTitle);
        return;
      }
      try {
        myChart.resize();
      } catch (err) {
        // ECharts 6.x 偶发在布局未就绪时 resize 抛错，吞掉以免打断后续交互（拖动/缩放）
        console.warn('[ECharts] resize skipped:', err);
      }
    });
  });
  resizeObserver.observe(el);
  // 启动顺序：课程列表 -> 章节列表 -> 图谱（章节列表是 category 映射的前提）
  loadCourses().then(() =>
    loadChapters(currentCourseId.value).then(() => loadGraph(currentCourseId.value))
  );
});

onUnmounted(() => {
  if (resizeRaf) {
    cancelAnimationFrame(resizeRaf);
    resizeRaf = null;
  }
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