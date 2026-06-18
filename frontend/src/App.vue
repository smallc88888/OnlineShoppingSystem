<script setup lang="ts">
</script>

<template>
  <div class="app-wrapper">

    <main class="main-content">
      <RouterView />
    </main>

    <footer class="course-footer">
      软件质量与测试课 2025-2026-2 学期
    </footer>

  </div>
</template>

<style scoped>
/* Flex 布局：确保页面内容不够多时，页脚也能被推到屏幕最底部 */
.app-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh; /* 撑满整个屏幕高度 */
}

/* 让主体内容自动撑开，把页脚挤下去 */
.main-content {
  flex: 1;
  width: 100%;
}

/* 课程专属页脚样式 */
.course-footer {
  text-align: center;
  padding: 25px 0;
  margin-top: 20px;
  background-color: transparent;
  color: #dc3545; /* 醒目且不刺眼的专业红色 (Bootstrap 默认红) */
  font-weight: bold;
  font-size: 15px;
  letter-spacing: 1px;
  border-top: 1px dashed #eaeaea; /* 上方加一条虚线与正文隔开 */
}
</style>