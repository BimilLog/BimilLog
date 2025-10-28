import * as React from "react"
import { Slot } from "@radix-ui/react-slot"
import { cva, type VariantProps } from "class-variance-authority"
import { LucideIcon } from "lucide-react"

import { cn } from "@/lib/utils"

const badgeVariants = cva(
  "inline-flex items-center justify-center rounded-md border px-2 py-0.5 text-xs font-medium w-fit whitespace-nowrap shrink-0 [&>svg]:size-3 gap-1 [&>svg]:pointer-events-none focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px] aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive transition-[color,box-shadow] overflow-hidden",
  {
    variants: {
      variant: {
        default:
          "border-transparent bg-primary text-primary-foreground [a&]:hover:bg-primary/90",
        secondary:
          "border-transparent bg-secondary text-secondary-foreground [a&]:hover:bg-secondary/90",
        destructive:
          "border-transparent bg-destructive text-white [a&]:hover:bg-destructive/90 focus-visible:ring-destructive/20 dark:focus-visible:ring-destructive/40 dark:bg-destructive/60",
        outline:
          "text-foreground [a&]:hover:bg-accent [a&]:hover:text-accent-foreground",
        info:
          "border-blue-200 bg-blue-50 text-blue-700 [a&]:hover:bg-blue-100",
        gray:
          "border-gray-200 bg-gray-50 text-gray-700 [a&]:hover:bg-gray-100",
        purple:
          "border-purple-200 bg-purple-50 text-purple-700 [a&]:hover:bg-purple-100",
        indigo:
          "border-indigo-200 bg-indigo-50 text-indigo-700 [a&]:hover:bg-indigo-100",
        success:
          "border-green-200 bg-green-50 text-green-700 [a&]:hover:bg-green-100",
        warning:
          "border-yellow-200 bg-yellow-50 text-yellow-700 [a&]:hover:bg-yellow-100",
        pink:
          "border-pink-200 bg-pink-50 text-pink-700 [a&]:hover:bg-pink-100",
      },
      size: {
        default: "px-2 py-0.5 text-xs",
        xs: "px-1.5 py-0.5 text-[10px]",
        sm: "px-2.5 py-1 text-sm",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

interface BadgeProps extends React.ComponentProps<"span">, VariantProps<typeof badgeVariants> {
  asChild?: boolean;
  icon?: LucideIcon;
}

const Badge = React.memo(({
  className,
  variant,
  size,
  asChild = false,
  icon: Icon,
  children,
  ...props
}: BadgeProps) => {
  const Comp = asChild ? Slot : "span"

  return (
    <Comp
      data-slot="badge"
      className={cn(badgeVariants({ variant, size }), className)}
      {...props}
    >
      {Icon && <Icon className="w-3 h-3" />}
      {children}
    </Comp>
  )
});

Badge.displayName = "Badge";

export { Badge, badgeVariants }
