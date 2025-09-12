import React from "react";

export const LoadingState: React.FC = () => {
  return (
    <div className="flex flex-col items-center justify-center py-12">
      <div className="animate-spin rounded-full h-10 w-10 border-2 border-purple-600 border-t-transparent mb-4"></div>
      <p className="text-gray-600 font-medium">신고 목록을 불러오는 중...</p>
      <p className="text-sm text-gray-500 mt-1">잠시만 기다려주세요</p>
    </div>
  );
};

export const LoadingSkeleton: React.FC = () => {
  return (
    <div className="space-y-4">
      {[...Array(3)].map((_, index) => (
        <div
          key={index}
          className="p-6 bg-gradient-to-br from-white to-gray-50 rounded-xl border border-gray-200 animate-pulse"
        >
          <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
            <div className="flex-1">
              <div className="flex gap-2 mb-3">
                <div className="h-5 w-16 bg-gray-200 rounded" />
                <div className="h-5 w-20 bg-gray-200 rounded" />
              </div>
              <div className="h-6 w-3/4 bg-gray-200 rounded mb-2" />
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mb-3">
                <div className="h-4 w-32 bg-gray-200 rounded" />
                <div className="h-4 w-24 bg-gray-200 rounded" />
              </div>
              <div className="h-4 w-full bg-gray-200 rounded mb-3" />
              <div className="h-3 w-28 bg-gray-200 rounded" />
            </div>
            <div className="flex-shrink-0">
              <div className="h-12 w-24 bg-gray-200 rounded" />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};