"use client";

import { useState } from "react";
import dynamic from "next/dynamic";
import { Modal, ModalHeader, ModalBody } from "flowbite-react";
import {
  Card,
  Badge,
  Button,
  ScrollArea,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger
} from "@/components";
import { Spinner as FlowbiteSpinner } from "flowbite-react";
import {
  User,
  Calendar,
  FileText,
  AlertTriangle,
  Ban,
  UserX,
  MessageSquare,
  Hash,
  Clock,
  ChevronRight
} from "lucide-react";
import { useReportActions } from "@/hooks/features/admin";
import type { Report } from "@/types/domains/admin";

/**
 * 신고 상세 정보를 보여주는 관리자용 모달 컴포넌트
 * - 신고 타입별 동적 아이콘과 색상 매핑
 * - 3개 탭으로 정보 구성: 상세정보, 신고내용, 처리작업
 * - 사용자 차단/강제탈퇴 액션 제공
 */

interface ReportDetailModalProps {
  report: Report;
  isOpen: boolean;
  onClose: () => void;
  onAction: () => void;
}

// 로딩 컴포넌트
const ReportDetailModalLoading = () => (
  <Modal show onClose={() => {}} size="2xl">
    <ModalBody>
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="flex flex-col items-center gap-3">
          <FlowbiteSpinner color="pink" size="xl" aria-label="신고 상세 정보 로딩 중..." />
          <p className="text-sm text-brand-secondary">신고 상세 정보 로딩 중...</p>
        </div>
      </div>
    </ModalBody>
  </Modal>
);

