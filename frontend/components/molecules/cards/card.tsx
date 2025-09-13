import * as React from "react";

import { cn } from "@/lib/utils";

const Card = React.memo(({ className, ...props }: React.ComponentProps<"div">) => {
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
        "leading-tight font-semibold text-lg text-gray-800",
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
      className={cn("text-gray-600 text-sm leading-relaxed", className)}
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
    <div data-slot="card-content" className={cn("", className)} {...props} />
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

export {
  Card,
  CardHeader,
  CardFooter,
  CardTitle,
  CardAction,
  CardDescription,
  CardContent,
};
