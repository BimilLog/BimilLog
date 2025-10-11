"use client";

import { useState, useEffect, useRef, useMemo } from "react";
import dynamic from "next/dynamic";
import {
  Modal,
  ModalHeader,
  ModalBody,
  Spinner as FlowbiteSpinner,
  Button as FlowbiteButton,
  Avatar,
  ListGroup,
  ListGroupItem,
  Badge,
  TextInput
} from "flowbite-react";
import { getInitials } from "@/lib/utils/format";
import {
  Spinner,
  EmptyState,
  Alert
} from "@/components";
import { KakaoFriend } from "@/types/domains/user";
import { logger } from '@/lib/utils/logger';
import { redirectToKakaoConsentOnly } from "@/lib/auth/kakao";
import { Users, MessageCircle, RefreshCw, AlertCircle, Loader2, Search, X } from "lucide-react";
import { useRouter } from "next/navigation";
import { useInfiniteUserFriendList } from "@/hooks/api/useUserQueries";
import { useDebounce } from "@/hooks/common/useDebounce";
import { cn } from "@/lib/utils";

interface KakaoFriendsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const SearchIcon = (props: React.ComponentProps<"svg">) => (
  <Search {...props} className={cn("h-4 w-4", props.className)} />
);

const ClearIcon = (props: React.ComponentProps<"svg">) => (
  <X {...props} className={cn("h-4 w-4", props.className)} />
);

/**
 * 카카오 친구 모달 로딩 컴포넌트
 * Dynamic import 중 카카오톡 스타일의 로딩 UI를 표시
 */
const KakaoFriendsModalLoading = () => (
  <Modal show onClose={() => {}} size="lg">
    <ModalHeader className="border-b border-gray-100 bg-white">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-full border border-purple-100 bg-purple-50 text-brand-primary">
          <Users className="h-5 w-5" />
        </div>
        <div>
          <span className="block text-lg font-semibold text-brand-primary">카카오 친구</span>
          <p className="text-xs text-brand-secondary">친구 목록을 불러오는 중이에요...</p>
        </div>
      </div>
    </ModalHeader>
    <ModalBody className="bg-white">
      <div className="flex min-h-[280px] flex-col items-center justify-center gap-4 px-6 py-10">
        <Spinner className="h-10 w-10 text-brand-primary" />
        <p className="text-sm font-medium text-brand-secondary">따뜻한 공간을 준비하고 있어요...</p>
      </div>
    </ModalBody>
  </Modal>
);

/**
 * 카카오 친구 모달 메인 컴포넌트
 * 카카오톡 친구 목록을 조회하고 비밀로그 회원인 친구들의 롤링페이퍼로 이동 가능
 * 카카오 API 에러 처리 및 동의 플로우 관리 포함
 * 무한 스크롤로 모든 친구 목록 로드 가능
 */
