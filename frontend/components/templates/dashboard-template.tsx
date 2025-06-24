import React from "react";
import { cn } from "@/lib/utils";

interface DashboardTemplateProps {
  children: React.ReactNode;
  header: React.ReactNode;
  sidebar: React.ReactNode;
  breadcrumb?: React.ReactNode;
  actions?: React.ReactNode;
  className?: string;
}

export const DashboardTemplate = React.forwardRef<
  HTMLDivElement,
  DashboardTemplateProps
>(({ children, header, sidebar, breadcrumb, actions, className }, ref) => {
  return (
    <div ref={ref} className={cn("h-screen flex overflow-hidden", className)}>
      {/* Sidebar */}
      <div className="hidden md:flex md:w-64 md:flex-col">
        <div className="flex flex-col flex-grow pt-5 overflow-y-auto bg-gray-900">
          {sidebar}
        </div>
      </div>

      {/* Main Content */}
      <div className="flex flex-col w-0 flex-1 overflow-hidden">
        {/* Header */}
        <div className="relative z-10 flex-shrink-0 flex h-16 bg-white shadow">
          {header}
        </div>

        {/* Page Content */}
        <main className="flex-1 relative overflow-y-auto focus:outline-none">
          <div className="py-6">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 md:px-8">
              {/* Breadcrumb and Actions */}
              {(breadcrumb || actions) && (
                <div className="flex items-center justify-between mb-8">
                  <div className="flex-1">{breadcrumb}</div>
                  {actions && (
                    <div className="flex items-center space-x-4">{actions}</div>
                  )}
                </div>
              )}

              {/* Main Content */}
              <div className="py-4">{children}</div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
});

DashboardTemplate.displayName = "DashboardTemplate";
