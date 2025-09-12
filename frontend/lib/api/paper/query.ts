import { apiClient } from '../client'
import { RollingPaperMessage, VisitMessage } from '@/types/domains/paper'

export const paperQuery = {
  getMy: () => 
    apiClient.get<RollingPaperMessage[]>("/api/paper/query/my"),
  
  getByUserName: (userName: string) => 
    apiClient.get<VisitMessage[]>(`/api/paper/query/user/${encodeURIComponent(userName)}`),
}