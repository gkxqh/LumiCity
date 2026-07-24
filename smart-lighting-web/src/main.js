import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { useUserStore } from './store/user'
import './style.css'

const app = createApp(App)

// 注册 Element Plus 所有图标为全局组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 按钮级权限指令：v-hasPerm="'system:role:add'"
// 当用户权限集合不包含所需标识时，隐藏该元素
app.directive('hasPerm', {
  mounted(el, binding) {
    const required = binding.value
    if (!required) return
    const store = useUserStore()
    const owned = store.perms || []
    const roles = store.roles || []
    // ADMIN 视为超级用户，按钮全显（与后端 JwtInterceptor 超级放行对称）
    const isAdmin = roles.includes('ADMIN')
    const list = Array.isArray(required) ? required : [required]
    const allowed = isAdmin || list.some(p => owned.includes(p))
    if (!allowed) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
})

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
