import type { AdminPermission, AdminRole, AdminSession } from '@/entities/admin/contracts'

export const adminApiPaths = {
  session: '/v2/admin/session',
  overview: '/v2/admin/overview',
  users: '/v2/admin/users',
  stores: '/v2/admin/stores',
  agentRuns: '/v2/admin/agent/runs',
  agentUsage: '/v2/admin/agent/usage',
  auditEvents: '/v2/admin/audit/events',
  systemHealth: '/v2/admin/system/health',
  exports: '/v2/admin/exports',
  retention: '/v2/admin/retention',
} as const

export interface AdminSessionContract extends AdminSession {
  role: AdminRole
  permissions: AdminPermission[]
}

/**
 * Admin API paths and DTO boundary. Request functions are intentionally added
 * with the authenticated admin session implementation in the I1 batch.
 */
export const adminApiContract = Object.freeze(adminApiPaths)
