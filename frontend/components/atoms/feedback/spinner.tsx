import React from "react";
import { cn } from "@/lib/utils";
import { Spinner as FlowbiteSpinner } from "flowbite-react";

interface SpinnerProps extends React.HTMLAttributes<HTMLDivElement> {
  size?: "xs" | "sm" | "md" | "lg" | "xl";
  variant?: "default" | "primary" | "secondary" | "gradient" | "cute" | "brand";
  message?: string;
  icon?: "spinner" | "refresh" | "star" | "loader" | "heart" | "sparkles" | "coffee" | "moon" | "sun" | "zap" | "smile";
  animation?: "spin" | "bounce" | "pulse" | "wiggle" | "float" | "heart-beat";
}

export const Spinner = React.forwardRef<HTMLDivElement, SpinnerProps>(
  ({
    size = "xl",
    message,
    className,
    ...props
  }, ref) => {
    return (
      <div ref={ref} className={cn("flex flex-col items-center justify-center", className)} {...props}>
        <FlowbiteSpinner
          color="pink"
          size={size}
          aria-label={message || "Loading..."}
        />
        {message && (
          <p className="mt-3 text-sm text-brand-secondary font-medium text-center">
            {message}
          </p>
        )}
      </div>
    );
  }
);

Spinner.displayName = "Spinner";

// 간편한 사용을 위한 사전 정의된 스피너들
export const CuteLoadingSpinner = ({ message, size = "xl", ...props }: Omit<SpinnerProps, 'variant' | 'icon'>) => (
  <Spinner variant="cute" icon="heart" animation="heart-beat" message={message} size={size} {...props} />
);

export const BrandLoadingSpinner = ({ message, size = "xl", ...props }: Omit<SpinnerProps, 'variant'>) => (
  <Spinner variant="brand" icon="sparkles" animation="float" message={message} size={size} {...props} />
);

export const QuickSpinner = ({ message, size = "xl", ...props }: Omit<SpinnerProps, 'variant' | 'icon'>) => (
  <Spinner variant="cute" icon="zap" animation="wiggle" message={message} size={size} {...props} />
);

// Legacy support for LoadingSpinner
export const LoadingSpinner = ({ variant, size = "xl", ...props }: SpinnerProps) => (
  <Spinner variant={variant || "default"} size={size} {...props} />
);