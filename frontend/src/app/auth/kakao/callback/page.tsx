"use client";

import { useEffect, useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import useAuthStore from "@/util/authStore";
import { getMessaging, getToken } from "firebase/messaging";
import { initializeApp } from "firebase/app";
import { DeviceType } from "@/components/types/schema";
import LoadingSpinner from "@/components/LoadingSpinner";
import fetchClient from "@/util/fetchClient";

const API_BASE = "https://grow-farm.com/api";

// Firebase 설정
const firebaseConfig = {
  apiKey: "AIzaSyDQHWI_zhIjqp_SJz0Fdv7xtG6mIZfwBhU",
  authDomain: "growfarm-6cd79.firebaseapp.com",
  projectId: "growfarm-6cd79",
  storageBucket: "growfarm-6cd79.firebasestorage.app",
  messagingSenderId: "763805350293",
  appId: "1:763805350293:web:68b1b3ca3a294b749b1e9c",
  measurementId: "G-G9C4KYCEEJ",
};

// 기기 유형 확인 함수
const getDeviceType = (): DeviceType | null => {
  if (typeof navigator === "undefined") return null;

  const userAgent = navigator.userAgent.toLowerCase();
  const isMobile =
    /android|webos|iphone|ipod|blackberry|iemobile|opera mini/i.test(userAgent);
  const isTablet =
    /ipad|android(?!.*mobile)/i.test(userAgent) ||
    (navigator.maxTouchPoints > 0 && /macintosh/i.test(userAgent));

  if (isMobile) return DeviceType.MOBILE;
  if (isTablet) return DeviceType.TABLET;
  return null; // 데스크톱은 null 반환 (FCM 토큰 요청 안 함)
};

// 실제 콘텐츠 분리 - Suspense로 감싸기 위한 컴포넌트
function KakaoCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<string>("처리 중...");
  const { checkAuth } = useAuthStore();

  // FCM 토큰 요청 및 서버 전송 함수
  const requestAndSendFcmToken = async () => {
    const deviceType = getDeviceType();

    // 데스크톱(deviceType이 null)인 경우 토큰 요청 및 전송 중단
    if (deviceType === null) {
      console.log("데스크톱 환경에서는 FCM 토큰을 요청하지 않습니다.");
      return;
    }

    try {
      // FCM 토큰 처리는 오류가 발생해도 로그인 과정에 영향을 주지 않아야 함
      if (typeof window === "undefined") return;

      // 알림 권한 확인
      if (!("Notification" in window)) {
        return;
      }

      // 권한 요청
      const permission = await Notification.requestPermission();
      if (permission !== "granted") {
        return;
      }

      // 간소화된 구조로 토큰 요청 시도
      const app = initializeApp(firebaseConfig);
      const messaging = getMessaging(app);

      // 서비스 워커 등록
      const swRegistration = await navigator.serviceWorker.register(
        "/firebase-messaging-sw.js"
      );

      // FCM 토큰 요청 (VAPID 키에 문제가 있을 수 있음)
      const token = await getToken(messaging, {
        serviceWorkerRegistration: swRegistration,
        // VAPID 키를 일시적으로 제거하고 테스트
      });

      // 서버로 전송
      if (token != null) {
        await fetchClient(
          `${API_BASE}/notification/fcm/token?deviceType=${deviceType}`,
          {
            method: "POST",
            headers: { "Content-Type": "text/plain" },
            body: token,
          }
        );
      }
    } catch (error) {
      // 오류 발생해도 로그인 진행에 영향 없음
      console.error("FCM 토큰 처리 오류 (무시 가능):", error);
    }
  };

  useEffect(() => {
    const processKakaoLogin = async () => {
      // URL에서 코드 파라미터 추출
      const code = searchParams.get("code");
      if (!code) {
        setStatus("인증 코드가 없습니다. 다시 로그인해주세요.");
        return;
      }

      try {
        setStatus("카카오 로그인 정보를 처리 중입니다...");

        // 백엔드 서버로 코드 전송
        const response = await fetchClient(
          `${API_BASE}/auth/login?code=${code}`
        );

        if (!response.ok) {
          throw new Error("로그인 처리 중 오류가 발생했습니다.");
        }

        // 응답 처리
        try {
          // 응답에 내용이 있는지 확인
          const text = await response.text();

          // 응답이 비어있으면 기존 회원
          if (!text) {
            // FCM 토큰 요청 및 전송 후 기존 회원 처리
            await requestAndSendFcmToken();
            await handleExistingUser();
            return;
          }

          // 응답이 있으면 JSON으로 파싱 시도
          const data = text ? JSON.parse(text) : null;

          if (typeof data === "number") {
            // 신규 회원 - 농장 이름 설정 페이지로 이동
            router.push(`/auth/signup?tokenId=${data}`);
          } else {
            // 예상치 못한 응답 형식이지만 로그인 처리
            // FCM 토큰 요청 및 전송 후 기존 회원 처리
            await requestAndSendFcmToken();
            await handleExistingUser();
          }
        } catch (jsonError) {
          // JSON 파싱 오류는 기존 회원으로 처리
          console.error("응답 처리 오류, 기존 회원으로 처리:", jsonError);
          // FCM 토큰 요청 및 전송 후 기존 회원 처리
          await requestAndSendFcmToken();
          await handleExistingUser();
        }
      } catch (error) {
        console.error("카카오 로그인 처리 오류:", error);
        setStatus("로그인 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
      }
    };

    // 기존 회원 처리 함수
    const handleExistingUser = async () => {
      setStatus("로그인 성공! 홈페이지로 이동합니다...");
      await checkAuth(); // 인증 상태 업데이트
      router.push("/");
    };

    processKakaoLogin();
  }, [searchParams, router, checkAuth]);

  return (
    <div className="text-center">
      <h3 className="mb-4">카카오 로그인</h3>
      <div className="d-flex justify-content-center mb-3">
        <LoadingSpinner />
      </div>
      <p className="mb-0">{status}</p>
    </div>
  );
}

// 로딩 중 컴포넌트
function LoadingCard() {
  return (
    <div className="text-center">
      <h3 className="mb-4">카카오 로그인</h3>
      <div className="d-flex justify-content-center mb-3">
        <LoadingSpinner />
      </div>
      <p className="mb-0">준비 중...</p>
    </div>
  );
}

// 메인 페이지 컴포넌트
export default function KakaoCallbackPage() {
  return (
    <main className="flex-shrink-0">
      <div className="container px-5 py-5">
        <div className="row justify-content-center">
          <div className="col-lg-6">
            <Suspense fallback={<LoadingCard />}>
              <KakaoCallbackContent />
            </Suspense>
          </div>
        </div>
      </div>
    </main>
  );
}
