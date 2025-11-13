import { Card, CardContent } from "@/components";
import { Heart, MessageSquare, Users } from "lucide-react";

export const HomeFeatures: React.FC = () => {
  const features = [
    {
      icon: Heart,
      title: "익명 메시지 쓰기",
      description:
        "친구들의 롤링페이퍼에 메시지를 남겨보세요",
      gradient: "bg-pink-500",
    },
    {
      icon: Users,
      title: "간단한 소셜 로그인",
      description: "간단하게 가입해서 롤링페이퍼에 친구들의 메시지를 받아보세요",
      gradient: "bg-orange-500",
    },
    {
      icon: MessageSquare,
      title: "커뮤니티",
      description: "다른 사용자들과 소통하고 인기글을 확인해보세요",
      gradient: "bg-green-500",
    },
  ];

  return (
    <section className="container mx-auto px-4 py-16">
      <h2 className="text-3xl md:text-4xl font-bold text-center mb-12 text-brand-primary">
        비밀로그 사용법
      </h2>
      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
        {features.map((feature, index) => {
          const IconComponent = feature.icon;
          return (
            <Card
              key={index}
              variant="elevated"
              className="hover:shadow-brand-xl transition-shadow"
            >
              <CardContent className="p-6 text-center">
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
