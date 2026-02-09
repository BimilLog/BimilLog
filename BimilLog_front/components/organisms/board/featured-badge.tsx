import { Badge } from "@/components";
import { Megaphone, Crown, Flame, Trophy } from "lucide-react";
import type { FeaturedType } from "@/types/domains/post";

interface FeaturedBadgeProps {
  featuredType: FeaturedType;
}

const FEATURED_CONFIG = {
  NOTICE: { label: "공지", variant: "info" as const, icon: Megaphone },
  WEEKLY: { label: "주간인기", variant: "purple" as const, icon: Flame },
  LEGEND: { label: "레전드", variant: "warning" as const, icon: Crown },
  REALTIME: { label: "실시간", variant: "pink" as const, icon: Trophy },
} as const;

export const FeaturedBadge = ({ featuredType }: FeaturedBadgeProps) => {
  const config = FEATURED_CONFIG[featuredType];
  if (!config) return null;

  return (
    <Badge variant={config.variant} icon={config.icon} className="inline-flex">
      {config.label}
    </Badge>
  );
};
