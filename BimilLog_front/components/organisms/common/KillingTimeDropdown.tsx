"use client";

import React from "react";
import { Brain, Music, ChevronDown } from "lucide-react";
import { Dropdown, DropdownItem } from "flowbite-react";
import { useConfirmModal } from "@/components/molecules/modals/confirm-modal";

const KILLING_TIME_DROPDOWN_THEME = {
  floating: {
    base: "z-50 w-44 rounded-lg divide-y divide-gray-100 shadow-lg focus:outline-none",
    content: "py-1 text-sm text-gray-700 dark:text-gray-200",
    style: {
      auto: "border border-gray-200 bg-white text-gray-900 dark:border-none dark:bg-gray-700 dark:text-white"
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
      icon: <Brain className="h-8 w-8 stroke-purple-600 fill-purple-100" />
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
      icon: <Music className="h-8 w-8 stroke-purple-600 fill-purple-100" />
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
          <span className="flex items-center gap-1 text-sm lg:text-base text-brand-muted hover:text-brand-primary cursor-pointer py-2 pr-4 pl-3 md:p-0">
            킬링타임
            <ChevronDown className="w-4 h-4" />
          </span>
        }
        theme={KILLING_TIME_DROPDOWN_THEME}
      >
        <DropdownItem onClick={handlePsychologyTest}>
          <Brain className="mr-2 h-4 w-4 stroke-purple-600 fill-purple-100" />
          심리테스트
        </DropdownItem>
        <DropdownItem onClick={handleBeatMaker}>
          <Music className="mr-2 h-4 w-4 stroke-purple-600 fill-purple-100" />
          비트만들기
        </DropdownItem>
      </Dropdown>

      <ConfirmModalComponent />
    </>
  );
});

KillingTimeDropdown.displayName = "KillingTimeDropdown";
