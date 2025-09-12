"use client";

import React from "react";
import { Input } from "@/components";
import { Button } from "@/components";
import { Icon } from "@/components";
import { Search, X } from "lucide-react";
import { cn } from "@/lib/utils";

interface SearchBoxProps {
  value: string;
  onChange: (value: string) => void;
  onSearch?: () => void;
  onClear?: () => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
}

export const SearchBox = React.forwardRef<HTMLDivElement, SearchBoxProps>(
  (
    {
      value,
      onChange,
      onSearch,
      onClear,
      placeholder = "검색어를 입력하세요...",
      disabled,
      className,
    },
    ref
  ) => {
    const handleKeyPress = (e: React.KeyboardEvent) => {
      if (e.key === "Enter" && onSearch) {
        onSearch();
      }
    };

    const handleClear = () => {
      onChange("");
      if (onClear) {
        onClear();
      }
    };

    return (
      <div ref={ref} className={cn("relative flex items-center", className)}>
        <Input
          type="text"
          value={value}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => onChange(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder={placeholder}
          disabled={disabled}
          className="pr-20"
        />

        <div className="absolute right-2 flex items-center gap-1">
          {value && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={handleClear}
              disabled={disabled}
              className="h-6 w-6 p-0 hover:bg-gray-100"
            >
              <Icon icon={X} size="xs" />
            </Button>
          )}

          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={onSearch}
            disabled={disabled}
            className="h-6 w-6 p-0 hover:bg-gray-100"
          >
            <Icon icon={Search} size="xs" />
          </Button>
        </div>
      </div>
    );
  }
);

SearchBox.displayName = "SearchBox";
