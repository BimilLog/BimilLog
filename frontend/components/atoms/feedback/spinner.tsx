import React from "react";
import { cn } from "@/lib/utils";
import { RefreshCw, Star, Loader2 } from "lucide-react";

interface SpinnerProps extends React.HTMLAttributes<HTMLDivElement> {
  size?: "xs" | "sm" | "md" | "lg" | "xl";
  variant?: "default" | "primary" | "secondary" | "gradient";
  message?: string;
  icon?: "spinner" | "refresh" | "star" | "loader";
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
  gradient: "",
};

export const Spinner = React.forwardRef<HTMLDivElement, SpinnerProps>(
  ({ size = "md", variant = "default", message, icon = "spinner", className, ...props }, ref) => {
    if (variant === "gradient") {
      return (
        <div ref={ref} className={cn("flex flex-col items-center justify-center py-16", className)} {...props}>
          <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-4">
            <Star className="w-6 h-6 text-white animate-pulse" />
          </div>
          {message && <p className="text-gray-600">{message}</p>}
        </div>
      );
    }

    if (icon === "refresh") {
      return (
        <div className={cn("flex items-center justify-center space-x-2", className)} {...props}>
          <RefreshCw className={cn(sizeClasses[size], "text-purple-600 animate-spin")} />
          {message && <span className="text-sm text-gray-600">{message}</span>}
        </div>
      );
    }

    if (icon === "loader") {
      return (
        <div className={cn("flex items-center justify-center space-x-2", className)} {...props}>
          <Loader2 className={cn(sizeClasses[size], "text-purple-600 animate-spin")} />
          {message && <span className="text-sm text-gray-600">{message}</span>}
        </div>
      );
    }

    return (
      <div className={cn("flex items-center justify-center space-x-2", className)} {...props}>
        <div
          ref={ref}
          className={cn(
            "animate-spin rounded-full border-2",
            sizeClasses[size],
            variantClasses[variant]
          )}
        />
        {message && <span className="text-sm text-gray-600">{message}</span>}
      </div>
    );
  }
);

Spinner.displayName = "Spinner";

// Legacy support for LoadingSpinner
export const LoadingSpinner = Spinner;
