import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const STORAGE_KEY = 'smart_lighting_bg'

/**
 * 预设渐变背景（5 套主题）
 */
export const presetGradients = [
  { name: '深海蓝', desc: '深邃沉稳', gradient: 'linear-gradient(135deg, #0a1628 0%, #111d2e 50%, #0d1a2c 100%)' },
  { name: '暗夜紫', desc: '神秘高雅', gradient: 'linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%)' },
  { name: '极光绿', desc: '清新自然', gradient: 'linear-gradient(135deg, #0b1a1e 0%, #1a3a2a 50%, #0d2818 100%)' },
  { name: '熔岩橙', desc: '温暖活力', gradient: 'linear-gradient(135deg, #1a0e0a 0%, #3a1a0a 50%, #2a1005 100%)' },
  { name: '暗夜蓝', desc: '科技冷峻', gradient: 'linear-gradient(135deg, #0a1628 0%, #1a2744 50%, #0d1a30 100%)' }
]

/**
 * 从 localStorage 读取持久化的背景配置
 */
function loadPersisted() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw)
  } catch { /* 静默 */ }
  return null
}

/**
 * 写入 localStorage
 */
function persist(data) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

export const useBackgroundStore = defineStore('background', () => {
  const saved = loadPersisted()

  // 背景模式：'gradient' | 'image'
  const mode = ref(saved?.mode || 'gradient')
  // 预设索引
  const presetIndex = ref(saved?.presetIndex ?? 0)
  // 用户上传图片的 base64 data URL
  const imageData = ref(saved?.imageData || '')

  // 计算属性：当前背景样式的 CSS background 值
  const backgroundStyle = computed(() => {
    if (mode.value === 'image' && imageData.value) {
      return `url(${imageData.value}) center / cover no-repeat fixed`
    }
    return presetGradients[presetIndex.value]?.gradient || presetGradients[0].gradient
  })

  function save() {
    persist({
      mode: mode.value,
      presetIndex: presetIndex.value,
      imageData: imageData.value
    })
  }

  // 设置预设渐变
  function setPreset(index) {
    if (index < 0 || index >= presetGradients.length) return
    mode.value = 'gradient'
    presetIndex.value = index
    save()
  }

  // 设置自定义图片
  function setImage(dataUrl) {
    mode.value = 'image'
    imageData.value = dataUrl
    save()
  }

  // 清空图片，回到渐变模式
  function clearImage() {
    imageData.value = ''
    mode.value = 'gradient'
    save()
  }

  // 重置为默认渐变
  function reset() {
    mode.value = 'gradient'
    presetIndex.value = 0
    imageData.value = ''
    save()
  }

  return {
    mode,
    presetIndex,
    imageData,
    backgroundStyle,
    setPreset,
    setImage,
    clearImage,
    reset
  }
})
