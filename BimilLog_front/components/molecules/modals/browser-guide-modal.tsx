"use client";

import { useState } from "react";
import dynamic from "next/dynamic";
import { Modal, ModalHeader, ModalBody, ModalFooter } from "flowbite-react";
import { Button, Spinner } from "@/components";
import { useBrowserGuide } from "@/hooks";
import { Smartphone } from "lucide-react";
import { APP_LINKS } from "@/lib/constants/app";

interface BrowserGuideModalProps {
  isOpen: boolean;
  onOpenChange: (isOpen: boolean) => void;
}

// 로딩 컴포넌트
const BrowserGuideModalLoading = () => (
  <Modal show onClose={() => {}} size="md">
    <ModalBody>
      <div className="flex items-center justify-center min-h-[300px]">
        <div className="flex flex-col items-center gap-3">
          <Spinner size="lg" />
          <p className="text-sm text-brand-secondary">브라우저 가이드 로딩 중...</p>
        </div>
      </div>
    </ModalBody>
  </Modal>
);

// 실제 모달 컴포넌트
function BrowserGuideModalContent({
  isOpen,
  onOpenChange,
}: BrowserGuideModalProps) {
  const { showGuide, hideGuide, getBrowserInfo } = useBrowserGuide();

  // 브라우저 정보 가져오기
  const browserInfo = getBrowserInfo();

  // iOS 기기 감지: iPad, iPhone, iPod 확인 (MSStream은 IE 구분용)
  const isIOS =
    typeof navigator !== "undefined" &&
    /iPad|iPhone|iPod/.test(navigator.userAgent) &&
    !(window as unknown as { MSStream?: unknown }).MSStream;

  // Android 기기 감지
  const isAndroidDevice =
    typeof navigator !== "undefined" &&
    /android/i.test(navigator.userAgent);

  // 데스크톱 사용자는 모달 표시하지 않음
  const isMobileDevice = isIOS || isAndroidDevice;

  // 모달 표시 조건: props로 받은 isOpen 또는 hook의 showGuide
  const effectiveShow = isOpen || showGuide;

  // 데스크톱이면 모달 표시 안 함
  if (!effectiveShow || !isMobileDevice) return null;

  // 모달 닫기: duration 파라미터 추가
  const handleClose = (duration: "session" | "7d" = "session") => {
    if (isOpen) {
      onOpenChange(false);
    } else {
      hideGuide(duration);
    }
  };

  // 확인 버튼 핸들러
  const handleConfirm = () => {
    if (isAndroidDevice) {
      // Android: 플레이스토어로 이동
      window.location.href = APP_LINKS.PLAY_STORE;
    } else if (isIOS) {
      // iOS: /install 페이지로 이동
      window.location.href = "/install";
    }
  };

  return (
    <Modal show={effectiveShow} onClose={() => handleClose("session")} size="md">
      <ModalHeader className="text-center">
        <div className="flex flex-col items-center">
          <Smartphone className="w-10 h-10 mb-3 text-indigo-600" />
          <span className="text-xl font-bold text-brand-primary">
            더 나은 이용을 위해 앱으로 설치해보세요!
          </span>
        </div>
      </ModalHeader>
      <ModalBody>
        <p className="text-sm text-brand-muted leading-relaxed text-center mb-6">
          비밀로그를 앱으로 설치하면 더 빠르고 편리하게 이용할 수 있어요.
        </p>

        {/* 플랫폼별 홍보 문구 */}
        {isIOS ? (
          // iOS: PWA 앱 홍보
          <div className="bg-blue-50 p-6 rounded-lg text-center">
            <Smartphone className="w-16 h-16 mx-auto mb-4 text-blue-600" />
            <h3 className="font-bold text-blue-900 mb-2 text-lg">
              iPhone/iPad 전용 앱
            </h3>
            <p className="text-sm text-blue-700 leading-relaxed">
              Safari 브라우저에서 홈 화면에 추가하여<br />
              네이티브 앱처럼 사용할 수 있습니다.
            </p>
          </div>
        ) : isAndroidDevice ? (
          // Android: 플레이스토어 앱 홍보
          <div className="bg-green-50 p-6 rounded-lg text-center">
            <Smartphone className="w-16 h-16 mx-auto mb-4 text-green-600" />
            <h3 className="font-bold text-green-900 mb-2 text-lg">
              비밀로그 공식 앱
            </h3>
            <p className="text-sm text-green-700 leading-relaxed">
              플레이스토어에서 공식 앱을 다운로드하여<br />
              더 빠르고 안정적으로 이용하세요!
            </p>
          </div>
        ) : null}

        {/* 디버깅 정보 (개발 환경에서만) */}
        {process.env.NODE_ENV === "development" && (
          <div className="mt-4 p-3 bg-gray-100 rounded text-xs">
            <p>브라우저: {browserInfo.name}</p>
            <p>iOS: {isIOS ? "Yes" : "No"}</p>
            <p>Android: {isAndroidDevice ? "Yes" : "No"}</p>
            <p>
              User Agent:{" "}
              {typeof navigator !== "undefined" ? navigator.userAgent : "N/A"}
            </p>
          </div>
        )}
      </ModalBody>
      <ModalFooter className="flex flex-col gap-3">
        {/* 메인 액션 버튼 (확인) */}
        <Button
          onClick={handleConfirm}
          className="w-full bg-indigo-600 hover:bg-indigo-700 text-white"
        >
          {isIOS ? "앱 설치 방법 보기" : "플레이스토어에서 다운로드"}
        </Button>

        {/* 보조 버튼들 (가로 2개) */}
        <div className="flex gap-2 w-full">
          <Button
            onClick={() => handleClose("session")}
            variant="outline"
            className="flex-1"
          >
            닫기
          </Button>
          <Button
            onClick={() => handleClose("7d")}
            variant="outline"
            className="flex-1"
          >
            일주일동안 안보기
          </Button>
        </div>
      </ModalFooter>
    </Modal>
  );
}

// 전체 모달을 Dynamic import로 래핑 (클라이언트 전용, 초기 번들 크기 감소)
const BrowserGuideModal = dynamic(
  () => Promise.resolve(BrowserGuideModalContent),
  {
    ssr: false, // 브라우저 감지 로직으로 인해 서버 렌더링 비활성화
    loading: () => <BrowserGuideModalLoading />, // 로딩 중 스켈레톤 UI 표시
  }
);

export { BrowserGuideModal };