// 실제 모달 컴포넌트
function ReportDetailModalContent({
  report,
  isOpen,
  onClose,
  onAction
}: ReportDetailModalProps) {
  const [activeTab, setActiveTab] = useState("details");
  const { banUser, forceWithdrawUser, isProcessing } = useReportActions();

  // 신고 타입별 UI 스타일과 아이콘 매핑
  // 각 신고 유형에 맞는 시각적 표현을 동적으로 생성
  const getReportTypeInfo = (type: string) => {
    switch(type) {
      case "POST":
        return { label: "게시글", color: "bg-yellow-100 text-yellow-700", icon: FileText };
      case "COMMENT":
        return { label: "댓글", color: "bg-green-100 text-green-700", icon: MessageSquare };
      case "ERROR":
        return { label: "오류", color: "bg-red-100 text-red-700", icon: AlertTriangle };
      case "IMPROVEMENT":
        return { label: "개선", color: "bg-blue-100 text-blue-700", icon: FileText };
      default:
        // 알 수 없는 타입의 경우 기본 스타일 적용
        return { label: type, color: "bg-gray-100 text-brand-primary", icon: FileText };
    }
  };

  // 선택된 신고 타입의 정보 추출
  const typeInfo = getReportTypeInfo(report.reportType);
  // 동적 아이콘 컴포넌트 생성 (Lucide React 아이콘을 컴포넌트로 사용)
  const TypeIcon = typeInfo.icon;

  // 사용자 차단 처리 (24시간 제한)
  const handleBanClick = async () => {
    await banUser(report);
    onAction(); // 부모 컴포넌트에 처리 완료 알림
    onClose();
  };

  // 사용자 강제 탈퇴 처리 (영구 삭제)
  const handleWithdrawClick = async () => {
    await forceWithdrawUser(report);
    onAction(); // 부모 컴포넌트에 처리 완료 알림
    onClose();
  };

  return (
    <Modal show={isOpen} onClose={onClose} size="2xl">
      <ModalHeader className="bg-gradient-to-r from-purple-50 to-pink-50">
        <div className="flex items-start justify-between w-full">
          <div>
            <span className="text-xl font-bold text-brand-primary flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 stroke-amber-600 fill-amber-100" />
              신고 상세 정보
            </span>
            <p className="mt-1 text-sm text-brand-muted">
              신고 ID: #{report.id}
            </p>
          </div>
        </div>
      </ModalHeader>
      <ModalBody className="p-0">

        {/* 탭 시스템: 상세정보, 신고내용, 처리작업으로 구성 */}
        <Tabs value={activeTab} onValueChange={setActiveTab} className="flex-1">
          <TabsList className="w-full rounded-none border-b px-6">
            <TabsTrigger value="details" className="flex-1">
              상세 정보
            </TabsTrigger>
            <TabsTrigger value="content" className="flex-1">
              신고 내용
            </TabsTrigger>
            <TabsTrigger value="actions" className="flex-1">
              처리 작업
            </TabsTrigger>
          </TabsList>

          <ScrollArea className="h-[400px]">
            {/* Details Tab */}
            <TabsContent value="details" className="px-6 py-4 space-y-4 mt-0">
              {/* 정보 카드들을 2x2 그리드로 배치 (모바일에서는 1열) */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* 신고 유형 카드 */}
                <Card className="p-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center">
                      <TypeIcon className="w-5 h-5 stroke-slate-600 fill-slate-100" />
                    </div>
                    <div>
                      <p className="text-xs text-brand-secondary mb-1">신고 유형</p>
                      <Badge className={typeInfo.color}>
                        {typeInfo.label}
                      </Badge>
                    </div>
                  </div>
                </Card>

                {/* 신고 대상 ID (게시글/댓글 ID) */}
                <Card className="p-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center">
                      <Hash className="w-5 h-5 stroke-slate-600" />
                    </div>
                    <div>
                      <p className="text-xs text-brand-secondary mb-1">대상 ID</p>
                      <p className="font-semibold text-brand-primary">{report.targetId}</p>
                    </div>
                  </div>
                </Card>

                {/* 신고를 당한 사용자 */}
                <Card className="p-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center">
                      <User className="w-5 h-5 stroke-slate-600 fill-slate-100" />
                    </div>
                    <div>
                      <p className="text-xs text-brand-secondary mb-1">신고 대상</p>
                      <p className="font-semibold text-brand-primary">
                        {report.reporterName || "익명"}
                      </p>
                    </div>
                  </div>
                </Card>

                {/* 신고 접수일 */}
                <Card className="p-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center">
                      <Calendar className="w-5 h-5 stroke-indigo-600 fill-indigo-100" />
                    </div>
                    <div>
                      <p className="text-xs text-brand-secondary mb-1">신고일</p>
                      <p className="font-semibold text-brand-primary">
                        {new Date(report.createdAt).toLocaleDateString('ko-KR')}
                      </p>
                    </div>
                  </div>
                </Card>
              </div>

              {/* 신고 시간 상세 정보 */}
              <Card className="p-4 bg-gray-50">
                <div className="flex items-start gap-3">
                  <Clock className="w-5 h-5 stroke-indigo-600 fill-indigo-100 mt-0.5" />
                  <div className="flex-1">
                    <p className="text-sm font-medium text-brand-primary mb-1">신고 시간</p>
                    <p className="text-sm text-brand-muted">
                      {new Date(report.createdAt).toLocaleString('ko-KR')}
                    </p>
                  </div>
                </div>
              </Card>
            </TabsContent>

            {/* 신고 내용 탭 - 신고 사유와 대상 콘텐츠 표시 */}
            <TabsContent value="content" className="px-6 py-4 mt-0">
              <Card className="p-6">
                <div className="space-y-4">
                  {/* 신고자가 입력한 신고 사유 */}
                  <div>
                    <h3 className="text-sm font-semibold text-brand-primary mb-2 flex items-center gap-2">
                      <FileText className="w-4 h-4 stroke-blue-600 fill-blue-100" />
                      신고 사유
                    </h3>
                    <div className="bg-white rounded-lg border border-gray-200 p-4">
                      <p className="text-brand-primary whitespace-pre-wrap">
                        {report.content || "신고 사유가 제공되지 않았습니다."}
                      </p>
                    </div>
                  </div>

                  {/* 신고된 콘텐츠가 있는 경우에만 표시 (조건부 렌더링) */}
                  {report.targetTitle && (
                    <div>
                      <h3 className="text-sm font-semibold text-brand-primary mb-2 flex items-center gap-2">
                        <MessageSquare className="w-4 h-4 stroke-slate-600 fill-slate-100" />
                        신고된 콘텐츠
                      </h3>
                      {/* 신고 대상이 된 게시글/댓글의 실제 내용 */}
                      <div className="bg-gray-50 rounded-lg border border-gray-200 p-4">
                        <p className="text-brand-primary whitespace-pre-wrap">
                          {report.targetTitle}
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              </Card>
            </TabsContent>

            {/* 관리자 처리 작업 탭 - 차단/탈퇴 등 관리자 액션 */}
            <TabsContent value="actions" className="px-6 py-4 space-y-4 mt-0">
              {/* 주의사항 알림 배너 */}
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                <div className="flex items-start gap-3">
                  <AlertTriangle className="w-5 h-5 stroke-amber-600 fill-amber-100 mt-0.5" />
                  <div>
                    <p className="text-sm font-medium text-yellow-900">주의사항</p>
                    <p className="text-sm text-yellow-700 mt-1">
                      아래 작업은 되돌릴 수 없습니다. 신중하게 결정해주세요.
                    </p>
                  </div>
                </div>
              </div>

              {/* 관리자 액션 버튼들 */}
              <div className="space-y-3">
                {/* 사용자 차단 액션 */}
                <Card className="p-4 hover:shadow-brand-md transition-shadow">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-orange-100 flex items-center justify-center">
                        <Ban className="w-5 h-5 stroke-orange-600" />
                      </div>
                      <div>
                        <p className="font-medium text-brand-primary">사용자 차단</p>
                        <p className="text-sm text-brand-secondary">24시간 동안 서비스 이용 제한</p>
                      </div>
                    </div>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleBanClick}
                      disabled={isProcessing || !report.reporterName} // 익명 사용자는 차단 불가
                      className="text-orange-600 border-orange-200 hover:bg-orange-50"
                    >
                      차단
                      <ChevronRight className="w-4 h-4 ml-1 stroke-slate-600" />
                    </Button>
                  </div>
                </Card>

                {/* 사용자 강제탈퇴 액션 */}
                <Card className="p-4 hover:shadow-brand-md transition-shadow">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-red-100 flex items-center justify-center">
                        <UserX className="w-5 h-5 stroke-red-600" />
                      </div>
                      <div>
                        <p className="font-medium text-brand-primary">강제 탈퇴</p>
                        <p className="text-sm text-brand-secondary">사용자 계정 영구 삭제</p>
                      </div>
                    </div>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleWithdrawClick}
                      disabled={isProcessing || !report.reporterName} // 익명 사용자는 탈퇴 처리 불가
                      className="text-red-600 border-red-200 hover:bg-red-50"
                    >
                      탈퇴
                      <ChevronRight className="w-4 h-4 ml-1 stroke-slate-600" />
                    </Button>
                  </div>
                </Card>
              </div>

              {/* 익명 사용자일 경우 액션 불가 알림 (조건부 렌더링) */}
              {!report.reporterName && (
                <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
                  <p className="text-sm text-brand-muted text-center">
                    익명 사용자는 차단 또는 강제 탈퇴할 수 없습니다.
                  </p>
                </div>
              )}
            </TabsContent>
          </ScrollArea>
        </Tabs>
      </ModalBody>
    </Modal>
  );
}

// Dynamic import로 컴포넌트 래핑 (번들 크기 최적화)
// SSR 비활성화로 클라이언트 전용 렌더링, 로딩 상태 제공
const ReportDetailModal = dynamic(
  () => Promise.resolve(ReportDetailModalContent),
  {
    ssr: false,
    loading: () => <ReportDetailModalLoading />,
  }
);

export { ReportDetailModal };