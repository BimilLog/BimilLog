import { apiClient } from '../client'

interface BatchUpdateRequest {
  readIds: number[];
  deletedIds: number[];
}

export const notificationCommand = {
  // 일괄 업데이트 (읽음/삭제 한번에 처리)
  batchUpdate: (updates: BatchUpdateRequest) => {
    console.log('📤 알림 일괄 업데이트 API 호출:', updates);
    return apiClient.post("/api/notification/update", updates);
  },
}