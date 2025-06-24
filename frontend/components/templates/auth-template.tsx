import React from "react";
import { cn } from "@/lib/utils";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/molecules/card";

interface AuthTemplateProps {
  children: React.ReactNode;
  title: string;
  description?: string;
  backgroundImage?: string;
  logo?: React.ReactNode;
  className?: string;
}

export const AuthTemplate = React.forwardRef<HTMLDivElement, AuthTemplateProps>(
  ({ children, title, description, backgroundImage, logo, className }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(
          "min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8",
          className
        )}
        style={
          backgroundImage
            ? {
                backgroundImage: `url(${backgroundImage})`,
                backgroundSize: "cover",
                backgroundPosition: "center",
              }
            : undefined
        }
      >
        {/* Background Overlay */}
        {backgroundImage && <div className="absolute inset-0 bg-black/50" />}

        {/* Content Container */}
        <div className="relative w-full max-w-md space-y-8">
          {/* Logo */}
          {logo && <div className="flex justify-center">{logo}</div>}

          {/* Auth Card */}
          <Card className="bg-white/90 backdrop-blur-sm">
            <CardHeader className="space-y-1">
              <CardTitle className="text-2xl font-bold text-center">
                {title}
              </CardTitle>
              {description && (
                <CardDescription className="text-center">
                  {description}
                </CardDescription>
              )}
            </CardHeader>
            <CardContent>{children}</CardContent>
          </Card>
        </div>
      </div>
    );
  }
);

AuthTemplate.displayName = "AuthTemplate";
