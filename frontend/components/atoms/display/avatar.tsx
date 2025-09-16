"use client";

import * as React from "react";
import { Avatar as FlowbiteAvatar, type AvatarProps as FlowbiteAvatarProps } from "flowbite-react";
import { cn } from "@/lib/utils";
import { getInitials } from "@/lib/utils/format";

interface AvatarProps extends FlowbiteAvatarProps {
  className?: string;
  fallback?: string;
  children?: React.ReactNode;
}

const Avatar = React.memo<AvatarProps>(({
  className,
  img,
  alt,
  fallback,
  children,
  placeholderInitials,
  ...props
}) => {
  const [imageError, setImageError] = React.useState(false);

  // 이미지 소스 처리
  const imageSrc = typeof img === 'string' ? img : (typeof img === 'object' && img !== null && 'src' in img) ? (img as { src: string }).src : undefined;
  const showImage = imageSrc && !imageError;

  // 이미지 로드 에러 처리
  React.useEffect(() => {
    if (imageSrc) {
      const img = new Image();
      img.onerror = () => setImageError(true);
      img.src = imageSrc;
    }
  }, [imageSrc]);

  // Fallback 텍스트 결정
  const initials = placeholderInitials || fallback || (alt ? getInitials(alt) : 'U');

  return (
    <FlowbiteAvatar
      img={showImage ? imageSrc : undefined}
      alt={alt}
      placeholderInitials={!showImage ? initials : undefined}
      className={cn("", className)}
      {...props}
    >
      {!showImage && fallback && fallback.length > 2 && (
        <div className="flex size-full items-center justify-center rounded-full bg-gradient-to-r from-pink-500 to-purple-600 text-white font-semibold">
          {fallback}
        </div>
      )}
      {children}
    </FlowbiteAvatar>
  );
});

Avatar.displayName = "Avatar";

// 기존 API 호환성을 위한 컴포넌트들
const AvatarImage = React.memo<{ src?: string; alt?: string; className?: string }>(
  () => null
);
AvatarImage.displayName = "AvatarImage";

const AvatarFallback = React.memo<{ children?: React.ReactNode; className?: string }>(
  () => null
);
AvatarFallback.displayName = "AvatarFallback";

// 간단한 래퍼 컴포넌트로 기존 API 지원
const AvatarWrapper = React.memo<{
  className?: string;
  children?: React.ReactNode;
  size?: FlowbiteAvatarProps['size'];
  rounded?: boolean;
  status?: FlowbiteAvatarProps['status'];
  statusPosition?: FlowbiteAvatarProps['statusPosition'];
}>(({ className, children, ...props }) => {
  let imgSrc: string | undefined;
  let imgAlt: string | undefined;
  let fallbackContent: React.ReactNode;

  // children에서 정보 추출
  React.Children.forEach(children, (child) => {
    if (React.isValidElement(child)) {
      if (child.type === AvatarImage) {
        const props = child.props as { src?: string; alt?: string };
        imgSrc = props.src;
        imgAlt = props.alt;
      } else if (child.type === AvatarFallback) {
        const props = child.props as { children?: React.ReactNode };
        fallbackContent = props.children;
      }
    }
  });

  // 추가 컨텐츠 (AvatarImage, AvatarFallback 제외)
  const additionalContent = React.Children.toArray(children).filter(
    (child) => React.isValidElement(child) &&
    child.type !== AvatarImage &&
    child.type !== AvatarFallback
  );

  return (
    <Avatar
      img={imgSrc}
      alt={imgAlt}
      fallback={typeof fallbackContent === 'string' ? fallbackContent : undefined}
      className={className}
      {...props}
    >
      {typeof fallbackContent !== 'string' && fallbackContent}
      {additionalContent}
    </Avatar>
  );
});

AvatarWrapper.displayName = "AvatarWrapper";

// Export 정리
export { Avatar, AvatarImage, AvatarFallback, AvatarWrapper };
export type { AvatarProps };
