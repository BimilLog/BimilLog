import React from "react";
import { Button } from "@/components";
import { Modal, ModalHeader, ModalBody } from "flowbite-react";
import { CheckCircle, PartyPopper, Sparkles } from "lucide-react";

interface ConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onGoToMyRollingPaper: () => void;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen,
  onClose,
  onGoToMyRollingPaper,
}) => {
  return (
    // 사용자가 자신의 닉네임을 검색했을 때 표시되는 확인 다이얼로그 (popup 모달)
    <Modal
      show={isOpen}
      onClose={onClose}
      size="md"
      popup
      dismissible
    >
      <ModalHeader />
      <ModalBody>
        <div className="text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
            <PartyPopper className="h-8 w-8 stroke-purple-500 dark:stroke-purple-400 fill-purple-100 dark:fill-purple-900" />
          </div>
          <h3 className="mb-5 text-lg font-bold text-gray-900 dark:text-gray-100 flex items-center justify-center gap-2">
            <CheckCircle className="w-5 h-5 text-blue-600 dark:text-blue-400" />
            내 롤링페이퍼 발견!
          </h3>
          <p className="mb-5 text-sm text-gray-500 dark:text-gray-400">
            입력하신 닉네임은 본인의 롤링페이퍼입니다!
            <br />내 롤링페이퍼로 이동하시겠어요?
          </p>
          <div className="flex justify-center gap-4">
            {/* 내 롤링페이퍼로 이동하는 버튼 (주요 액션) */}
            <Button
              onClick={onGoToMyRollingPaper}
              className="bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white font-semibold"
            >
              <Sparkles className="w-4 h-4 mr-1 stroke-yellow-500 fill-yellow-100" />
              내 롤링페이퍼 보기
            </Button>

            {/* 다이얼로그 닫고 다른 닉네임 검색하기 (보조 액션) */}
            <Button
              variant="outline"
              onClick={onClose}
              className="border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-800"
            >
              다른 롤링페이퍼 찾기
            </Button>
          </div>
        </div>
      </ModalBody>
    </Modal>
  );
};