import React from "react";
import { Button, Dialog } from "@/components";
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
    // 사용자가 자신의 닉네임을 검색했을 때 표시되는 확인 다이얼로그
    <Dialog open={isOpen} onOpenChange={onClose}>
      <div className="max-w-sm mx-4 bg-gradient-to-br from-blue-50 to-indigo-50 border-2 border-blue-200 rounded-2xl p-6">
        <div className="mb-4">
          <h3 className="text-center text-blue-800 font-bold flex items-center justify-center space-x-2">
            <CheckCircle className="w-5 h-5" />
            <span>내 롤링페이퍼 발견!</span>
            <PartyPopper className="w-5 h-5" />
          </h3>
        </div>
        <div className="space-y-4 p-2">
          <div className="text-center">
            <p className="text-blue-700 text-sm mb-4">
              입력하신 닉네임은 본인의 롤링페이퍼입니다!
              <br />내 롤링페이퍼로 이동하시겠어요?
            </p>

            <div className="space-y-3">
              {/* 내 롤링페이퍼로 이동하는 버튼 (주요 액션) */}
              <Button
                onClick={onGoToMyRollingPaper}
                className="w-full bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white font-semibold py-2 px-4 rounded-xl"
              >
                <Sparkles className="w-4 h-4 mr-1" />
                내 롤링페이퍼 보기
              </Button>

              {/* 다이얼로그 닫고 다른 닉네임 검색하기 (보조 액션) */}
              <Button
                variant="outline"
                onClick={onClose}
                className="w-full border-blue-300 text-blue-700 hover:bg-blue-50 py-2 px-4 rounded-xl"
              >
                다른 롤링페이퍼 찾기
              </Button>
            </div>
          </div>
        </div>
      </div>
    </Dialog>
  );
};