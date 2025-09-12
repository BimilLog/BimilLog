import React from "react";
import { sanitizeHtml } from "@/lib/sanitize";

interface SafeHTMLProps {
  html: string;
  className?: string;
  allowedTags?: string[];
  forbiddenTags?: string[];
}

/**
 * 사용자 입력 HTML을 안전하게 렌더링하는 컴포넌트
 *
 * DOMPurify를 사용하여 XSS 공격을 방지합니다.
 * dangerouslySetInnerHTML 대신 이 컴포넌트를 사용하세요.
 */
const SafeHTML: React.FC<SafeHTMLProps> = ({
  html,
  className,
  allowedTags,
  forbiddenTags,
}) => {
  // 빈 문자열이면 렌더링하지 않음
  if (!html) return null;

  // 줄바꿈 문자를 <br> 태그로 변환
  const htmlWithBreaks = html.replace(/\n/g, "<br>");

  // HTML 콘텐츠 정화 (사용자 정의 태그 설정 지원)
  const sanitizedHtml = sanitizeHtml(htmlWithBreaks, {
    allowedTags,
    forbiddenTags,
  });

  return (
    <div
      className={className}
      dangerouslySetInnerHTML={{ __html: sanitizedHtml }}
    />
  );
};

export default SafeHTML;
