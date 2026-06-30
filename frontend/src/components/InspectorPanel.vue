<script setup>
import { Activity, BrainCircuit, Database, FileText, History, Terminal } from 'lucide-vue-next'

defineProps({
  activeTab: {
    type: String,
    required: true
  },
  response: {
    type: Object,
    default: null
  },
  trace: {
    type: Object,
    default: null
  },
  prompt: {
    type: Object,
    default: null
  },
  history: {
    type: Object,
    default: null
  },
  activeOperation: {
    type: Object,
    default: null
  },
  rawPayload: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:activeTab'])

const tabs = [
  { key: 'trace', label: 'Trace', icon: Activity },
  { key: 'intent', label: 'Intent', icon: BrainCircuit },
  { key: 'prompt', label: 'Prompt', icon: FileText },
  { key: 'history', label: 'History', icon: History },
  { key: 'raw', label: 'Raw', icon: Terminal }
]
</script>

<template>
  <section class="panel inspector-panel">
    <header class="panel-header">
      <div>
        <p class="eyebrow">Inspect</p>
        <h2>工程化视图</h2>
      </div>
    </header>

    <nav class="tab-list" aria-label="Inspector tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        :class="{ active: activeTab === tab.key }"
        @click="emit('update:activeTab', tab.key)"
      >
        <component :is="tab.icon" :size="15" />
        {{ tab.label }}
      </button>
    </nav>

    <div class="inspector-body">
      <div v-if="activeTab === 'trace'" class="trace-list">
        <div v-if="activeOperation" class="running-banner compact">
          <span class="spinner"></span>
          <div>
            <strong>{{ activeOperation.label }}</strong>
            <small>{{ activeOperation.phase }}</small>
          </div>
        </div>
        <div v-if="trace" class="trace-summary">
          <span>{{ trace.workflowType }}</span>
          <strong>{{ trace.status }}</strong>
          <small>{{ trace.latencyMs }}ms</small>
        </div>
        <div
          v-for="step in trace?.steps || []"
          :key="`${step.stepName}-${step.startedAt}`"
          class="trace-step"
          :class="step.status.toLowerCase()"
        >
          <span class="step-dot"></span>
          <div>
            <strong>{{ step.stepName }}</strong>
            <small>{{ step.status }} · {{ step.latencyMs }}ms</small>
          </div>
        </div>
        <p v-if="!trace" class="muted">No trace selected.</p>
      </div>

      <div v-else-if="activeTab === 'intent'" class="json-block">
        <pre>{{ JSON.stringify(response?.intent || activeOperation || {}, null, 2) }}</pre>
      </div>

      <div v-else-if="activeTab === 'prompt'" class="prompt-view">
        <div v-if="prompt" class="prompt-meta">
          <span>{{ prompt.promptName }}</span>
          <strong>{{ prompt.promptVersion }}</strong>
          <small>{{ prompt.promptLength }} chars</small>
        </div>
        <div v-if="prompt" class="module-list">
          <span v-for="name in prompt.moduleNames" :key="name">{{ name }}</span>
        </div>
        <pre v-if="prompt">{{ prompt.userPrompt }}</pre>
        <p v-else class="muted">No rendered prompt.</p>
      </div>

      <div v-else-if="activeTab === 'history'" class="history-list">
        <div v-if="history" class="trace-summary">
          <span>{{ history.conversationId }}</span>
          <strong>v{{ history.currentVersion }}</strong>
          <small>{{ history.status }}</small>
        </div>
        <div v-for="message in history?.messages || []" :key="message.messageId" class="history-item">
          <Database :size="15" />
          <div>
            <strong>{{ message.role }} · v{{ message.version }}</strong>
            <small>{{ message.intent || '-' }} · {{ message.modelName || '-' }}</small>
            <p>{{ message.content }}</p>
          </div>
        </div>
        <p v-if="!history" class="muted">No conversation history.</p>
      </div>

      <div v-else class="json-block">
        <pre>{{ JSON.stringify(rawPayload || {}, null, 2) }}</pre>
      </div>
    </div>
  </section>
</template>
