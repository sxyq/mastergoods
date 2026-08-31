import { createApp } from 'vue'
import App from './App.vue'
import { router } from './app/router/routes'
import './style.css'

createApp(App).use(router).mount('#app')
