import React from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

interface SettingsSectionProps {
  icon: React.ReactNode;
  title: string;
  description: string;
  children: React.ReactNode;
  className?: string;
}

export const SettingsSection: React.FC<SettingsSectionProps> = ({
  icon,
  title,
  description,
  children,
  className,
}) => (
  <Card className={`border-0 shadow-lg hover:shadow-xl transition-shadow bg-white/80 backdrop-blur-sm ${className || ""}`}>
    <CardHeader>
      <CardTitle className="flex items-center gap-2">
        {icon}
        {title}
      </CardTitle>
      <CardDescription>{description}</CardDescription>
    </CardHeader>
    <CardContent>{children}</CardContent>
  </Card>
);