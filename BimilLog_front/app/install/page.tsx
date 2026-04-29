"use client";

import { Button, Card, CardContent } from "@/components";
import { Spinner as FlowbiteSpinner } from "flowbite-react";
import {
  Smartphone,
  MessageCircle,
  ArrowLeft,
  Zap,
  Shield,
  Wifi,
} from "lucide-react";
import Link from "next/link";
import { PWAInstallButton } from "@/components/molecules/pwa-install-button";
import { useBrowserGuide } from "@/hooks";
import { useState, useEffect } from "react";
import { isIOS, isSafari } from "@/lib/utils";
import { CleanLayout } from "@/components/organisms/layout/BaseLayout";
import { APP_LINKS } from "@/lib/constants/app";

// Style constants — 종이/편지 메타포 통합
const GRADIENTS = {
  primary: "bg-paper",
  titleText: "font-display text-stamp-red tracking-tight",
  button: "bg-stamp-red hover:bg-stamp-red/90",
  card: "bg-paper-card backdrop-blur-sm",
} as const;

const FEATURE_BENEFITS = [
  {
    icon: Zap,
    title: "빠른 접속",
    description: "홈 화면에서 바로 실행할 수 있어 더욱 빠르게 접속 가능해요",
    gradient: "bg-stamp-red",
  },
  {
    icon: MessageCircle,
    title: "알림 받기",
    description: "새로운 메시지나 댓글 알림을 바로 받아볼 수 있어요",
    gradient: "bg-postal-navy",
  },
  {
    icon: Wifi,
    title: "오프라인 지원",
    description: "인터넷 연결이 없어도 이전에 본 내용을 계속 확인할 수 있어요",
    gradient: "bg-stamp-red",
  },
  {
    icon: Shield,
    title: "보안 강화",
    description: "전용 앱으로 더 안전하고 개인적인 공간을 제공해요",
    gradient: "bg-postal-navy",
  },
];

// Reusable components
interface InstallStepProps {
  step: number;
  children: React.ReactNode;
  bgColor?: string;
}

function InstallStep({ step, children, bgColor = "bg-stamp-red/10" }: InstallStepProps) {
  return (
    <li className="flex gap-4 items-start">
      <span className={`font-display font-bold text-base text-stamp-red ${bgColor} border border-stamp-red/30 min-w-[2rem] w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0`}>
        {step}
      </span>
      <span className="font-body text-base leading-relaxed flex-1">{children}</span>
    </li>
  );
}

interface InstallGuideCardProps {
  title: string;
  icon: React.ComponentType<{ className?: string }>;
  bgGradient: string;
  borderColor: string;
  textColor: string;
  stepBgColor: string;
  children: React.ReactNode;
}

function InstallGuideCard({
  title,
  icon: Icon,
  bgGradient,
  borderColor,
  textColor,
  children
}: InstallGuideCardProps) {
  return (
    <Card className={`${bgGradient} ${borderColor} w-full max-w-4xl mx-auto shadow-brand-xl`}>
      <CardContent className="p-6 sm:p-8 md:p-10">
        <h3 className={`font-bold ${textColor} mb-6 flex items-center justify-center gap-3 text-xl`}>
          <Icon className="w-6 h-6" />
          {title}
        </h3>
        <ol className={`text-left ${textColor} space-y-4`}>
          {children}
        </ol>
      </CardContent>
    </Card>
  );
}

interface FeatureCardProps {
  icon: React.ComponentType<{ className?: string }>;
  title: string;
  description: string;
  gradient: string;
}

function FeatureCard({ icon: Icon, title, description, gradient }: FeatureCardProps) {
  return (
    <Card className="border-2 border-ink-soft shadow-brand-xl hover:shadow-brand-2xl transition-all duration-300 bg-paper-card backdrop-blur-sm hover:scale-105 group">
      <CardContent className="p-8 text-center">
        <div className={`w-20 h-20 ${gradient} rounded-2xl flex items-center justify-center mx-auto mb-6 group-hover:scale-110 transition-transform duration-300 shadow-lg`}>
          <Icon className="w-10 h-10 text-white stroke-[2.5]" />
        </div>
        <h3 className="font-display text-xl font-bold mb-4 text-ink dark:text-foreground tracking-tight">{title}</h3>
        <p className="font-body text-ink-soft dark:text-muted-foreground leading-relaxed">{description}</p>
      </CardContent>
    </Card>
  );
}

