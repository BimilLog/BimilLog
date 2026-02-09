import { Badge } from "@/components";
import { Megaphone, Crown, Flame, Trophy } from "lucide-react";

type FeaturedKey = "NOTICE" | "WEEKLY" | "LEGEND" | "REALTIME";

interface FeaturedBadgeProps {
  type: FeaturedKey;
}

const FEATURED_CONFIG = {
  NOTICE: { label: "공지", variant: "info" as const, icon: Megaphone },
  WEEKLY: { label: "주간인기", variant: "purple" as const, icon: Flame },
  LEGEND: { label: "레전드", variant: "warning" as const, icon: Crown },
  REALTIME: { label: "실시간", variant: "pink" as const, icon: Trophy },
} as const;

export const FeaturedBadge = ({ type }: FeaturedBadgeProps) => {
  const config = FEATURED_CONFIG[type];
  if (!config) return null;

  return (
    <Badge variant={config.variant} icon={config.icon} className="inline-flex">
      {config.label}
    </Badge>
  );
};

interface FeaturedBadgesProps {
  weekly?: boolean;
  legend?: boolean;
  notice?: boolean;
}

export const FeaturedBadges = ({ weekly, legend, notice }: FeaturedBadgesProps) => {
  const badges: FeaturedKey[] = [];
  if (notice) badges.push("NOTICE");
  if (legend) badges.push("LEGEND");
  if (weekly) badges.push("WEEKLY");

  if (badges.length === 0) return null;

  return (
    <>
      {badges.map((type) => (
        <FeaturedBadge key={type} type={type} />
      ))}
    </>
  );
};
