<!--
  系统管理 - 背景设置
  - 预设渐变主题选择（5 套）
  - 自定义图片上传（文件选择 + base64 预览）
  - 实时预览面板
  - 重置到默认
-->
<template>
  <div class="bg-settings-page">
    <!-- ============ 标题 ============ -->
    <el-card class="page-header-card" shadow="never">
      <div class="page-header">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="3"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
        </svg>
        <span>背景设置</span>
        <el-tag size="small" effect="plain" class="live-tag">实时预览</el-tag>
      </div>
      <div class="page-desc">更换系统背景主题，支持纯渐变色背景和自定义背景图片。修改后立即生效。</div>
    </el-card>

    <!-- ============ 预设渐变主题 ============ -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="section-header">
          <span>预设渐变主题</span>
          <el-tag size="small" type="success" v-if="bgStore.mode === 'gradient'">使用中</el-tag>
        </div>
      </template>
      <div class="preset-grid">
        <div
          v-for="(item, i) in presetGradients"
          :key="i"
          class="preset-item"
          :class="{ 'preset-item--active': bgStore.presetIndex === i && bgStore.mode === 'gradient' }"
          @click="bgStore.setPreset(i)"
        >
          <div class="preset-preview" :style="{ background: item.gradient }">
            <div v-if="bgStore.presetIndex === i && bgStore.mode === 'gradient'" class="preset-check">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#409eff" stroke-width="2.5">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
          </div>
          <div class="preset-name">{{ item.name }}</div>
          <div class="preset-desc">{{ item.desc }}</div>
        </div>
      </div>
    </el-card>

    <!-- ============ 自定义图片 ============ -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="section-header">
          <span>自定义背景图片</span>
          <el-tag size="small" type="warning" v-if="bgStore.mode === 'image'">使用中</el-tag>
        </div>
      </template>

      <div class="image-upload-area">
        <!-- 上传按钮 -->
        <div class="upload-controls">
          <el-button type="primary" :icon="UploadFilled" @click="triggerUpload">选择图片</el-button>
          <el-button v-if="bgStore.imageData" type="danger" plain :icon="Delete" @click="bgStore.clearImage()">移除图片</el-button>
          <span class="upload-hint">支持 JPG / PNG / WebP，建议 1920×1080 以上</span>
        </div>

        <!-- 图片预览 + 已上传图片信息 -->
        <div v-if="bgStore.imageData" class="image-preview-wrap">
          <div class="image-preview-frame">
            <img :src="bgStore.imageData" alt="自定义背景" class="image-preview-img" />
            <span class="image-size">{{ uploadedSize }}</span>
          </div>
          <div class="image-info">
            <div class="info-row">
              <span class="info-label">文件名</span>
              <span class="info-value">{{ uploadedName }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">尺寸</span>
              <span class="info-value">{{ uploadedSize }}</span>
            </div>
            <el-button type="success" :icon="Check" @click="applyImage" v-if="!bgStore.imageData || bgStore.mode !== 'image'">
              应用此背景
            </el-button>
            <el-text v-else type="success" class="applied-hint">✓ 已应用</el-text>
          </div>
        </div>

        <!-- 未上传 -->
        <div v-else class="image-empty">
          <el-icon :size="36" color="rgba(200,214,229,0.25)"><PictureFilled /></el-icon>
          <span>尚未选择背景图片</span>
        </div>

        <!-- 隐藏的文件选择器 -->
        <input
          ref="fileInputRef"
          type="file"
          accept="image/jpeg,image/png,image/webp"
          style="display: none"
          @change="onFileChange"
        />
      </div>
    </el-card>

    <!-- ============ 实时预览 ============ -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="section-header">
          <span>实时预览</span>
          <el-button size="small" text :icon="RefreshRight" @click="bgStore.reset()">重置为默认</el-button>
        </div>
      </template>
      <div class="preview-container">
        <div class="preview-stage" :style="{ background: previewBg }">
          <!-- 预览上的磨砂覆盖层 -->
          <div class="preview-overlay"></div>
          <!-- 预览内容模拟 -->
          <div class="preview-content">
            <div class="preview-sidebar">
              <div class="preview-logo">🌆</div>
              <div class="preview-menu-item preview-menu-active">数据大盘</div>
              <div class="preview-menu-item">设备管理</div>
              <div class="preview-menu-item">故障告警</div>
              <div class="preview-menu-item">系统管理</div>
            </div>
            <div class="preview-main">
              <div class="preview-header">
                <div class="preview-breadcrumb"></div>
                <div class="preview-user"></div>
              </div>
              <div class="preview-body">
                <div class="preview-card-row">
                  <div class="preview-card"></div>
                  <div class="preview-card"></div>
                  <div class="preview-card"></div>
                </div>
                <div class="preview-table">
                  <div class="preview-table-row" v-for="n in 3" :key="n"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="preview-label">系统布局预览（示意）</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Delete, Check, PictureFilled, RefreshRight } from '@element-plus/icons-vue'
import { useBackgroundStore, presetGradients } from '@/store/background'

const bgStore = useBackgroundStore()

/* ---- 文件上传 ---- */
const fileInputRef = ref(null)
const uploadedName = ref('')
const uploadedSize = ref('')

