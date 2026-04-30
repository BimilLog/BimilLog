"use client";

import React from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import { Button } from "@/components";
import { Mail, Heart, Home, Users, PenLine } from "lucide-react";

interface SuggestSuccessExitsProps {
  reporterName: string;
  isAuthenticated: boolean;
  onWriteAnother: () => void;
}

interface ExitDestination {
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  title: string;
  description: string;
}

const ANONYMOUS_DESTINATIONS: ReadonlyArray<ExitDestination> = [
  {
    href: "/",
    icon: Home,
    title: "비밀편지 살펴보기",
    description: "내 비밀편지함이 궁금하다면",
  },
  {
    href: "/board",
    icon: Users,
    title: "이야기 나누러 가기",
    description: "다른 사람들과 자유롭게 대화해요",
  },
];

const AUTHENTICATED_DESTINATIONS: ReadonlyArray<ExitDestination> = [
  {
    href: "/",
    icon: Home,
    title: "비밀편지함으로",
    description: "오늘 도착한 편지를 살펴봐요",
  },
  {
    href: "/mypage",
    icon: Mail,
    title: "내 편지 둘러보기",
    description: "받은 메시지를 모아 볼 수 있어요",
  },
  {
    href: "/board",
    icon: Users,
    title: "이야기 나누러 가기",
    description: "다른 사람들과 자유롭게 대화해요",
  },
];

/**
 * 건의 제출 성공 후 사후 진입로 카드 (라운드 12 / F-12-BUG-10).
 *
 * 기능:
 * - 폼 reset 후 막다른 골목 해소: 진입로 3개 + "다시 의견 보내기" 액션.
 * - 익명/실명 카피 분기 (F-12-BUG-9 일관).
 * - 종이/편지 메타포 카피 ("잘 받았어요", "비밀편지함으로", "이야기 나누러 가기").
 *
 * 접근성:
 * - aria-live="polite" 로 sr 사용자에게 성공 알림.
 * - 카드 진입로는 기본 Link a 태그라 키보드/SR 접근 자동 보장.
 */
export const SuggestSuccessExits: React.FC<SuggestSuccessExitsProps> = ({
  reporterName,
  isAuthenticated,
  onWriteAnother,
}) => {
  const destinations = isAuthenticated
    ? AUTHENTICATED_DESTINATIONS
    : ANONYMOUS_DESTINATIONS;

  const greetingCopy = isAuthenticated
    ? `${reporterName} 님이 보내주신 의견은 차근차근 살펴볼게요.`
    : "익명으로 보내주신 의견도 차근차근 살펴볼게요.";

  return (
    <Card
      role="region"
      aria-live="polite"
      aria-label="건의사항 접수 완료"
      className="border border-ink-soft shadow-brand-xl bg-paper-aged backdrop-blur-sm"
    >
      <CardHeader className="text-center">
        <div className="flex items-center justify-center space-x-3 mb-2">
          <div className="w-12 h-12 bg-seal-gold rounded-full flex items-center justify-center">
            <Heart className="w-6 h-6 text-paper-50" aria-hidden="true" />
          </div>
        </div>
        <CardTitle className="font-display text-2xl text-ink break-keep">
          편지를 잘 받았어요
        </CardTitle>
        <p className="text-ink-soft mt-2 break-keep">{greetingCopy}</p>
      </CardHeader>

      <CardContent className="p-6 space-y-6">
        <div>
          <h3 className="text-sm font-semibold text-ink mb-3 break-keep">
            이제 어디로 가볼까요?
          </h3>
          <ul className="grid gap-3 sm:grid-cols-2">
            {destinations.map((dest) => {
              const Icon = dest.icon;
              return (
                <li key={dest.href}>
                  <Link
                    href={dest.href}
                    className="group flex items-start gap-3 rounded-lg border border-ink-soft bg-paper-50/80 p-4 min-h-[44px] transition-colors hover:bg-paper-50 hover:border-stamp-red/40 focus:outline-none focus-visible:ring-2 focus-visible:ring-stamp-red focus-visible:ring-offset-2 focus-visible:ring-offset-paper-aged"
                  >
                    <div className="w-10 h-10 rounded-full bg-postal-navy/10 flex items-center justify-center shrink-0 group-hover:bg-postal-navy/20">
                      <Icon className="w-5 h-5 text-postal-navy" aria-hidden="true" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-semibold text-ink break-keep">
                        {dest.title}
                      </p>
                      <p className="text-xs text-ink-soft mt-1 break-keep">
                        {dest.description}
                      </p>
                    </div>
                  </Link>
                </li>
              );
            })}
          </ul>
        </div>

        <div className="border-t border-ink-soft pt-4">
          <Button
            type="button"
            variant="outline"
            onClick={onWriteAnother}
            className="w-full"
          >
            <PenLine className="w-4 h-4" aria-hidden="true" />
            <span>다른 의견도 들려주기</span>
          </Button>
        </div>
      </CardContent>
    </Card>
  );
};

SuggestSuccessExits.displayName = "SuggestSuccessExits";
