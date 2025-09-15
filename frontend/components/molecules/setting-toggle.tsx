import React from "react";
import { ToggleSwitch } from "flowbite-react";
import { Label } from "@/components";

interface SettingToggleProps {
  icon: React.ReactNode;
  label: string;
  description: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  disabled?: boolean;
  gradient?: string;
  className?: string;
}

export const SettingToggle: React.FC<SettingToggleProps> = ({
  icon,
  label,
  description,
  checked,
  onChange,
  disabled = false,
  gradient = "from-gray-500 to-gray-600",
  className,
}) => (
  <div className={`flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors ${className || ""}`}>
    <div className="flex items-center gap-3">
      <div
        className={`w-8 h-8 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center`}
      >
        <div className="text-white">{icon}</div>
      </div>
      <div className="space-y-1">
        <Label className="font-medium text-brand-primary">{label}</Label>
        <p className="text-sm text-brand-muted">{description}</p>
      </div>
    </div>
    <ToggleSwitch
      checked={checked}
      onChange={onChange}
      disabled={disabled}
    />
  </div>
);