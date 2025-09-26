"use client";

import React, { useState, useCallback } from "react";
import { Button } from "@/components";
import { Input } from "@/components";
import { Label } from "@/components";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components";
import { AlertCircle } from "lucide-react";
import { useAuth } from "@/hooks";
import { useNotificationList } from "@/hooks/features";
import { useRouter } from "next/navigation";
import { userQuery } from "@/lib/api";
import { validationRules } from "@/lib/utils/validation-helpers";
import { logger } from '@/lib/utils/logger';

interface NicknameSetupFormProps {
  tempUuid: string;
  onSuccess: () => void;
  onError: (error: string) => void;
}

export function NicknameSetupForm({ tempUuid, onSuccess, onError }: NicknameSetupFormProps) {
  const { signUp } = useAuth();
  const { refetch: refetchNotifications } = useNotificationList();
  const router = useRouter();
  
  const [nickname, setNickname] = useState("");
  const [nicknameMessage, setNicknameMessage] = useState("");
  const [isNicknameFormatValid, setIsNicknameFormatValid] = useState(false);
  const [isNicknameAvailable, setIsNicknameAvailable] = useState<boolean | null>(null);
  const [isChecking, setIsChecking] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // UUID 유효성 검증: 카카오 OAuth 로그인 후 임시 저장된 UUID 형식 검증
  // 사용자가 직접 URL을 조작하여 가짜 회원가입을 시도하는 것을 방지
  const validateUuid = useCallback((uuid: string): boolean => {
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    return uuidRegex.test(uuid);
  }, []);

  // 닉네임 입력 처리: 실시간 형식 검증 및 중복확인 상태 초기화
  // 사용자가 닉네임을 변경할 때마다 중복확인을 다시 하도록 유도
  const handleNicknameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newNickname = e.target.value.trim();
    setNickname(newNickname);
    setIsNicknameAvailable(null); // 중복확인 상태 초기화

    const { isValid: valid, error: message } = validationRules.nickname(newNickname);
    setIsNicknameFormatValid(valid);
    setNicknameMessage(message || "");
  };

  // 닉네임 중복확인: 서버에서 기존 사용자와 중복되지 않는지 확인
  // 성공/실패 메시지를 명확히 전달하여 사용자 경험 개선
  const handleCheckNickname = async () => {
    if (!isNicknameFormatValid) return;

    setIsChecking(true);
    try {
      const response = await userQuery.checkUserName(nickname);

      if (response.success) {
        const isAvailable = response.data ?? false;
        setIsNicknameAvailable(isAvailable);
        setNicknameMessage(
          isAvailable
            ? "사용 가능한 닉네임입니다."
            : "이미 사용중인 닉네임입니다."
        );
      } else {
        setIsNicknameAvailable(false);
        setNicknameMessage(response.error || "닉네임 확인 중 오류가 발생했습니다.");
      }
    } catch (error) {
      logger.error("Nickname check failed:", error);
      setIsNicknameAvailable(false);
      setNicknameMessage("닉네임 확인 중 오류가 발생했습니다.");
    } finally {
      setIsChecking(false);
    }
  };

  // 회원가입 완료 처리: 닉네임과 임시 UUID를 사용하여 최종 계정 생성
  // 성공 시 세션 정리 및 알림 설정, 메인 페이지로 리다이렉트
  const handleSubmit = async () => {
    if (!isNicknameFormatValid || !isNicknameAvailable) return;

    // UUID 유효성 재확인: 보안 강화를 위해 최종 제출 전 다시 한 번 검증
    if (!validateUuid(tempUuid)) {
      onError("잘못된 회원가입 정보입니다.");
      return;
    }

    setIsSubmitting(true);
    try {
      const result = await signUp(nickname, tempUuid);

      if (result.success) {
        // 세션스토리지에서 임시 UUID 제거: 보안상 중요한 정리 작업
        sessionStorage.removeItem("tempUserUuid");

        // 알림 목록 새로고침: 새 계정에 대한 알림 설정 업데이트
        await refetchNotifications();

        onSuccess();
        router.push("/");
      } else {
        onError(result.error || "회원가입에 실패했습니다.");
        setIsNicknameAvailable(false); // 실패 시 중복확인 상태 초기화
      }
    } catch (error) {
      logger.error("SignUp failed:", error);
      onError("회원가입 중 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card variant="elevated">
      <CardHeader className="text-center pb-2">
        <CardTitle className="text-2xl font-bold text-brand-primary">
          닉네임 설정
        </CardTitle>
        <CardDescription className="text-brand-muted">
          2~8자의 한글, 영문, 숫자만 사용 가능합니다.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6 pt-6">
        <div className="space-y-2">
          <Label htmlFor="nickname" className="text-sm font-medium text-brand-primary">
            닉네임
          </Label>
          <div className="flex space-x-2">
            <div className="flex-1 relative">
              <Input
                id="nickname"
                type="text"
                placeholder="사용할 닉네임을 입력하세요"
                value={nickname}
                onChange={handleNicknameChange}
                className={`pr-10 ${
                  isNicknameAvailable === true
                    ? "border-green-500 focus-visible:ring-green-500"
                    : isNicknameAvailable === false
                    ? "border-red-500 focus-visible:ring-red-500"
                    : ""
                }`}
                disabled={isSubmitting}
              />
              {(isNicknameAvailable === false ||
                (!isNicknameFormatValid && nickname.length > 0)) && (
                <AlertCircle className="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-red-500" />
              )}
            </div>
            <Button
              variant="outline"
              onClick={handleCheckNickname}
              disabled={!isNicknameFormatValid || isChecking || isSubmitting}
              className="px-4"
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
                  ? "text-blue-600"
                  : "text-red-600"
              }`}
            >
              {nicknameMessage}
            </p>
          )}
        </div>

        <Button
          className="w-full h-12 bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700 font-semibold"
          disabled={!isNicknameAvailable || isSubmitting}
          onClick={handleSubmit}
        >
          {isSubmitting ? "설정 중..." : "닉네임 설정 완료"}
        </Button>
      </CardContent>
    </Card>
  );
}