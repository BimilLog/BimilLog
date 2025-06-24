import * as React from "react";

import { cn } from "@/lib/utils";

function Card({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card"
      className={cn(
        "bg-white/80 backdrop-blur-sm text-gray-800 flex flex-col gap-6 rounded-lg border-0 shadow-lg hover:shadow-xl transition-all duration-300 p-6",
        className
      )}
      {...props}
    />
  );
}

function CardHeader({ className, ...props }: React.ComponentProps<"div">) {
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
}

function CardTitle({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-title"
      className={cn(
        "leading-tight font-semibold text-lg text-gray-800",
        className
      )}
      {...props}
    />
  );
}

function CardDescription({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-description"
      className={cn("text-gray-600 text-sm leading-relaxed", className)}
      {...props}
    />
  );
}

function CardAction({ className, ...props }: React.ComponentProps<"div">) {
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
}

function CardContent({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div data-slot="card-content" className={cn("", className)} {...props} />
  );
}

function CardFooter({ className, ...props }: React.ComponentProps<"div">) {
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
}

function FeatureCard({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <Card
      className={cn(
        "text-center p-6 hover:scale-[1.02] active:scale-[0.98] cursor-pointer",
        className
      )}
      {...props}
    />
  );
}

function CTACard({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="cta-card"
      className={cn(
        "bg-gradient-to-r from-pink-500 via-purple-600 to-indigo-600 text-white flex flex-col gap-6 rounded-lg border-0 shadow-2xl p-8 md:p-12",
        className
      )}
      {...props}
    />
  );
}

function BottomSheetCard({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="bottom-sheet-card"
      className={cn(
        "bg-white rounded-t-2xl border-0 shadow-2xl p-6 pb-8",
        "pb-[calc(1.5rem+env(safe-area-inset-bottom))]",
        className
      )}
      {...props}
    />
  );
}

export {
  Card,
  CardHeader,
  CardFooter,
  CardTitle,
  CardAction,
  CardDescription,
  CardContent,
  FeatureCard,
  CTACard,
  BottomSheetCard,
};
