const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

async function request(apiBase, path, options = {}) {
  const response = await fetch(`${apiBase}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  })
  const payload = await response.json().catch(() => null)
  if (!response.ok || payload?.success === false) {
    const message = payload?.message || `HTTP ${response.status}`
    const error = new Error(message)
    error.payload = payload
    throw error
  }
  return payload
}

export function normalizeApiBase(value) {
  const trimmed = value?.trim()
  const base = !trimmed || trimmed === 'http://127.0.0.1:8080/api' || trimmed === 'http://localhost:8080/api'
    ? DEFAULT_API_BASE
    : trimmed
  return base.endsWith('/') ? base.slice(0, -1) : base
}

export function health(apiBase) {
  return request(apiBase, '/health')
}

export function generateForecast(apiBase, body) {
  return request(apiBase, '/weather/generate', {
    method: 'POST',
    body: JSON.stringify(body)
  })
}

export function rewriteForecast(apiBase, body) {
  return request(apiBase, '/weather/chat', {
    method: 'POST',
    body: JSON.stringify(body)
  })
}

export async function streamGenerateForecast(apiBase, body, handlers) {
  return streamRequest(apiBase, '/weather/generate/stream', body, handlers)
}

export async function streamRewriteForecast(apiBase, body, handlers) {
  return streamRequest(apiBase, '/weather/chat/stream', body, handlers)
}

async function streamRequest(apiBase, path, body, handlers = {}) {
  const response = await fetch(`${apiBase}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream'
    },
    body: JSON.stringify(body)
  })
  if (!response.ok || !response.body) {
    throw new Error(`HTTP ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let completePayload = null

  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n\n')
    buffer = parts.pop() || ''
    for (const part of parts) {
      const event = parseSseEvent(part)
      if (!event) {
        continue
      }
      if (event.name === 'complete') {
        completePayload = event.data
      }
      handlers[event.name]?.(event.data)
    }
  }

  if (buffer.trim()) {
    const event = parseSseEvent(buffer)
    if (event) {
      if (event.name === 'complete') {
        completePayload = event.data
      }
      handlers[event.name]?.(event.data)
    }
  }

  return completePayload
}

function parseSseEvent(raw) {
  const lines = raw.split('\n')
  let name = 'message'
  const dataLines = []
  for (const line of lines) {
    if (line.startsWith('event:')) {
      name = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }
  if (dataLines.length === 0) {
    return null
  }
  const dataText = dataLines.join('\n')
  let data
  try {
    data = JSON.parse(dataText)
  } catch {
    data = dataText
  }
  return { name, data }
}

export function renderPrompt(apiBase, body) {
  return request(apiBase, '/prompts/render', {
    method: 'POST',
    body: JSON.stringify(body)
  })
}

export function fetchHistory(apiBase, conversationId, sessionId, includePrompt = true) {
  const query = new URLSearchParams({
    conversationId,
    sessionId,
    includePrompt: String(includePrompt)
  })
  return request(apiBase, `/conversation/history?${query}`)
}

export function fetchTrace(apiBase, traceId) {
  return request(apiBase, `/traces/${encodeURIComponent(traceId)}`)
}
