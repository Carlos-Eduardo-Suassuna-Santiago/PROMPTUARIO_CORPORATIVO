import http from 'k6/http'
import { check, sleep } from 'k6'
import { Counter, Rate, Trend } from 'k6/metrics'

const baseUrl = (__ENV.K6_BASE_URL || 'http://localhost:8000').replace(/\/$/, '')
const apiUrl = `${baseUrl}/api/v1`
const errors = new Rate('promptuario_errors')
const loginDuration = new Trend('login_duration', true)
const listDuration = new Trend('paginated_list_duration', true)
const aiRequests = new Counter('ai_requests')

export const options = {
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1500'],
    login_duration: ['p(95)<1200'],
    paginated_list_duration: ['p(95)<1200'],
    promptuario_errors: ['rate<0.05'],
  },
}

function checkResponse(response, name, expected = 200) {
  const ok = check(response, { [`${name} status ${expected}`]: (r) => r.status === expected })
  errors.add(!ok)
  return ok
}

export default function () {
  const login = http.post(`${apiUrl}/auth/login`, JSON.stringify({
    email: __ENV.K6_EMAIL,
    password: __ENV.K6_PASSWORD,
  }), { headers: { 'Content-Type': 'application/json' } })
  loginDuration.add(login.timings.duration)
  if (!checkResponse(login, 'login')) return

  const token = login.json('access_token')
  if (!token) {
    errors.add(true)
    return
  }
  const headers = { headers: { Authorization: `Bearer ${token}` } }

  const patients = http.get(`${apiUrl}/patients?page=0&size=20`, headers)
  listDuration.add(patients.timings.duration)
  checkResponse(patients, 'patients list')

  const appointments = http.get(`${apiUrl}/appointments?page=0&size=20`, headers)
  listDuration.add(appointments.timings.duration)
  checkResponse(appointments, 'appointments list')

  if (__ENV.K6_PATIENT_ID) {
    const analysis = http.post(`${apiUrl}/ai/analyze`, JSON.stringify({
      analysis_type: 'SYMPTOM_ANALYSIS',
      patient_id: __ENV.K6_PATIENT_ID,
      context: { source: 'k6-load-test' },
    }), { ...headers, headers: { ...headers.headers, 'Content-Type': 'application/json' } })
    aiRequests.add(1)
    checkResponse(analysis, 'AI analysis request', 202)
  }

  sleep(Number(__ENV.K6_SLEEP_SECONDS || 1))
}
