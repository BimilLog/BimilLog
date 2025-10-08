import React from "react";
import { Card, CardContent } from "@/components";
import { TrendingUp } from "lucide-react";
import { formatNumber } from "@/lib/utils";

interface StatCardProps {
  icon: React.ReactNode;
  value: number;
  label: string;
  color: string;
  gradient: string;
  description: string;
  className?: string;
}

export const StatCard = React.memo<StatCardProps>(({
  icon,
  value,
  label,
  color,
  gradient,
  description,
  className,
}) => (
  <Card variant="elevated" interactive={true} className={`group hover:scale-105 cursor-pointer ${className || ""}`}>
    <CardContent className="p-6 text-center relative overflow-hidden">
      <div
        className={`absolute inset-0 bg-gradient-to-br ${gradient} opacity-5 group-hover:opacity-10 transition-opacity duration-300`}
      />

      <div
        className={`w-14 h-14 bg-gradient-to-r ${gradient} rounded-xl flex items-center justify-center mx-auto mb-4 group-hover:scale-110 transition-transform duration-300`}
      >
        <div className="text-white">{icon}</div>
      </div>

      <div className="relative z-10">
        <p
          className={`text-3xl font-bold ${color} mb-1 group-hover:scale-110 transition-transform duration-300`}
        >
          {formatNumber(value)}
        </p>
        <p className="text-sm font-medium text-brand-primary mb-2">{label}</p>
        <p className="text-xs text-brand-secondary">{description}</p>
      </div>

      <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
        <TrendingUp className="w-4 h-4 stroke-green-600 fill-green-100" />
      </div>
    </CardContent>
  </Card>
));

StatCard.displayName = "StatCard";