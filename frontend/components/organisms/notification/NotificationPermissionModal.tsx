"use client";

import { useState } from "react";
import { Modal, ModalHeader, ModalBody, ModalFooter, Button } from "flowbite-react";
import { Bell, BellOff } from "lucide-react";
import { fcmManager } from "@/lib/auth/fcm";
import { logger } from "@/lib/utils";

interface NotificationPermissionModalProps {
  show: boolean;
  onClose: () => void;
  onSuccess?: (token: string) => void;
  onSkip?: () => void;
}

export function NotificationPermissionModal({
  show,
  onClose,
  onSuccess,
  onSkip,
}: NotificationPermissionModalProps) {
  const [isRequesting, setIsRequesting] = useState(false);

  const handleEnableNotifications = async () => {
    setIsRequesting(true);
    try {
      const result = await fcmManager.getToken();

      if (result.token) {
        logger.log("FCM 토큰 획득 성공:", result.token.substring(0, 20) + "...");
        onSuccess?.(result.token);
        onClose();
      } else {
        logger.error("FCM 토큰 획득 실패:", result.error);
        alert("알림 권한을 허용해주세요. 브라우저 설정에서 변경할 수 있습니다.");
      }
    } catch (error) {
      logger.error("알림 권한 요청 중 오류:", error);
      alert("알림 설정 중 오류가 발생했습니다.");
    } finally {
      setIsRequesting(false);
    }
  };

  const handleSkip = () => {
    onSkip?.();
    onClose();
  };

  return (
    <Modal show={show} onClose={onClose} size="md">
      <ModalHeader>
        <div className="flex items-center gap-2">
          <Bell className="w-5 h-5 text-purple-600" />
          <span>실시간 알림 받기</span>
        </div>
      </ModalHeader>
      <ModalBody>
        <div className="space-y-4">
          <p className="text-sm text-gray-700 dark:text-gray-300">
            롤링페이퍼에 새로운 메시지가 도착하면 실시간으로 알림을 받을 수 있습니다.
          </p>

          <div className="bg-purple-50 dark:bg-purple-900/20 p-4 rounded-lg">
            <p className="text-sm font-medium text-purple-900 dark:text-purple-100 mb-2">
              알림을 받으면 좋은 점:
            </p>
            <ul className="space-y-2 text-sm text-purple-800 dark:text-purple-200">
              <li className="flex items-start gap-2">
                <span className="text-purple-600 dark:text-purple-400 mt-0.5">•</span>
                <span>새로운 메시지를 놓치지 않고 바로 확인</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-purple-600 dark:text-purple-400 mt-0.5">•</span>
                <span>댓글이나 좋아요 등의 소식을 실시간으로 수신</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-purple-600 dark:text-purple-400 mt-0.5">•</span>
                <span>나중에 설정 페이지에서 알림을 끌 수 있습니다</span>
              </li>
            </ul>
          </div>

          <p className="text-xs text-gray-500 dark:text-gray-400">
            브라우저에서 알림 권한을 요청하는 팝업이 표시됩니다. &ldquo;허용&rdquo;을 눌러주세요.
          </p>
        </div>
      </ModalBody>
      <ModalFooter>
        <div className="flex gap-2 w-full">
          <Button
            color="gray"
            onClick={handleSkip}
            disabled={isRequesting}
            className="flex-1"
          >
            <BellOff className="w-4 h-4 mr-2" />
            나중에
          </Button>
          <Button
            onClick={handleEnableNotifications}
            disabled={isRequesting}
            className="flex-1 bg-purple-600 hover:bg-purple-700"
          >
            <Bell className="w-4 h-4 mr-2" />
            {isRequesting ? "설정 중..." : "알림 받기"}
          </Button>
        </div>
      </ModalFooter>
    </Modal>
  );
}
