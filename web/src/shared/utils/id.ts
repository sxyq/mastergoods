export type EntityId = string | number

export function readQueryId(value: string | null | Array<string | null> | undefined) {
  const raw = Array.isArray(value) ? value[0] : value
  if (typeof raw !== 'string') return null
  const normalized = raw.trim()
  const length = normalized.length
  if (length === 0) return null
  for (let index = 0; index < length; index += 1) {
    const code = normalized.charCodeAt(index)
    if (code < 48 || code > 57) return null
  }
  return BigInt(normalized) > 0n ? normalized : null
}

export function sameEntityId(left: EntityId | null | undefined, right: EntityId | null | undefined) {
  if (left === null || left === undefined || right === null || right === undefined) {
    return false
  }
  return String(left) === String(right)
}
