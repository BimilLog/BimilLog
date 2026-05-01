"use client";

import { memo, useEffect, useState } from "react";
import { Modal, ModalHeader, ModalBody } from "flowbite-react";
import {
  Card,
  Badge,
  Button,
  ScrollArea,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Alert,
  AlertTitle,
  AlertDescription
} from "@/components";
import {
  User,
  Calendar,
  FileText,
  AlertTriangle,
  Ban,
  UserX,
  MessageSquare,
  Clock,
  ChevronRight
} from "lucide-react";
import { useReportActions } from "@/hooks/features/admin";
import { useConfirmModal } from "@/components/molecules/modals/confirm-modal";
import {
  getReportTypeLabel,
  getReportTypeBadgeColor,
} from "@/lib/utils/admin/config";
import { formatKoreanDateTime } from "@/lib/utils/date";
import type { Report } from "@/types/domains/admin";

/**
 * 신고 상세 정보를 보여주는 관리자용 모달 컴포넌트
 *
 * 라운드 16 F-16-030~038/042/043:
 * - aria-labelledby + 첫 포커스 (Modal 의 close 버튼 대신 details 탭)
 * - paper 토큰 (헤더 그라데이션 / red-50 / gray-50 → paper-aged + stamp-red)
 * - confirm 메시지 emoji 제거 + stamp-red 통일
 * - 동일 reportType 별 단일 헬퍼 (config.ts 통합)
 * - 차단/탈퇴 성공 시 옵티미스틱 정리 콜백 호출 (F-16-036 옵션 B)
 */
interface ReportDetailModalProps {
  report: Report;
  isOpen: boolean;
  onClose: () => void;
  /**
   * F-16-036: mutation 성공 시 호출. 같은 targetId+reportType 의 신고를 옵티미스틱 제거.
   * (이전 시그니처 `() => void` 가 refetch 였으나 잔존 회귀 → 시그니처 변경)
   */
  onAction: (targetId: number, reportType: string) => void;
  /**
   * F-16-037: 진행 중 상태를 부모에 전달 → 다른 카드 클릭 차단.
   */
  onProcessingChange?: (isProcessing: boolean) => void;
}

// 라운드 16 F-16-031: 모달 내부 신고 종류 → 아이콘 매핑 (color 는 config 헬퍼로 통합)
const TYPE_ICON_MAP = {
  POST: FileText,
  COMMENT: MessageSquare,
  ERROR: AlertTriangle,
  IMPROVEMENT: FileText,
} as const;

