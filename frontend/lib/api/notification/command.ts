import { apiClient } from '../client'

export const notificationCommand = {
  markAsRead: (notificationId: number) => 
    apiClient.post(`/api/notification/command/read/${notificationId}`),
  
  markAllAsRead: () => 
    apiClient.post("/api/notification/command/read-all"),
  
  delete: (notificationId: number) => 
    apiClient.delete(`/api/notification/command/delete/${notificationId}`),
}