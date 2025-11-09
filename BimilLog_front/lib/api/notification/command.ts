import { apiClient } from '../client'

interface BatchUpdateRequest {
  readIds: number[];
  deletedIds: number[];
}

export const notificationCommand = {
  // ?�괄 ?�데?�트 (?�음/??�� ?�번??처리)
  batchUpdate: (updates: BatchUpdateRequest) => {
    return apiClient.post("/api/notification/update", updates);
  },

  registerFcmToken: (fcmToken: string) => {
    const params = new URLSearchParams({ fcmToken });
    return apiClient.post(`/api/notification/fcm?${params.toString()}`);
  },
}
