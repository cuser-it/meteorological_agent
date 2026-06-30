<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import WeatherInputPanel from './components/WeatherInputPanel.vue'
import ForecastResult from './components/ForecastResult.vue'
import InspectorPanel from './components/InspectorPanel.vue'
import { createDemoRequest } from './data/demoWeatherContext'
import {
  fetchHistory,
  fetchTrace,
  health,
  normalizeApiBase,
  renderPrompt,
  streamGenerateForecast,
  streamRewriteForecast
} from './api/meteorologicalApi'

const apiBase = ref(normalizeApiBase(localStorage.getItem('meteorological-agent-api-base')))
const requestJson = ref(JSON.stringify(createDemoRequest(), null, 2))
const rewriteMessage = ref('简单一点，并增加风险提示')
const response = ref(null)
const trace = ref(null)
const prompt = ref(null)
const history = ref(null)
const activeTab = ref('trace')
const backendStatus = ref('UNKNOWN')
const busy = ref(false)
const errorMessage = ref('')
const activeOperation = ref(null)
const liveTrace = ref(null)
const streamingContent = ref('')
let operationClock = null
let liveStepTimers = []

const canRewrite = computed(() => Boolean(response.value?.conversationId && response.value?.sessionId))
const visibleTrace = computed(() => liveTrace.value || trace.value)
const visibleRaw = computed(() => response.value || activeOperation.value)
const visibleResponse = computed(() => {
  if (!streamingContent.value) {
    return response.value
  }
  return {
    ...(response.value || {}),
    version: response.value?.version || 'streaming',
    intent: response.value?.intent || { intent: activeOperation.value?.phase || 'STREAMING' },
    aiResponse: {
      ...(response.value?.aiResponse || {}),
      content: streamingContent.value,
      modelName: response.value?.aiResponse?.modelName || 'qwen3.6-flash-2026-04-16',
      latencyMs: activeOperation.value?.elapsedMs || 0
    }
  }
})

function persistApiBase() {
  apiBase.value = normalizeApiBase(apiBase.value)
  localStorage.setItem('meteorological-agent-api-base', apiBase.value)
}

function parseRequestJson() {
  try {
    return JSON.parse(requestJson.value)
  } catch (error) {
    throw new Error(`WeatherContext JSON 格式错误：${error.message}`)
  }
}

function nowIso() {
  return new Date().toISOString()
}

function clearLiveTimers() {
  if (operationClock) {
    clearInterval(operationClock)
    operationClock = null
  }
  liveStepTimers.forEach((timer) => clearTimeout(timer))
  liveStepTimers = []
}

function startLiveWorkflow({ workflowType, sessionId, label, steps }) {
  clearLiveTimers()
  streamingContent.value = ''
  const startedAt = Date.now()
  activeOperation.value = {
    label,
    workflowType,
    phase: 'Request sent',
    startedAt,
    elapsedMs: 0
  }
  liveTrace.value = {
    traceId: 'pending',
    conversationId: response.value?.conversationId || null,
    sessionId,
    workflowType,
    status: 'RUNNING',
    startedAt: nowIso(),
    finishedAt: null,
    latencyMs: 0,
    steps: steps.map((stepName) => ({
      stepName,
      status: 'PENDING',
      startedAt: null,
      finishedAt: null,
      latencyMs: 0,
      attributes: {}
    })),
    metadata: {
      source: 'frontend-live-progress'
    }
  }
  activeTab.value = 'trace'

  operationClock = setInterval(() => {
    const elapsedMs = Date.now() - startedAt
    if (activeOperation.value) {
      activeOperation.value.elapsedMs = elapsedMs
    }
    if (liveTrace.value) {
      liveTrace.value.latencyMs = elapsedMs
      const runningStep = liveTrace.value.steps.find((step) => step.status === 'RUNNING')
      if (runningStep?.startedAt) {
        runningStep.latencyMs = Date.now() - Date.parse(runningStep.startedAt)
      }
    }
  }, 250)

  steps.forEach((stepName, index) => {
    const delay = index < 3 ? index * 450 : 1350
    const timer = setTimeout(() => {
      if (!liveTrace.value || liveTrace.value.status !== 'RUNNING') {
        return
      }
      liveTrace.value.steps.forEach((step, stepIndex) => {
        if (stepIndex < index && step.status === 'RUNNING') {
          step.status = 'SUCCESS'
          step.finishedAt = nowIso()
          step.latencyMs = Math.max(0, Date.parse(step.finishedAt) - Date.parse(step.startedAt))
        }
      })
      const current = liveTrace.value.steps[index]
      if (current && current.status === 'PENDING') {
        current.status = 'RUNNING'
        current.startedAt = nowIso()
        activeOperation.value.phase = stepName
      }
    }, delay)
    liveStepTimers.push(timer)
  })
}

