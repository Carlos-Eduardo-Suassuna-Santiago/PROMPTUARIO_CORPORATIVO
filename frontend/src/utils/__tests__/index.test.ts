import { describe, expect, it, vi } from 'vitest'
import {
  calculateAge,
  cn,
  formatCPF,
  formatDate,
  formatDateTime,
  formatPhone,
  formatRelative,
  getErrorMessage,
  initials,
  truncate,
  downloadBlob,
} from '@/utils'

describe('utils', () => {
  it('formats valid and invalid dates', () => {
    expect(formatDate('2024-01-15')).toBe('15/01/2024')
    expect(formatDate('invalid')).toBe('—')
    expect(formatDate(null)).toBe('—')
    expect(formatDateTime('2024-01-15T13:45:00Z')).toContain('15/01/2024')
    expect(formatRelative('invalid')).toBe('—')
  })

  it('calculates age and handles missing birth dates', () => {
    expect(calculateAge(null)).toBeNull()
    expect(calculateAge('invalid')).toBeNull()
    const age = calculateAge('2000-01-01')
    expect(age).toBeGreaterThanOrEqual(25)
  })

  it('formats identity and text values', () => {
    expect(formatCPF('12345678901')).toBe('123.456.789-01')
    expect(formatCPF(null)).toBe('—')
    expect(formatPhone('11999999999')).toBe('11999999999')
    expect(formatPhone(null)).toBe('—')
    expect(initials('Maria da Silva')).toBe('MD')
    expect(initials('')).toBe('')
    expect(truncate('abcdef', 3)).toBe('abc…')
    expect(truncate('abc', 3)).toBe('abc')
  })

  it('merges tailwind classes', () => {
    expect(cn('p-2', 'p-4', false && 'hidden')).toBe('p-4')
  })

  it('extracts API errors from all supported response formats', () => {
    expect(getErrorMessage({ response: { data: { detail: 'Detalhe' } } })).toBe('Detalhe')
    expect(getErrorMessage({ response: { data: { detail: [{ msg: 'Campo obrigatório' }] } } })).toBe('Campo obrigatório')
    expect(getErrorMessage({ response: { data: { message: 'Mensagem' } } })).toBe('Mensagem')
    expect(getErrorMessage({ response: { data: { error: 'Falha' } } })).toBe('Falha')
    expect(getErrorMessage({ response: { status: 401 } })).toContain('inválidos')
    expect(getErrorMessage({ response: { status: 403 } })).toContain('administradores')
    expect(getErrorMessage({ response: { status: 500 } })).toBe('Erro desconhecido')
    expect(getErrorMessage(new Error('network'))).toBe('Erro ao processar requisição')
  })

  it('downloads a blob and releases the object URL', () => {
    const click = vi.fn()
    vi.spyOn(document, 'createElement').mockReturnValue({ click } as unknown as HTMLElement)
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test')
    const revoke = vi.spyOn(URL, 'revokeObjectURL')

    downloadBlob(new Blob(['content']), 'file.pdf')

    expect(click).toHaveBeenCalledOnce()
    expect(revoke).toHaveBeenCalledWith('blob:test')
  })
})
