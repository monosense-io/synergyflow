'use client'

import { TargetEntity } from '../tray/useTimeTray'

// API types matching the backend DTOs
export interface CreateTimeEntryRequest {
  durationMinutes: number
  description: string
  occurredAt?: Date
  targetEntities: TargetEntity[]
}

export interface TimeEntryResponse {
  trackingId: string
  message: string
  timeEntryIds: string[]
}

export interface BulkTimeEntryRequest {
  entries: CreateTimeEntryRequest[]
}

export interface BulkTimeEntryResponse {
  message: string
  trackingIds: string[]
}

// API client using axios
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

class TimeEntryAPI {
  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${API_BASE_URL}${endpoint}`

    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    })

    if (!response.ok) {
      // Handle RFC7807 error responses
      if (response.headers.get('content-type')?.includes('application/problem+json')) {
        const error = await response.json()
        throw new APIError(error.title, error.detail, error.status, error.fields)
      }
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    return response.json()
  }

  // Create a single time entry
  async createTimeEntry(request: CreateTimeEntryRequest): Promise<TimeEntryResponse> {
    return this.request<TimeEntryResponse>('/api/v1/time-entries', {
      method: 'POST',
      body: JSON.stringify({
        ...request,
        occurredAt: request.occurredAt?.toISOString(),
      }),
    })
  }

  // Create multiple time entries in bulk
  async createBulkTimeEntries(request: BulkTimeEntryRequest): Promise<BulkTimeEntryResponse> {
    return this.request<BulkTimeEntryResponse>('/api/v1/time-entries/bulk', {
      method: 'POST',
      body: JSON.stringify({
        entries: request.entries.map(entry => ({
          ...entry,
          occurredAt: entry.occurredAt?.toISOString(),
        })),
      }),
    })
  }

  // Get time entries for the current user
  async getUserTimeEntries(): Promise<any[]> {
    return this.request<any[]>('/api/v1/time-entries')
  }

  // Get a specific time entry by ID
  async getTimeEntry(id: string): Promise<any> {
    return this.request<any>(`/api/v1/time-entries/${id}`)
  }
}

// Custom error class for API errors
export class APIError extends Error {
  constructor(
    public title: string,
    public detail: string,
    public status: number,
    public fields?: Array<{ name: string; message: string; code: string }>
  ) {
    super(`${title}: ${detail}`)
    this.name = 'APIError'
  }
}

// Export singleton instance
export const timeEntryAPI = new TimeEntryAPI()

// Export convenience functions for React Query
export const createTimeEntry = (request: CreateTimeEntryRequest) =>
  timeEntryAPI.createTimeEntry(request)

export const createBulkTimeEntries = (request: BulkTimeEntryRequest) =>
  timeEntryAPI.createBulkTimeEntries(request)

export const getUserTimeEntries = () =>
  timeEntryAPI.getUserTimeEntries()

export const getTimeEntry = (id: string) =>
  timeEntryAPI.getTimeEntry(id)