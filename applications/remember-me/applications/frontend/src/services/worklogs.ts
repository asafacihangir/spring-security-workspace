import { apiFetch } from './api'

export interface WorkLog {
  id: number
  explanation: string
  createdDate: string
  createdBy: number
}

export interface CreateWorkLogData {
  explanation: string
}

export const getWorkLogs = () => apiFetch<WorkLog[]>('/api/work-logs')
export const getMyWorkLogs = () => apiFetch<WorkLog[]>('/api/work-logs/my')
export const getWorkLog = (id: number) => apiFetch<WorkLog>(`/api/work-logs/${id}`)
export const createWorkLog = (data: CreateWorkLogData) =>
  apiFetch<{ id: number }>('/api/work-logs', { method: 'POST', body: JSON.stringify(data) })
