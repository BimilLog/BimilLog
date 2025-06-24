import React from "react";
import { cn } from "@/lib/utils";

interface PageTemplateProps {
  children: React.ReactNode;
  header?: React.ReactNode;
  sidebar?: React.ReactNode;
  footer?: React.ReactNode;
  className?: string;
  headerClassName?: string;
  mainClassName?: string;
  sidebarClassName?: string;
  footerClassName?: string;
}

export const PageTemplate = React.forwardRef<HTMLDivElement, PageTemplateProps>(
  (
    {
      children,
      header,
      sidebar,
      footer,
      className,
      headerClassName,
      mainClassName,
      sidebarClassName,
      footerClassName,
    },
    ref
  ) => {
    return (
      <div ref={ref} className={cn("min-h-screen flex flex-col", className)}>
        {/* Header */}
        {header && (
          <header className={cn("sticky top-0 z-50", headerClassName)}>
            {header}
          </header>
        )}

        {/* Main Content Area */}
        <div className="flex flex-1">
          {/* Sidebar */}
          {sidebar && (
            <aside
              className={cn(
                "w-64 bg-gray-50 border-r border-gray-200 hidden lg:block",
                sidebarClassName
              )}
            >
              {sidebar}
            </aside>
          )}

          {/* Main Content */}
          <main className={cn("flex-1", mainClassName)}>{children}</main>
        </div>

        {/* Footer */}
        {footer && (
          <footer className={cn("mt-auto", footerClassName)}>{footer}</footer>
        )}
      </div>
    );
  }
);

PageTemplate.displayName = "PageTemplate";
