import * as React from "react"
import { Slot } from "@radix-ui/react-slot"
import { cva, type VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg font-medium transition-all disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg:not([class*='size-'])]:size-4 shrink-0 [&_svg]:shrink-0 outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-background select-none touch-manipulation min-h-touch min-w-touch",
  {
    variants: {
      variant: {
        // 기본 브랜드 버튼 (종이 위 stamp-red 우표 단색)
        default:
          "bg-brand-button hover:bg-brand-hover text-white shadow-brand-lg hover:shadow-brand-xl active:scale-[0.98] focus-visible:ring-stamp-red",

        // 아웃라인 (stamp-red 테두리 + 종이 톤 배경)
        outline:
          "border-2 border-stamp-red/30 text-stamp-red bg-paper-card backdrop-blur-sm hover:bg-paper-soft hover:border-stamp-red/60 shadow-brand-sm hover:shadow-brand-md active:scale-[0.98] focus-visible:ring-stamp-red",

        // 위험한 작업용 (빨간색 단색 — destructive 신호)
        destructive:
          "bg-red-600 hover:bg-red-700 text-white shadow-brand-lg hover:shadow-brand-xl active:scale-[0.98] focus-visible:ring-red-500",

        // 보조 버튼 (회색)
        secondary:
          "bg-gray-100 text-brand-primary hover:bg-gray-200 border border-gray-200 shadow-brand-sm hover:shadow-brand-md active:scale-[0.98] focus-visible:ring-gray-500",

        // 성공/확인 버튼 (녹색 단색)
        success:
          "bg-green-600 hover:bg-green-700 text-white shadow-brand-lg hover:shadow-brand-xl active:scale-[0.98] focus-visible:ring-green-500",

        // 카카오 로그인 버튼
        kakao:
          "bg-[#FEE500] hover:bg-[#FAD900] text-[#191919] font-semibold shadow-brand-lg hover:shadow-brand-xl active:scale-[0.98] focus-visible:ring-yellow-500",

        // 고스트 버튼 (배경 없음, postal-navy 정보 톤)
        ghost:
          "text-ink hover:bg-paper-soft hover:text-stamp-red active:scale-[0.98] focus-visible:ring-stamp-red",

        // 링크 스타일 (postal-navy)
        link:
          "text-postal-navy underline-offset-4 hover:underline hover:text-stamp-red active:scale-[0.98] focus-visible:ring-stamp-red",
      },
      size: {
        // 모바일 최적화 크기 (최소 44px 터치 타겟 보장)
        xs: "h-8 px-3 text-xs min-w-[2rem]", // 32px 높이 (작은 버튼, 데스크톱 전용)
        sm: "h-10 px-4 text-sm min-w-[2.75rem]", // 40px 높이 (최소 터치 타겟에 약간 못 미침)
        default: "h-12 px-6 text-base min-w-[3rem]", // 48px 높이 (권장 터치 타겟)
        lg: "h-14 px-8 text-lg min-w-[3.5rem]", // 56px 높이 (편안한 터치 타겟)
        xl: "h-16 px-10 text-xl min-w-[4rem]", // 64px 높이 (큰 터치 타겟)

        // 아이콘 전용 버튼 (정사각형, 터치 타겟 보장)
        "icon-sm": "size-10 p-0", // 40px x 40px (최소 터치 타겟에 약간 못 미침)
        icon: "size-12 p-0", // 48px x 48px (권장 터치 타겟)
        "icon-lg": "size-14 p-0", // 56px x 56px (편안한 터치 타겟)
        "icon-xl": "size-16 p-0", // 64px x 64px (큰 터치 타겟)

        // 전체 너비 버튼 (모바일 최적화)
        full: "w-full h-12 px-6 text-base", // 48px 높이로 터치 타겟 보장
        "full-lg": "w-full h-14 px-8 text-lg", // 56px 높이로 더 편안한 터치
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

const Button = React.memo(({
  className,
  variant,
  size,
  asChild = false,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean
  }) => {
  const Comp = asChild ? Slot : "button"

  return (
    <Comp
      data-slot="button"
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
});

Button.displayName = "Button";

// 사전 정의된 버튼 타입들 (자주 사용되는 패턴)
export const PrimaryButton = React.memo((props: Omit<React.ComponentProps<typeof Button>, 'variant'>) => (
  <Button variant="default" {...props} />
));
PrimaryButton.displayName = "PrimaryButton";

export const SecondaryButton = React.memo((props: Omit<React.ComponentProps<typeof Button>, 'variant'>) => (
  <Button variant="secondary" {...props} />
));
SecondaryButton.displayName = "SecondaryButton";

export const DangerButton = React.memo((props: Omit<React.ComponentProps<typeof Button>, 'variant'>) => (
  <Button variant="destructive" {...props} />
));
DangerButton.displayName = "DangerButton";

export const TouchButton = React.memo((props: Omit<React.ComponentProps<typeof Button>, 'size'>) => (
  <Button size="default" {...props} />
));
TouchButton.displayName = "TouchButton";

export const IconButton = React.memo((props: Omit<React.ComponentProps<typeof Button>, 'size'>) => (
  <Button size="icon" {...props} />
));
IconButton.displayName = "IconButton";

export const KakaoButton = React.memo((props: Omit<React.ComponentProps<typeof Button>, 'variant'>) => (
  <Button variant="kakao" {...props} />
));
KakaoButton.displayName = "KakaoButton";

export { Button, buttonVariants }