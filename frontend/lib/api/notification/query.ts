import { apiClient } from '../client'
import { Notification } from '@/types/domains/notification'

export const notificationQuery = {
  getAll: () => 
    apiClient.get<Notification[]>("/api/notification/list"),
  
  getSSEUrl: () => {
    const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
    return `${API_BASE_URL}/api/notification/connect`
  },
}