function KakaoFriendsModalContent({ isOpen, onClose }: KakaoFriendsModalProps) {
  // 무한 스크롤 친구 목록 조회 (TanStack Query)
  const {
    data,
    isLoading,
    isError,
    error,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    refetch
  } = useInfiniteUserFriendList(20);

  // 카카오 친구 목록 접근 동의가 필요한 상태 (scope 추가 필요)
  const [needsConsent, setNeedsConsent] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // 검색 기능
  const [searchQuery, setSearchQuery] = useState("");
  const debouncedSearchQuery = useDebounce(searchQuery, 300);

  // Next.js 라우터 (친구 롤링페이퍼 페이지 이동용)
  const router = useRouter();

  // 무한 스크롤을 위한 ref
  const observerTarget = useRef<HTMLDivElement>(null);

  // 전체 친구 목록 (모든 페이지의 친구들을 하나의 배열로)
  const allFriends: KakaoFriend[] = useMemo(() => {
    return data?.pages.flatMap(page =>
      page.success && page.data ? page.data.elements : []
    ) || [];
  }, [data?.pages]);

  // 검색 필터링된 친구 목록
  const filteredFriends = useMemo(() => {
    if (!debouncedSearchQuery.trim()) {
      return allFriends;
    }
    const query = debouncedSearchQuery.toLowerCase();
    return allFriends.filter(friend =>
      friend.profile_nickname.toLowerCase().includes(query) ||
      (friend.memberName && friend.memberName.toLowerCase().includes(query))
    );
  }, [allFriends, debouncedSearchQuery]);

  // 전체 친구 수
  const totalCount = data?.pages[0]?.data?.total_count || 0;

  // 에러 처리
  useEffect(() => {
    if (isError && error) {
      const err = error as { message?: string };
      const errMsg = err.message || "친구 목록을 가져올 수 없습니다.";

      // 카카오 친구 목록 접근 동의가 필요한 경우 감지
      const normalizedMessage = errMsg.replace(/[\s.]/g, "");
      const consentKeywords = [
        "카카오친구추가동의를해야합니다",   // 마침표/공백 제거 후 매칭
        "insufficientscopes",              // 백엔드 원본 Kakao API 에러
        "friendsconsent"                   // 추가 패턴
      ];

      if (consentKeywords.some((keyword) => normalizedMessage.toLowerCase().includes(keyword))) {
        setNeedsConsent(true);
        setErrorMessage("카카오 친구 목록 접근을 위해 추가 동의가 필요합니다.");
      } else {
        setErrorMessage(errMsg);
      }
    }
  }, [isError, error]);

  /**
   * 카카오 친구 목록 동의 처리 함수
   * 사용자 확인 후 카카오 동의 페이지로 리다이렉트 (로그아웃 없음)
   * 동의 완료 후 자동으로 원래 페이지로 복귀
   */
  const handleConsentClick = () => {
    try {
      // 사용자에게 동의 프로세스 안내
      const confirmed = window.confirm(
        "카카오 친구 목록 접근 권한을 받기 위해 카카오 동의 페이지로 이동합니다.\n\n" +
          "동의 완료 후 자동으로 이 페이지로 돌아옵니다."
      );

      if (confirmed) {
        redirectToKakaoConsentOnly();
      }
    } catch (err) {
      logger.error("카카오 동의 처리 실패:", err);
      setErrorMessage(
        err instanceof Error
          ? err.message
          : "동의 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
      );
    }
  };

  // 무한 스크롤 IntersectionObserver 설정
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasNextPage && !isFetchingNextPage) {
          fetchNextPage();
        }
      },
      { threshold: 1.0 }
    );

    const target = observerTarget.current;
    if (target) {
      observer.observe(target);
    }

    return () => {
      if (target) {
        observer.unobserve(target);
      }
    };
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  /**
   * 친구의 롤링페이퍼 페이지로 이동하는 핸들러
   * memberName을 URL 인코딩하여 안전한 라우팅 보장
   */
  const handleVisitRollingPaper = (memberName: string) => {
    if (memberName) {
      // 친구 롤링페이퍼 페이지로 라우팅 후 모달 닫기
      router.push(`/rolling-paper/${encodeURIComponent(memberName)}`);
      onClose();
    }
  };

  // 모달이 닫힐 때 검색어 초기화
  useEffect(() => {
    if (!isOpen) {
      setSearchQuery("");
      setNeedsConsent(false);
      setErrorMessage(null);
    }
  }, [isOpen]);


  return (
    <Modal show={isOpen} onClose={onClose} size="lg">
      <ModalHeader className="border-b border-gray-100 bg-white">
        <div className="flex w-full flex-col gap-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-full border border-purple-100 bg-purple-50 text-brand-primary">
                <Users className="h-5 w-5" />
              </div>
              <div>
                <p className="text-lg font-semibold text-brand-primary">카카오 친구</p>
                <p className="text-sm text-brand-secondary">비밀로그를 함께하는 친구들을 찾아보세요</p>
              </div>
            </div>
            <FlowbiteButton
              type="button"
              color="light"
              pill
              size="sm"
              onClick={() => refetch()}
              disabled={isLoading}
              className="shrink-0"
              aria-label="친구 목록 새로고침"
            >
              {isLoading ? (
                <FlowbiteSpinner size="sm" aria-label="새로고침 중..." />
              ) : (
                <RefreshCw className="h-4 w-4" />
              )}
            </FlowbiteButton>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {totalCount > 0 && (
              <Badge color="purple" className="font-medium">
                {debouncedSearchQuery ? `${filteredFriends.length}명` : `${allFriends.length}/${totalCount}명`} 연결됨
              </Badge>
            )}
            {debouncedSearchQuery && (
              <span className="text-xs text-gray-500">“{searchQuery}” 검색 결과</span>
            )}
          </div>
          <div className="relative">
            <TextInput
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="친구 이름 검색..."
              icon={SearchIcon}
              className="w-full"
              shadow
              color="gray"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery("")}
                className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full bg-gray-100 p-1 text-gray-500 transition hover:bg-gray-200 hover:text-gray-700"
                aria-label="검색어 지우기"
              >
                <ClearIcon />
              </button>
            )}
          </div>
        </div>
      </ModalHeader>
      <ModalBody className="bg-white">
        <div className="max-h-[420px] space-y-6 overflow-y-auto pr-1">
          {isLoading ? (
            <div className="flex flex-col items-center gap-3 rounded-xl border border-gray-100 bg-gray-50 py-12">
              <Spinner className="h-9 w-9 text-brand-primary" />
              <p className="text-sm font-medium text-brand-secondary">친구 목록을 불러오는 중이에요...</p>
            </div>
          ) : needsConsent ? (
            <div className="rounded-xl border border-purple-100 bg-purple-50/60 p-6">
              <Alert className="gap-3 border-none bg-transparent p-0">
                <AlertCircle className="h-5 w-5 text-brand-primary" />
                <div className="space-y-2">
                  <h4 className="text-sm font-semibold text-brand-primary">카카오 친구 목록 동의가 필요해요</h4>
                  <p className="text-sm leading-relaxed text-brand-secondary">
                    카카오톡 친구 목록을 확인하려면 추가 동의가 필요합니다. 동의 페이지로 이동하여 &lsquo;친구 목록 제공&rsquo;을 허용해주세요.
                  </p>
                  <div className="flex flex-wrap gap-2">
                    <FlowbiteButton
                      type="button"
                      size="sm"
                      onClick={handleConsentClick}
                      className="bg-gradient-to-r from-purple-600 to-pink-600 text-white hover:from-purple-700 hover:to-pink-700"
                    >
                      동의하러 가기
                    </FlowbiteButton>
                    <FlowbiteButton type="button" color="light" size="sm" onClick={() => refetch()}>
                      다시 시도
                    </FlowbiteButton>
                  </div>
                </div>
              </Alert>
            </div>
          ) : errorMessage ? (
            <div className="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
              <EmptyState
                icon={<Users className="h-12 w-12 text-brand-primary" />}
                title="친구 목록을 불러올 수 없습니다"
                description={errorMessage}
                actionLabel="다시 시도"
                onAction={() => refetch()}
              />
            </div>
          ) : allFriends.length === 0 ? (
            <div className="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
              <EmptyState
                icon={<Users className="h-12 w-12 text-brand-primary" />}
                title="친구가 없습니다"
                description="카카오톡에서 친구를 추가하고 다시 시도해보세요"
                actionLabel="새로고침"
                onAction={() => refetch()}
              />
            </div>
          ) : filteredFriends.length === 0 ? (
            <div className="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
              <EmptyState
                icon={<Search className="h-12 w-12 text-brand-primary" />}
                title="검색 결과가 없습니다"
                description={`"${searchQuery}"와 일치하는 친구를 찾을 수 없습니다`}
                actionLabel="검색 초기화"
                onAction={() => setSearchQuery("")}
              />
            </div>
          ) : (
            <div className="space-y-3">
              <ListGroup className="divide-y divide-gray-100 rounded-2xl border border-gray-100 bg-white">
                {filteredFriends.map((friend) => (
                  <ListGroupItem
                    key={friend.id}
                    className="flex flex-col gap-3 bg-white transition hover:bg-purple-50/40 focus:outline-none focus:ring-2 focus:ring-purple-200 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div className="flex w-full flex-1 items-center gap-3">
                      <Avatar
                        img={friend.profile_thumbnail_image}
                        alt={friend.profile_nickname}
                        placeholderInitials={getInitials(friend.profile_nickname)}
                        rounded
                        size="md"
                        className="h-12 w-12"
                      />
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <h3 className="truncate text-sm font-semibold text-brand-primary">
                            {friend.profile_nickname}
                          </h3>
                          {friend.memberName && (
                            <Badge color="purple" className="text-[11px] font-semibold">
                              @{friend.memberName}
                            </Badge>
                          )}
                        </div>
                        {friend.memberName && (
                          <p className="text-xs text-brand-secondary">비밀로그 사용자</p>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center justify-end gap-2">
                      {friend.memberName ? (
                        <FlowbiteButton
                          type="button"
                          size="sm"
                          onClick={() => handleVisitRollingPaper(friend.memberName)}
                          className="bg-gradient-to-r from-purple-600 to-pink-600 text-white hover:from-purple-700 hover:to-pink-700"
                        >
                          <MessageCircle className="h-4 w-4" />
                          <span className="ml-1 text-sm">롤링페이퍼</span>
                        </FlowbiteButton>
                      ) : (
                        <Badge color="gray" className="text-xs font-medium">
                          미가입
                        </Badge>
                      )}
                    </div>
                  </ListGroupItem>
                ))}
              </ListGroup>
              {!debouncedSearchQuery && hasNextPage && (
                <div ref={observerTarget} className="flex justify-center py-2">
                  {isFetchingNextPage && (
                    <div className="flex items-center gap-2 rounded-full border border-gray-200 bg-white px-4 py-2 text-sm text-brand-secondary shadow-sm">
                      <Loader2 className="h-4 w-4 animate-spin" />
                      더 많은 친구 불러오는 중...
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
        {allFriends.length > 0 && (
          <div className="pt-4 text-center text-xs text-brand-secondary">
            비밀로그에 가입한 친구에게만 롤링페이퍼를 보낼 수 있어요.
          </div>
        )}
      </ModalBody>
    </Modal>
  );
}

/**
 * 카카오 친구 모달 컴포넌트 (Dynamic Import)
 * SSR 환경에서 카카오 API 호출 문제를 방지하기 위해
 * 클라이언트 사이드에서만 렌더링되도록 설정
 */
const KakaoFriendsModal = dynamic(
  () => Promise.resolve(KakaoFriendsModalContent),
  {
    ssr: false,
    loading: () => <KakaoFriendsModalLoading />,
  }
);

export { KakaoFriendsModal };
