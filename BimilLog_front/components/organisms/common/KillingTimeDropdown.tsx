"use client";

import React from "react";
import { Brain, Music, ChevronDown } from "lucide-react";
import { Dropdown, DropdownItem } from "flowbite-react";
import { useConfirmModal } from "@/components/molecules/modals/confirm-modal";

/**
 * 라운드 17 F-17-BUG-4/5: 폐기 토큰 일괄 정정.
 * vanilla gray + brand-* + purple-* → paper/ink/postal-navy/stamp-red 토큰.
 */
const KILLING_TIME_DROPDOWN_THEME = {
  floating: {
    base: "z-auth-header w-44 rounded-lg divide-y divide-postal-navy/15 dark:divide-postal-navy/40 shadow-lg focus:outline-none",
    content: "py-1 text-sm text-ink dark:text-ink-900",
    style: {
      auto: "border border-postal-navy/15 bg-paper-card text-ink dark:border-postal-navy/40 dark:bg-paper-200 dark:text-ink-900"
    },
  },
  inlineWrapper: "flex items-center"
} as const;

export const KillingTimeDropdown = React.memo(() => {
  const { confirm, ConfirmModalComponent } = useConfirmModal();

  const handlePsychologyTest = async () => {
    const confirmed = await confirm({
      title: "외부 사이트 이동",
      message: "개발자가 만든 킬링타임용 심리테스트 사이트로 이동됩니다.",
      confirmText: "이동",
      cancelText: "취소",
      confirmButtonVariant: "default",
      icon: <Brain className="h-8 w-8 stroke-postal-navy fill-paper-100" aria-hidden="true" />
    });

    if (confirmed) {
      window.open('https://liketests.vercel.app/', '_blank', 'noopener,noreferrer');
    }
  };

  const handleBeatMaker = async () => {
    const confirmed = await confirm({
      title: "외부 사이트 이동",
      message: "개발자가 만든 비트 만들기 사이트로 이동됩니다.",
      confirmText: "이동",
      cancelText: "취소",
      confirmButtonVariant: "default",
      icon: <Music className="h-8 w-8 stroke-postal-navy fill-paper-100" aria-hidden="true" />
    });

    if (confirmed) {
      window.open('https://v0-drum-machine-with-claude.vercel.app/', '_blank', 'noopener,noreferrer');
    }
  };

  return (
    <>
      <Dropdown
        arrowIcon={false}
        inline
        label={
          <span className="flex items-center gap-1 text-sm lg:text-base text-ink-soft hover:text-postal-navy dark:text-ink-500 dark:hover:text-stamp-red cursor-pointer py-2 pr-4 pl-3 md:p-0 break-keep">
            킬링타임
            <ChevronDown className="w-4 h-4" aria-hidden="true" />
          </span>
        }
        theme={KILLING_TIME_DROPDOWN_THEME}
      >
        <DropdownItem onClick={handlePsychologyTest}>
          <Brain className="mr-2 h-4 w-4 stroke-postal-navy fill-paper-100" aria-hidden="true" />
          심리테스트
        </DropdownItem>
        <DropdownItem onClick={handleBeatMaker}>
          <Music className="mr-2 h-4 w-4 stroke-postal-navy fill-paper-100" aria-hidden="true" />
          비트만들기
        </DropdownItem>
      </Dropdown>

      <ConfirmModalComponent />
    </>
  );
});

KillingTimeDropdown.displayName = "KillingTimeDropdown";
