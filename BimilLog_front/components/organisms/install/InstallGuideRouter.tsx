"use client";

import React, { useState } from "react";
import { Button, Card, CardContent } from "@/components";
import { Spinner as FlowbiteSpinner } from "flowbite-react";
import {
  Smartphone,
  ExternalLink,
  AlertTriangle,
  Monitor,
  Apple,
} from "lucide-react";
import { useBrowserGuide, useBrowserDetection } from "@/hooks";
import { APP_LINKS } from "@/lib/constants/app";

interface InstallStepProps {
  step: number;
  children: React.ReactNode;
  bgColor?: string;
}

function InstallStep({ step, children, bgColor = "bg-stamp-red/10" }: InstallStepProps) {
  return (
    <li className="flex gap-4 items-start">
      <span
        aria-hidden="true"
        className={`font-display font-bold text-base text-stamp-red ${bgColor} border border-stamp-red/30 min-w-[2rem] w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0`}
      >
        {step}
      </span>
      <span className="font-body text-base leading-relaxed flex-1 break-keep">
        {children}
      </span>
    </li>
  );
}

interface InstallGuideCardProps {
  id?: string;
  title: string;
  icon: React.ComponentType<{ className?: string; "aria-hidden"?: boolean | "true" | "false" }>;
  borderColor: string;
  textColor: string;
  children: React.ReactNode;
  /** 카드 하단 보조 안내. */
  extra?: React.ReactNode;
}

function InstallGuideCard({
  id,
  title,
  icon: Icon,
  borderColor,
  textColor,
  children,
  extra,
}: InstallGuideCardProps) {
  return (
    <Card
      id={id}
      className={`bg-paper-aged ${borderColor} w-full max-w-4xl mx-auto shadow-brand-xl scroll-mt-24`}
    >
      <CardContent className="p-6 sm:p-8 md:p-10">
        <h3
          className={`font-display font-bold ${textColor} mb-6 flex items-center justify-center gap-3 text-xl break-keep`}
        >
          <Icon className="w-6 h-6" aria-hidden="true" />
          {title}
        </h3>
        <ol className={`text-left ${textColor} space-y-4`}>{children}</ol>
        {extra && <div className="mt-6">{extra}</div>}
      </CardContent>
    </Card>
  );
}

/**
 * /install 페이지 디바이스/브라우저 분기 라우터.
 *
 * 분기 종류 (라운드 14):
 *  - kakao-inapp / line-inapp / instagram-inapp / facebook-inapp → InAppGuide (F-14-BUG-9)
 *  - ios-safari → Safari PWA 가이드 (F-14-BUG-11 trap 방지)
 *  - ios-chrome → Chrome 가이드 + Safari 권장 (F-14-016)
 *  - android-chrome → PWA 설치 + 플레이스토어 (F-14-BUG-12)
 *  - desktop → PWA 설치 + 플레이스토어 새 탭 (F-14-BUG-10/13)
 */
