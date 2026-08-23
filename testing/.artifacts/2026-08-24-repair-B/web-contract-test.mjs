import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const stream = readFileSync('Code/frontend/web/src/shared/api/agent-stream.ts', 'utf8')
const client = readFileSync('Code/frontend/web/src/shared/api/client.ts', 'utf8')

assert.match(stream, /Last-Event-ID/)
assert.match(stream, /parseAgentStreamEvent/)
assert.match(stream, /acceptAgentStreamEvent/)
assert.match(stream, /terminalSeen/)
assert.match(client, /Idempotency-Key/)
assert.match(client, /confirmAgentDraft\(token: string, id: EntityId, idempotencyKey\?/)
console.log('WEB-AGENT-STREAM-CONTRACT: Passed')
