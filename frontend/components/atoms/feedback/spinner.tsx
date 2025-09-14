import React from "react";
import { cn } from "@/lib/utils";
import { RefreshCw, Star, Loader2, Heart, Sparkles, Coffee, Moon, Sun, Zap, Smile } from "lucide-react";

interface SpinnerProps extends React.HTMLAttributes<HTMLDivElement> {
  size?: "xs" | "sm" | "md" | "lg" | "xl";
  variant?: "default" | "primary" | "secondary" | "gradient" | "cute" | "brand";
  message?: string;
  icon?: "spinner" | "refresh" | "star" | "loader" | "heart" | "sparkles" | "coffee" | "moon" | "sun" | "zap" | "smile";
  animation?: "spin" | "bounce" | "pulse" | "wiggle" | "float" | "heart-beat";
}

const sizeClasses = {
  xs: "w-3 h-3",
  sm: "w-4 h-4",
  md: "w-6 h-6",
  lg: "w-8 h-8",
  xl: "w-12 h-12",
};

const iconSizeClasses = {
  xs: "w-2 h-2",
  sm: "w-3 h-3",
  md: "w-4 h-4",
  lg: "w-5 h-5",
  xl: "w-6 h-6",
};

const variantClasses = {
  default: "border-gray-300 border-t-gray-600",
  primary: "border-primary/30 border-t-primary",
  secondary: "border-secondary/30 border-t-secondary",
  gradient: "",
  cute: "",
  brand: "",
};

const getAnimationClass = (animation: string) => {
  switch (animation) {
    case "bounce": return "animate-bounce-cute";
    case "pulse": return "animate-pulse-cute";
    case "wiggle": return "animate-wiggle";
    case "float": return "animate-float";
    case "heart-beat": return "animate-heart-beat";
    default: return "animate-spin";
  }
};

const IconComponent = ({ icon, size, animation }: { icon: string, size: string, animation: string }) => {
  const iconClass = cn(iconSizeClasses[size as keyof typeof iconSizeClasses], "text-white", getAnimationClass(animation));

  switch (icon) {
    case "heart": return <Heart className={iconClass} fill="currentColor" />;
    case "sparkles": return <Sparkles className={iconClass} />;
    case "coffee": return <Coffee className={iconClass} />;
    case "moon": return <Moon className={iconClass} fill="currentColor" />;
    case "sun": return <Sun className={iconClass} />;
    case "zap": return <Zap className={iconClass} fill="currentColor" />;
    case "smile": return <Smile className={iconClass} />;
    case "refresh": return <RefreshCw className={iconClass} />;
    case "loader": return <Loader2 className={iconClass} />;
    default: return <Star className={iconClass} fill="currentColor" />;
  }
};

export const Spinner = React.forwardRef<HTMLDivElement, SpinnerProps>(
  ({
    size = "md",
    variant = "default",
    message,
    icon = "spinner",
    animation = "spin",
    className,
    ...props
  }, ref) => {
    // 귀여운 브랜드 스피너
    if (variant === "cute" || variant === "brand") {
      const containerSize = size === "xl" ? "w-16 h-16" : size === "lg" ? "w-14 h-14" : "w-12 h-12";

      return (
        <div ref={ref} className={cn("flex flex-col items-center justify-center py-8", className)} {...props}>
          <div className={cn(
            containerSize,
            "bg-brand-button rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-brand-lg",
            getAnimationClass(animation)
          )}>
            <IconComponent icon={icon} size={size} animation={animation} />
          </div>
          {message && (
            <p className={cn(
              "text-brand-secondary font-medium text-center animate-pulse-cute",
              size === "xs" ? "text-xs" : size === "sm" ? "text-sm" : "text-base"
            )}>
              {message}
            </p>
          )}
        </div>
      );
    }

    // 그라디언트 스피너 (기존 유지하되 개선)
    if (variant === "gradient") {
      return (
        <div ref={ref} className={cn("flex flex-col items-center justify-center py-16", className)} {...props}>
          <div className="w-12 h-12 bg-brand-button rounded-xl flex items-center justify-center mx-auto mb-4 animate-pulse-cute shadow-brand-lg">
            <Star className="w-6 h-6 text-white animate-float" fill="currentColor" />
          </div>
          {message && <p className="text-brand-secondary font-medium">{message}</p>}
        </div>
      );
    }

    // 아이콘 기반 스피너들
    if (icon === "refresh") {
      return (
        <div className={cn("flex items-center justify-center space-x-2", className)} {...props}>
          <RefreshCw className={cn(sizeClasses[size], "text-purple-600", getAnimationClass(animation))} />
          {message && <span className="text-sm text-brand-secondary font-medium">{message}</span>}
        </div>
      );
    }

    if (icon === "loader") {
      return (
        <div className={cn("flex items-center justify-center space-x-2", className)} {...props}>
          <Loader2 className={cn(sizeClasses[size], "text-purple-600", getAnimationClass(animation))} />
          {message && <span className="text-sm text-brand-secondary font-medium">{message}</span>}
        </div>
      );
    }

    // 기본 원형 스피너 (개선된 디자인)
    return (
      <div className={cn("flex items-center justify-center space-x-3", className)} {...props}>
        <div
          ref={ref}
          className={cn(
            "rounded-full border-2",
            sizeClasses[size],
            variantClasses[variant],
            getAnimationClass(animation),
            "border-purple-200 border-t-purple-600"
          )}
        />
        {message && <span className="text-sm text-brand-secondary font-medium">{message}</span>}
      </div>
    );
  }
);

Spinner.displayName = "Spinner";

// 간편한 사용을 위한 사전 정의된 스피너들
export const CuteLoadingSpinner = ({ message, size = "md", ...props }: Omit<SpinnerProps, 'variant' | 'icon'>) => (
  <Spinner variant="cute" icon="heart" animation="heart-beat" message={message} size={size} {...props} />
);

export const BrandLoadingSpinner = ({ message, size = "md", ...props }: Omit<SpinnerProps, 'variant'>) => (
  <Spinner variant="brand" icon="sparkles" animation="float" message={message} size={size} {...props} />
);

export const QuickSpinner = ({ message, size = "sm", ...props }: Omit<SpinnerProps, 'variant' | 'icon'>) => (
  <Spinner variant="cute" icon="zap" animation="wiggle" message={message} size={size} {...props} />
);

// Legacy support for LoadingSpinner
export const LoadingSpinner = ({ variant, ...props }: SpinnerProps) => (
  <Spinner variant={variant || "cute"} {...props} />
);