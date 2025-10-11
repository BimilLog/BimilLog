import { apiClient } from '../client'
import { RollingPaperMessage, VisitMessage } from '@/types/domains/paper'

export const paperQuery = {
  getMy: () => 
    apiClient.get<RollingPaperMessage[]>("/api/paper"),
  
  getByUserName: (userName: string) =>
    apiClient.get<VisitMessage[]>(`/api/paper/${encodeURIComponent(userName)}`),
}