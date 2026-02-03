import { apiClient } from '../client'
import { DecoType } from '@/types/domains/paper'

export const paperCommand = {
  createMessage: (message: {
    ownerId: number
    decoType: DecoType
    anonymity: string
    content: string
    x: number
    y: number
  }) => apiClient.post('/api/paper/write', message),

  deleteMessage: (messageId: number) =>
    apiClient.post('/api/paper/delete', { messageId }),
}