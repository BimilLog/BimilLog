import * as React from "react"
import { Slot } from "@radix-ui/react-slot"
import { cva, type VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg text-sm font-medium transition-all disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg:not([class*='size-'])]:size-4 shrink-0 [&_svg]:shrink-0 outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-background select-none touch-manipulation",
  {
    variants: {
      variant: {
        // 메인페이지 스타일 - Pink-Purple 그라디언트
        default:
          "bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700 text-white shadow-lg hover:shadow-xl active:scale-[0.98] focus-visible:ring-purple-500",
        
        // 메인페이지 아웃라인 스타일
        outline:
          "border-2 border-purple-200 text-purple-600 bg-white/80 backdrop-blur-sm hover:bg-purple-50 hover:border-purple-300 shadow-sm hover:shadow-md active:scale-[0.98] focus-visible:ring-purple-500",
        
        // 위험한 작업용 (빨간색)
        destructive:
          "bg-gradient-to-r from-red-500 to-red-600 hover:from-red-600 hover:to-red-700 text-white shadow-lg hover:shadow-xl active:scale-[0.98] focus-visible:ring-red-500",
        
        // 보조 버튼 (회색)
        secondary:
          "bg-gray-100 text-gray-700 hover:bg-gray-200 border border-gray-200 shadow-sm hover:shadow-md active:scale-[0.98] focus-visible:ring-gray-500",
        
        // 성공/확인 버튼 (녹색)
        success:
          "bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white shadow-lg hover:shadow-xl active:scale-[0.98] focus-visible:ring-green-500",
        
        // 고스트 버튼 (배경 없음)
        ghost:
          "text-purple-600 hover:bg-purple-50 hover:text-purple-700 active:scale-[0.98] focus-visible:ring-purple-500",
        
        // 링크 스타일
        link: 
          "text-purple-600 underline-offset-4 hover:underline hover:text-purple-700 active:scale-[0.98] focus-visible:ring-purple-500",
      },
      size: {
        // 모바일 최적화 크기 (최소 44px 터치 타겟)
        sm: "h-10 px-4 text-sm min-w-[2.75rem]", // 44px 높이
        default: "h-11 px-6 text-base min-w-[3rem]", // 48px 높이 (권장)
        lg: "h-12 px-8 text-lg min-w-[3.5rem]", // 56px 높이 (편안한 크기)
        xl: "h-14 px-10 text-xl min-w-[4rem]", // 64px 높이 (큰 버튼)
        
        // 아이콘 전용 버튼
        icon: "size-11 p-0", // 44px x 44px
        "icon-lg": "size-12 p-0", // 48px x 48px
        "icon-xl": "size-14 p-0", // 56px x 56px
        
        // 전체 너비 버튼 (모바일 최적화)
        full: "w-full h-12 px-6 text-lg",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

function Button({
  className,
  variant,
  size,
  asChild = false,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean
  }) {
  const Comp = asChild ? Slot : "button"

  return (
    <Comp
      data-slot="button"
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { Button, buttonVariants }
