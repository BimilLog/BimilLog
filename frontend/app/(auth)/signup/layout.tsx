import { Metadata } from "next";

export const metadata: Metadata = {
  title: "회원가입",
  description: "비밀로그에 가입하여 친구들과 익명으로 소통해보세요.",
  robots: "noindex, nofollow", // 검색 엔진에 색인되지 않도록 설정
};

export default function SignupLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