function completeLiveWorkflow() {
  clearLiveTimers()
  liveTrace.value = null
  activeOperation.value = null
  streamingContent.value = ''
}

function failLiveWorkflow(message) {
  clearLiveTimers()
  if (activeOperation.value) {
    activeOperation.value.phase = 'Failed'
  }
  if (liveTrace.value) {
    liveTrace.value.status = 'FAILED'
    liveTrace.value.finishedAt = nowIso()
    liveTrace.value.metadata = {
      ...liveTrace.value.metadata,
      error: message
    }
    const runningStep = liveTrace.value.steps.find((step) => step.status === 'RUNNING')
    if (runningStep) {
      runningStep.status = 'FAILED'
      runningStep.finishedAt = nowIso()
      runningStep.latencyMs = Math.max(0, Date.parse(runningStep.finishedAt) - Date.parse(runningStep.startedAt))
    }
  }
  activeOperation.value = null
}

function applyStreamStep(stepEvent) {
  if (!liveTrace.value || !stepEvent?.stepName) {
    return
  }
  const step = liveTrace.value.steps.find((item) => item.stepName === stepEvent.stepName)
  if (!step) {
    return
  }
  step.status = stepEvent.status || step.status
  step.latencyMs = stepEvent.latencyMs ?? step.latencyMs
  step.attributes = stepEvent.attributes || step.attributes
  if (step.status === 'RUNNING' && !step.startedAt) {
    step.startedAt = nowIso()
  }
  if ((step.status === 'SUCCESS' || step.status === 'FAILED') && !step.finishedAt) {
    step.finishedAt = nowIso()
  }
  if (activeOperation.value) {
    activeOperation.value.phase = step.stepName
  }
}

function streamHandlers() {
  return {
    workflow: (data) => {
      if (liveTrace.value) {
        liveTrace.value.traceId = data.traceId || liveTrace.value.traceId
        liveTrace.value.workflowType = data.workflowType || liveTrace.value.workflowType
      }
    },
    step: applyStreamStep,
    intent: (data) => {
      response.value = {
        ...(response.value || {}),
        intent: data
      }
    },
    prompt: (data) => {
      prompt.value = {
        ...(prompt.value || {}),
        ...data
      }
    },
    delta: (data) => {
      streamingContent.value += data.content || ''
      if (activeOperation.value) {
        activeOperation.value.phase = 'llm-call streaming'
      }
    },
    error: (data) => {
      throw new Error(data.message || data.code || 'stream failed')
    }
  }
}

async function runWithState(action) {
  busy.value = true
  errorMessage.value = ''
  persistApiBase()
  try {
    await action()
  } catch (error) {
    errorMessage.value = error.payload?.message || error.message
  } finally {
    busy.value = false
  }
}

async function checkHealth() {
  await runWithState(async () => {
    const result = await health(apiBase.value)
    backendStatus.value = result.data?.status || 'UNKNOWN'
  })
}

function loadDemo() {
  requestJson.value = JSON.stringify(createDemoRequest(), null, 2)
  rewriteMessage.value = '简单一点，并增加风险提示'
  errorMessage.value = ''
}

async function refreshTraceAndHistory(nextResponse) {
  if (nextResponse.trace?.traceId) {
    const traceResult = await fetchTrace(apiBase.value, nextResponse.trace.traceId)
    trace.value = traceResult.data
  } else {
    trace.value = nextResponse.trace || null
  }
  if (nextResponse.conversationId && nextResponse.sessionId) {
    const historyResult = await fetchHistory(apiBase.value, nextResponse.conversationId, nextResponse.sessionId, true)
    history.value = historyResult.data
  }
}

