<script setup>
import { CloudSun, FileText, Play, RefreshCw, RotateCcw, Send } from 'lucide-vue-next'

defineProps({
  apiBase: {
    type: String,
    required: true
  },
  requestJson: {
    type: String,
    required: true
  },
  rewriteMessage: {
    type: String,
    required: true
  },
  canRewrite: {
    type: Boolean,
    default: false
  },
  busy: {
    type: Boolean,
    default: false
  },
  backendStatus: {
    type: String,
    default: 'UNKNOWN'
  },
  activeOperation: {
    type: Object,
    default: null
  }
})

const emit = defineEmits([
  'update:apiBase',
  'update:requestJson',
  'update:rewriteMessage',
  'checkHealth',
  'loadDemo',
  'generate',
  'rewrite',
  'renderPrompt'
])

const quickMessages = [
  '简单一点',
  '专业一点',
  '再正式一点',
  '把雨量调大一点',
  '增加风险提示'
]

function elapsedSeconds(operation) {
  return operation ? Math.floor(operation.elapsedMs / 1000) : 0
}
</script>

<template>
  <section class="panel input-panel">
    <header class="panel-header">
      <div>
        <p class="eyebrow">Input</p>
        <h2>气象数据</h2>
      </div>
      <span class="status-pill" :class="backendStatus === 'UP' ? 'ok' : 'warn'">
        {{ backendStatus }}
      </span>
    </header>

    <label class="field-label" for="api-base">API Base</label>
    <div class="inline-control">
      <input
        id="api-base"
        :value="apiBase"
        spellcheck="false"
        @input="emit('update:apiBase', $event.target.value)"
      />
      <button class="icon-button" type="button" :disabled="busy" title="检查后端" @click="emit('checkHealth')">
        <RefreshCw :size="16" />
      </button>
    </div>

    <div class="section-title">
      <CloudSun :size="16" />
      <span>WeatherContext</span>
    </div>
    <textarea
      class="json-editor"
      :value="requestJson"
      spellcheck="false"
      @input="emit('update:requestJson', $event.target.value)"
    />

    <div class="button-row">
      <button class="secondary-button" type="button" :disabled="busy" @click="emit('loadDemo')">
        <RotateCcw :size="16" />
        Demo Data
      </button>
      <button class="secondary-button" type="button" :disabled="busy" @click="emit('renderPrompt')">
        <FileText :size="16" />
        Render Prompt
      </button>
      <button class="primary-button" type="button" :disabled="busy" @click="emit('generate')">
        <Play :size="16" />
        {{ activeOperation?.workflowType === 'WEATHER_GENERATE' ? 'Generating' : 'Generate' }}
      </button>
    </div>

    <div v-if="activeOperation" class="operation-card">
      <span class="spinner"></span>
      <div>
        <strong>{{ activeOperation.label }}</strong>
        <small>{{ activeOperation.phase }} · {{ elapsedSeconds(activeOperation) }}s</small>
      </div>
    </div>

    <div class="section-title">
      <Send :size="16" />
      <span>连续改写</span>
    </div>
    <div class="quick-actions">
      <button
        v-for="message in quickMessages"
        :key="message"
        type="button"
        :disabled="busy || !canRewrite"
        @click="emit('update:rewriteMessage', message)"
      >
        {{ message }}
      </button>
    </div>
    <textarea
      class="rewrite-input"
      :value="rewriteMessage"
      spellcheck="false"
      placeholder="简单一点，并增加风险提示"
      @input="emit('update:rewriteMessage', $event.target.value)"
    />
    <button class="primary-button full" type="button" :disabled="busy || !canRewrite" @click="emit('rewrite')">
      <Send :size="16" />
      {{ activeOperation?.workflowType === 'WEATHER_REWRITE' ? 'Rewriting' : 'Rewrite' }}
    </button>
  </section>
</template>