export function InstallGuideRouter() {
  const detection = useBrowserDetection();
  const { isPWAInstallable, installPWA } = useBrowserGuide();
  const [pwaError, setPwaError] = useState<string | null>(null);

  // SSR / hydration 전: 가이드 자리 placeholder
  if (!detection.isClient) {
    return (
      <div
        className="flex items-center justify-center min-h-[200px]"
        aria-live="polite"
      >
        <FlowbiteSpinner color="failure" size="xl" aria-label="브라우저 감지 중" />
      </div>
    );
  }

  const handlePWAInstall = async () => {
    try {
      setPwaError(null);
      await installPWA();
    } catch (err) {
      setPwaError(err instanceof Error ? err.message : "설치에 실패했습니다.");
    }
  };

  // 1) InApp 분기 — 우선 처리 (PWA 설치 자체 불가)
  if (detection.isInApp) {
    return (
      <Card
        id="inapp-guide"
        className="bg-paper-aged border-2 border-stamp-red/40 w-full max-w-4xl mx-auto shadow-brand-xl scroll-mt-24"
      >
        <CardContent className="p-6 sm:p-8 md:p-10">
          <div className="flex flex-col items-center text-center">
            <AlertTriangle
              className="w-16 h-16 mb-4 text-stamp-red"
              aria-hidden="true"
            />
            <h3 className="font-display font-bold text-2xl text-ink dark:text-foreground mb-3 break-keep tracking-tight">
              지금은 인앱 브라우저예요
            </h3>
            <p className="font-body text-base sm:text-lg text-ink-soft dark:text-muted-foreground leading-relaxed mb-6 max-w-2xl break-keep">
              {detection.name}에서는 앱으로 설치할 수 없어요. 아래 단계로 외부
              브라우저에서 다시 열어주세요.
            </p>
            <ol className="text-left text-ink dark:text-foreground space-y-3 mb-6 w-full max-w-md">
              <InstallStep step={1}>
                화면 우측 상단의{" "}
                <span className="font-bold bg-stamp-red/10 text-stamp-red px-2 py-1 rounded">
                  [⋯ 더보기]
                </span>{" "}
                버튼을 누르세요
              </InstallStep>
              <InstallStep step={2}>
                <span className="font-bold bg-stamp-red/10 text-stamp-red px-2 py-1 rounded">
                  [다른 브라우저로 열기]
                </span>{" "}
                또는{" "}
                <span className="font-bold bg-stamp-red/10 text-stamp-red px-2 py-1 rounded">
                  [Chrome에서 열기]
                </span>
                를 선택
              </InstallStep>
              <InstallStep step={3}>
                Chrome 또는 Safari 에서 이 페이지를 다시 열고 안내를 따라 설치하세요
              </InstallStep>
            </ol>
            <p className="font-body text-sm text-ink-soft dark:text-muted-foreground break-keep">
              현재 브라우저: <span className="font-medium">{detection.name}</span>
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  // 2) iOS Safari — 무한 루프 방지: PWAInstallButton 대신 인페이지 가이드 직접 노출
  if (detection.kind === "ios-safari") {
    return (
      <InstallGuideCard
        id="ios-safari-guide"
        title="iPhone/iPad Safari 설치 방법"
        icon={Apple}
        borderColor="border-2 border-postal-navy/40"
        textColor="text-ink dark:text-foreground"
      >
        <InstallStep step={1} bgColor="bg-postal-navy/10">
          Safari 브라우저에서 이 페이지를 여세요
        </InstallStep>
        <InstallStep step={2} bgColor="bg-postal-navy/10">
          하단 메뉴의{" "}
          <span className="font-bold bg-postal-navy/10 text-postal-navy px-2 py-1 rounded">
            [공유]
          </span>{" "}
          버튼을 누르세요
        </InstallStep>
        <InstallStep step={3} bgColor="bg-postal-navy/10">
          <span className="font-bold bg-postal-navy/10 text-postal-navy px-2 py-1 rounded">
            [홈 화면에 추가]
          </span>
          를 선택하면 설치 완료!
        </InstallStep>
      </InstallGuideCard>
    );
  }

  // 3) iOS Chrome — Apple 정책상 진짜 PWA 불가 → Safari 권장 카피 추가
  if (detection.kind === "ios-chrome") {
    return (
      <InstallGuideCard
        id="ios-chrome-guide"
        title="iPhone/iPad Chrome 설치 방법"
        icon={Smartphone}
        borderColor="border-2 border-stamp-red/40"
        textColor="text-ink dark:text-foreground"
        extra={
          <div className="bg-postal-navy/10 border border-postal-navy/30 rounded-lg p-4 text-left">
            <p className="font-body text-sm text-ink dark:text-foreground leading-relaxed break-keep">
              <span className="font-semibold text-postal-navy">참고:</span>{" "}
              iPhone에서는 Safari를 사용하면 더 완성된 앱 경험을 받을 수
              있어요. 가능하다면 Safari로 다시 열어보세요.
            </p>
          </div>
        }
      >
        <InstallStep step={1} bgColor="bg-stamp-red/10">
          Chrome 앱에서 이 페이지를 여세요
        </InstallStep>
        <InstallStep step={2} bgColor="bg-stamp-red/10">
          오른쪽 상단{" "}
          <span className="font-bold bg-stamp-red/10 text-stamp-red px-2 py-1 rounded">
            [⋯ 메뉴]
          </span>{" "}
          버튼을 탭하세요
        </InstallStep>
        <InstallStep step={3} bgColor="bg-stamp-red/10">
          <span className="font-bold bg-stamp-red/10 text-stamp-red px-2 py-1 rounded">
            [홈 화면에 추가]
          </span>
          를 선택하면 설치 완료!
        </InstallStep>
      </InstallGuideCard>
    );
  }

  // 4) Android Chrome — PWA + 플레이스토어 동시 옵션
  if (detection.kind === "android-chrome") {
    return (
      <Card
        id="android-guide"
        className="bg-paper-aged border-2 border-stamp-red/40 w-full max-w-4xl mx-auto shadow-brand-xl scroll-mt-24"
      >
        <CardContent className="p-6 sm:p-8 md:p-10">
          <div className="flex flex-col items-center text-center">
            <Smartphone
              className="w-20 h-20 mb-6 text-stamp-red stroke-[1.5]"
              aria-hidden="true"
            />
            <h3 className="font-display font-bold text-ink dark:text-foreground mb-4 text-2xl tracking-tight break-keep">
              Android에서 설치하기
            </h3>
            <p className="font-body text-base sm:text-lg text-ink-soft dark:text-muted-foreground leading-relaxed mb-8 max-w-lg break-keep">
              Chrome 의 빠른 PWA 설치와 플레이스토어 정식 앱 다운로드 중 원하는
              방법을 선택하세요.
            </p>

            <div className="flex flex-col sm:flex-row gap-3 w-full max-w-md">
              {isPWAInstallable && (
                <Button
                  onClick={handlePWAInstall}
                  size="lg"
                  className="bg-stamp-red hover:bg-stamp-red/90 text-white font-bold shadow-brand-xl rounded-2xl flex-1"
                >
                  앱으로 바로 설치
                </Button>
              )}
              <Button
                asChild
                size="lg"
                variant={isPWAInstallable ? "outline" : "default"}
                className={
                  isPWAInstallable
                    ? "flex-1 border-stamp-red/40 text-stamp-red hover:bg-stamp-red/10 rounded-2xl"
                    : "flex-1 bg-stamp-red hover:bg-stamp-red/90 text-white font-bold shadow-brand-xl rounded-2xl"
                }
              >
                <a
                  href={APP_LINKS.PLAY_STORE}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center justify-center gap-2"
                >
                  플레이스토어에서 받기
                  <ExternalLink className="w-4 h-4" aria-hidden="true" />
                </a>
              </Button>
            </div>

            {pwaError && (
              <p
                role="alert"
                className="mt-4 text-sm text-stamp-red break-keep"
              >
                {pwaError}
              </p>
            )}
          </div>
        </CardContent>
      </Card>
    );
  }

  // 5) Desktop — PWA 설치 + 플레이스토어 새 탭
  return (
    <Card
      id="desktop-guide"
      className="bg-paper-aged border-2 border-postal-navy/40 w-full max-w-4xl mx-auto shadow-brand-xl scroll-mt-24"
    >
      <CardContent className="p-6 sm:p-8 md:p-10">
        <div className="flex flex-col items-center text-center">
          <Monitor
            className="w-20 h-20 mb-6 text-postal-navy stroke-[1.5]"
            aria-hidden="true"
          />
          <h3 className="font-display font-bold text-ink dark:text-foreground mb-4 text-2xl tracking-tight break-keep">
            데스크톱에서 설치하기
          </h3>
          <p className="font-body text-base sm:text-lg text-ink-soft dark:text-muted-foreground leading-relaxed mb-6 max-w-2xl break-keep">
            Chrome 또는 Edge 사용 시 주소창 우측의 [설치] 아이콘을 눌러 앱처럼
            이용할 수 있어요. 모바일로 받고 싶다면 플레이스토어 링크를 새 탭에서
            열어주세요.
          </p>

          <ol className="text-left text-ink dark:text-foreground space-y-3 mb-6 w-full max-w-md">
            <InstallStep step={1} bgColor="bg-postal-navy/10">
              Chrome 또는 Edge 브라우저로 이 페이지를 여세요
            </InstallStep>
            <InstallStep step={2} bgColor="bg-postal-navy/10">
              주소창 우측의{" "}
              <span className="font-bold bg-postal-navy/10 text-postal-navy px-2 py-1 rounded">
                [설치 ⊕]
              </span>{" "}
              아이콘을 클릭
            </InstallStep>
            <InstallStep step={3} bgColor="bg-postal-navy/10">
              팝업에서 <span className="font-bold">[설치]</span>를 선택하면 완료!
            </InstallStep>
          </ol>

          <div className="flex flex-col sm:flex-row gap-3 w-full max-w-md">
            {isPWAInstallable && (
              <Button
                onClick={handlePWAInstall}
                size="lg"
                className="bg-postal-navy hover:bg-postal-navy/90 text-white font-bold shadow-brand-xl rounded-2xl flex-1"
              >
                지금 설치하기
              </Button>
            )}
            <Button
              asChild
              size="lg"
              variant="outline"
              className="flex-1 border-postal-navy/40 text-postal-navy hover:bg-postal-navy/10 rounded-2xl"
            >
              <a
                href={APP_LINKS.PLAY_STORE}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center gap-2"
              >
                모바일은 플레이스토어
                <ExternalLink className="w-4 h-4" aria-hidden="true" />
              </a>
            </Button>
          </div>

          {pwaError && (
            <p role="alert" className="mt-4 text-sm text-stamp-red break-keep">
              {pwaError}
            </p>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
