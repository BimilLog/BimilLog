import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { AlertCircle, CheckCircle, Info, XCircle, AlertTriangle } from "lucide-react";

import { cn } from "@/lib/utils";

const alertVariants = cva(
  "relative w-full rounded-lg p-4 shadow-brand-sm [&>svg~*]:pl-7 [&>svg+div]:translate-y-[-3px] [&>svg]:absolute [&>svg]:left-4 [&>svg]:top-4 transition-all duration-200",
  {
    variants: {
      variant: {
        default:
          "bg-blue-50 border border-blue-200 text-blue-800 [&>svg]:text-blue-600",
        destructive:
          "bg-red-50 border border-red-200 text-red-800 [&>svg]:text-red-600",
        success:
          "bg-green-50 border border-green-200 text-green-800 [&>svg]:text-green-600",
        warning:
          "bg-yellow-50 border border-yellow-200 text-yellow-800 [&>svg]:text-yellow-600",
        info:
          "bg-brand-gradient border-0 text-brand-primary [&>svg]:text-purple-600",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

interface AlertProps extends React.HTMLAttributes<HTMLDivElement>, VariantProps<typeof alertVariants> {
  icon?: boolean;
}

const Alert = React.forwardRef<HTMLDivElement, AlertProps>(
  ({ className, variant, icon = true, children, ...props }, ref) => {
    const getIcon = () => {
      if (!icon) return null;

      switch (variant) {
        case "destructive":
          return <XCircle className="h-4 w-4 stroke-red-600 fill-red-100" />;
        case "success":
          return <CheckCircle className="h-4 w-4 stroke-green-600 fill-green-100" />;
        case "warning":
          return <AlertTriangle className="h-4 w-4 stroke-amber-600 fill-amber-100" />;
        case "info":
          return <Info className="h-4 w-4 stroke-blue-600 fill-blue-100" />;
        default:
          return <AlertCircle className="h-4 w-4 stroke-red-600 fill-red-100" />;
      }
    };

    return (
      <div
        ref={ref}
        role="alert"
        className={cn(alertVariants({ variant }), className)}
        {...props}
      >
        {getIcon()}
        <div className={icon ? "pl-7" : ""}>{children}</div>
      </div>
    );
  }
);
Alert.displayName = "Alert";

const AlertTitle = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLHeadingElement>
>(({ className, ...props }, ref) => (
  <h5
    ref={ref}
    className={cn("mb-1 font-semibold leading-none tracking-tight", className)}
    {...props}
  />
));
AlertTitle.displayName = "AlertTitle";

const AlertDescription = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn("text-sm [&_p]:leading-relaxed", className)}
    {...props}
  />
));
AlertDescription.displayName = "AlertDescription";

// 사전 정의된 Alert 타입들
export const SuccessAlert = React.memo(({ children, ...props }: Omit<AlertProps, 'variant'>) => (
  <Alert variant="success" {...props}>{children}</Alert>
));
SuccessAlert.displayName = "SuccessAlert";

export const ErrorAlert = React.memo(({ children, ...props }: Omit<AlertProps, 'variant'>) => (
  <Alert variant="destructive" {...props}>{children}</Alert>
));
ErrorAlert.displayName = "ErrorAlert";

export const WarningAlert = React.memo(({ children, ...props }: Omit<AlertProps, 'variant'>) => (
  <Alert variant="warning" {...props}>{children}</Alert>
));
WarningAlert.displayName = "WarningAlert";

export const InfoAlert = React.memo(({ children, ...props }: Omit<AlertProps, 'variant'>) => (
  <Alert variant="info" {...props}>{children}</Alert>
));
InfoAlert.displayName = "InfoAlert";

export { Alert, AlertTitle, AlertDescription, alertVariants, type AlertProps };