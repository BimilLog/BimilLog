"use client";

import React, { useId, useRef } from "react";
import { Lightbulb, Bug } from "lucide-react";
import { cn } from "@/lib/utils";

export type SuggestionType = "ERROR" | "IMPROVEMENT";

interface SuggestionOption {
  value: SuggestionType;
  label: string;
  shortLabel: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
  iconBg: string;
}

const OPTIONS: ReadonlyArray<SuggestionOption> = [
  {
    value: "IMPROVEMENT",
    label: "기능 개선 제안",
    shortLabel: "기능 제안",
    description: "새로운 기능이나 기존 기능 개선 아이디어",
    icon: Lightbulb,
    iconBg: "bg-postal-navy",
  },
  {
    value: "ERROR",
    label: "오류 신고",
    shortLabel: "오류 신고",
    description: "버그, 오작동, 기술적 문제 신고",
    icon: Bug,
    iconBg: "bg-stamp-red",
  },
];

interface SuggestTypeRadioGroupProps {
  value: SuggestionType | "";
  onChange: (next: SuggestionType) => void;
  /** 라벨이 외부에 있을 때 사용 */
  ariaLabelledBy?: string;
}

/**
 * 건의 종류 라디오 그룹 (WAI-ARIA APG §3.16 Radio Group, 라운드 12 / F-12-BUG-2).
 *
 * 키보드 동작:
 * - Tab: 그룹 진입 (선택된 옵션 또는 첫 옵션이 tabbable)
 * - Space/Enter: 현재 포커스된 옵션 선택
 * - ArrowRight/Down: 다음 옵션 선택 + 포커스 (wrap)
 * - ArrowLeft/Up: 이전 옵션 선택 + 포커스 (wrap)
 *
 * roving tabindex: 선택된 옵션만 tabIndex=0, 나머지는 -1.
 * 미선택 상태: 첫 옵션이 tabbable.
 */
export function SuggestTypeRadioGroup({
  value,
  onChange,
  ariaLabelledBy,
}: SuggestTypeRadioGroupProps) {
  const groupLabelId = useId();
  const itemRefs = useRef<Array<HTMLButtonElement | null>>([]);

  const handleKeyDown = (
    e: React.KeyboardEvent<HTMLButtonElement>,
    index: number
  ) => {
    switch (e.key) {
      case "ArrowRight":
      case "ArrowDown": {
        e.preventDefault();
        const next = (index + 1) % OPTIONS.length;
        onChange(OPTIONS[next].value);
        itemRefs.current[next]?.focus();
        break;
      }
      case "ArrowLeft":
      case "ArrowUp": {
        e.preventDefault();
        const prev = (index - 1 + OPTIONS.length) % OPTIONS.length;
        onChange(OPTIONS[prev].value);
        itemRefs.current[prev]?.focus();
        break;
      }
      case " ":
      case "Enter": {
        e.preventDefault();
        onChange(OPTIONS[index].value);
        break;
      }
    }
  };

  return (
    <div className="mb-8">
      <h2
        id={groupLabelId}
        className="font-display text-2xl font-bold text-ink mb-6 text-center break-keep"
      >
        어떤 의견을 들려주시겠어요?
      </h2>
      <div
        role="radiogroup"
        aria-labelledby={ariaLabelledBy ?? groupLabelId}
        className="grid md:grid-cols-2 gap-4"
      >
        {OPTIONS.map((option, index) => {
          const Icon = option.icon;
          const isSelected = value === option.value;
          const isTabbable = isSelected || (value === "" && index === 0);

          return (
            <button
              key={option.value}
              ref={(el) => {
                itemRefs.current[index] = el;
              }}
              type="button"
              role="radio"
              aria-checked={isSelected}
              tabIndex={isTabbable ? 0 : -1}
              onClick={() => onChange(option.value)}
              onKeyDown={(e) => handleKeyDown(e, index)}
              className={cn(
                "flex flex-col rounded-lg backdrop-blur-sm transition-all duration-300 text-brand-primary dark:text-brand-primary",
                "border-2 cursor-pointer min-h-[44px] w-full text-left",
                "shadow-brand-lg hover:shadow-brand-xl dark:shadow-brand-md dark:hover:shadow-brand-xl",
                isSelected
                  ? "border-stamp-red bg-paper-aged ring-2 ring-stamp-red/30"
                  : "border-ink-soft hover:border-stamp-red/40 bg-paper-50/80",
                "focus:outline-none focus-visible:ring-2 focus-visible:ring-stamp-red focus-visible:ring-offset-2 focus-visible:ring-offset-paper-50"
              )}
            >
              <div className="p-6 text-center flex-1">
                <div
                  className={cn(
                    "w-12 h-12 rounded-full flex items-center justify-center mx-auto mb-4",
                    option.iconBg
                  )}
                >
                  <Icon className="w-6 h-6 text-paper-50" />
                </div>
                <h3 className="text-lg font-semibold mb-2 text-brand-primary break-keep">
                  <span className="hidden sm:inline">{option.label}</span>
                  <span className="sm:hidden">{option.shortLabel}</span>
                </h3>
                <p className="text-sm text-brand-muted break-keep">
                  {option.description}
                </p>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

export const SUGGEST_OPTIONS = OPTIONS;
