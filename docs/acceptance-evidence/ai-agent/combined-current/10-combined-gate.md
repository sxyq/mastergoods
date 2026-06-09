# AI Agent Combined Evidence Gate

Status: `partial-combined-chat-evidence`

- Scenario: `chat`
- Interface evidence: `docs/acceptance-evidence/ai-agent/20260609-0740-4a6bc46e-7e10-4b9c-b680-97f2cbee51b7`
- Device evidence: `docs/acceptance-evidence/ai-agent/20260609-075707-device-ai-chat`
- Interface status: `pass-for-interface`
- Device status: `blocked-by-locked-device`
- Provider model_stream observed: `false`
- Stream ordering risk observed: `false`

Reasons:

- device status is `blocked-by-locked-device`, expected `pass-for-device-ai-chat-evidence`
- provider `model_stream` was not observed; ChatGPT-like streaming remains partial

This combined gate intentionally does not replace manual review of screenshots, raw SSE, audit JSON, logcat, or frame timing.
It prevents interface-only or device-only evidence from being used as a full acceptance claim.
