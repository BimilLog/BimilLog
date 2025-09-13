"use client";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  Card,
  Badge,
  Button,
  ScrollArea,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger
} from "@/components";
import { 
  X, 
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

interface ReportDetailModalProps {
  report: Report;
  isOpen: boolean;
  onClose: () => void;
  onAction: () => void;
}

export function ReportDetailModal({
  report,
  isOpen,
  onClose,
  onAction
}: ReportDetailModalProps) {
  const [activeTab, setActiveTab] = useState("details");
  const { banUser, forceWithdrawUser, isProcessing } = useReportActions();

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
        return { label: type, color: "bg-gray-100 text-gray-700", icon: FileText };
    }
  };

  const typeInfo = getReportTypeInfo(report.reportType);
  const TypeIcon = typeInfo.icon;

  const handleBanClick = async () => {
    await banUser(report);
    onAction();
    onClose();
  };

  const handleWithdrawClick = async () => {
    await forceWithdrawUser(report);
    onAction();
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] p-0">
        {/* Header */}
        <div className="px-6 py-4 border-b bg-gradient-to-r from-purple-50 to-pink-50">
          <DialogHeader>
            <div className="flex items-start justify-between">
              <div>
                <DialogTitle className="text-xl font-bold text-gray-900 flex items-center gap-2">
                  <AlertTriangle className="w-5 h-5 text-red-500" />
                  신고 상세 정보
                </DialogTitle>
                <DialogDescription className="mt-1 text-sm text-gray-600">
                  신고 ID: #{report.id}
                </DialogDescription>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={onClose}
                className="h-8 w-8 p-0"
              >
                <X className="h-4 w-4" />
              </Button>
            </div>
          </DialogHeader>
        </div>

        {/* Tabs */}
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
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* 신고 유형 */}
                <Card className="p-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center">
                      <TypeIcon className="w-5 h-5 text-gray-600" />
                    </div>
                    <div>
                      <p className="text-xs text-gray-500 mb-1">신고 유형</p>
                      <Badge className={typeInfo.color}>
                        {typeInfo.label}
                      </Badge>
                    </div>
                  </div>
                </Card>

                {/* 대상 ID */}
                <Card className="p-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center">
                      <Hash className="w-5 h-5 text-gray-600" />
                    </div>
                    <div>
                      <p className="text-xs text-gray-500 mb-1">대상 ID</p>
                      <p className="font-semibold text-gray-900">{report.targetId}</p>
                    </div>
                  </div>
                </Card>

                {/* 신고 대상 */}
                <Card className="p-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center">
                      <User className="w-5 h-5 text-gray-600" />
                    </div>
                    <div>
                      <p className="text-xs text-gray-500 mb-1">신고 대상</p>
                      <p className="font-semibold text-gray-900">
                        {report.reporterName || "익명"}
                      </p>
                    </div>
                  </div>
                </Card>

                {/* 신고일 */}
                <Card className="p-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center">
                      <Calendar className="w-5 h-5 text-gray-600" />
                    </div>
                    <div>
                      <p className="text-xs text-gray-500 mb-1">신고일</p>
                      <p className="font-semibold text-gray-900">
                        {new Date(report.createdAt).toLocaleDateString('ko-KR')}
                      </p>
                    </div>
                  </div>
                </Card>
              </div>

              {/* 추가 정보 */}
              <Card className="p-4 bg-gray-50">
                <div className="flex items-start gap-3">
                  <Clock className="w-5 h-5 text-gray-400 mt-0.5" />
                  <div className="flex-1">
                    <p className="text-sm font-medium text-gray-700 mb-1">신고 시간</p>
                    <p className="text-sm text-gray-600">
                      {new Date(report.createdAt).toLocaleString('ko-KR')}
                    </p>
                  </div>
                </div>
              </Card>
            </TabsContent>

            {/* Content Tab */}
            <TabsContent value="content" className="px-6 py-4 mt-0">
              <Card className="p-6">
                <div className="space-y-4">
                  <div>
                    <h3 className="text-sm font-semibold text-gray-700 mb-2 flex items-center gap-2">
                      <FileText className="w-4 h-4" />
                      신고 사유
                    </h3>
                    <div className="bg-white rounded-lg border border-gray-200 p-4">
                      <p className="text-gray-800 whitespace-pre-wrap">
                        {report.content || "신고 사유가 제공되지 않았습니다."}
                      </p>
                    </div>
                  </div>

                  {report.targetTitle && (
                    <div>
                      <h3 className="text-sm font-semibold text-gray-700 mb-2 flex items-center gap-2">
                        <MessageSquare className="w-4 h-4" />
                        신고된 콘텐츠
                      </h3>
                      <div className="bg-gray-50 rounded-lg border border-gray-200 p-4">
                        <p className="text-gray-700 whitespace-pre-wrap">
                          {report.targetTitle}
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              </Card>
            </TabsContent>

            {/* Actions Tab */}
            <TabsContent value="actions" className="px-6 py-4 space-y-4 mt-0">
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                <div className="flex items-start gap-3">
                  <AlertTriangle className="w-5 h-5 text-yellow-600 mt-0.5" />
                  <div>
                    <p className="text-sm font-medium text-yellow-900">주의사항</p>
                    <p className="text-sm text-yellow-700 mt-1">
                      아래 작업은 되돌릴 수 없습니다. 신중하게 결정해주세요.
                    </p>
                  </div>
                </div>
              </div>

              <div className="space-y-3">
                {/* 차단 버튼 */}
                <Card className="p-4 hover:shadow-md transition-shadow">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-orange-100 flex items-center justify-center">
                        <Ban className="w-5 h-5 text-orange-600" />
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">사용자 차단</p>
                        <p className="text-sm text-gray-500">24시간 동안 서비스 이용 제한</p>
                      </div>
                    </div>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleBanClick}
                      disabled={isProcessing || !report.reporterName}
                      className="text-orange-600 border-orange-200 hover:bg-orange-50"
                    >
                      차단
                      <ChevronRight className="w-4 h-4 ml-1" />
                    </Button>
                  </div>
                </Card>

                {/* 강제 탈퇴 버튼 */}
                <Card className="p-4 hover:shadow-md transition-shadow">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-red-100 flex items-center justify-center">
                        <UserX className="w-5 h-5 text-red-600" />
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">강제 탈퇴</p>
                        <p className="text-sm text-gray-500">사용자 계정 영구 삭제</p>
                      </div>
                    </div>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleWithdrawClick}
                      disabled={isProcessing || !report.reporterName}
                      className="text-red-600 border-red-200 hover:bg-red-50"
                    >
                      탈퇴
                      <ChevronRight className="w-4 h-4 ml-1" />
                    </Button>
                  </div>
                </Card>
              </div>

              {!report.reporterName && (
                <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
                  <p className="text-sm text-gray-600 text-center">
                    익명 사용자는 차단 또는 강제 탈퇴할 수 없습니다.
                  </p>
                </div>
              )}
            </TabsContent>
          </ScrollArea>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}