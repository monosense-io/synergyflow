'use client'

import { create } from 'zustand'
import { devtools } from 'zustand/middleware'

// Types for time entries
export interface TimeEntry {
  id: string
  userId: string
  durationMinutes: number
  description: string
  occurredAt: string
  status: 'DRAFT' | 'CONFIRMED' | 'MIRRORING' | 'COMPLETED' | 'FAILED' | 'OPTIMISTIC'
  createdAt: string
  targetEntities?: TargetEntity[]
  trackingId?: string
  errorMessage?: string
}

export interface TargetEntity {
  type: 'INCIDENT' | 'TASK' | 'PROJECT'
  entityId: string
  entityTitle?: string
}

export interface FreshnessBadge {
  entityId: string
  entityType: string
  status: 'PENDING' | 'MIRRORING' | 'COMPLETED' | 'FAILED'
  lastUpdated: string
}

interface TimeTrayState {
  // State
  isOpen: boolean
  timeEntries: TimeEntry[]
  freshnessBadges: FreshnessBadge[]
  activeTimeEntry: TimeEntry | null

  // Actions
  openTimeTray: () => void
  closeTimeTray: () => void
  addTimeEntry: (entry: TimeEntry) => void
  updateTimeEntryStatus: (entryId: string, status: TimeEntry['status'], errorMessage?: string) => void
  updateTimeEntryTrackingId: (entryId: string, trackingId: string) => void
  setFreshnessBadge: (badge: FreshnessBadge) => void
  clearCompletedEntries: () => void
  setActiveTimeEntry: (entry: TimeEntry | null) => void
}

export const useTimeTrayStore = create<TimeTrayState>()(
  devtools(
    (set, get) => ({
      // Initial state
      isOpen: false,
      timeEntries: [],
      freshnessBadges: [],
      activeTimeEntry: null,

      // Actions
      openTimeTray: () => {
        set({ isOpen: true }, false, 'openTimeTray')
      },

      closeTimeTray: () => {
        set({ isOpen: false, activeTimeEntry: null }, false, 'closeTimeTray')
      },

      addTimeEntry: (entry) => {
        set(
          (state) => ({
            timeEntries: [entry, ...state.timeEntries],
            activeTimeEntry: entry,
          }),
          false,
          'addTimeEntry'
        )

        // Set initial freshness badges for target entities
        if (entry.targetEntities) {
          entry.targetEntities.forEach((target) => {
            get().setFreshnessBadge({
              entityId: target.entityId,
              entityType: target.type,
              status: 'PENDING',
              lastUpdated: new Date().toISOString(),
            })
          })
        }
      },

      updateTimeEntryStatus: (entryId, status, errorMessage) => {
        set(
          (state) => ({
            timeEntries: state.timeEntries.map((entry) =>
              entry.id === entryId ? { ...entry, status, errorMessage } : entry
            ),
          }),
          false,
          'updateTimeEntryStatus'
        )

        // Update freshness badges when time entry status changes
        const entry = get().timeEntries.find((e) => e.id === entryId)
        if (entry?.targetEntities) {
          const badgeStatus = status === 'COMPLETED' ? 'COMPLETED' :
                            status === 'FAILED' ? 'FAILED' :
                            status === 'MIRRORING' ? 'MIRRORING' : 'PENDING'

          entry.targetEntities.forEach((target) => {
            get().setFreshnessBadge({
              entityId: target.entityId,
              entityType: target.type,
              status: badgeStatus,
              lastUpdated: new Date().toISOString(),
            })
          })
        }
      },

      updateTimeEntryTrackingId: (entryId, trackingId) => {
        set(
          (state) => ({
            timeEntries: state.timeEntries.map((entry) =>
              entry.id === entryId ? { ...entry, trackingId } : entry
            ),
          }),
          false,
          'updateTimeEntryTrackingId'
        )
      },

      setFreshnessBadge: (badge) => {
        set(
          (state) => {
            const existingIndex = state.freshnessBadges.findIndex(
              (b) => b.entityId === badge.entityId && b.entityType === badge.entityType
            )

            if (existingIndex >= 0) {
              const updatedBadges = [...state.freshnessBadges]
              updatedBadges[existingIndex] = badge
              return { freshnessBadges: updatedBadges }
            } else {
              return { freshnessBadges: [...state.freshnessBadges, badge] }
            }
          },
          false,
          'setFreshnessBadge'
        )
      },

      clearCompletedEntries: () => {
        set(
          (state) => ({
            timeEntries: state.timeEntries.filter(
              (entry) => entry.status !== 'COMPLETED' && entry.status !== 'OPTIMISTIC'
            ),
          }),
          false,
          'clearCompletedEntries'
        )
      },

      setActiveTimeEntry: (entry) => {
        set({ activeTimeEntry: entry }, false, 'setActiveTimeEntry')
      },
    }),
    {
      name: 'time-tray-store',
    }
  )
)

// Selectors for derived state
export const useTimeTrayEntries = () => useTimeTrayStore((state) => state.timeEntries)
export const useTimeTrayIsOpen = () => useTimeTrayStore((state) => state.isOpen)
export const useFreshnessBadges = () => useTimeTrayStore((state) => state.freshnessBadges)
export const useActiveTimeEntry = () => useTimeTrayStore((state) => state.activeTimeEntry)