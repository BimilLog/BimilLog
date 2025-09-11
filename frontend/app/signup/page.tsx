"use client";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Check, AlertCircle } from "lucide-react";
import Link from "next/link";
import { useState, useEffect, useRef } from "react";
import { useAuth } from "@/hooks/useAuth";
import { useRouter, useSearchParams } from "next/navigation";
import { authApi, userApi } from "@/lib/api";
import { validateNickname } from "@/util/inputValidation";
import { useNotifications } from "@/hooks/useNotifications";
import { AuthHeader } from "@/components/organisms/auth-header";
import { HomeFooter } from "@/components/organisms/home/HomeFooter";
import { useToast } from "@/hooks/useToast";
import { ToastContainer } from "@/components/molecules/toast";

export default function SignUpPage() {
  const { login, isAuthenticated, isLoading, refreshUser } = useAuth();
  const { fetchNotifications } = useNotifications();
  const router = useRouter();
  const searchParams = useSearchParams();
  const needsNickname = searchParams.get("required") === "true";
  const { showSuccess, showError, toasts, removeToast } = useToast();

  // 임시 사용자 UUID 관리
  const [tempUuid, setTempUuid] = useState<string | null>(null);
  const isMounted = useRef(true);

  const [nickname, setNicknameInput] = useState("");
  const [nicknameMessage, setNicknameMessage] = useState("");
  const [isNicknameFormatValid, setIsNicknameFormatValid] = useState(false);
  const [isNicknameAvailable, setIsNicknameAvailable] = useState<
    boolean | null
  >(null);
  const [isChecking, setIsChecking] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!isLoading && isAuthenticated && !needsNickname) {
      router.push("/");
    }
  }, [isLoading, isAuthenticated, router, needsNickname]);

  // 세션스토리지에서 임시 UUID 가져오기
  useEffect(() => {
    if (needsNickname) {
      const storedUuid = sessionStorage.getItem("tempUserUuid");
      if (storedUuid) {
        setTempUuid(storedUuid);
      } else {
        // UUID가 없는 경우 로그인 페이지로 리다이렉트
        showError("회원가입 오류", "회원가입 정보가 없습니다. 다시 로그인해주세요.");
        setTimeout(() => {
          if (isMounted.current) {
            router.push("/login");
          }
        }, 2000);
      }
    }
  }, [needsNickname, router, showError]);

  // cleanup: 컴포넌트 언마운트 시 isMounted 플래그 업데이트
  useEffect(() => {
    return () => {
      isMounted.current = false;
    };
  }, []);

  const handleNicknameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newNickname = e.target.value.trim();
    setNicknameInput(newNickname);
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
        setNicknameMessage(
          response.error || "닉네임 확인 중 오류가 발생했습니다."
        );
      }
    } catch (error) {
      console.error(error);
      setIsNicknameAvailable(false);
      setNicknameMessage("닉네임 확인 중 오류가 발생했습니다.");
    } finally {
      setIsChecking(false);
    }
  };

  const handleSetNickname = async () => {
    if (!isNicknameFormatValid || !isNicknameAvailable || !tempUuid) return;
    setIsSubmitting(true);
    try {
      const response = await authApi.signUp(nickname, tempUuid);
      if (response.success) {
        // 세션스토리지에서 임시 UUID 제거
        sessionStorage.removeItem("tempUserUuid");

        // 사용자 정보 갱신
        await refreshUser();

        // 알림 목록 조회
        await fetchNotifications();

        if (isMounted.current) {
          router.push("/");
        }
      } else {
        const errorMessage = response.error || "회원가입에 실패했습니다.";
        showError("회원가입 실패", errorMessage);
        setIsNicknameAvailable(false);
      }
    } catch (error) {
      console.error(error);
      showError("회원가입 실패", "오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <img
            src="/log.png"
            alt="비밀로그"
            className="h-12 object-contain mx-auto mb-4 animate-pulse"
          />
          <p className="text-gray-600">로딩 중...</p>
        </div>
      </div>
    );
  }

  // 닉네임 설정이 필요한 경우
  if (needsNickname) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <AuthHeader />

        <div className="flex items-center justify-center p-4 py-16">
          <div className="w-full max-w-md">
            <div className="text-center mb-8">
              <Link href="/" className="inline-block">
                <img
                  src="/log.png"
                  alt="비밀로그"
                  className="h-16 object-contain mx-auto"
                />
              </Link>
            </div>

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
                  <Label
                    htmlFor="nickname"
                    className="text-sm font-medium text-gray-700"
                  >
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
                      disabled={!isNicknameFormatValid || isChecking}
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
                  disabled={!isNicknameAvailable || isSubmitting || !tempUuid}
                  onClick={handleSetNickname}
                >
                  {isSubmitting ? "설정 중..." : "닉네임 설정 완료"}
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>

        <HomeFooter />
        <ToastContainer toasts={toasts} onRemove={removeToast} />
      </div>
    );
  }

  // 일반 회원가입 페이지
  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      <div className="flex items-center justify-center p-4 py-16">
        <div className="w-full max-w-md">
          <div className="text-center mb-8">
            <Link href="/" className="inline-block">
              <img
                src="/log.png"
                alt="비밀로그"
                className="h-16 object-contain mx-auto"
              />
            </Link>
          </div>

          <Card className="border-0 shadow-2xl bg-white/80 backdrop-blur-sm">
            <CardHeader className="text-center pb-2">
              <CardTitle className="text-2xl font-bold text-gray-800">
                회원가입
              </CardTitle>
              <CardDescription className="text-gray-600">
                나만의 롤링페이퍼를 만들어보세요
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6 pt-6">
              <Button
                className="w-full h-12 bg-yellow-400 hover:bg-yellow-500 text-gray-800 font-semibold text-base"
                onClick={() => login("/signup?required=true")}
              >
                <svg
                  className="w-5 h-5 mr-2"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                >
                  <path d="M12 3c5.799 0 10.5 3.664 10.5 8.185 0 4.52-4.701 8.184-10.5 8.184a13.5 13.5 0 0 1-1.727-.11l-4.408 2.883c-.501.265-.678.236-.472-.413l.892-3.678c-2.88-1.46-4.785-3.99-4.785-6.866C1.5 6.665 6.201 3 12 3z" />
                </svg>
                카카오로 시작하기
              </Button>

              <div className="text-center">
                <p className="text-sm text-gray-600">
                  이미 계정이 있으신가요?{" "}
                  <Link
                    href="/login"
                    className="text-purple-600 hover:text-purple-700 font-medium"
                  >
                    로그인
                  </Link>
                </p>
              </div>
            </CardContent>
          </Card>

          <div className="text-center mt-6">
            <Link
              href="/"
              className="text-gray-500 hover:text-gray-700 transition-colors"
            >
              ← 홈으로 돌아가기
            </Link>
          </div>
        </div>
      </div>

      <HomeFooter />
      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </div>
  );
}
