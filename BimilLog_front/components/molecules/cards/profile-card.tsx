"use client";

import React, { useState } from "react";
import { Button } from "@/components";
import { Card, CardContent } from "@/components";
import { Input } from "@/components";
import { Label } from "@/components";
import { Badge } from "@/components";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogFooter,
} from "@/components";
import { getInitials } from "@/lib/utils/format";
import {
  AlertCircle,
  Check,
  Crown,
  Edit,
  Settings,
  Shield,
  Star,
} from "lucide-react";
import { Member, userQuery } from "@/lib/api";
import { validationRules } from "@/lib/utils/validation-helpers";
import { useToast } from "@/hooks";
import { logger } from '@/lib/utils/logger';
import { ToastContainer } from "@/components";

interface ProfileCardProps {
  user: Member;
  /**
   * 닉네임 변경 콜백. auth store 에 위임하여 백엔드 호출 + 상태 갱신을 단일 경로로 처리한다.
   * @returns 변경 성공 여부
   */
  onNicknameChange: (newNickname: string) => Promise<boolean>;
  onLogout: () => Promise<void>;
  className?: string;
}

export const ProfileCard: React.FC<ProfileCardProps> = React.memo(({
  user,
  onNicknameChange,
  onLogout,
  className,
}) => {
  const [nicknameInput, setNicknameInput] = useState(user.memberName);
  const [nicknameMessage, setNicknameMessage] = useState("");
  const [isNicknameFormatValid, setIsNicknameFormatValid] = useState(false);
  const [isNicknameAvailable, setIsNicknameAvailable] = useState<boolean | null>(null);
  const [isChecking, setIsChecking] = useState(false);
  const [isNicknameChangeSubmitting, setIsNicknameChangeSubmitting] = useState(false);
  const [isNicknameDialogOpen, setIsNicknameDialogOpen] = useState(false);
  const { toasts, showSuccess, showError, removeToast } = useToast();


  const handleNicknameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newNickname = e.target.value;
    setNicknameInput(newNickname);
    setIsNicknameAvailable(null);

    const { isValid: valid, error: message } = validationRules.nickname(newNickname);
    setIsNicknameFormatValid(valid);
    setNicknameMessage(message || "");
  };

  const handleCheckNickname = async () => {
    if (!isNicknameFormatValid) return;
    if (nicknameInput === user.memberName) {
      setNicknameMessage("현재 닉네임과 같습니다.");
      setIsNicknameAvailable(false);
      return;
    }

    setIsChecking(true);
    try {
      const response = await userQuery.checkUserName(nicknameInput.trim());
      if (response.success) {
        const isAvailable = response.data ?? false;
        setIsNicknameAvailable(isAvailable);
        setNicknameMessage(
          isAvailable ? "사용 가능한 닉네임입니다." : "이미 사용중인 닉네임입니다."
        );
      } else {
        setIsNicknameAvailable(false);
        setNicknameMessage(response.error || "닉네임 확인 중 오류가 발생했습니다.");
      }
    } catch (error) {
      logger.error(error);
      setIsNicknameAvailable(false);
      setNicknameMessage("닉네임 확인 중 오류가 발생했습니다.");
    } finally {
      setIsChecking(false);
    }
  };

  const handleNicknameSubmit = async () => {
    if (!isNicknameFormatValid || !isNicknameAvailable) return;

    // B-301: 부모(useMyPage → auth store)가 단일 경로로 백엔드 호출.
    // ProfileCard 의 직접 action 호출은 제거하여 더블 POST 차단.
    // 다이얼로그 안의 "닉네임 변경" 버튼 클릭 = 최종 확인이므로 native confirm 도 제거.
    setIsNicknameChangeSubmitting(true);
    try {
      const success = await onNicknameChange(nicknameInput.trim());
      if (success) {
        setIsNicknameDialogOpen(false);
        setNicknameMessage("");
        setIsNicknameAvailable(null);
        showSuccess(
          "닉네임을 바꿨어요",
          "새 이름으로 다시 만나요. 잠시 후 로그인 페이지로 안내할게요."
        );
        // 토스트 가독성 확보 위해 1.5s → 2.5s 로 연장 (WCAG 2.2.1 Timing Adjustable)
        setTimeout(async () => {
          await onLogout();
        }, 2500);
      } else {
        showError("닉네임 변경 실패", "닉네임 변경에 실패했습니다.");
        setIsNicknameAvailable(false);
      }
    } catch (error) {
      logger.error("Failed to update nickname:", error);
      showError("닉네임 변경 실패", "닉네임 변경 중 오류가 발생했습니다.");
    } finally {
      setIsNicknameChangeSubmitting(false);
    }
  };

  const handleDialogOpenChange = (open: boolean) => {
    setIsNicknameDialogOpen(open);
    if (!open) {
      setNicknameInput(user.memberName);
      setNicknameMessage("");
      setIsNicknameFormatValid(false);
      setIsNicknameAvailable(null);
    }
  };

  return (
    <>
      <Card variant="elevated" className={`mb-8 overflow-hidden bg-paper-aged dark:bg-gradient-to-br dark:from-stamp-red/10 dark:to-postal-navy/10 border-2 border-stamp-red/20 dark:border-stamp-red/30 shadow-xl ${className || ""}`}>
        <CardContent className="p-6 md:p-8 relative">
          <div className="absolute inset-0 bg-paper-50/40 dark:bg-stamp-red/5" />
          <div className="relative z-10">
          <div className="flex flex-col md:flex-row items-center md:items-start md:space-x-6">
            {/* 프로필 이미지 */}
            <div className="relative mb-4 md:mb-0 shrink-0">
              <div className="w-28 h-28 md:w-32 md:h-32 rounded-full overflow-hidden ring-4 ring-stamp-red/30 dark:ring-stamp-red/40 shadow-xl">
                {user.thumbnailImage ? (
                  <img
                    src={user.thumbnailImage}
                    alt={user.memberName}
                    className="w-full h-full object-cover"
                    referrerPolicy="no-referrer"
                  />
                ) : (
                  <div className="w-full h-full bg-stamp-red flex items-center justify-center">
                    <span className="text-white text-3xl md:text-4xl font-bold font-display">
                      {getInitials(user.memberName)}
                    </span>
                  </div>
                )}
              </div>

              {user.role === "ADMIN" && (
                <div className="absolute top-0 left-0 w-10 h-10 bg-stamp-red rounded-full flex items-center justify-center shadow-lg">
                  <Crown className="w-5 h-5 text-white" />
                </div>
              )}
            </div>

            {/* 프로필 정보 */}
            <div className="flex-1 w-full">
              <div className="flex flex-col space-y-4">
                {/* 닉네임 & 관리자 뱃지 */}
                <div className="flex flex-col md:flex-row items-center md:items-start md:space-x-6 px-6 gap-4">
                  <div className="flex flex-nowrap items-center justify-center md:justify-start gap-3">
                    <h2 className="text-3xl md:text-4xl font-display font-bold text-ink dark:text-foreground whitespace-nowrap">
                      {user.memberName}
                    </h2>
                    {user.role === "ADMIN" && (
                      <Badge className="bg-stamp-red text-white border-0 shadow-md whitespace-nowrap">
                        <Shield className="w-3 h-3 mr-1" />
                        관리자
                      </Badge>
                    )}
                  </div>

                  {/* 닉네임 변경 버튼 */}
                  <div className="w-full md:w-auto">
                    <Dialog open={isNicknameDialogOpen} onOpenChange={handleDialogOpenChange}>
                      <DialogTrigger asChild>
                        <Button
                          variant="outline"
                          size="sm"
                          className="w-full md:w-auto border-stamp-red/40 text-stamp-red hover:bg-stamp-red/10 dark:border-stamp-red/50 dark:text-stamp-red dark:hover:bg-stamp-red/20"
                        >
                          <Edit className="w-4 h-4 mr-2" />
                          닉네임 변경
                        </Button>
                      </DialogTrigger>
                      <DialogContent popup size="md">
                      <DialogHeader>
                        <DialogTitle>닉네임 변경</DialogTitle>
                      </DialogHeader>
                      <div className="space-y-4">
                        <div className="space-y-2">
                          <Label htmlFor="nickname" className="text-sm font-medium text-brand-primary">
                            닉네임
                          </Label>
                          <div className="flex space-x-2">
                            <div className="flex-1 relative">
                              <Input
                                id="nickname"
                                type="text"
                                placeholder="새 닉네임을 입력하세요"
                                value={nicknameInput}
                                onChange={handleNicknameChange}
                                className={`pr-10 ${
                                  isNicknameAvailable === true
                                    ? "border-green-500 focus-visible:ring-green-500"
                                    : isNicknameAvailable === false
                                    ? "border-red-500 focus-visible:ring-red-500"
                                    : ""
                                }`}
                              />
                              {isNicknameAvailable === true && (
                                <Check className="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-green-500" />
                              )}
                              {(isNicknameAvailable === false ||
                                (!isNicknameFormatValid && nicknameInput.length > 0)) && (
                                <AlertCircle className="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-red-500" />
                              )}
                            </div>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={handleCheckNickname}
                              disabled={!isNicknameFormatValid || isChecking}
                            >
                              {isChecking ? "확인중..." : "중복확인"}
                            </Button>
                          </div>
                          {nicknameMessage && (
                            <p
                              className={`text-sm ${
                                isNicknameAvailable === true
                                  ? "text-green-600"
                                  : isNicknameAvailable === false
                                  ? "text-red-600"
                                  : isNicknameFormatValid
                                  ? "text-postal-navy"
                                  : "text-red-600"
                              }`}
                            >
                              {nicknameMessage}
                            </p>
                          )}
                        </div>
                      </div>
                      <DialogFooter>
                        <Button
                          variant="outline"
                          onClick={() => handleDialogOpenChange(false)}
                        >
                          취소
                        </Button>
                        <Button
                          variant="default"
                          onClick={handleNicknameSubmit}
                          disabled={!isNicknameAvailable || isNicknameChangeSubmitting}
                        >
                          {isNicknameChangeSubmitting ? "변경 중..." : "닉네임 변경"}
                        </Button>
                      </DialogFooter>
                      </DialogContent>
                    </Dialog>
                  </div>
                </div>

                {/* 사용자 정보 - 가로로 배치 */}
                <div className="flex flex-wrap items-center justify-center gap-4">
                  {user.socialNickname && (
                    <div className="flex items-center gap-2 px-3 py-2 rounded-full bg-postal-navy/10 dark:bg-postal-navy/20">
                      <Star className="w-4 h-4 text-postal-navy dark:text-postal-navy" />
                      <span className="text-sm font-medium text-postal-navy dark:text-foreground">{user.socialNickname}</span>
                    </div>
                  )}
                  {user.settingId && (
                    <div className="flex items-center gap-2 px-3 py-2 rounded-full bg-stamp-red/10 dark:bg-stamp-red/20">
                      <Settings className="w-4 h-4 text-stamp-red dark:text-stamp-red" />
                      <span className="text-sm font-medium text-stamp-red dark:text-foreground">알림 ON</span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
          </div>
        </CardContent>
      </Card>

      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </>
  );
}, (prevProps, nextProps) => {
  // 사용자 정보가 변경되었는지 확인
  return (
    prevProps.user.memberId === nextProps.user.memberId &&
    prevProps.user.memberName === nextProps.user.memberName &&
    prevProps.user.thumbnailImage === nextProps.user.thumbnailImage &&
    prevProps.user.role === nextProps.user.role &&
    prevProps.user.socialNickname === nextProps.user.socialNickname &&
    prevProps.user.settingId === nextProps.user.settingId &&
    prevProps.className === nextProps.className
  );
});

ProfileCard.displayName = "ProfileCard";
