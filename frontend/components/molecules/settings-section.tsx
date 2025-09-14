import React from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components";

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
  <Card variant="elevated" className={`hover:shadow-xl transition-shadow ${className || ""}`}>
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