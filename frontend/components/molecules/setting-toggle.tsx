import React from "react";
import { Spinner } from "flowbite-react";
import { Label, Switch } from "@/components";

interface SettingToggleProps {
  icon: React.ReactNode;
  label: string;
  description: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  disabled?: boolean;
  saving?: boolean;
  saved?: boolean;
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
  saving = false,
  saved = false,
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
    <div className="flex items-center gap-2">
      {saving && (
        <Spinner size="sm" color="purple" aria-label="저장 중..." />
      )}
      <Switch
        checked={checked}
        onCheckedChange={onChange}
        disabled={disabled || saving}
      />
    </div>
  </div>
);