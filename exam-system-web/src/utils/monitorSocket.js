import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

function wsBaseUrl() {
  const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:'
  return `${protocol}//${window.location.host}/ws`
}

export function connectExamMonitor(token, examId, onSummaryUpdate) {
  if (!token || !examId) return () => {}

  const client = new Client({
    webSocketFactory: () => new SockJS(wsBaseUrl()),
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(`/topic/exam/${examId}/monitor`, message => {
        try {
          const summary = JSON.parse(message.body)
          onSummaryUpdate(summary)
        } catch {
          // ignore malformed payloads
        }
      })
    }
  })

  client.activate()
  return () => {
    client.deactivate()
  }
}
