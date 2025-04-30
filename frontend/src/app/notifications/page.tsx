"use client";

// 이 페이지는 더 이상 필요하지 않습니다.
// 대신 Navigation.tsx의 알림 드롭다운에서 모든 알림을 확인할 수 있습니다.
import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function NotificationsRedirect() {
  const router = useRouter();

  useEffect(() => {
    // 홈으로 리다이렉트
    router.push("/");
  }, [router]);

  return (
    <div className="container py-5 text-center">
      <p>리다이렉트 중...</p>
    </div>
  );
}
