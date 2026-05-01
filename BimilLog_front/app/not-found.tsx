import { Metadata } from "next";
import { NotFoundView } from "@/components/molecules/feedback";

export const metadata: Metadata = {
  title: "404 - 페이지를 찾을 수 없습니다",
  description: "요청하신 페이지가 삭제되었거나 주소가 변경되었을 수 있어요. 비밀로그 홈으로 돌아가보세요.",
  robots: {
    index: false,
    follow: true,
  },
  openGraph: {
    title: "404 - 페이지를 찾을 수 없습니다 | 비밀로그",
    description: "요청하신 페이지가 삭제되었거나 주소가 변경되었을 수 있어요.",
    siteName: "비밀로그",
    locale: "ko_KR",
    type: "website",
  },
};

export default function NotFoundPage() {
  return <NotFoundView />;
}
