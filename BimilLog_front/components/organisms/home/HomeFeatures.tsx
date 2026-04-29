import { Card, CardContent } from "@/components";
import { LogIn, Share2, MailOpen } from "lucide-react";

interface FeatureStep {
  step: 1 | 2 | 3;
  icon: typeof LogIn;
  title: string;
  description: string;
  gradient: string;
}

const FEATURE_STEPS: FeatureStep[] = [
  {
    step: 1,
    icon: LogIn,
    title: "카카오로 로그인",
    description: "1초만에 가입하고 내 롤링페이퍼를 만들어보세요",
    gradient: "bg-paper-button",
  },
  {
    step: 2,
    icon: Share2,
    title: "내 롤링페이퍼 링크 공유",
    description: "친구들에게 카카오톡으로 내 롤링페이퍼 링크를 공유하세요",
    gradient: "bg-[var(--color-postal-navy,#1F3A68)]",
  },
  {
    step: 3,
    icon: MailOpen,
    title: "익명 메시지 받기",
    description: "친구들이 남긴 따뜻하고 솔직한 익명 메시지를 확인하세요",
    gradient: "bg-[var(--color-seal-gold,#C99B5C)]",
  },
];

export const HomeFeatures: React.FC = () => {
  return (
    <section
      data-testid="home-features"
      className="container-paper px-4 py-16"
    >
      <div className="flex flex-col items-center mb-12">
        <span className="font-display text-xs tracking-[0.22em] text-stamp-red uppercase mb-3">
          · How it works ·
        </span>
        <h2 className="font-display text-3xl md:text-4xl font-bold text-center text-ink dark:text-gray-100">
          비밀로그 사용법
        </h2>
        <span className="mt-3 inline-block w-12 h-px bg-ink/40" aria-hidden="true" />
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl mx-auto">
        {FEATURE_STEPS.map((feature) => {
          const IconComponent = feature.icon;
          return (
            <Card
              key={feature.step}
              variant="elevated"
              data-step={feature.step}
              className="bg-paper-card border border-ink-soft hover:shadow-brand-lg transition-shadow"
            >
              <CardContent className="p-6 text-center">
                <div className="font-display text-xs font-bold tracking-[0.18em] text-stamp-red mb-3 uppercase">
                  Step 0{feature.step}
                </div>
                <div className="font-display text-[11px] font-semibold tracking-[0.12em] text-ink-soft dark:text-brand-secondary mb-3">
                  {feature.step}단계
                </div>
                <div
                  className={`w-12 h-12 ${feature.gradient} rounded-full flex items-center justify-center mx-auto mb-4 shadow-brand-sm`}
                >
                  <IconComponent className="w-6 h-6 text-white" />
                </div>
                <h3 className="font-display text-lg font-semibold mb-2 text-ink dark:text-gray-100">
                  {feature.title}
                </h3>
                <p className="text-ink-soft dark:text-brand-secondary text-sm">{feature.description}</p>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </section>
  );
};

HomeFeatures.displayName = "HomeFeatures";