export default function InstallPage() {
  const [isClient, setIsClient] = useState(false);
  const { getBrowserInfo } = useBrowserGuide();

  const browserInfo = isClient ? getBrowserInfo() : { name: "브라우저", isInApp: false };
  const isIOSDevice = isClient ? isIOS() : false;
  const isSafariBrowser = isClient ? isSafari() : false;

  useEffect(() => {
    setIsClient(true);
  }, []);

  // Loading state for SSR
  if (!isClient) {
    return (
      <CleanLayout className={GRADIENTS.primary}>
        <div className="container mx-auto px-4 py-16 text-center">
          <Smartphone className="w-16 h-16 mb-6 animate-pulse stroke-stamp-red fill-stamp-red/10 mx-auto" />
          <h1 className={`text-4xl md:text-6xl font-bold mb-6 ${GRADIENTS.titleText}`}>
            비밀로그를 앱으로 설치하세요
          </h1>
          <div className="flex items-center justify-center">
            <FlowbiteSpinner color="failure" size="xl" aria-label="로딩 중..." />
          </div>
        </div>
      </CleanLayout>
    );
  }

  return (
    <CleanLayout className={GRADIENTS.primary}>
      {/* Header */}
      <header data-toast-anchor className="bg-paper-card backdrop-blur-lg border-b border-ink-soft sticky top-0 z-50 shadow-brand-sm">
        <div className="container mx-auto px-4 py-4 flex items-center gap-4">
          <Button variant="ghost" size="sm" asChild className="hover:bg-paper-100 text-ink transition-colors">
            <Link href="/" className="flex items-center gap-2">
              <ArrowLeft className="w-4 h-4 stroke-slate-600" />
              뒤로가기
            </Link>
          </Button>
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-stamp-red rounded-lg flex items-center justify-center">
              <Smartphone className="w-4 h-4 text-white" />
            </div>
            <span className="font-display font-bold text-lg text-ink dark:text-foreground">비밀로그 설치</span>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="container mx-auto px-4 py-16 md:py-24 text-center">
        <div className="max-w-4xl mx-auto">
          <Smartphone className="w-20 h-20 mb-8 animate-bounce stroke-stamp-red fill-stamp-red/10 mx-auto" />
          <h1 className={`text-3xl sm:text-5xl md:text-7xl font-bold mb-8 ${GRADIENTS.titleText} leading-tight`}>
            비밀로그를
            <br className="md:hidden" />
            <span className="block">앱으로 설치하세요</span>
          </h1>
          <p className="font-body text-lg sm:text-xl md:text-2xl text-ink-soft dark:text-muted-foreground mb-12 leading-relaxed max-w-5xl mx-auto">
            더 빠르고 편리한 앱 경험으로 언제 어디서나 마음을 전해보세요
          </p>

          {/* PWA Install Button - iOS만 표시 */}
          <div className="flex flex-col gap-6 items-center mb-16">
            {isIOSDevice && (
              <PWAInstallButton
                size="lg"
                className={`${GRADIENTS.button} px-16 py-5 text-xl font-bold shadow-brand-xl transform transition-all duration-300 hover:scale-105 hover:shadow-brand-2xl rounded-2xl`}
              />
            )}
            <div className="flex items-center gap-2 text-sm font-body text-ink-soft dark:text-muted-foreground bg-paper-card backdrop-blur-sm px-4 py-2 rounded-full border border-ink-soft">
              <div className="w-3 h-3 bg-green-500 rounded-full"></div>
              현재 브라우저: {browserInfo.name}
            </div>
          </div>

          {/* Device-specific Install Guide */}
          {isIOSDevice ? (
            isSafariBrowser ? (
              // iOS Safari PWA 가이드
              <InstallGuideCard
                title="iPhone/iPad Safari 설치 방법"
                icon={Smartphone}
                bgGradient="bg-paper-aged"
                borderColor="border-2 border-postal-navy/30"
                textColor="text-ink dark:text-foreground"
                stepBgColor="bg-postal-navy/10"
              >
                <InstallStep step={1} bgColor="bg-postal-navy/10">
                  Safari 브라우저에서 이 페이지를 여세요
                </InstallStep>
                <InstallStep step={2} bgColor="bg-postal-navy/10">
                  하단 메뉴의{" "}
                  <span className="font-bold bg-postal-navy/10 text-postal-navy px-2 py-1 rounded">[공유]</span>{" "}
                  버튼을 누르세요
                </InstallStep>
                <InstallStep step={3} bgColor="bg-postal-navy/10">
                  <span className="font-bold bg-postal-navy/10 text-postal-navy px-2 py-1 rounded">[홈 화면에 추가]</span>
                  를 선택하면 설치 완료!
                </InstallStep>
              </InstallGuideCard>
            ) : (
              // iOS Chrome PWA 가이드
              <InstallGuideCard
                title="iPhone/iPad Chrome 설치 방법"
                icon={Smartphone}
                bgGradient="bg-paper-aged"
                borderColor="border-2 border-stamp-red/30"
                textColor="text-ink dark:text-foreground"
                stepBgColor="bg-stamp-red/10"
              >
                <InstallStep step={1} bgColor="bg-stamp-red/10">
                  Chrome 앱에서 이 페이지를 여세요
                </InstallStep>
                <InstallStep step={2} bgColor="bg-stamp-red/10">
                  오른쪽 상단{" "}
                  <span className="font-bold bg-stamp-red/10 text-stamp-red px-2 py-1 rounded">[메뉴 ⋯]</span>{" "}
                  버튼을 탭하세요
                </InstallStep>
                <InstallStep step={3} bgColor="bg-stamp-red/10">
                  <span className="font-bold bg-stamp-red/10 text-stamp-red px-2 py-1 rounded">[홈 화면에 추가]</span>
                  를 선택하면 설치 완료!
                </InstallStep>
              </InstallGuideCard>
            )
          ) : (
            // Android: 플레이스토어 다운로드 카드로 변경 (종이/편지 톤)
            <Card className="bg-paper-aged border-2 border-stamp-red/30 w-full max-w-4xl mx-auto shadow-brand-xl">
              <CardContent className="p-6 sm:p-8 md:p-10 text-center">
                <div className="flex flex-col items-center">
                  <Smartphone className="w-20 h-20 mb-6 text-stamp-red stroke-[1.5]" />
                  <h3 className="font-display font-bold text-ink dark:text-foreground mb-4 text-2xl tracking-tight">
                    비밀로그 공식 앱
                  </h3>
                  <p className="font-body text-lg text-ink-soft dark:text-muted-foreground leading-relaxed mb-8 max-w-lg">
                    플레이스토어에서 공식 앱을 다운로드하여<br />
                    더 빠르고 안정적으로 이용하세요!
                  </p>

                  <Button
                    onClick={() => window.location.href = APP_LINKS.PLAY_STORE}
                    size="lg"
                    className="bg-stamp-red hover:bg-stamp-red/90 text-white px-12 py-5 text-xl font-bold shadow-brand-xl transform transition-all duration-300 hover:scale-105 rounded-2xl"
                  >
                    플레이스토어에서 다운로드
                  </Button>
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </section>

      {/* Benefits Section */}
      <section className="container mx-auto px-4 py-20 bg-paper-aged/40 backdrop-blur-sm">
        <h2 className="font-display text-4xl md:text-5xl font-bold text-center mb-4 text-ink dark:text-foreground tracking-tight">
          앱으로 설치하면 더 좋은 점
        </h2>
        <p className="font-body text-center text-ink-soft dark:text-muted-foreground mb-16 text-lg mx-auto">
          브라우저보다 훨씬 빠르고 편리한 앱 경험을 제공합니다
        </p>
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
          {FEATURE_BENEFITS.map((benefit) => (
            <FeatureCard key={benefit.title} {...benefit} />
          ))}
        </div>
      </section>
    </CleanLayout>
  );
}
