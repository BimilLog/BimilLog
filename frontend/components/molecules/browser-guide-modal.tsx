"use client";

import { useState } from "react";
import { Dialog, DialogContent } from "@/components/molecules/dialog";
import { Button } from "@/components/atoms/button";
import { useBrowserGuide } from "@/hooks/useBrowserGuide";
import { PWAInstallButton } from "@/components/molecules/pwa-install-button";
import { CheckCircle, Link } from "lucide-react";

interface BrowserGuideModalProps {
  isOpen: boolean;
  onOpenChange: (isOpen: boolean) => void;
}

export function BrowserGuideModal({
  isOpen,
  onOpenChange,
}: BrowserGuideModalProps) {
  const { showGuide, hideGuide, getBrowserInfo } = useBrowserGuide();
  const [copiedToClipboard, setCopiedToClipboard] = useState(false);

  const browserInfo = getBrowserInfo();
  const isIOS =
    typeof navigator !== "undefined" &&
    /iPad|iPhone|iPod/.test(navigator.userAgent) &&
    !(window as any).MSStream;

  const copyToClipboard = async () => {
    try {
      if (typeof window !== "undefined" && navigator.clipboard) {
        await navigator.clipboard.writeText("https://grow-farm.com/install");
        setCopiedToClipboard(true);
        setTimeout(() => setCopiedToClipboard(false), 2000);
      }
    } catch (err) {
      console.error("링크 복사에 실패했습니다:", err);
    }
  };

  const effectiveShow = isOpen || showGuide;

  if (!effectiveShow) return null;

  const handleClose = () => {
    if (isOpen) {
      onOpenChange(false);
    } else {
      hideGuide();
    }
  };

  return (
    <Dialog open={effectiveShow} onOpenChange={handleClose}>
      <DialogContent className="p-6 max-w-md mx-auto">
        <div className="text-center mb-6">
          <div className="text-4xl mb-3">📱</div>
          <h2 className="text-xl font-bold text-gray-800 mb-2">
            더 나은 이용을 위해 앱으로 설치해보세요!
          </h2>
          <p className="text-sm text-gray-600 leading-relaxed">
            비밀로그를 앱으로 설치하면 더 빠르고 편리하게 이용할 수 있어요.
          </p>
        </div>

        <div className="space-y-4 mb-6">
          {isIOS ? (
            <div className="bg-blue-50 p-4 rounded-lg">
              <h3 className="font-semibold text-blue-800 mb-2 flex items-center">
                iPhone/iPad에서 홈 화면에 추가하기
              </h3>
              <ol className="text-sm text-blue-700 space-y-1">
                <li>1. Safari 브라우저에서 이 페이지를 여세요.</li>
                <li>
                  2. 하단 메뉴의 <span className="font-bold">[공유]</span>{" "}
                  버튼을 누르세요.
                </li>
                <li>
                  3. <span className="font-bold">[홈 화면에 추가]</span>를
                  선택하면 설치 완료!
                </li>
              </ol>
            </div>
          ) : (
            <>
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
                  <li>2. Chrome, Edge 등 브라우저 앱에서 붙여넣기</li>
                  <li>3. 주소창의 설치 버튼을 눌러 설치하세요!</li>
                </ol>
              </div>

              {/* 링크 복사 버튼 */}
              <Button
                onClick={copyToClipboard}
                variant="outline"
                className="w-full"
              >
                {copiedToClipboard ? (
                  <div className="flex items-center space-x-2">
                    <CheckCircle className="w-4 h-4" />
                    <span>링크 복사됨!</span>
                  </div>
                ) : (
                  <div className="flex items-center space-x-2">
                    <Link className="w-4 h-4" />
                    <span>링크 복사하기</span>
                  </div>
                )}
              </Button>
            </>
          )}
        </div>

        <Button onClick={handleClose} variant="outline" className="w-full">
          닫기
        </Button>

        {/* 디버깅 정보 (개발 환경에서만) */}
        {process.env.NODE_ENV === "development" && (
          <div className="mt-4 p-3 bg-gray-100 rounded text-xs">
            <p>브라우저: {browserInfo.name}</p>
            <p>iOS: {isIOS ? "Yes" : "No"}</p>
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
