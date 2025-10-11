import { apiClient } from '../client'
import { Member } from '@/types/domains/user'

export const authQuery = {
  getCurrentUser: () => apiClient.get<Member>("/api/auth/me"),
  
  healthCheck: () => apiClient.get<string>("/api/auth/health"),
}