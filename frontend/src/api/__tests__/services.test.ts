import { beforeEach, describe, expect, it, vi } from 'vitest'

const { api } = vi.hoisted(() => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('@/api/client', () => ({ default: api }))

import { appointmentsApi, authApi, patientsApi, aiApi, auditApi } from '@/api/services'

describe('API service contracts', () => {
  beforeEach(() => vi.clearAllMocks())

  it('maps authentication methods and returns response data', async () => {
    api.post.mockResolvedValue({ data: { access_token: 'a' } })
    api.get.mockResolvedValue({ data: { id: 'u1' } })

    await expect(authApi.login({ email: 'a@b.com', password: 'secret' })).resolves.toEqual({ access_token: 'a' })
    await expect(authApi.me()).resolves.toEqual({ id: 'u1' })
    await authApi.logout('refresh')
    expect(api.post).toHaveBeenCalledWith('/auth/logout', { refresh_token: 'refresh' })
  })

  it('sends patient filters and mutation payloads to canonical paths', async () => {
    api.get.mockResolvedValue({ data: { items: [], total: 0 } })
    api.post.mockResolvedValue({ data: { id: 'p1' } })

    await patientsApi.list({ page: 2, size: 10, search: 'Maria' })
    expect(api.get).toHaveBeenCalledWith('/patients', { params: { page: 2, size: 10, search: 'Maria' } })
    await patientsApi.addAllergy('p1', { substance: 'Dipirona', severity: 'MODERATE' } as never)
    expect(api.post).toHaveBeenCalledWith('/patients/p1/allergies', expect.anything())
  })

  it('sends appointment filters and cancellation data', async () => {
    api.get.mockResolvedValue({ data: { items: [] } })
    api.patch.mockResolvedValue({ data: { status: 'CANCELLED' } })

    await appointmentsApi.list({ status: 'CONFIRMED', patient_id: 'p1' })
    expect(api.get).toHaveBeenCalledWith('/appointments', { params: expect.objectContaining({ status: 'CONFIRMED', patientId: 'p1' }) })
    await appointmentsApi.cancel('a1', 'Conflito')
    expect(api.patch).toHaveBeenCalledWith('/appointments/a1/cancel', { reason: 'Conflito' })
  })

  it('submits AI analysis and audit filters', async () => {
    api.post.mockResolvedValue({ data: { id: 'job1' } })
    api.get.mockResolvedValue({ data: { total: 0 } })

    await aiApi.analyze({ patient_id: 'p1', analysis_type: 'SYMPTOM_ANALYSIS', context: {} } as never)
    expect(api.post).toHaveBeenCalledWith('/ai/analyze', expect.anything())
    await auditApi.logs({ service: 'IAM', operation: 'LOGIN' })
    expect(api.get).toHaveBeenCalledWith('/audit/logs', { params: { service: 'IAM', operation: 'LOGIN' } })
  })
})