export const ReportDetailModal = memo(function ReportDetailModal({
  report,
  isOpen,
  onClose,
  onAction,
  onProcessingChange,
}: ReportDetailModalProps) {
  const [activeTab, setActiveTab] = useState("details");
  const { banUser, forceWithdrawUser, isProcessing } = useReportActions();
  const { confirm, ConfirmModalComponent } = useConfirmModal();

  // F-16-037: isProcessing 변화를 부모에 알림
  useEffect(() => {
    onProcessingChange?.(isProcessing);
  }, [isProcessing, onProcessingChange]);

  // 모달 닫힘 시 처리 상태 reset 보장
  useEffect(() => {
    if (!isOpen) {
      onProcessingChange?.(false);
    }
  }, [isOpen, onProcessingChange]);

  const typeLabel = getReportTypeLabel(report.reportType);
  const typeColor = getReportTypeBadgeColor(report.reportType);
  const TypeIcon =
    TYPE_ICON_MAP[report.reportType as keyof typeof TYPE_ICON_MAP] ?? FileText;

  // 사용자 차단 처리 (24시간 제한)
  const handleBanClick = async () => {
    const confirmed = await confirm({
      title: "이 사용자의 이용을 24시간 막을까요?",
      message:
        `'${report.targetAuthorName}'님에게 다음과 같은 조치가 적용돼요.\n\n` +
        `• 24시간 동안 서비스 이용 제한\n` +
        `• 모든 세션 및 토큰 무효화\n` +
        `• 차단 명단에 등록\n\n` +
        `이 작업은 되돌릴 수 없어요.`,
      confirmText: "이용 막기",
      cancelText: "취소",
      confirmButtonVariant: "destructive",
      icon: <Ban className="h-8 w-8 stroke-stamp-red" aria-hidden="true" />,
    });

    if (!confirmed) return;

    const success = await banUser(report);
    if (success && report.targetId) {
      // F-16-036: 동일 targetId+reportType 신고 옵티미스틱 정리
      onAction(report.targetId, report.reportType);
      onClose();
    }
  };

  // 사용자 강제 탈퇴 처리 (영구 삭제)
  const handleWithdrawClick = async () => {
    const confirmed = await confirm({
      title: "이 사용자를 영구히 탈퇴 처리할까요?",
      message:
        `'${report.targetAuthorName}'님이 작성한 모든 데이터가 영구적으로 삭제돼요.\n\n` +
        `삭제 예정 항목:\n` +
        `• 사용자 계정 정보\n` +
        `• 작성한 모든 게시글 및 댓글\n` +
        `• 받은 롤링페이퍼 메시지\n` +
        `• 알림 및 FCM 토큰\n` +
        `• 소셜 계정 연동\n` +
        `• 인증 토큰 및 세션\n\n` +
        `정리는 백그라운드에서 약 10초 정도 소요돼요.\n` +
        `이 작업은 되돌릴 수 없어요.`,
      confirmText: "탈퇴 처리",
      cancelText: "취소",
      confirmButtonVariant: "destructive",
      icon: <UserX className="h-8 w-8 stroke-stamp-red" aria-hidden="true" />,
    });

    if (!confirmed) return;

    const success = await forceWithdrawUser(report);
    if (success && report.targetId) {
      onAction(report.targetId, report.reportType);
      onClose();
    }
  };

  const titleId = `report-modal-title-${report.id}`;

  return (
    <Modal
      show={isOpen}
      onClose={onClose}
      size="2xl"
      dismissible={!isProcessing}
      // F-16-030: aria-labelledby — Flowbite Modal 은 root 에 prop pass-through
      // (react-aria 호환 안됨 → ModalHeader 의 span 에도 id 동시 부여)
    >
      <ModalHeader className="bg-paper-aged border-b border-postal-navy/15 dark:bg-postal-navy/15">
        <div className="flex items-start justify-between w-full">
          <div>
            <span
              id={titleId}
              className="text-xl font-bold font-display text-ink dark:text-foreground flex items-center gap-2 break-keep"
            >
              <AlertTriangle
                className="w-5 h-5 stroke-stamp-red"
                aria-hidden="true"
              />
              신고 상세 정보
            </span>
            <p className="mt-1 text-sm text-ink-soft dark:text-muted-foreground">
              신고 ID: #{report.id}
            </p>
          </div>
        </div>
      </ModalHeader>
      <ModalBody className="p-0" aria-labelledby={titleId}>
        {/* 탭 시스템 */}
        <Tabs value={activeTab} onValueChange={setActiveTab} className="flex-1">
          <TabsList className="w-full rounded-none border-b border-postal-navy/15 px-3 sm:px-6">
            <TabsTrigger value="details" className="flex-1 min-h-[44px]">
              상세 정보
            </TabsTrigger>
            <TabsTrigger value="content" className="flex-1 min-h-[44px]">
              신고 내용
            </TabsTrigger>
            <TabsTrigger value="actions" className="flex-1 min-h-[44px]">
              처리 작업
            </TabsTrigger>
          </TabsList>

          <ScrollArea className="h-[60vh] max-h-[480px]">
            {/* Details Tab */}
            <TabsContent value="details" className="px-3 sm:px-6 py-4 space-y-4 mt-0">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* 신고 유형 카드 */}
                <Card className="p-4 bg-paper-card border border-postal-navy/15">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-paper-aged flex items-center justify-center shrink-0">
                      <TypeIcon
                        className="w-5 h-5 stroke-postal-navy"
                        aria-hidden="true"
                      />
                    </div>
                    <div>
                      <p className="text-xs text-ink-soft dark:text-muted-foreground mb-1">
                        신고 유형
                      </p>
                      <Badge className={typeColor}>{typeLabel}</Badge>
                    </div>
                  </div>
                </Card>

                {/* 신고자 */}
                <Card className="p-4 bg-paper-card border border-postal-navy/15">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-paper-aged flex items-center justify-center shrink-0">
                      <User
                        className="w-5 h-5 stroke-postal-navy"
                        aria-hidden="true"
                      />
                    </div>
                    <div>
                      <p className="text-xs text-ink-soft dark:text-muted-foreground mb-1">
                        신고자
                      </p>
                      {report.reporterName ? (
                        <p className="font-semibold text-ink dark:text-foreground">
                          {report.reporterName}
                        </p>
                      ) : (
                        <p className="font-semibold italic text-ink-soft dark:text-muted-foreground">
                          익명
                        </p>
                      )}
                    </div>
                  </div>
                </Card>

                {/* 신고 대상 — F-16-043 paper-aged + stamp-red */}
                {(report.reportType === "POST" || report.reportType === "COMMENT") && (
                  <Card className="p-4 bg-stamp-red/10 border border-stamp-red/30 dark:bg-stamp-red/15">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-stamp-red/15 flex items-center justify-center shrink-0">
                        <User
                          className="w-5 h-5 stroke-stamp-red"
                          aria-hidden="true"
                        />
                      </div>
                      <div>
                        <p className="text-xs text-stamp-red mb-1 font-medium">
                          신고 대상
                        </p>
                        {report.targetAuthorName ? (
                          <>
                            <p className="font-bold text-ink dark:text-foreground">
                              {report.targetAuthorName}
                            </p>
                            <p className="text-xs text-stamp-red mt-1">
                              ID: {report.targetId}
                            </p>
                          </>
                        ) : (
                          <p className="font-bold italic text-stamp-red">삭제됨</p>
                        )}
                      </div>
                    </div>
                  </Card>
                )}

                {/* 신고일 카드 */}
                <Card className="p-4 bg-paper-card border border-postal-navy/15">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-paper-aged flex items-center justify-center shrink-0">
                      <Calendar
                        className="w-5 h-5 stroke-postal-navy"
                        aria-hidden="true"
                      />
                    </div>
                    <div>
                      <p className="text-xs text-ink-soft dark:text-muted-foreground mb-1">
                        신고일
                      </p>
                      <time
                        dateTime={report.createdAt}
                        className="font-semibold text-ink dark:text-foreground"
                      >
                        {new Date(report.createdAt).toLocaleDateString("ko-KR")}
                      </time>
                    </div>
                  </div>
                </Card>
              </div>

              {/* 신고 시간 상세 — F-16-042 bg-paper-soft */}
              <Card className="p-4 bg-paper-soft border border-postal-navy/15">
                <div className="flex items-start gap-3">
                  <Clock
                    className="w-5 h-5 stroke-postal-navy mt-0.5"
                    aria-hidden="true"
                  />
                  <div className="flex-1">
                    <p className="text-sm font-medium text-ink dark:text-foreground mb-1">
                      신고 시간
                    </p>
                    <time
                      dateTime={report.createdAt}
                      className="text-sm text-ink-soft dark:text-muted-foreground"
                    >
                      {formatKoreanDateTime(report.createdAt)}
                    </time>
                  </div>
                </div>
              </Card>
            </TabsContent>

            {/* 신고 내용 탭 */}
            <TabsContent value="content" className="px-3 sm:px-6 py-4 mt-0">
              <Card className="p-6 bg-paper-card border border-postal-navy/15">
                <div className="space-y-4">
                  <div>
                    <h3 className="text-sm font-semibold text-ink dark:text-foreground mb-2 flex items-center gap-2">
                      <FileText
                        className="w-4 h-4 stroke-postal-navy"
                        aria-hidden="true"
                      />
                      신고 사유
                    </h3>
                    <div className="bg-paper-soft rounded-lg border border-postal-navy/15 p-4">
                      {/*
                        F-16-033 (LOW): 백엔드 @NotBlank @Size(min=10,max=500) 검증으로
                        빈 사유는 발생하지 않으나 null 안전성을 위한 fallback 유지.
                      */}
                      <p className="text-ink dark:text-foreground whitespace-pre-wrap">
                        {report.content || "신고 사유가 제공되지 않았어요."}
                      </p>
                    </div>
                  </div>
                </div>
              </Card>
            </TabsContent>

            {/* 처리 작업 탭 */}
            <TabsContent value="actions" className="px-3 sm:px-6 py-4 space-y-4 mt-0">
              {/* 주의사항 알림 — paper-aged + stamp-red */}
              <div
                className="bg-paper-aged border border-stamp-red/30 dark:bg-stamp-red/15 rounded-lg p-4"
                role="note"
              >
                <div className="flex items-start gap-3">
                  <AlertTriangle
                    className="w-5 h-5 stroke-stamp-red mt-0.5"
                    aria-hidden="true"
                  />
                  <div>
                    <p className="text-sm font-medium text-ink dark:text-foreground">
                      잠깐, 한 번만 더 살펴봐 주세요
                    </p>
                    <p className="text-sm text-ink-soft dark:text-muted-foreground mt-1 break-keep">
                      아래 작업은 되돌릴 수 없어요. 신중하게 결정해 주세요.
                    </p>
                  </div>
                </div>
              </div>

              {(report.reportType === "POST" || report.reportType === "COMMENT") && (
                <div className="space-y-3">
                  {/* 사용자 차단 액션 */}
                  <Card className="p-4 bg-paper-card border border-postal-navy/15 hover:shadow-brand-md transition-shadow">
                    <div className="flex flex-col sm:flex-row sm:items-center gap-3 sm:justify-between">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-stamp-red/10 flex items-center justify-center flex-shrink-0">
                          <Ban
                            className="w-5 h-5 stroke-stamp-red"
                            aria-hidden="true"
                          />
                        </div>
                        <div className="min-w-0">
                          <p className="font-medium text-ink dark:text-foreground">
                            이용 막기 (24시간)
                          </p>
                          <p className="text-sm text-ink-soft dark:text-muted-foreground break-keep">
                            24시간 동안 서비스 이용을 제한해요.
                          </p>
                        </div>
                      </div>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={handleBanClick}
                        disabled={isProcessing || !report.targetAuthorName}
                        className="text-stamp-red border-stamp-red/30 hover:bg-stamp-red/10 disabled:opacity-50 disabled:cursor-not-allowed flex-shrink-0 w-full sm:w-auto min-h-[44px]"
                      >
                        이용 막기
                        <ChevronRight
                          className="w-4 h-4 ml-1 stroke-stamp-red"
                          aria-hidden="true"
                        />
                      </Button>
                    </div>
                  </Card>

                  {/* 강제탈퇴 액션 */}
                  <Card className="p-4 bg-paper-card border border-postal-navy/15 hover:shadow-brand-md transition-shadow">
                    <div className="flex flex-col sm:flex-row sm:items-center gap-3 sm:justify-between">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-stamp-red/15 flex items-center justify-center flex-shrink-0">
                          <UserX
                            className="w-5 h-5 stroke-stamp-red"
                            aria-hidden="true"
                          />
                        </div>
                        <div className="min-w-0">
                          <p className="font-medium text-ink dark:text-foreground">
                            영구 탈퇴 처리
                          </p>
                          <p className="text-sm text-ink-soft dark:text-muted-foreground break-keep">
                            사용자 계정을 영구적으로 정리해요.
                          </p>
                        </div>
                      </div>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={handleWithdrawClick}
                        disabled={isProcessing || !report.targetAuthorName}
                        className="text-stamp-red border-stamp-red/30 hover:bg-stamp-red/10 disabled:opacity-50 disabled:cursor-not-allowed flex-shrink-0 w-full sm:w-auto min-h-[44px]"
                      >
                        탈퇴 처리
                        <ChevronRight
                          className="w-4 h-4 ml-1 stroke-stamp-red"
                          aria-hidden="true"
                        />
                      </Button>
                    </div>
                  </Card>
                </div>
              )}

              {/* 제재 불가 알림 */}
              {!report.targetAuthorName && (report.reportType === "POST" || report.reportType === "COMMENT") && (
                <Alert variant="warning">
                  <AlertTitle>제재할 수 없어요</AlertTitle>
                  <AlertDescription>
                    신고 대상 {report.reportType === "POST" ? "게시글" : "댓글"}이 삭제되었거나
                    익명 사용자라 제재가 불가능해요.
                  </AlertDescription>
                </Alert>
              )}
              {(report.reportType === "ERROR" || report.reportType === "IMPROVEMENT") && (
                <Alert variant="info">
                  <AlertTitle>안내</AlertTitle>
                  <AlertDescription>
                    오류 신고와 개선 제안은 사용자 제재 대상이 아니에요.
                  </AlertDescription>
                </Alert>
              )}
            </TabsContent>
          </ScrollArea>
        </Tabs>
      </ModalBody>

      {/* 확인 다이얼로그 */}
      <ConfirmModalComponent />
    </Modal>
  );
});

ReportDetailModal.displayName = "ReportDetailModal";
