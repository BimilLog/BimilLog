import { Metadata } from "next";
import { AuthHeader } from "@/components/organisms/common";
import { HomeFooter } from "@/components/organisms/home";
import SuggestClient from "@/components/organisms/suggest/SuggestClient";

export const metadata: Metadata = {
  title: "건의하기 - 비밀로그",
  description:
    "비밀로그를 더 좋은 곳으로 만드는 데 함께해 주세요. 작은 의견 하나가 다음 편지를 따뜻하게 만들어요.",
  keywords: ["비밀로그", "건의사항", "피드백", "개선제안", "버그신고"],
  openGraph: {
    title: "건의하기 - 비밀로그",
    description: "비밀로그 서비스 개선을 위한 건의사항을 접수합니다.",
    url: "https://grow-farm.com/suggest",
    siteName: "비밀로그",
    locale: "ko_KR",
    type: "website",
  },
};

export default function SuggestPage() {
  return (
    <div className="min-h-screen bg-paper">
      <AuthHeader />

      {/* Header — F-12-BUG-3: whitespace-nowrap 제거 + break-keep 적용 (WCAG 1.4.10) */}
      <header className="py-8">
        <div className="container mx-auto px-4 text-center">
          <div className="flex items-center justify-center space-x-3 mb-4">
            <h1 className="font-display text-3xl md:text-4xl font-bold text-ink break-keep">
              아이디어를 들려주세요
            </h1>
          </div>
          <p className="text-base md:text-lg text-brand-muted mx-auto max-w-2xl leading-relaxed">
            비밀로그를 더 좋은 곳으로 만드는 데 함께해 주세요. 남기신 한 줄이 다음 편지를 더 따뜻하게 만들어요.
          </p>
        </div>
      </header>

      <main>
        <SuggestClient />
      </main>

      {/* Footer */}
      <HomeFooter />
    </div>
  );
}
