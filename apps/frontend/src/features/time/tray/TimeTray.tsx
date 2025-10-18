'use client'

import { useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Clock, X, Send, AlertCircle, CheckCircle, Loader2 } from 'lucide-react'
import { createBulkTimeEntries, createTimeEntry } from '../api/time-entry-api'
import { useTimeTrayStore } from './useTimeTray'
import { cn } from '@/lib/utils'

// Validation schema using Zod
const timeEntrySchema = z.object({
  durationMinutes: z.number().min(1, 'Duration must be at least 1 minute'),
  description: z.string().min(1, 'Description is required').max(500, 'Description too long'),
  occurredAt: z.string().optional(),
  targetEntities: z.array(z.object({
    type: z.enum(['INCIDENT', 'TASK', 'PROJECT']),
    entityId: z.string().min(1, 'Entity ID is required'),
    entityTitle: z.string().optional(),
  })).min(1, 'At least one target entity is required'),
})

type TimeEntryForm = z.infer<typeof timeEntrySchema>

interface TimeTrayProps {
  isOpen: boolean
  onClose: () => void
}

export function TimeTray({ isOpen, onClose }: TimeTrayProps) {
  const queryClient = useQueryClient()
  const { addTimeEntry, updateTimeEntryStatus, setFreshnessBadge } = useTimeTrayStore()
  const activeTimeEntry = useTimeTrayStore((s) => s.activeTimeEntry)
  const freshnessBadges = useTimeTrayStore((s) => s.freshnessBadges)
  const [bulkMode, setBulkMode] = useState(false)
  const [bulkEntries, setBulkEntries] = useState<Array<{ durationMinutes: number; description: string }>>([])
  const [bulkSuccess, setBulkSuccess] = useState<{ message: string; ids: string[] } | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isValid },
  } = useForm<TimeEntryForm>({
    resolver: zodResolver(timeEntrySchema),
    defaultValues: {
      durationMinutes: 30,
      description: '',
      occurredAt: new Date().toISOString().slice(0, 16),
      targetEntities: [],
    },
  })

  // Mutation for creating time entry with optimistic updates
  const createMutation = useMutation({
    mutationFn: createTimeEntry,
    onMutate: async (newEntry) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['timeEntries'] })

      // Optimistically update to the new value
      const id = `temp-${Date.now()}`
      // Infer default target entity (e.g., "incident #123") if none provided
      let inferredTargets = newEntry.targetEntities ?? []
      if ((!inferredTargets || inferredTargets.length === 0) && typeof newEntry.description === 'string') {
        const m = /incident\s*#(\d+)/i.exec(newEntry.description)
        if (m) {
          inferredTargets = [{ type: 'INCIDENT', entityId: `incident-${m[1]}`, entityTitle: `Incident ${m[1]}` } as any]
        }
      }
      const optimisticEntry = {
        id,
        ...newEntry,
        targetEntities: inferredTargets,
        status: 'OPTIMISTIC' as const,
        createdAt: new Date().toISOString(),
      }

      addTimeEntry(optimisticEntry)

      return { optimisticEntryId: id }
    },
    onSuccess: (response, _variables, context) => {
      // Replace optimistic entry with real data
      if (context?.optimisticEntryId) {
        updateTimeEntryStatus(context.optimisticEntryId, 'CONFIRMED')
      }

      // Show success notification
      // This would integrate with a toast system
      console.log('Time entry created successfully:', response.trackingId)

      // Reset form and close
      reset()
      onClose()
    },
    onError: (error) => {
      // Rollback optimistic update
      console.error('Failed to create time entry:', error)
    },
    onSettled: () => {
      // Always refetch after error or success
      queryClient.invalidateQueries({ queryKey: ['timeEntries'] })
    },
  })

  const onSubmit = (data: TimeEntryForm) => {
    createMutation.mutate({
      ...data,
      occurredAt: data.occurredAt ? new Date(data.occurredAt) : new Date(),
    })
  }

  const isLoading = createMutation.isPending

  // Bulk mode mutation
  const bulkMutation = useMutation({
    mutationFn: createBulkTimeEntries,
    onSuccess: (response) => {
      setBulkSuccess({ message: response.message, ids: response.trackingIds })
    },
  })

  // Listen for mirroring-complete events to update badges
  useEffect(() => {
    const handler = (e: any) => {
      const { entityId, status } = e.detail || {}
      if (entityId) {
        setFreshnessBadge({
          entityId,
          entityType: 'INCIDENT',
          status: (status as any) || 'COMPLETED',
          lastUpdated: new Date().toISOString(),
        })
      }
    }
    window.addEventListener('mirroring-complete', handler)
    return () => window.removeEventListener('mirroring-complete', handler)
  }, [setFreshnessBadge])

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-end pt-16" data-testid="time-tray-root">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        data-testid="time-tray-backdrop"
        onClick={onClose}
      />

      {/* Time Tray Panel */}
      <div className="relative w-96 h-full bg-white shadow-2xl border-l border-gray-200 transform transition-transform duration-300 ease-in-out" data-testid="time-tray-panel">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-gray-200">
          <div className="flex items-center gap-2">
            <Clock className="w-5 h-5 text-blue-600" />
            <h2 className="text-lg font-semibold text-gray-900">Time Tray</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 hover:bg-gray-100 rounded-md transition-colors"
          >
            <X className="w-5 h-5 text-gray-500" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit(onSubmit)} className="p-4 space-y-4">
          {/* Duration Input */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Duration (minutes)
            </label>
            <input
              type="number"
              min="1"
              className={cn(
                "w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500",
                errors.durationMinutes ? "border-red-500" : "border-gray-300"
              )}
              data-testid="duration-input"
              {...register('durationMinutes', { valueAsNumber: true })}
            />
            {errors.durationMinutes && (
              <p className="mt-1 text-sm text-red-600 flex items-center gap-1" data-testid="duration-error">
                <AlertCircle className="w-4 h-4" />
                {errors.durationMinutes.message}
              </p>
            )}
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              rows={3}
              className={cn(
                "w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none",
                errors.description ? "border-red-500" : "border-gray-300"
              )}
              placeholder="What did you work on?"
              data-testid="description-input"
              {...register('description')}
            />
            {errors.description && (
              <p className="mt-1 text-sm text-red-600 flex items-center gap-1" data-testid="description-error">
                <AlertCircle className="w-4 h-4" />
                {errors.description.message}
              </p>
            )}
          </div>

          {/* Occurred At */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              When (optional)
            </label>
            <input
              type="datetime-local"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              {...register('occurredAt')}
            />
          </div>

          {/* Target Entities - This would be enhanced with entity selection */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Link to
            </label>
            <div className="text-sm text-gray-500 p-3 bg-gray-50 rounded-md">
              Entity selection will be implemented based on current context
            </div>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={!isValid || isLoading}
            className={cn(
              "w-full py-2 px-4 rounded-md font-medium flex items-center justify-center gap-2 transition-colors",
              isValid && !isLoading
                ? "bg-blue-600 text-white hover:bg-blue-700"
                : "bg-gray-300 text-gray-500 cursor-not-allowed"
            )}
            data-testid="create-time-entry-button"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Creating...
              </>
            ) : (
              <>
                <Send className="w-4 h-4" />
                Create Time Entry
              </>
            )}
          </button>

          {/* Status Messages */}
          {createMutation.error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-md" data-testid="error-message">
              <p className="text-sm text-red-600 flex items-center gap-1">
                <AlertCircle className="w-4 h-4" />
                Failed to create time entry. Please try again.
              </p>
            </div>
          )}
        </form>
        {/* Bulk Mode Toggle and UI */}
        <div className="px-4">
          <button
            type="button"
            data-testid="bulk-mode-toggle"
            className="text-sm text-blue-600 hover:underline"
            onClick={() => setBulkMode((b) => !b)}
          >
            {bulkMode ? 'Disable Bulk Mode' : 'Enable Bulk Mode'}
          </button>
        </div>
        {bulkMode && (
          <div className="p-4 space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold">Bulk Time Entries</h3>
              <button
                type="button"
                data-testid="add-time-entry-button"
                className="text-sm text-blue-600 hover:underline"
                onClick={() => setBulkEntries((arr) => [...arr, { durationMinutes: 30, description: '' }])}
              >
                Add Entry
              </button>
            </div>
            {bulkEntries.map((entry, idx) => (
              <div key={idx} className="grid grid-cols-2 gap-2">
                <input
                  type="number"
                  min={1}
                  value={entry.durationMinutes}
                  onChange={(e) => {
                    const v = Number(e.target.value)
                    setBulkEntries((arr) => arr.map((it, i) => (i === idx ? { ...it, durationMinutes: v } : it)))
                  }}
                  data-testid={`duration-input-${idx}`}
                  className="px-3 py-2 border rounded-md border-gray-300"
                />
                <input
                  type="text"
                  value={entry.description}
                  onChange={(e) => {
                    const v = e.target.value
                    setBulkEntries((arr) => arr.map((it, i) => (i === idx ? { ...it, description: v } : it)))
                  }}
                  data-testid={`description-input-${idx}`}
                  className="px-3 py-2 border rounded-md border-gray-300"
                  placeholder="Description"
                />
              </div>
            ))}
            <button
              type="button"
              data-testid="create-bulk-entries-button"
              className="w-full py-2 px-4 rounded-md font-medium bg-blue-600 text-white hover:bg-blue-700"
              onClick={() => bulkMutation.mutate({ entries: bulkEntries.map((e) => ({ ...e, occurredAt: new Date(), targetEntities: [] })) })}
            >
              Create Bulk Entries
            </button>
            {bulkSuccess && (
              <div className="p-3 bg-green-50 border border-green-200 rounded-md" data-testid="bulk-success-message">
                <p className="text-sm text-green-700">{bulkSuccess.message}</p>
                <div className="text-xs text-green-700" data-testid="tracking-ids">{bulkSuccess.ids.join(', ')}</div>
              </div>
            )}
          </div>
        )}

        {/* Active Time Entry Status */}
        {activeTimeEntry && (
          <div className="p-4 border-t border-gray-200">
            <div className="text-sm text-gray-700">
              Status: <span data-testid="time-entry-status">{activeTimeEntry.status}</span>
            </div>
          </div>
        )}

        {/* Freshness Badges */}
        {freshnessBadges.length > 0 && (
          <div className="p-4 border-t border-gray-200 space-y-1">
            {freshnessBadges.map((b) => (
              <div key={`${b.entityId}`} className="text-xs text-gray-700" data-testid={`freshness-badge-${b.entityId}`}>
                {b.status}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

// Optional widget with toggle for E2E selector
export default function TimeTrayWidget() {
  const [open, setOpen] = useState(false)
  return (
    <>
      <button data-testid="time-tray-toggle" onClick={() => setOpen(true)} className="hidden">Open Time Tray</button>
      <TimeTray isOpen={open} onClose={() => setOpen(false)} />
    </>
  )
}
