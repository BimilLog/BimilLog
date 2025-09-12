import { apiClient } from '../client'
import { DecoType } from '@/types/domains/paper'

export const paperCommand = {
  createMessage: (
    userName: string,
    message: {
      decoType: DecoType
      anonymity: string
      content: string
      x: number
      y: number
    },
  ) => apiClient.post(`/api/paper/command/create/${encodeURIComponent(userName)}`, message),
  
  deleteMessage: (messageId: number) => 
    apiClient.delete(`/api/paper/command/delete/${messageId}`),
}