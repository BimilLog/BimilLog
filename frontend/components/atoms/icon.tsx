import React from "react";
import { cn } from "@/lib/utils";
import { LucideIcon } from "lucide-react";

interface IconProps extends React.HTMLAttributes<SVGElement> {
  icon: LucideIcon;
  size?: "xs" | "sm" | "md" | "lg" | "xl";
  variant?: "default" | "muted" | "primary" | "secondary" | "destructive";
}

const sizeClasses = {
  xs: "w-3 h-3",
  sm: "w-4 h-4",
  md: "w-5 h-5",
  lg: "w-6 h-6",
  xl: "w-8 h-8",
};

const variantClasses = {
  default: "text-foreground",
  muted: "text-muted-foreground",
  primary: "text-primary",
  secondary: "text-secondary",
  destructive: "text-destructive",
};

export const Icon = React.forwardRef<SVGSVGElement, IconProps>(
  (
    {
      icon: IconComponent,
      size = "md",
      variant = "default",
      className,
      ...props
    },
    ref
  ) => {
    return (
      <IconComponent
        ref={ref}
        className={cn(sizeClasses[size], variantClasses[variant], className)}
        {...props}
      />
    );
  }
);

Icon.displayName = "Icon";
