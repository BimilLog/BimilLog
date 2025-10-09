"use client";

import React from "react";
import { Card, CardContent, CardHeader, CardTitle, Input, Label, SafeHTML } from "@/components";
import { Save, Lightbulb } from "lucide-react";
import { LazyEditor } from "@/lib/utils/lazy-components";
import type { Member } from "@/types/domains/user";

interface WriteFormProps {
  // Form states - 폼의 상태 관리를 위한 props
  title: string;
  setTitle: (value: string) => void;
  content: string;
  setContent: (value: string) => void;
  password: string;
  setPassword: (value: string) => void;

  // User info - 사용자 정보 및 인증 상태
  user: Member | null;
  isAuthenticated: boolean;

  // Preview - 미리보기 모드 여부
  isPreview: boolean;

  // Content length - 순수 텍스트 길이 (HTML 태그 제외)
  plainTextLength: number;
}

/**
 * 게시글 작성 폼 컴포넌트
 *
 * 주요 기능:
 * - 편집 모드와 미리보기 모드 토글 지원
 * - 사용자 인증 상태에 따른 조건부 UI 렌더링
 *   - 로그인 사용자: 사용자명 표시, 비밀번호 입력 불필요
 *   - 비로그인 사용자: 익명 게시, 비밀번호 입력 필수
 * - 리치텍스트 에디터(LazyEditor)로 풍부한 컨텐츠 작성 지원
 */
export const WriteForm: React.FC<WriteFormProps> = ({
  title,
  setTitle,
  content,
  setContent,
  password,
  setPassword,
  user,
  isAuthenticated,
  isPreview,
  plainTextLength,
}) => {
  // HTML 컨텐츠를 안전하게 렌더링하는 함수
  const formatPreviewContent = (htmlContent: string) => {
    return <SafeHTML html={htmlContent} />;
  };

  return (
    <Card variant="elevated">
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <Save className="w-5 h-5 stroke-green-600 fill-green-100" />
          <span>게시글 작성</span>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* 메인 UI 렌더링 조건부 로직: 미리보기 모드 vs 편집 모드 */}
        {!isPreview ? (
          <>
            {/* 제목 입력 필드 */}
            <div className="space-y-2">
              <Label
                htmlFor="title"
                className="text-sm font-medium text-brand-primary"
              >
                제목
              </Label>
              <Input
                id="title"
                placeholder="제목을 입력하세요 (2~30자)"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                maxLength={30}
                className="text-lg font-medium border-2 border-gray-200 focus:border-purple-400"
              />
              {title.length > 0 && (
                <p className={`text-xs ${title.length >= 30 ? 'text-red-600' : 'text-brand-muted'}`}>
                  {title.length}/30자
                </p>
              )}
            </div>

            {/* 내용 입력 필드 - LazyEditor로 지연 로딩 */}
            <div className="space-y-2">
              <Label
                htmlFor="content"
                className="text-sm font-medium text-brand-primary"
              >
                내용
              </Label>
              <LazyEditor value={content} onChange={setContent} />
              <div className="flex items-center justify-between">
                <p className="text-xs text-brand-secondary flex items-center space-x-1">
                  <Lightbulb className="w-3 h-3 stroke-indigo-600 fill-indigo-100" />
                  <span>다양한 스타일로 내용을 꾸며보세요.</span>
                </p>
                {content && (
                  <p className={`text-xs ${
                    plainTextLength >= 1000
                      ? 'text-red-600 font-semibold'
                      : plainTextLength >= 900
                      ? 'text-orange-500 font-medium'
                      : 'text-brand-muted'
                  }`}>
                    {plainTextLength}/1000자
                  </p>
                )}
              </div>
            </div>

            {/* 비로그인 사용자용 비밀번호 입력 필드 (조건부 렌더링) */}
            {!isAuthenticated && (
              <div className="space-y-2 pt-4">
                <Label
                  htmlFor="password"
                  className="text-sm font-medium text-brand-primary"
                >
                  비밀번호 (1000~9999)
                  <span className="text-xs text-brand-secondary ml-2 font-normal">
                    게시글 수정/삭제 시 필요합니다
                  </span>
                </Label>
                <Input
                  id="password"
                  type="text"
                  inputMode="numeric"
                  maxLength={4}
                  placeholder="4자리 숫자 (1000~9999)"
                  value={password}
                  onChange={(e) => {
                    const value = e.target.value.replace(/\D/g, '');
                    setPassword(value);
                  }}
                  className={`border-2 focus:border-purple-400 ${
                    password && (password.length < 4 || isNaN(parseInt(password)) || parseInt(password) < 1000 || parseInt(password) > 9999)
                      ? 'border-red-300 focus:border-red-400'
                      : 'border-gray-200'
                  }`}
                />
                {password && (password.length < 4 || isNaN(parseInt(password)) || parseInt(password) < 1000 || parseInt(password) > 9999) && (
                  <p className="text-xs text-red-600 flex items-center gap-1">
                    <span>⚠️</span>
                    <span>비밀번호는 1000~9999 범위의 4자리 숫자여야 합니다</span>
                  </p>
                )}
              </div>
            )}

            {/* 로그인 사용자용 작성자 정보 표시 (조건부 렌더링) */}
            {isAuthenticated && user && (
              <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                <div className="flex items-center space-x-2">
                  <div className="w-8 h-8 bg-gradient-to-r from-pink-500 to-purple-600 rounded-full flex items-center justify-center">
                    <span className="text-white text-sm font-bold">
                      {user?.memberName?.charAt(0) || "?"}
                    </span>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-brand-primary">
                      작성자: {user?.memberName}
                    </p>
                    <p className="text-xs text-brand-muted">
                      게시글은 수정 및 삭제가 가능합니다
                    </p>
                  </div>
                </div>
              </div>
            )}
          </>
        ) : (
          /* 미리보기 모드: 실제 게시글 렌더링 형태로 표시 */
          <div className="prose max-w-none">
            <h1 className="text-3xl font-bold mb-4">{title}</h1>
            <div className="text-sm text-brand-secondary mb-6">
              {/* 작성자 표시: 인증된 사용자는 이름, 비인증 사용자는 '익명' */}
              작성자: {isAuthenticated ? user?.memberName : "익명"}
            </div>
            {formatPreviewContent(content)}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
