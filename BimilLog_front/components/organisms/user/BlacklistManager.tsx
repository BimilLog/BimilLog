"use client";

import React, { useState } from "react";
import { UserX, Trash2 } from "lucide-react";
import { Button, Spinner } from "@/components";
import { useBlacklist } from "@/hooks/api/useBlacklistQueries";
import { useRemoveFromBlacklistAction } from "@/hooks/actions/useBlacklistActions";
import { BlacklistDTO } from "@/types/domains/blacklist";
import { formatDate } from "@/lib/utils";

interface BlacklistManagerProps {
  className?: string;
}

export const BlacklistManager: React.FC<BlacklistManagerProps> = ({ className }) => {
  const [page, setPage] = useState(0);
  const size = 20;

  // 쿼리 및 뮤테이션 hooks
  const { data: blacklistResponse, isLoading, error } = useBlacklist(page, size);
  const { removeFromBlacklist, isPending: isRemoving } = useRemoveFromBlacklistAction();

  // 블랙리스트 데이터
  const blacklistData = blacklistResponse?.data;
  const blacklistItems = blacklistData?.content || [];
  const totalPages = blacklistData?.totalPages || 0;
  const isEmpty = blacklistData?.empty ?? true;

  // 사용자 삭제 핸들러
  const handleRemoveUser = (id: number, userName: string) => {
    if (!confirm(`'${userName}' 님을 블랙리스트에서 삭제하시겠습니까?`)) {
      return;
    }

    removeFromBlacklist(id);
  };

  // 로딩 상태
  if (isLoading) {
    return (
      <div className={`flex justify-center items-center min-h-[400px] ${className || ""}`}>
        <Spinner message="블랙리스트를 불러오는 중..." />
      </div>
    );
  }

  // 에러 상태
  if (error) {
    return (
      <div className={`p-8 text-center ${className || ""}`}>
        <UserX className="w-16 h-16 mx-auto mb-4 text-gray-400" />
        <p className="text-gray-600">블랙리스트를 불러올 수 없습니다.</p>
        <p className="text-sm text-gray-500 mt-2">{error.message}</p>
      </div>
    );
  }

  return (
    <div className={className}>
      {/* 헤더 */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <UserX className="w-7 h-7" />
          블랙리스트 관리
        </h1>
      </div>

      {/* 블랙리스트 목록 */}
      {isEmpty ? (
        <div className="text-center py-16 bg-gray-50 rounded-lg">
          <UserX className="w-16 h-16 mx-auto mb-4 text-gray-400" />
          <p className="text-gray-600 font-medium">블랙리스트가 비어있습니다.</p>
        </div>
      ) : (
        <>
          <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
            <ul className="divide-y divide-gray-200">
              {blacklistItems.map((item: BlacklistDTO) => (
                <li
                  key={item.id}
                  className="flex items-center justify-between p-4 hover:bg-gray-50 transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <UserX className="w-5 h-5 text-gray-400" />
                    <div>
                      <p className="font-medium text-gray-900">{item.memberName}</p>
                      {item.createdAt && (
                        <p className="text-xs text-gray-500 mt-1">
                          {formatDate(item.createdAt)} 차단
                        </p>
                      )}
                    </div>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleRemoveUser(item.id!, item.memberName)}
                    disabled={isRemoving}
                    className="text-red-600 hover:text-red-700 hover:bg-red-50"
                  >
                    <Trash2 className="w-4 h-4 mr-1" />
                    삭제
                  </Button>
                </li>
              ))}
            </ul>
          </div>

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 mt-6">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                이전
              </Button>
              <span className="text-sm text-gray-600">
                {page + 1} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
              >
                다음
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
};
