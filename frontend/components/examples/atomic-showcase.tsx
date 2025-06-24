"use client";

import React, { useState } from "react";

// Atoms
import { Button, Input, Label, Icon, Spinner, Badge } from "@/components/atoms";
import { Search, Heart, Star } from "lucide-react";

// Molecules
import {
  SearchBox,
  FormField,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/molecules";

// Organisms
import { AuthHeader } from "@/components/organisms";

// Templates
import { PageTemplate } from "@/components/templates";

// Design Tokens
import { designTokens } from "@/lib/design-tokens";

/**
 * 아토믹 디자인 패턴 쇼케이스 컴포넌트
 *
 * 이 컴포넌트는 아토믹 디자인의 모든 레벨(Atoms, Molecules, Organisms, Templates)을
 * 실제로 사용하는 방법을 보여주는 예제입니다.
 */
export function AtomicShowcase() {
  const [searchValue, setSearchValue] = useState("");
  const [formData, setFormData] = useState({
    name: "",
    email: "",
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSearch = () => {
    setIsLoading(true);
    setTimeout(() => setIsLoading(false), 2000);
  };

  return (
    <PageTemplate header={<AuthHeader />} className="bg-gray-50">
      <div className="max-w-6xl mx-auto p-6 space-y-8">
        {/* 페이지 헤더 */}
        <div className="text-center space-y-4">
          <h1
            className="text-4xl font-bold"
            style={{ color: designTokens.colors.primary[600] }}
          >
            🧬 아토믹 디자인 쇼케이스
          </h1>
          <p className="text-gray-600 max-w-2xl mx-auto">
            이 페이지는 아토믹 디자인 패턴의 모든 레벨(Atoms, Molecules,
            Organisms, Templates)을 실제로 사용하는 방법을 보여줍니다.
          </p>
        </div>

        {/* Atoms 섹션 */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Icon icon={Star} variant="primary" />
              Atoms (원자) - 기본 UI 요소들
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {/* Buttons */}
              <div className="space-y-2">
                <Label>Buttons</Label>
                <div className="flex gap-2 flex-wrap">
                  <Button variant="default">Primary</Button>
                  <Button variant="secondary">Secondary</Button>
                  <Button variant="outline">Outline</Button>
                </div>
              </div>

              {/* Input */}
              <div className="space-y-2">
                <Label>Input</Label>
                <Input placeholder="Enter text..." />
              </div>

              {/* Icons & Badges */}
              <div className="space-y-2">
                <Label>Icons & Badges</Label>
                <div className="flex items-center gap-2">
                  <Icon icon={Heart} variant="destructive" />
                  <Badge variant="default">New</Badge>
                  <Badge variant="secondary">Popular</Badge>
                  {isLoading && <Spinner size="sm" />}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Molecules 섹션 */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Icon icon={Search} variant="primary" />
              Molecules (분자) - Atoms의 조합
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            {/* SearchBox */}
            <div className="space-y-2">
              <Label>SearchBox (Input + Button 조합)</Label>
              <SearchBox
                value={searchValue}
                onChange={setSearchValue}
                onSearch={handleSearch}
                onClear={() => setSearchValue("")}
                placeholder="검색어를 입력하세요..."
              />
            </div>

            {/* FormField */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <FormField
                label="이름"
                name="name"
                value={formData.name}
                onChange={(value) =>
                  setFormData((prev) => ({ ...prev, name: value }))
                }
                placeholder="이름을 입력하세요"
                required
              />

              <FormField
                label="이메일"
                name="email"
                type="email"
                value={formData.email}
                onChange={(value) =>
                  setFormData((prev) => ({ ...prev, email: value }))
                }
                placeholder="이메일을 입력하세요"
                required
              />
            </div>
          </CardContent>
        </Card>

        {/* Design Tokens 섹션 */}
        <Card>
          <CardHeader>
            <CardTitle>🎨 Design Tokens</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Colors */}
              <div>
                <Label className="text-sm font-semibold">Colors</Label>
                <div className="mt-2 space-y-2">
                  <div className="flex items-center gap-2">
                    <div
                      className="w-6 h-6 rounded-full border"
                      style={{
                        backgroundColor: designTokens.colors.primary[500],
                      }}
                    />
                    <span className="text-sm">Primary</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div
                      className="w-6 h-6 rounded-full border"
                      style={{
                        backgroundColor: designTokens.colors.secondary[500],
                      }}
                    />
                    <span className="text-sm">Secondary</span>
                  </div>
                </div>
              </div>

              {/* Typography */}
              <div>
                <Label className="text-sm font-semibold">Typography</Label>
                <div className="mt-2 space-y-1">
                  <div
                    style={{ fontSize: designTokens.typography.fontSize.sm }}
                  >
                    Small text
                  </div>
                  <div
                    style={{ fontSize: designTokens.typography.fontSize.base }}
                  >
                    Base text
                  </div>
                  <div
                    style={{ fontSize: designTokens.typography.fontSize.lg }}
                  >
                    Large text
                  </div>
                </div>
              </div>

              {/* Spacing */}
              <div>
                <Label className="text-sm font-semibold">Spacing</Label>
                <div className="mt-2 space-y-2">
                  <div
                    className="bg-blue-100 border border-blue-300"
                    style={{ padding: designTokens.spacing[2] }}
                  >
                    Padding: {designTokens.spacing[2]}
                  </div>
                  <div
                    className="bg-green-100 border border-green-300"
                    style={{ padding: designTokens.spacing[4] }}
                  >
                    Padding: {designTokens.spacing[4]}
                  </div>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 사용법 안내 */}
        <Card>
          <CardHeader>
            <CardTitle>📚 사용법 안내</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="prose max-w-none">
              <h4>아토믹 디자인 패턴 사용하기</h4>
              <pre className="bg-gray-100 p-4 rounded-lg overflow-x-auto">
                {`// 개별 import
import { Button, Icon } from '@/components/atoms';
import { SearchBox, FormField } from '@/components/molecules';
import { AuthHeader } from '@/components/organisms';
import { PageTemplate } from '@/components/templates';

// 통합 import
import { 
  Button, Icon,           // Atoms
  SearchBox, FormField,   // Molecules
  AuthHeader,             // Organisms
  PageTemplate            // Templates
} from '@/components';

// Design Tokens 사용
import { designTokens } from '@/lib/design-tokens';`}
              </pre>
            </div>
          </CardContent>
        </Card>
      </div>
    </PageTemplate>
  );
}
