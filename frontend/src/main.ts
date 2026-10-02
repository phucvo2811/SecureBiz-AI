import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { authorizeNavigation } from './router/guard'
import StatusBadge from './components/StatusBadge.vue'
import './styles/main.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.component('StatusBadge', StatusBadge)

router.beforeEach(async (to) => {
  const auth = useAuthStore(pinia)
  return authorizeNavigation(to, auth)
})

app.mount('#app')