function triggerUpload() {
  fileInputRef.value?.click()
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return

  // 校验类型
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG / PNG / WebP 格式')
    return
  }

  // 校验大小（最大 5MB）
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    return
  }

  uploadedName.value = file.name
  uploadedSize.value = file.size > 1024 * 1024
    ? (file.size / (1024 * 1024)).toFixed(1) + ' MB'
    : (file.size / 1024).toFixed(0) + ' KB'

  // 读取为 base64 data URL
  const reader = new FileReader()
  reader.onload = (ev) => {
    bgStore.setImage(ev.target.result)
    ElMessage.success('背景已更新')
  }
  reader.readAsDataURL(file)
}

function applyImage() {
  if (bgStore.imageData) {
    bgStore.setImage(bgStore.imageData)
    ElMessage.success('已应用自定义背景')
  }
}

/* ---- 预览背景 ---- */
const previewBg = computed(() => {
  if (bgStore.mode === 'image' && bgStore.imageData) {
    return `url(${bgStore.imageData}) center / cover no-repeat fixed`
  }
  return presetGradients[bgStore.presetIndex]?.gradient || presetGradients[0].gradient
})
</script>

<style scoped>
.bg-settings-page {
  max-width: 960px;
  margin: 0 auto;
}

/* ======== 顶部标题 ======== */
.page-header-card {
  margin-bottom: 16px;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}
.page-header svg {
  color: var(--accent);
}
.live-tag {
  margin-left: 8px;
  font-weight: 400;
  animation: pulseTag 2s ease-in-out infinite;
}
@keyframes pulseTag {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
.page-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 6px;
}

/* ======== 区块卡片 ======== */
.section-card {
  margin-bottom: 16px;
}
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

/* ======== 预设渐变网格 ======== */
.preset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 14px;
}
.preset-item {
  cursor: pointer;
  border-radius: 10px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.03);
  border: 1.5px solid transparent;
  transition: all 0.2s;
  text-align: center;
}
.preset-item:hover {
  border-color: rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.06);
}
.preset-item--active {
  border-color: var(--accent) !important;
  background: rgba(64, 158, 255, 0.08) !important;
}
.preset-preview {
  position: relative;
  height: 90px;
  border-radius: 8px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 0.5px solid rgba(255, 255, 255, 0.06);
}
.preset-check {
  width: 28px;
  height: 28px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}
.preset-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}
.preset-desc {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

/* ======== 自定义图片上传 ======== */
.image-upload-area {
  min-height: 120px;
}
.upload-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.upload-hint {
  font-size: 12px;
  color: var(--text-muted);
}
.image-preview-wrap {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}
.image-preview-frame {
  position: relative;
  width: 240px;
  height: 135px;
  border-radius: 8px;
  overflow: hidden;
  border: 0.5px solid var(--border-subtle);
  flex-shrink: 0;
}
.image-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.image-size {
  position: absolute;
  bottom: 6px;
  right: 6px;
  font-size: 11px;
  background: rgba(0, 0, 0, 0.6);
  color: #ccc;
  padding: 1px 6px;
  border-radius: 4px;
}
.image-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 4px;
}
.info-row {
  display: flex;
  gap: 8px;
  font-size: 13px;
}
.info-label {
  color: var(--text-muted);
  min-width: 48px;
}
.info-value {
  color: var(--text-primary);
}
.applied-hint {
  font-size: 13px;
}
.image-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 40px 0;
  color: var(--text-muted);
  font-size: 14px;
}

/* ======== 实时预览 ======== */
.preview-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.preview-stage {
  position: relative;
  width: 100%;
  height: 260px;
  border-radius: 10px;
  overflow: hidden;
  border: 0.5px solid rgba(255, 255, 255, 0.08);
  transition: background 0.5s ease;
}
.preview-overlay {
  position: absolute;
  inset: 0;
  background: rgba(10, 10, 15, 0.35);
  backdrop-filter: blur(4px);
  pointer-events: none;
}
.preview-content {
  position: relative;
  display: flex;
  height: 100%;
  z-index: 1;
}
.preview-sidebar {
  width: 56px;
  background: rgba(18, 26, 46, 0.7);
  backdrop-filter: blur(8px);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 4px;
  gap: 10px;
  border-right: 0.5px solid rgba(255, 255, 255, 0.06);
}
.preview-logo {
  font-size: 20px;
  margin-bottom: 8px;
}
.preview-menu-item {
  font-size: 9px;
  color: rgba(200, 214, 229, 0.5);
  padding: 4px 6px;
  border-radius: 4px;
  text-align: center;
  width: 48px;
  line-height: 1.2;
}
.preview-menu-active {
  background: rgba(64, 158, 255, 0.15);
  color: #409eff;
}
.preview-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 10px;
}
.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 28px;
  margin-bottom: 8px;
}
.preview-breadcrumb {
  width: 120px;
  height: 10px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}
.preview-user {
  width: 40px;
  height: 10px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}
.preview-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.preview-card-row {
  display: flex;
  gap: 8px;
}
.preview-card {
  flex: 1;
  height: 40px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 6px;
  border: 0.5px solid rgba(255, 255, 255, 0.06);
}
.preview-table {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 6px;
}
.preview-table-row {
  flex: 1;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 4px;
  border: 0.5px solid rgba(255, 255, 255, 0.04);
}
.preview-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
}
</style>
