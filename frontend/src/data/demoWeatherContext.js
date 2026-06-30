export const demoRequest = {
  sessionId: 's-ui-demo-001',
  style: 'FORMAL',
  outputFormat: 'STANDARD_FORECAST',
  weatherContext: {
    city: '深圳',
    forecastTime: '2026-06-30T10:00:00+08:00',
    validPeriod: '未来3小时',
    rainForecast: {
      level: '中到大雨',
      amountRange: '10-30毫米',
      peakPeriod: '10:30-12:00',
      trend: '逐渐增强后减弱',
      confidence: 0.82
    },
    regionForecasts: [
      {
        regionName: '南山区',
        rainLevel: '大雨',
        startTime: '2026-06-30T10:30:00+08:00',
        endTime: '2026-06-30T12:00:00+08:00',
        impact: '局地道路积水风险较高'
      },
      {
        regionName: '福田区',
        rainLevel: '中到大雨',
        startTime: '2026-06-30T10:20:00+08:00',
        endTime: '2026-06-30T11:50:00+08:00',
        impact: '早高峰后段交通通行压力增加'
      }
    ],
    riskSignals: ['短时强降水', '道路积水风险', '雷电'],
    dataSource: '深圳气象业务系统'
  }
}

export function createDemoRequest() {
  return JSON.parse(JSON.stringify(demoRequest))
}
