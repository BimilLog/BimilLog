import { Metadata } from "next";
import { AuthHeader } from "@/components/layouts";
import { HomeFooter } from "@/components/organisms/home";
import { SuggestClient } from "@/components/organisms/suggest";

export const metadata: Metadata = {
  title: "건의하기 - 비밀로그",
  description: "비밀로그를 더 좋은 서비스로 만들어가는데 도움을 주세요. 여러분의 소중한 의견을 기다립니다.",
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
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      {/* Header */}
      <header className="py-8">
        <div className="container mx-auto px-4 text-center">
          <div className="flex items-center justify-center space-x-3 mb-4">
            <h1 className="text-3xl md:text-4xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent">
              건의하기
            </h1>
          </div>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            비밀로그를 더 좋은 서비스로 만들어가는데 도움을 주세요. 여러분의
            소중한 의견을 기다립니다.
          </p>
        </div>
      </header>

      <SuggestClient />

      {/* Footer */}
      <HomeFooter />
    </div>
  );
}
