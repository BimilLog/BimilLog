import { apiClient } from '../client'
import { User } from '@/types/domains/user'

export const authQuery = {
  getCurrentUser: () => apiClient.get<User>("/api/auth/me"),
  
  healthCheck: () => apiClient.get<string>("/api/auth/health"),
}