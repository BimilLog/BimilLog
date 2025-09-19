"use client";

import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components';
import { Button, Badge } from '@/components';
import {
  Star,
  Clock,
  AlertCircle,
  Users,
  Gift,
  Folder,
  FileText,
  Heart,
  MessageCircle,
  Search,
  Trash2,
  ExternalLink,
  Bookmark as BookmarkIcon,
} from 'lucide-react';
import Link from 'next/link';
import { useBookmark } from '@/hooks/features/useBookmark';
import {
  type BookmarkCategory,
  CATEGORY_LABELS,
} from '@/lib/utils/bookmark';

const CATEGORY_ICONS = {
  'favorite': Star,
  'read-later': Clock,
  'important': AlertCircle,
  'friends': Users,
  'birthday': Gift,
  'other': Folder,
};

const TYPE_ICONS = {
  'post': FileText,
  'rolling-paper': (props: { className?: string }) => <Heart {...props} className={`stroke-red-500 fill-red-100 ${props.className || ''}`} />,
  'comment': MessageCircle,
};

export function BookmarkSection() {
  const [selectedCategory, setSelectedCategory] = useState<BookmarkCategory | undefined>(undefined);
  const [selectedType, setSelectedType] = useState<'post' | 'rolling-paper' | 'comment' | undefined>(undefined);

  const {
    bookmarks,
    stats,
    searchQuery,
    setSearchQuery,
    removeBookmarkItem,
    clearAllBookmarks,
    isLoading,
  } = useBookmark({
    category: selectedCategory,
    type: selectedType,
    sortBy: 'createdAt',
    sortOrder: 'desc',
  });

  if (isLoading) {
    return (
      <Card variant="elevated" className="mb-8">
        <CardContent className="p-6">
          <div className="flex items-center justify-center py-8">
            <div className="animate-pulse text-brand-muted">북마크를 불러오는 중...</div>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6 mb-8">
      {/* 헤더 */}
      <Card variant="elevated">
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <BookmarkIcon className="w-5 h-5 stroke-slate-600 fill-slate-100" />
              내 북마크
              <Badge variant="secondary">{stats.total}</Badge>
            </CardTitle>
            {stats.total > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={clearAllBookmarks}
                className="text-red-600 hover:bg-red-50"
              >
                <Trash2 className="w-4 h-4 mr-1 stroke-red-600 fill-red-100" />
                모두 삭제
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent className="p-6">
          {/* 통계 */}
          <div className="grid grid-cols-3 md:grid-cols-6 gap-2 mb-4">
            {Object.entries(CATEGORY_LABELS).map(([key, label]) => {
              const Icon = CATEGORY_ICONS[key as BookmarkCategory];
              const count = stats.byCategory[key as BookmarkCategory];
              const isSelected = selectedCategory === key;

              return (
                <button
                  key={key}
                  onClick={() => setSelectedCategory(isSelected ? undefined : key as BookmarkCategory)}
                  className={`
                    flex flex-col items-center p-3 rounded-lg transition-all
                    ${isSelected
                      ? 'bg-gradient-to-r from-pink-500 to-purple-600 text-white'
                      : 'bg-gray-50 hover:bg-gray-100 text-gray-700'
                    }
                  `}
                >
                  <Icon className="w-4 h-4 mb-1" />
                  <span className="text-xs font-medium">{label}</span>
                  <span className="text-xs opacity-75">{count}</span>
                </button>
              );
            })}
          </div>

          {/* 타입 필터 */}
          <div className="flex gap-2 mb-4">
            <button
              onClick={() => setSelectedType(undefined)}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                !selectedType
                  ? 'bg-brand-primary text-white'
                  : 'bg-gray-100 hover:bg-gray-200 text-gray-700'
              }`}
            >
              전체 ({stats.total})
            </button>
            <button
              onClick={() => setSelectedType('post')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                selectedType === 'post'
                  ? 'bg-brand-primary text-white'
                  : 'bg-gray-100 hover:bg-gray-200 text-gray-700'
              }`}
            >
              <FileText className="w-3.5 h-3.5 inline mr-1" />
              게시글 ({stats.byType.post})
            </button>
            <button
              onClick={() => setSelectedType('rolling-paper')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                selectedType === 'rolling-paper'
                  ? 'bg-brand-primary text-white'
                  : 'bg-gray-100 hover:bg-gray-200 text-gray-700'
              }`}
            >
              <Heart className="w-3.5 h-3.5 inline mr-1 stroke-red-500 fill-red-100" />
              롤링페이퍼 ({stats.byType['rolling-paper']})
            </button>
          </div>

          {/* 검색 */}
          <div className="relative mb-4">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 stroke-slate-600 fill-slate-100" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="북마크 검색..."
              className="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-primary"
            />
          </div>

          {/* 북마크 목록 */}
          {bookmarks.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              <BookmarkIcon className="w-12 h-12 mx-auto mb-3 opacity-30 stroke-slate-600 fill-slate-100" />
              <p>북마크가 없습니다</p>
            </div>
          ) : (
            <div className="space-y-2 max-h-96 overflow-y-auto">
              {bookmarks.map((bookmark) => {
                const TypeIcon = TYPE_ICONS[bookmark.type];
                const CategoryIcon = CATEGORY_ICONS[bookmark.category];

                return (
                  <div
                    key={bookmark.id}
                    className="flex items-center justify-between p-3 bg-gray-50 hover:bg-gray-100 rounded-lg transition-colors"
                  >
                    <div className="flex items-center gap-3 flex-1 min-w-0">
                      <div className="flex-shrink-0">
                        <TypeIcon className="w-5 h-5 stroke-slate-600 fill-slate-100" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <Link
                          href={bookmark.url}
                          className="font-medium text-brand-primary hover:underline truncate block"
                        >
                          {bookmark.title}
                        </Link>
                        <div className="flex items-center gap-2 text-xs text-gray-500 mt-1">
                          <CategoryIcon className="w-3 h-3 stroke-slate-600 fill-slate-100" />
                          <span>{CATEGORY_LABELS[bookmark.category]}</span>
                          {bookmark.visitCount > 0 && (
                            <span>• 방문 {bookmark.visitCount}회</span>
                          )}
                        </div>
                        {bookmark.memo && (
                          <p className="text-xs text-gray-600 mt-1 truncate">{bookmark.memo}</p>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-2 flex-shrink-0">
                      <Link href={bookmark.url}>
                        <Button variant="ghost" size="sm">
                          <ExternalLink className="w-4 h-4 stroke-blue-600 fill-blue-100" />
                        </Button>
                      </Link>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => removeBookmarkItem(bookmark.id)}
                        className="text-red-600 hover:bg-red-50"
                      >
                        <Trash2 className="w-4 h-4 stroke-red-600 fill-red-100" />
                      </Button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}