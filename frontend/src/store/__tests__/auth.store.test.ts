import { beforeEach, describe, expect, it, vi } from 'vitest'

const { storage, authApi } = vi.hoisted(() => ({
  storage: {
    access: null as string | null,
    refresh: null as string | null,
    getAccess: vi.fn(() => storage.access),
    getRefresh: vi.fn(() => storage.refresh),
    set: vi.fn((tokens: { access_token: string; refresh_token: string }) => {
      storage.access = tokens.access_token
      storage.refresh = tokens.refresh_token
    }),
    clear: vi.fn(() => {
      storage.access = null
      storage.refresh = null
    }),
  },
  authApi: { login: vi.fn(), logout: vi.fn(), me: vi.fn() },
}))

vi.mock('@/api/client', () => ({ tokenStorage: storage }))
vi.mock('@/api/services', () => ({ authApi }))

import { hasRole, useAuthStore } from '@/store/auth.store'

const token = (role = 'ADMIN', exp = Math.floor(Date.now() / 1000) + 3600) =>
  `header.${btoa(JSON.stringify({ role, exp }))}.signature`

const user = { id: 'u1', email: 'admin@example.com', full_name: 'Admin', role: 'ADMIN', is_active: true, created_at: '', updated_at: '' } as const

describe('auth store', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    storage.access = null
    storage.refresh = null
    useAuthStore.setState({ user: null, role: null, isAuthenticated: false, isLoading: false })
  })

  it('checks allowed roles safely', () => {
    expect(hasRole('ADMIN', 'ADMIN')).toBe(true)
    expect(hasRole('PATIENT', 'ADMIN')).toBe(false)
    expect(hasRole(null, 'ADMIN')).toBe(false)
  })

  it('logs in, decodes role and loads current user', async () => {
    authApi.login.mockResolvedValue({ access_token: token(), refresh_token: 'r' })
    authApi.me.mockResolvedValue(user)

    await useAuthStore.getState().login('admin@example.com', 'secret')

    expect(storage.clear).toHaveBeenCalled()
    expect(useAuthStore.getState()).toMatchObject({ user, role: 'ADMIN', isAuthenticated: true, isLoading: false })
  })

  it('clears loading state when login fails', async () => {
    authApi.login.mockRejectedValue(new Error('unauthorized'))
    await expect(useAuthStore.getState().login('x', 'bad')).rejects.toThrow('unauthorized')
    expect(useAuthStore.getState().isLoading).toBe(false)
  })

  it('loads a valid token and ignores missing or expired tokens', async () => {
    await useAuthStore.getState().loadUser()
    expect(useAuthStore.getState().isAuthenticated).toBe(false)

    storage.access = token('PATIENT')
    authApi.me.mockResolvedValue({ ...user, role: 'PATIENT' })
    await useAuthStore.getState().loadUser()
    expect(useAuthStore.getState()).toMatchObject({ role: 'PATIENT', isAuthenticated: true })

    storage.access = token('ADMIN', Math.floor(Date.now() / 1000) - 10)
    await useAuthStore.getState().loadUser()
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
  })

  it('logs out with and without a refresh token', async () => {
    storage.refresh = 'refresh'
    authApi.logout.mockResolvedValue({})
    await useAuthStore.getState().logout()
    expect(authApi.logout).toHaveBeenCalledWith('refresh')
    expect(storage.clear).toHaveBeenCalled()

    storage.refresh = null
    await useAuthStore.getState().logout()
    expect(authApi.logout).toHaveBeenCalledTimes(1)
  })
})
