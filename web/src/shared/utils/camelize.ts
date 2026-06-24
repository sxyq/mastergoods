export function camelize(value: unknown): unknown {
  if (Array.isArray(value)) {
    const result = new Array(value.length)
    for (let index = 0; index < value.length; index += 1) {
      result[index] = camelize(value[index])
    }
    return result
  }
  if (!value || typeof value !== 'object') {
    return value
  }
  const record = value as Record<string, unknown>
  const result: Record<string, unknown> = {}
  for (const key in record) {
    if (!Object.prototype.hasOwnProperty.call(record, key)) continue
    const normalizedKey = key.replace(/_([a-z])/g, (_, letter: string) => letter.toUpperCase())
    result[normalizedKey] = camelize(record[key])
  }
  return result
}
