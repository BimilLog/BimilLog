import { apiClient } from '../client'
import { Notification } from '@/types/domains/notification'

export const notificationQuery = {
  getAll: () =>
    apiClient.get<Notification[]>("/api/notification/list"),
}