<template>
  <router-view />
</template>

<script setup>
</script>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
}
/* 业务页通用容器：art 卡片化 */
.page-card {
  margin: 16px;
  padding: 20px;
  background: var(--default-box-color);
  border: 1px solid var(--art-card-border);
  border-radius: var(--art-radius-card);
  box-shadow: var(--art-card-shadow);
}
.toolbar { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; margin-bottom: 14px; }
.pager { margin-top: 14px; display: flex; justify-content: flex-end; }
.code-text { font-family: 'Courier New', monospace; font-weight: 700; letter-spacing: 2px; white-space: nowrap; }

/* 手机端：卡片容器贴边、表格横向滚动 */
@media (max-width: 640px) {
  .page-card { margin: 12px 12px 76px; padding: 14px; border-radius: var(--art-radius-card-sm); }
  .toolbar { gap: 8px; }
  .toolbar .el-radio-group { display: flex; flex-wrap: wrap; }
  .toolbar .el-radio-button__inner { padding: 7px 11px; }
  .pager { justify-content: center; }
}
</style>