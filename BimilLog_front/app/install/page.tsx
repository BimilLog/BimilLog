import type { Metadata } from "next";
import dynamic from "next/dynamic";
import { Card, CardContent } from "@/components";
import { Smartphone, MessageCircle, Zap, Shield, Wifi } from "lucide-react";
import { CleanLayout } from "@/components/organisms/layout/BaseLayout";

// 라운드 14: F-14-BUG-15 — page-level metadata
export const metadata: Metadata = {
  title: "앱 설치 안내",
  description:
    "비밀로그를 PWA 앱으로 설치하는 방법을 디바이스(iOS, Android, 데스크톱)별로 안내합니다.",
  robots: { index: true, follow: true },
  alternates: { canonical: "/install" },
  openGraph: {
    title: "비밀로그 앱 설치 안내",
    description:
      "비밀로그를 PWA 앱으로 설치하는 방법을 디바이스(iOS, Android, 데스크톱)별로 안내합니다.",
    url: "https://grow-farm.com/install",
  },
};

// 디바이스 분기 카드만 client island — Hero/Benefits 는 SSR 렌더 (F-14-BUG-18)
const InstallGuideRouter = dynamic(
  () =>
    import("@/components/organisms/install/InstallGuideRouter").then(
      (mod) => ({ default: mod.InstallGuideRouter })
    ),
  {
    loading: () => (
      <div className="flex items-center justify-center min-h-[200px]">
        <div className="text-ink-soft dark:text-muted-foreground font-body text-sm">
          브라우저 정보를 확인하는 중…
        </div>
      </div>
    ),
  }
);

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
] as const;

interface FeatureCardProps {
  icon: React.ComponentType<{ className?: string; "aria-hidden"?: boolean | "true" | "false" }>;
  title: string;
  description: string;
  gradient: string;
}

function FeatureCard({ icon: Icon, title, description, gradient }: FeatureCardProps) {
  return (
    <Card className="border-2 border-ink-soft shadow-brand-xl hover:shadow-brand-2xl transition-all duration-300 bg-paper-card backdrop-blur-sm hover:scale-105 group">
      <CardContent className="p-8 text-center">
        <div
          aria-hidden="true"
          className={`w-20 h-20 ${gradient} rounded-2xl flex items-center justify-center mx-auto mb-6 group-hover:scale-110 transition-transform duration-300 shadow-lg`}
        >
          <Icon className="w-10 h-10 text-white stroke-[2.5]" aria-hidden="true" />
        </div>
        <h3 className="font-display text-xl font-bold mb-4 text-ink dark:text-foreground tracking-tight break-keep">
          {title}
        </h3>
        <p className="font-body text-ink-soft dark:text-muted-foreground leading-relaxed break-keep">
          {description}
        </p>
      </CardContent>
    </Card>
  );
}

export default function InstallPage() {
  return (
    <CleanLayout className="bg-paper">
      {/* Hero Section — SSR 렌더 가능 (F-14-BUG-18) */}
      <section className="container mx-auto px-4 py-16 md:py-24 text-center">
        <div className="max-w-4xl mx-auto">
          <Smartphone
            className="w-20 h-20 mb-8 animate-bounce stroke-stamp-red fill-stamp-red/10 mx-auto"
            aria-hidden="true"
          />
          <h1 className="font-display text-3xl sm:text-5xl md:text-7xl font-bold mb-8 text-stamp-red tracking-tight leading-tight break-keep">
            비밀로그를
            <br className="md:hidden" />
            <span className="block">앱으로 설치하세요</span>
          </h1>
          <p className="font-body text-lg sm:text-xl md:text-2xl text-ink-soft dark:text-muted-foreground mb-12 leading-relaxed max-w-5xl mx-auto break-keep">
            더 빠르고 편리한 앱 경험으로 언제 어디서나 마음을 전해보세요
          </p>

          {/* Device-specific Install Guide — client island */}
          <InstallGuideRouter />
        </div>
      </section>

      {/* Benefits Section — SSR 렌더 가능 */}
      <section className="container mx-auto px-4 py-20 bg-paper-aged/40 backdrop-blur-sm">
        <h2 className="font-display text-4xl md:text-5xl font-bold text-center mb-4 text-ink dark:text-foreground tracking-tight break-keep">
          앱으로 설치하면 더 좋은 점
        </h2>
        <p className="font-body text-center text-ink-soft dark:text-muted-foreground mb-16 text-lg mx-auto break-keep">
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
