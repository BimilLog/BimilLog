import React from "react";
import { cn } from "@/lib/utils";

interface SpinnerProps extends React.HTMLAttributes<HTMLDivElement> {
  size?: "xs" | "sm" | "md" | "lg" | "xl";
  variant?: "default" | "primary" | "secondary";
}

const sizeClasses = {
  xs: "w-3 h-3",
  sm: "w-4 h-4",
  md: "w-6 h-6",
  lg: "w-8 h-8",
  xl: "w-12 h-12",
};

const variantClasses = {
  default: "border-gray-300 border-t-gray-600",
  primary: "border-primary/30 border-t-primary",
  secondary: "border-secondary/30 border-t-secondary",
};

export const Spinner = React.forwardRef<HTMLDivElement, SpinnerProps>(
  ({ size = "md", variant = "default", className, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(
          "animate-spin rounded-full border-2",
          sizeClasses[size],
          variantClasses[variant],
          className
        )}
        {...props}
      />
    );
  }
);

Spinner.displayName = "Spinner";
