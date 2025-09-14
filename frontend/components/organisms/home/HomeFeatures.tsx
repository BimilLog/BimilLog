import { Card, CardContent } from "@/components";
import { Heart, MessageCircle, Users, Sparkles } from "lucide-react";

export const HomeFeatures: React.FC = () => {
  const features = [
    {
      icon: Heart,
      title: "익명 메시지",
      description:
        "로그인 없이도 누구나 익명으로 따뜻한 메시지를 남길 수 있어요",
      gradient: "from-pink-500 to-red-500",
    },
    {
      icon: Users,
      title: "카카오 연동",
      description: "카카오톡으로 간편하게 로그인하고 친구들에게 공유해보세요",
      gradient: "from-orange-500 to-yellow-500",
    },
    {
      icon: Sparkles,
      title: "다양한 디자인",
      description: "예쁜 디자인으로 메시지를 꾸며서 더욱 특별하게 만들어보세요",
      gradient: "from-purple-500 to-indigo-500",
    },
    {
      icon: MessageCircle,
      title: "커뮤니티",
      description: "다른 사용자들과 소통하고 인기글을 확인해보세요",
      gradient: "from-green-500 to-teal-500",
    },
  ];

  return (
    <section className="container mx-auto px-4 py-16">
      <h2 className="text-3xl md:text-4xl font-bold text-center mb-12 text-brand-primary">
        비밀로그의 특별한 기능들
      </h2>
      <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
        {features.map((feature, index) => {
          const IconComponent = feature.icon;
          return (
            <Card
              key={index}
              variant="elevated"
              className="hover:shadow-xl transition-shadow"
            >
              <CardContent className="p-6 text-center">
                <div
                  className={`w-12 h-12 bg-gradient-to-r ${feature.gradient} rounded-full flex items-center justify-center mx-auto mb-4`}
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
