"use client";

import { useState } from "react";
import { Dialog, DialogContent } from "@/components/molecules/dialog";
import { Button } from "@/components/atoms/button";
import { useBrowserGuide } from "@/hooks/useBrowserGuide";
import { PWAInstallButton } from "@/components/molecules/pwa-install-button";

export function BrowserGuideModal() {
  const { showGuide, hideGuide, getBrowserInfo } = useBrowserGuide();
  const [showDurationOptions, setShowDurationOptions] = useState(false);
  const [copiedToClipboard, setCopiedToClipboard] = useState(false);

  const browserInfo = getBrowserInfo();

  const copyToClipboard = async () => {
    try {
      // 클라이언트 환경에서만 실행
      if (typeof window !== "undefined" && navigator.clipboard) {
        await navigator.clipboard.writeText(window.location.href);
        setCopiedToClipboard(true);
        setTimeout(() => setCopiedToClipboard(false), 2000);
      }
    } catch (err) {
      console.error("링크 복사에 실패했습니다:", err);
    }
  };

  const handleHideGuide = (
    duration: "session" | "1h" | "24h" | "7d" | "forever"
  ) => {
    hideGuide(duration);
    setShowDurationOptions(false);
  };

  if (!showGuide) return null;

  return (
    <Dialog open={showGuide} onOpenChange={() => hideGuide()}>
      <DialogContent className="p-6 max-w-md mx-auto">
        <div className="text-center mb-6">
          <div className="text-4xl mb-3">📱</div>
          <h2 className="text-xl font-bold text-gray-800 mb-2">
            더 나은 이용을 위해 앱으로 설치해보세요!
          </h2>
          <p className="text-sm text-gray-600 leading-relaxed">
            현재 {browserInfo.name}에서 이용 중이시네요.
            <br />
            비밀로그를 앱으로 설치하면 더 빠르고 편리하게 이용할 수 있어요.
          </p>
        </div>

        <div className="space-y-4 mb-6">
          {/* PWA 설치 버튼 (지원 브라우저에서만 표시) */}
          <div className="text-center">
            <PWAInstallButton className="w-full" />
          </div>

          {/* 브라우저 여는 방법 안내 */}
          <div className="bg-blue-50 p-4 rounded-lg">
            <h3 className="font-semibold text-blue-800 mb-2 flex items-center">
              🌐 브라우저에서 열기
            </h3>
            <ol className="text-sm text-blue-700 space-y-1">
              <li>1. 아래 버튼으로 링크를 복사하세요</li>
              <li>2. Chrome, Safari 등 브라우저 앱에서 붙여넣기</li>
              <li>3. 더 빠르고 안정적으로 이용하세요!</li>
            </ol>
          </div>

          {/* 링크 복사 버튼 */}
          <Button
            onClick={copyToClipboard}
            variant="outline"
            className="w-full"
          >
            {copiedToClipboard ? "✅ 링크 복사됨!" : "🔗 링크 복사하기"}
          </Button>
        </div>

        {/* 하단 액션 버튼들 */}
        <div className="space-y-3">
          {!showDurationOptions ? (
            <div className="flex gap-2">
              <Button
                onClick={() => hideGuide("session")}
                variant="outline"
                size="sm"
                className="flex-1"
              >
                오늘 안 보기
              </Button>
              <Button
                onClick={() => setShowDurationOptions(true)}
                variant="outline"
                size="sm"
                className="flex-1"
              >
                더 오래 숨기기
              </Button>
            </div>
          ) : (
            <div className="space-y-2">
              <div className="grid grid-cols-2 gap-2">
                <Button
                  onClick={() => handleHideGuide("1h")}
                  variant="outline"
                  size="sm"
                >
                  1시간
                </Button>
                <Button
                  onClick={() => handleHideGuide("24h")}
                  variant="outline"
                  size="sm"
                >
                  하루
                </Button>
                <Button
                  onClick={() => handleHideGuide("7d")}
                  variant="outline"
                  size="sm"
                >
                  일주일
                </Button>
                <Button
                  onClick={() => handleHideGuide("forever")}
                  variant="outline"
                  size="sm"
                >
                  영원히
                </Button>
              </div>
              <Button
                onClick={() => setShowDurationOptions(false)}
                variant="outline"
                size="sm"
                className="w-full"
              >
                돌아가기
              </Button>
            </div>
          )}
        </div>

        {/* 디버깅 정보 (개발 환경에서만) */}
        {process.env.NODE_ENV === "development" && (
          <div className="mt-4 p-3 bg-gray-100 rounded text-xs">
            <p>브라우저: {browserInfo.name}</p>
            <p>인앱: {browserInfo.isInApp ? "Yes" : "No"}</p>
            <p>
              User Agent:{" "}
              {typeof navigator !== "undefined" ? navigator.userAgent : "N/A"}
            </p>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
