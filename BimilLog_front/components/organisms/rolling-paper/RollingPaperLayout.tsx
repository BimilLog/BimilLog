import React from "react";
import { AuthHeader } from "@/components/organisms/common";
import { HomeFooter } from "@/components/organisms/home";

interface RollingPaperLayoutProps {
  children: React.ReactNode;
}

export const RollingPaperLayout: React.FC<RollingPaperLayoutProps> = React.memo(({
  children,
}) => {
  return (
    <div className="min-h-screen bg-brand-gradient">
      {/* Auth Header: 롤링페이퍼 페이지에서는 RollingPaperHeader 와 sticky 가 겹치지 않도록
          disableSticky 로 sticky 를 해제하여 단일 sticky 영역(=RollingPaperHeader) 만 유지 */}
      <AuthHeader disableSticky />
      <main>
        {children}
      </main>
      <HomeFooter />
    </div>
  );
});

RollingPaperLayout.displayName = "RollingPaperLayout";
