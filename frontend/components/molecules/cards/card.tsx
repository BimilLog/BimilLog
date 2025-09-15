import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const cardVariants = cva(
  "flex flex-col rounded-lg border-0 backdrop-blur-sm transition-all duration-300 text-brand-primary",
  {
    variants: {
      variant: {
        // 기본 브랜드 카드 (가장 자주 사용)
        default:
          "bg-white/80 shadow-brand-lg hover:shadow-brand-xl",

        // 강조 카드 (중요한 정보)
        elevated:
          "bg-white/90 shadow-brand-xl hover:shadow-brand-2xl",

        // 부드러운 카드 (배경에 잘 어우러짐)
        soft:
          "bg-white/60 shadow-brand-sm hover:shadow-brand-md",

        // 그라디언트 배경 카드 (특별한 경우)
        gradient:
          "bg-brand-gradient shadow-brand-lg hover:shadow-brand-xl",

        // 보더 카드 (구분이 필요한 경우)
        outlined:
          "bg-white/80 border border-gray-100 shadow-brand-sm hover:shadow-brand-md",

        // 투명 카드 (오버레이)
        ghost:
          "bg-white/40 shadow-brand-sm hover:bg-white/60 hover:shadow-brand-md",
      },
      size: {
        sm: "p-4 gap-3",
        default: "p-6 gap-4",
        lg: "p-8 gap-6",
      },
      interactive: {
        true: "cursor-pointer hover:scale-[1.02] active:scale-[0.98] transition-transform",
        false: "",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
      interactive: false,
    },
  }
);

interface CardProps extends React.ComponentProps<"div">, VariantProps<typeof cardVariants> {
  asChild?: boolean;
}

const Card = React.memo<CardProps>(({ className, variant, size, interactive, asChild = false, ...props }) => {
  if (asChild) {
    return (
      <>
        {React.Children.map(props.children as React.ReactNode, (child) =>
          React.isValidElement(child)
            ? React.cloneElement(child as React.ReactElement<{className?: string}>, {
                className: cn(
                  cardVariants({ variant, size, interactive, className }),
                  (child.props as {className?: string}).className
                ),
              })
            : child
        )}
      </>
    );
  }

  return (
    <div
      data-slot="card"
      className={cn(cardVariants({ variant, size, interactive, className }))}
      {...props}
    />
  );
});

Card.displayName = "Card";

const CardHeader = React.memo(({ className, ...props }: React.ComponentProps<"div">) => {
  return (
    <div
      data-slot="card-header"
      className={cn(
        "@container/card-header grid auto-rows-min grid-rows-[auto_auto] items-start gap-3 has-data-[slot=card-action]:grid-cols-[1fr_auto] [.border-b]:pb-4 [.border-b]:border-gray-200",
        className
      )}
      {...props}
    />
  );
});

CardHeader.displayName = "CardHeader";

const CardTitle = React.memo(({ className, ...props }: React.ComponentProps<"div">) => {
  return (
    <div
      data-slot="card-title"
      className={cn(
        "leading-tight font-semibold text-lg text-brand-primary",
        className
      )}
      {...props}
    />
  );
});

CardTitle.displayName = "CardTitle";

const CardDescription = React.memo(({ className, ...props }: React.ComponentProps<"div">) => {
  return (
    <div
      data-slot="card-description"
      className={cn("text-brand-secondary text-sm leading-relaxed", className)}
      {...props}
    />
  );
});

CardDescription.displayName = "CardDescription";

const CardAction = React.memo(({ className, ...props }: React.ComponentProps<"div">) => {
  return (
    <div
      data-slot="card-action"
      className={cn(
        "col-start-2 row-span-2 row-start-1 self-start justify-self-end",
        className
      )}
      {...props}
    />
  );
});

CardAction.displayName = "CardAction";

const CardContent = React.memo(({ className, ...props }: React.ComponentProps<"div">) => {
  return (
    <div data-slot="card-content" className={cn("flex-1", className)} {...props} />
  );
});

CardContent.displayName = "CardContent";

const CardFooter = React.memo(({ className, ...props }: React.ComponentProps<"div">) => {
  return (
    <div
      data-slot="card-footer"
      className={cn(
        "flex items-center gap-3 [.border-t]:pt-4 [.border-t]:border-gray-200",
        className
      )}
      {...props}
    />
  );
});

CardFooter.displayName = "CardFooter";

// 사전 정의된 카드 타입들 (자주 사용되는 패턴)
export const DefaultCard = React.memo(({ children, ...props }: Omit<CardProps, 'variant'>) => (
  <Card variant="default" {...props}>{children}</Card>
));
DefaultCard.displayName = "DefaultCard";

export const ElevatedCard = React.memo(({ children, ...props }: Omit<CardProps, 'variant'>) => (
  <Card variant="elevated" {...props}>{children}</Card>
));
ElevatedCard.displayName = "ElevatedCard";

export const InteractiveCard = React.memo(({ children, ...props }: Omit<CardProps, 'interactive'>) => (
  <Card interactive={true} {...props}>{children}</Card>
));
InteractiveCard.displayName = "InteractiveCard";

export const GradientCard = React.memo(({ children, ...props }: Omit<CardProps, 'variant'>) => (
  <Card variant="gradient" {...props}>{children}</Card>
));
GradientCard.displayName = "GradientCard";

export {
  Card,
  CardHeader,
  CardFooter,
  CardTitle,
  CardAction,
  CardDescription,
  CardContent,
  cardVariants,
  type CardProps,
};