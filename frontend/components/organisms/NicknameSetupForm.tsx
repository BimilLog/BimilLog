"use client";

import { useState, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Check, AlertCircle } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { useNotifications } from "@/hooks/useNotifications";
import { useRouter } from "next/navigation";
import { userApi } from "@/lib/api";
import { validateNickname } from "@/lib/utils/validation";
import { useToast } from "@/hooks/useToast";

interface NicknameSetupFormProps {
  tempUuid: string;
  onSuccess: () => void;
  onError: (error: string) => void;
}

export function NicknameSetupForm({ tempUuid, onSuccess, onError }: NicknameSetupFormProps) {
  const { signUp } = useAuth();
  const { fetchNotifications } = useNotifications();
  const router = useRouter();
  const { showError } = useToast();
  
  const [nickname, setNickname] = useState("");
  const [nicknameMessage, setNicknameMessage] = useState("");
  const [isNicknameFormatValid, setIsNicknameFormatValid] = useState(false);
  const [isNicknameAvailable, setIsNicknameAvailable] = useState<boolean | null>(null);
  const [isChecking, setIsChecking] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // UUID 유효성 검증
  const validateUuid = useCallback((uuid: string): boolean => {
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    return uuidRegex.test(uuid);
  }, []);

  const handleNicknameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newNickname = e.target.value.trim();
    setNickname(newNickname);
    setIsNicknameAvailable(null);

    const { valid, message } = validateNickname(newNickname);
    setIsNicknameFormatValid(valid);
    setNicknameMessage(message);
  };

  const handleCheckNickname = async () => {
    if (!isNicknameFormatValid) return;
    
    setIsChecking(true);
    try {
      const response = await userApi.checkUserName(nickname);
      
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
      console.error("Nickname check failed:", error);
      setIsNicknameAvailable(false);
      setNicknameMessage("닉네임 확인 중 오류가 발생했습니다.");
    } finally {
      setIsChecking(false);
    }
  };

  const handleSubmit = async () => {
    if (!isNicknameFormatValid || !isNicknameAvailable) return;
    
    // UUID 유효성 재확인
    if (!validateUuid(tempUuid)) {
      onError("잘못된 회원가입 정보입니다.");
      return;
    }
    
    setIsSubmitting(true);
    try {
      const result = await signUp(nickname, tempUuid);
      
      if (result.success) {
        // 세션스토리지에서 임시 UUID 제거
        sessionStorage.removeItem("tempUserUuid");

        // 알림 목록 조회
        await fetchNotifications();

        onSuccess();
        router.push("/");
      } else {
        onError(result.error || "회원가입에 실패했습니다.");
        setIsNicknameAvailable(false);
      }
    } catch (error) {
      console.error("SignUp failed:", error);
      onError("회원가입 중 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card className="border-0 shadow-2xl bg-white/80 backdrop-blur-sm">
      <CardHeader className="text-center pb-2">
        <CardTitle className="text-2xl font-bold text-gray-800">
          닉네임 설정
        </CardTitle>
        <CardDescription className="text-gray-600">
          2~8자의 한글, 영문, 숫자만 사용 가능합니다.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6 pt-6">
        <div className="space-y-2">
          <Label htmlFor="nickname" className="text-sm font-medium text-gray-700">
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
              {isNicknameAvailable === true && (
                <Check className="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-green-500" />
              )}
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