import type { Permission } from '@/entities/auth/roles'

export type ApiMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export interface ApiContract {
  method: ApiMethod
  path: string
  purpose: string
  permission: Permission
  tables: string[]
}

// 业务端点清单随新领域模型重写，本轮仅保留骨架。
export const endpointCatalog: Record<string, ApiContract[]> = {}

export function contractsForRoute(route: string): ApiContract[] {
  return endpointCatalog[route] ?? []
}

export function api(method: ApiMethod, path: string, purpose: string, permission: Permission, tables: string[]): ApiContract {
  return { method, path, purpose, permission, tables }
}
