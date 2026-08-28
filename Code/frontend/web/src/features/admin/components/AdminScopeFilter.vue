<script setup lang="ts">
import { Filter } from 'lucide-vue-next'

withDefaults(defineProps<{
  ownerUserIds?: string[]
  storeIds?: string[]
  ownerUserId?: string
  storeId?: string
}>(), { ownerUserIds: () => [], storeIds: () => [], ownerUserId: '', storeId: '' })

const emit = defineEmits<{ 'update:ownerUserId': [value: string]; 'update:storeId': [value: string] }>()
</script>

<template>
  <div class="admin-scope-filter" aria-label="管理员授权范围筛选">
    <Filter aria-hidden="true" />
    <label v-if="ownerUserIds.length"><span>Owner</span><select :value="ownerUserId" aria-label="Owner 范围" @change="emit('update:ownerUserId', ($event.target as HTMLSelectElement).value)"><option value="">全部授权 Owner</option><option v-for="id in ownerUserIds" :key="id" :value="id">{{ id }}</option></select></label>
    <label v-if="storeIds.length"><span>门店</span><select :value="storeId" aria-label="门店范围" @change="emit('update:storeId', ($event.target as HTMLSelectElement).value)"><option value="">全部授权门店</option><option v-for="id in storeIds" :key="id" :value="id">{{ id }}</option></select></label>
    <span v-if="!ownerUserIds.length && !storeIds.length">当前会话使用服务端授权范围</span>
  </div>
</template>

<style scoped>
.admin-scope-filter { display:flex; min-height:34px; align-items:center; flex-wrap:wrap; gap:10px; color:#737373; font-size:11px; }
.admin-scope-filter > svg { width:15px; color:#a0a0a0; }
.admin-scope-filter label { display:flex; align-items:center; gap:5px; }
.admin-scope-filter select { max-width:180px; border:0; border-bottom:1px solid #e8e8e5; background:#fff; color:#454545; padding:5px 2px; font-size:11px; }
</style>
