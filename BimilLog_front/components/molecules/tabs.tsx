"use client"

import * as React from "react"
import * as TabsPrimitive from "@radix-ui/react-tabs"

import { cn } from "@/lib/utils"

const Tabs = React.memo(({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.Root>) => {
  return (
    <TabsPrimitive.Root
      data-slot="tabs"
      className={cn("flex flex-col gap-2", className)}
      {...props}
    />
  )
});
Tabs.displayName = "Tabs";

const TabsList = React.memo(({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.List>) => {
  return (
    <TabsPrimitive.List
      data-slot="tabs-list"
      className={cn(
        "flex h-10 items-center justify-center rounded-lg p-0.5 sm:p-1 bg-muted border border-border",
        className
      )}
      {...props}
    />
  )
});
TabsList.displayName = "TabsList";

const TabsTrigger = React.memo(({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.Trigger>) => {
  return (
    <TabsPrimitive.Trigger
      data-slot="tabs-trigger"
      className={cn(
        "inline-flex h-8 flex-1 items-center justify-center gap-1.5 rounded-md border border-transparent px-1.5 sm:px-3 py-1.5 text-[10px] sm:text-sm font-medium transition-all focus-visible:ring-2 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
        "text-muted-foreground hover:text-foreground",
        "data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:font-bold data-[state=active]:shadow-md data-[state=active]:border-purple-200 dark:data-[state=active]:border-purple-600 data-[state=active]:ring-1 data-[state=active]:ring-purple-300 dark:data-[state=active]:ring-purple-500 data-[state=active]:scale-[1.02]",
        className
      )}
      {...props}
    />
  )
});
TabsTrigger.displayName = "TabsTrigger";

const TabsContent = React.memo(({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.Content>) => {
  return (
    <TabsPrimitive.Content
      data-slot="tabs-content"
      className={cn("flex-1 outline-none", className)}
      {...props}
    />
  )
});
TabsContent.displayName = "TabsContent";

export { Tabs, TabsList, TabsTrigger, TabsContent }
