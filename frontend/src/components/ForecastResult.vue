<script setup>
import { Activity, AlertTriangle, BrainCircuit, Clock, CloudRain, Hash, Server } from 'lucide-vue-next'

defineProps({
  response: {
    type: Object,
    default: null
  },
  busy: {
    type: Boolean,
    default: false
  },
  activeOperation: {
    type: Object,
    default: null
  },
  errorMessage: {
    type: String,
    default: ''
  }
})

function elapsedSeconds(operation) {
  return operation ? Math.floor(operation.elapsedMs / 1000) : 0
}
</script>

<template>
  <section class="panel result-panel">
    <header class="panel-header">
      <div>
        <p class="eyebrow">Output</p>
        <h2>预报结果</h2>
      </div>
      <span v-if="busy" class="status-pill warn">RUNNING</span>
      <span v-else-if="response" class="status-pill ok">READY</span>
    </header>

    <div v-if="errorMessage" class="error-box">
      <AlertTriangle :size="18" />
      <span>{{ errorMessage }}</span>
    </div>

    <div v-if="activeOperation" class="running-banner">
      <span class="spinner"></span>
      <div>
        <strong>{{ activeOperation.label }}</strong>
        <small>{{ activeOperation.phase }} · {{ elapsedSeconds(activeOperation) }}s elapsed</small>
      </div>
    </div>

    <div v-if="response || activeOperation" class="metric-grid">
      <div class="metric">
        <Hash :size="16" />
        <span>Version</span>
        <strong>{{ response ? response.version : 'pending' }}</strong>
      </div>
      <div class="metric">
        <BrainCircuit :size="16" />
        <span>Intent</span>
        <strong>{{ response?.intent?.intent || (activeOperation ? 'detecting' : '-') }}</strong>
      </div>
      <div class="metric">
        <Server :size="16" />
        <span>Model</span>
        <strong>{{ response?.aiResponse?.modelName || (activeOperation ? 'qwen3.6-flash' : '-') }}</strong>
      </div>
      <div class="metric">
        <Clock :size="16" />
        <span>Latency</span>
        <strong>{{ response?.aiResponse?.latencyMs ? `${response.aiResponse.latencyMs}ms` : `${activeOperation?.elapsedMs || 0}ms` }}</strong>
      </div>
    </div>

    <div v-if="response?.evaluation" class="quality-bar">
      <div>
        <span>Evaluation</span>
        <strong>{{ response.evaluation.score }}</strong>
      </div>
      <progress :value="response.evaluation.score" max="100" />
      <span :class="response.evaluation.passed ? 'quality-pass' : 'quality-fail'">
        {{ response.evaluation.passed ? 'PASSED' : 'REVIEW' }}
      </span>
    </div>

    <article v-if="response?.aiResponse?.content" class="forecast-output">
      <div class="section-title">
        <CloudRain :size="16" />
        <span>AI Forecast</span>
      </div>
      <pre>{{ response.aiResponse.content }}</pre>
    </article>

    <div v-else-if="activeOperation" class="loading-forecast">
      <div class="loading-line wide"></div>
      <div class="loading-line"></div>
      <div class="loading-line short"></div>
      <div class="loading-block"></div>
      <span>Waiting for model response</span>
    </div>

    <div v-if="response?.aiResponse?.structuredOutput" class="structured-output">
      <div class="section-title">
        <Activity :size="16" />
        <span>Structured Output</span>
      </div>
      <div class="structured-grid">
        <div>
          <span>Regions</span>
          <strong>{{ response.aiResponse.structuredOutput.affectedRegions?.join('、') || '-' }}</strong>
        </div>
        <div>
          <span>Risks</span>
          <strong>{{ response.aiResponse.structuredOutput.riskSignals?.join('、') || '-' }}</strong>
        </div>
      </div>
    </div>

    <div v-if="!response && !busy && !activeOperation" class="empty-state">
      <CloudRain :size="34" />
      <span>等待生成</span>
    </div>
  </section>
</template>
