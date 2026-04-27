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
    gradient: "bg-brand-pink-500",
  },
  {
    step: 2,
    icon: Share2,
    title: "내 롤링페이퍼 링크 공유",
    description: "친구들에게 카카오톡으로 내 롤링페이퍼 링크를 공유하세요",
    gradient: "bg-brand-purple-500",
  },
  {
    step: 3,
    icon: MailOpen,
    title: "익명 메시지 받기",
    description: "친구들이 남긴 따뜻하고 솔직한 익명 메시지를 확인하세요",
    gradient: "bg-brand-indigo-500",
  },
];

export const HomeFeatures: React.FC = () => {
  return (
    <section
      data-testid="home-features"
      className="container mx-auto px-4 py-16"
    >
      <h2 className="text-3xl md:text-4xl font-bold text-center mb-12 text-brand-primary">
        비밀로그 사용법
      </h2>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl mx-auto">
        {FEATURE_STEPS.map((feature) => {
          const IconComponent = feature.icon;
          return (
            <Card
              key={feature.step}
              variant="elevated"
              data-step={feature.step}
              className="hover:shadow-brand-xl transition-shadow"
            >
              <CardContent className="p-6 text-center">
                <div className="text-xs font-bold tracking-wider text-brand-secondary mb-3">
                  {feature.step}단계
                </div>
                <div
                  className={`w-12 h-12 ${feature.gradient} rounded-full flex items-center justify-center mx-auto mb-4`}
                >
                  <IconComponent className="w-6 h-6 text-white" />
                </div>
                <h3 className="text-lg font-semibold mb-2 text-brand-primary">
                  {feature.title}
                </h3>
                <p className="text-brand-secondary text-sm">{feature.description}</p>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </section>
  );
};

HomeFeatures.displayName = "HomeFeatures";