async function handleGenerate() {
  busy.value = true
  errorMessage.value = ''
  persistApiBase()
  const request = parseRequestJson()
  startLiveWorkflow({
    workflowType: 'WEATHER_GENERATE',
    sessionId: request.sessionId,
    label: 'Generating forecast',
    steps: ['validate-weather-context', 'intent-detection', 'prompt-render', 'llm-call', 'evaluation', 'memory-save']
  })
  renderPrompt(apiBase.value, {
    promptName: 'generate',
    style: request.style,
    outputFormat: request.outputFormat,
    intent: 'GENERATE',
    weatherContext: request.weatherContext
  }).then((result) => {
    prompt.value = result.data
  }).catch(() => {})
  try {
    const data = await streamGenerateForecast(apiBase.value, request, streamHandlers())
    response.value = data
    await refreshTraceAndHistory(data)
    completeLiveWorkflow()
    activeTab.value = 'trace'
  } catch (error) {
    const message = error.payload?.message || error.message
    errorMessage.value = message
    failLiveWorkflow(message)
  } finally {
    busy.value = false
  }
}

async function handleRewrite() {
  if (!canRewrite.value) {
    errorMessage.value = '请先生成一版预报'
    return
  }
  busy.value = true
  errorMessage.value = ''
  persistApiBase()
  const request = parseRequestJson()
  startLiveWorkflow({
    workflowType: 'WEATHER_REWRITE',
    sessionId: response.value.sessionId,
    label: 'Rewriting forecast',
    steps: ['memory-load', 'intent-detection', 'prompt-render', 'llm-call', 'evaluation', 'memory-save']
  })
  renderPrompt(apiBase.value, {
    promptName: 'rewrite',
    style: request.style,
    outputFormat: request.outputFormat,
    userInstruction: rewriteMessage.value,
    intent: 'SIMPLIFY',
    weatherContext: request.weatherContext,
    previousResponse: response.value?.aiResponse?.content
  }).then((result) => {
    prompt.value = result.data
  }).catch(() => {})
  try {
    const data = await streamRewriteForecast(apiBase.value, {
      conversationId: response.value.conversationId,
      sessionId: response.value.sessionId,
      message: rewriteMessage.value
    }, streamHandlers())
    response.value = data
    await refreshTraceAndHistory(data)
    completeLiveWorkflow()
    activeTab.value = 'trace'
  } catch (error) {
    const message = error.payload?.message || error.message
    errorMessage.value = message
    failLiveWorkflow(message)
  } finally {
    busy.value = false
  }
}

async function handleRenderPrompt() {
  busy.value = true
  errorMessage.value = ''
  persistApiBase()
  startLiveWorkflow({
    workflowType: 'PROMPT_RENDER',
    sessionId: response.value?.sessionId || 'debug',
    label: 'Rendering prompt',
    steps: ['parse-input', 'prompt-render']
  })
  try {
    const request = parseRequestJson()
    const result = await renderPrompt(apiBase.value, {
      promptName: canRewrite.value ? 'rewrite' : 'generate',
      style: request.style,
      outputFormat: request.outputFormat,
      userInstruction: rewriteMessage.value,
      intent: canRewrite.value ? 'SIMPLIFY' : 'GENERATE',
      weatherContext: request.weatherContext,
      previousResponse: response.value?.aiResponse?.content
    })
    prompt.value = result.data
    completeLiveWorkflow()
    activeTab.value = 'prompt'
  } catch (error) {
    const message = error.payload?.message || error.message
    errorMessage.value = message
    failLiveWorkflow(message)
  } finally {
    busy.value = false
  }
}

onMounted(() => {
  checkHealth()
})

onUnmounted(() => {
  clearLiveTimers()
})
</script>

<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">Shenzhen Meteorological Agent</p>
        <h1>短临预报 AI 工作台</h1>
      </div>
      <div class="topbar-meta">
        <span>{{ response?.conversationId || 'No conversation' }}</span>
        <strong>{{ response ? `v${response.version}` : 'v0' }}</strong>
      </div>
    </header>

    <div class="workspace">
      <WeatherInputPanel
        v-model:api-base="apiBase"
        v-model:request-json="requestJson"
        v-model:rewrite-message="rewriteMessage"
        :can-rewrite="canRewrite"
        :busy="busy"
        :backend-status="backendStatus"
        :active-operation="activeOperation"
        @check-health="checkHealth"
        @load-demo="loadDemo"
        @generate="handleGenerate"
        @rewrite="handleRewrite"
        @render-prompt="handleRenderPrompt"
      />

      <ForecastResult
        :response="visibleResponse"
        :busy="busy"
        :active-operation="activeOperation"
        :error-message="errorMessage"
      />

      <InspectorPanel
        v-model:active-tab="activeTab"
        :response="visibleResponse"
        :trace="visibleTrace"
        :prompt="prompt"
        :history="history"
        :active-operation="activeOperation"
        :raw-payload="visibleRaw"
      />
    </div>
  </main>
</template>
