import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const repositoryRoot = process.cwd()
const sourcePath = resolve(repositoryRoot, 'Code/frontend/web/src/shared/api/client.ts')
const source = readFileSync(sourcePath, 'utf8')
const payOrderBodyStart = source.indexOf('function toPayOrderBody')
assert.notEqual(payOrderBodyStart, -1, 'toPayOrderBody must remain the single pay-order request mapper')
const payOrderBody = source.slice(payOrderBodyStart)

assert.match(source, /export interface PayOrderCreatePayload \{[\s\S]*?idempotencyKey\?: string \| null/)
assert.match(source, /request<PayOrder>\('\/v2\/pay-orders'/)
assert.match(payOrderBody, /idempotency_key: payload\.idempotencyKey\?\.trim\(\) \|\| createPayOrderIdempotencyKey\(\)/)
assert.match(payOrderBody, /supplier_id: payload\.supplierId \?\? null/)
assert.match(payOrderBody, /account_id: payload\.accountId \?\? null/)
assert.doesNotMatch(payOrderBody, /Number\(payload\.(supplierId|accountId)\)/)
assert.match(source, /conversation_id: payload\.conversationId \?\? null/)
assert.doesNotMatch(source, /['"`]\/v2\/payments(?:['"`]|\$\{)/)

console.log('WEB-CLIENT-CONTRACT: Passed')
console.log('  pay-order path: /v2/pay-orders')
console.log('  idempotency_key: generated or caller-supplied and retained in request body')
console.log('  entity IDs: forwarded without Number coercion')
console.log('  conversation_id: snake_case')
