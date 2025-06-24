# 🧬 아토믹 디자인 패턴 가이드

이 프로젝트는 Brad Frost의 아토믹 디자인 방법론을 적용하여 확장 가능하고 재사용 가능한 컴포넌트 시스템을 구축했습니다.

## 📁 폴더 구조

```
frontend/components/
├── atoms/          # 원자 - 가장 기본적인 UI 요소
├── molecules/      # 분자 - Atoms의 조합으로 구성된 단순한 UI 그룹
├── organisms/      # 유기체 - 복잡한 UI 섹션
├── templates/      # 템플릿 - 페이지 레이아웃 구조 (향후 확장)
├── index.ts        # 메인 export 파일
├── ui.ts          # 호환성을 위한 re-export 파일
└── README.md       # 이 문서
```

## 🧪 아토믹 레벨별 컴포넌트

### Atoms (원자)

더 이상 쪼갤 수 없는 가장 기본적인 UI 요소들입니다.

- `Button` - 기본 버튼 컴포넌트
- `Input` - 입력 필드
- `Label` - 레이블
- `Textarea` - 텍스트 영역
- `Avatar` - 사용자 아바타
- `Badge` - 뱃지 표시
- `Switch` - 토글 스위치
- `Icon` - 아이콘 컴포넌트 (Lucide 아이콘 지원)
- `Spinner` - 로딩 스피너
- `SafeHTML` - 안전한 HTML 렌더링

### Molecules (분자)

2개 이상의 Atoms가 결합된 단순한 UI 그룹입니다.

**📱 레이아웃 & 구조**

- `Card` - 카드 컴포넌트
  - `FeatureCard` - 기능 소개용 카드 (메인페이지 스타일)
  - `CTACard` - 액션 유도 카드 (그라디언트 배경)
  - `BottomSheetCard` - 모바일 바텀시트 스타일
- `Alert` - 알림 메시지
- `Tabs` - 탭 인터페이스

**🎛️ 인터랙티브 컴포넌트**

- `Dialog` - 모달 다이얼로그
- `Sheet` - 시트(사이드 패널)
- `Popover` - 팝오버 (모바일 최적화, 완전 불투명 배경)
- `DropdownMenu` - 드롭다운 메뉴 (모바일 최적화, 완전 불투명 배경)
- `Select` - 선택 박스 (모바일 최적화, 완전 불투명 배경)

**📝 폼 & 입력 컴포넌트**

- `SearchBox` - 검색 박스 (Input + Button 조합)
- `FormField` - 폼 필드 (Label + Input + ErrorText 조합)

**🎨 콘텐츠 컴포넌트**

- `Editor` - 텍스트 에디터 (Quill 기반, SSR 안전)
- `ReportModal` - 신고 모달

**⏳ 상태 컴포넌트 (모바일 최적화)**

- `Loading` - 범용 로딩 컴포넌트
- `BrandSpinner` - 브랜드 일관성 있는 로딩 스피너
- `Skeleton` - 스켈레톤 로딩 (콘텐츠 자리표시자)
- `CardSkeleton` - 카드형 스켈레톤
- `ListSkeleton` - 리스트형 스켈레톤
- `PullToRefreshLoader` - 모바일 당겨서 새로고침

**📭 빈 상태 컴포넌트 (Empty States)**

- `EmptyState` - 범용 빈 상태 컴포넌트
- `EmptyPosts` - 게시글 없음 상태
- `EmptyMessages` - 메시지 없음 상태
- `EmptySearch` - 검색 결과 없음 상태
- `ErrorState` - 에러 상태
- `OfflineState` - 오프라인 상태
- `WelcomeState` - 환영 메시지 (신규 사용자용)
- `PageEmptyState` - 전체 페이지 빈 상태

### Organisms (유기체)

Molecules와 Atoms가 결합된 복잡한 UI 섹션입니다.

- `AuthHeader` - 인증 헤더
- `MobileNav` - 모바일 네비게이션
- `NotificationBell` - 알림 벨
- `BoardSearch` - 게시판 검색
- `BoardPagination` - 게시판 페이지네이션
- `PostList` - 게시글 목록
- `PopularPostList` - 인기 게시글 목록
- `NoticeList` - 공지사항 목록

## 🔧 사용 방법

### 새로운 아토믹 구조 사용

```typescript
// 아토믹 구조를 직접 사용
import { Button, Icon, Spinner } from "@/components/atoms/button";
import { Card, SearchBox, FormField } from "@/components/molecules/card";
import { AuthHeader } from "@/components/organisms/auth-header";
import { PageTemplate } from "@/components/templates/page-template";

// 또는 메인 index에서 일괄 import
import {
  Button,
  Icon,
  Spinner, // Atoms
  Card,
  SearchBox,
  FormField, // Molecules
  AuthHeader, // Organisms
  PageTemplate, // Templates
} from "@/components";
```

### 기존 UI 경로 호환성

기존 코드의 변경을 최소화하기 위해 호환성 레이어를 제공합니다:

```typescript
// 기존 방식 (여전히 작동함)
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";

// 새로운 방식 (권장)
import { Button } from "@/components/atoms/button";
import { Card } from "@/components/molecules/card";
```

## 🎯 컴포넌트 개발 가이드라인

### Atoms 개발 원칙

- 단일 책임 원칙을 따름
- 독립적으로 동작 가능
- Props로 다양한 variant, size, color 등을 받아 유연하게 활용
- TypeScript 타입 정의 필수

### Molecules 개발 원칙

- 2개 이상의 Atoms 조합으로 구성
- 특정 기능을 수행하는 최소 단위
- 내부 Atoms 간의 상호작용 관리
- 기본적인 비즈니스 로직 포함 가능

### Organisms 개발 원칙

- 페이지의 주요 섹션을 구성
- 상태 관리와 비즈니스 로직 포함
- API 호출 및 데이터 처리
- 하위 컴포넌트들의 협업 관리

## 📝 명명 규칙

- **Atoms**: 명사형 (Button, Input, Label)
- **Molecules**: 기능 중심 (SearchBox, FormField, AlertDialog)
- **Organisms**: 섹션 중심 (Header, ProductList, CommentSection)
- **Templates**: ~Template 접미사 (PageTemplate, DashboardTemplate)
- **Pages**: ~Page 접미사 (HomePage, ProductDetailPage)

## 🔄 마이그레이션 가이드

### 기존 컴포넌트를 아토믹 구조로 변경하기

1. **컴포넌트 분석**: 기존 컴포넌트가 어떤 아토믹 레벨에 속하는지 분석
2. **적절한 폴더로 이동**: atoms, molecules, organisms 중 적절한 위치로 이동
3. **Import 경로 업데이트**: 새로운 경로로 import 구문 변경
4. **Index 파일 업데이트**: 해당 레벨의 index.ts에 export 추가

### 새로운 컴포넌트 추가하기

1. **아토믹 레벨 결정**: 컴포넌트의 복잡도와 책임에 따라 레벨 결정
2. **적절한 폴더에 생성**: atoms/molecules/organisms 중 선택
3. **타입 정의**: TypeScript 인터페이스로 Props 정의
4. **Index 파일 업데이트**: 해당 레벨의 index.ts에 export 추가

## 📄 Templates (템플릿)

페이지 레이아웃 구조를 정의하는 템플릿들입니다.

- `PageTemplate` - 기본 페이지 레이아웃 (Header + Sidebar + Main + Footer)
- `AuthTemplate` - 인증 페이지 레이아웃 (로그인/회원가입용)
- `DashboardTemplate` - 대시보드 레이아웃 (관리자/사용자 대시보드용)

### Templates 사용 예시

```typescript
import { PageTemplate, AuthTemplate } from '@/components/templates';

// 기본 페이지 레이아웃
<PageTemplate
  header={<AuthHeader />}
  sidebar={<Sidebar />}
  footer={<Footer />}
>
  <YourPageContent />
</PageTemplate>

// 인증 페이지 레이아웃
<AuthTemplate
  title="로그인"
  description="계정에 로그인하세요"
  logo={<Logo />}
>
  <LoginForm />
</AuthTemplate>
```

## 🏗️ 향후 확장 계획

### Design System 통합 ✅

- **Design Token 시스템 구축 완료** - `@/lib/design-tokens`에서 사용 가능
- 일관된 스타일 가이드 적용
- Storybook 통합으로 컴포넌트 문서화 (계획 중)

### Storybook 통합 (계획 중)

- 각 컴포넌트별 스토리 작성
- 인터랙티브 문서화
- Visual regression testing

### 테스팅 전략 (계획 중)

- Unit Tests: Atoms, Molecules 단위 테스트
- Integration Tests: Organisms 기능 테스트
- E2E Tests: Templates과 Pages 통합 테스트
- Visual Regression Tests: 스토리북 스냅샷 테스트

## 🎨 디자인 토큰 시스템 ✅

완전한 디자인 토큰 시스템이 구현되었습니다. `@/lib/design-tokens`에서 사용할 수 있습니다.

### 사용 방법

```typescript
import { designTokens, getColor, getSpacing } from "@/lib/design-tokens";

// 직접 사용
const primaryColor = designTokens.colors.primary[500];
const baseSpacing = designTokens.spacing[4];

// 유틸리티 함수 사용
const color = getColor("primary", "500");
const spacing = getSpacing(4);
```

### 포함된 토큰

**🎨 Colors (메인페이지 기준)**

- **Primary**: Pink 계열 (pink-500 ~ pink-900) - 메인 브랜드 색상
- **Secondary**: Purple 계열 (purple-500 ~ purple-900) - 보조 브랜드 색상
- **Accent**: Indigo 계열 (indigo-500 ~ indigo-900) - 강조 색상
- **Semantic**: Success(green), Warning(orange), Error(red), Info(blue)
- **Neutral**: Gray 계열 (gray-50 ~ gray-900)
- **Gradients**: 메인페이지 실제 사용 그라디언트 매핑

**📱 Typography (모바일 최적화)**

- **Font families**: Inter 기반 시스템 폰트
- **Font sizes**: 모바일 기준 최소 16px 기본 크기
- **Font weights**: Light(300) ~ Extrabold(800)
- **Line heights**: 모바일 가독성 최적화 (normal: 1.5)

**📏 Spacing (터치 친화적)**

- **일관된 여백**: 4px 기준 배수 체계
- **터치 타겟**: 최소 44px, 권장 48px, 편안함 56px
- **Safe Area**: 모바일 노치/홈바 대응

**🔄 기타 토큰**

- **Border Radius**: 모바일 카드 최적화 (lg: 8px 기본)
- **Shadows**: 모바일 특화 그림자 (card, modal 등)
- **Animation**: 터치 피드백 최적화 (즉시: 50ms, 일반: 300ms)
- **Breakpoints**: 모바일 퍼스트 (xs: 320px ~ 2xl: 1536px)

## 📚 참고 자료

- [Atomic Design by Brad Frost](https://atomicdesign.bradfrost.com/)
- [React Component Patterns](https://kentcdodds.com/blog/compound-components-with-react-hooks)
- [Design Systems](https://www.designsystems.com/)

## 📊 현재 구현 현황

### ✅ 완료된 기능

**📱 모바일 퍼스트 아토믹 시스템**

- **아토믹 폴더 구조** - atoms, molecules, organisms, templates
- **모바일 최적화 Atoms** - Button, Input, Label, Textarea, Avatar, Badge, Switch, Icon, Spinner, SafeHTML
- **고급 Molecules 컴포넌트** - Card 변형들, Dialog, Alert, SearchBox, FormField, ReportModal
- **상태 관리 Molecules** - Loading, BrandSpinner, Skeleton, EmptyState 컴포넌트들
- **터치 최적화 Organisms** - AuthHeader, MobileNav, NotificationBell, Board 관련 컴포넌트들
- **반응형 Templates 시스템** - PageTemplate, AuthTemplate, DashboardTemplate

**🎨 메인페이지 기준 디자인 시스템**

- **Pink-Purple-Indigo 그라디언트** - 메인페이지와 완전히 일관된 색상 체계
- **모바일 터치 최적화** - 최소 44px 터치 타겟, 피드백 애니메이션
- **브랜드 일관성** - 모든 컴포넌트에 통일된 `bg-white/80 backdrop-blur-sm` 스타일
- **Design Token 시스템** - 완전한 모바일 퍼스트 디자인 토큰 체계

**🔧 개발자 경험**

- **호환성 레이어** - 기존 `@/components/ui/*` 경로 완전 지원
- **TypeScript 지원** - 완전한 타입 안전성 및 자동완성
- **체계적인 Export** - index.ts를 통한 깔끔한 import 구조

### 🔄 마이그레이션 상태

- **기존 컴포넌트 분류 완료** - UI 컴포넌트들이 적절한 아토믹 레벨로 이동
- **Import 경로 호환성** - 기존 `@/components/ui/*` 경로 계속 지원
- **점진적 마이그레이션 가능** - 새로운 구조와 기존 구조 병행 사용

### 🚀 개발 환경 개선

- **체계적인 컴포넌트 구조** - 역할과 책임이 명확한 컴포넌트 계층
- **재사용성 극대화** - Atoms부터 Templates까지 계층적 재사용
- **일관된 디자인 시스템** - Design Token 기반의 통일된 스타일링
- **개발자 경험 향상** - 직관적인 import와 사용법

## 🎯 사용 권장 사항

1. **새로운 컴포넌트**: 아토믹 구조를 사용하여 개발
2. **기존 컴포넌트**: 점진적으로 새로운 구조로 마이그레이션
3. **스타일링**: Design Token 시스템 활용
4. **페이지 개발**: Templates을 활용한 일관된 레이아웃

## 💡 실제 사용 예제

### 1. 모바일 최적화 검색 페이지 만들기

```typescript
"use client";

import React, { useState } from "react";
import {
  Button, // Atoms
  SearchBox,
  FeatureCard,
  EmptySearch,
  Loading,
  BrandSpinner, // Molecules
  AuthHeader, // Organisms
  PageTemplate, // Templates
} from "@/components";
import { Search } from "lucide-react";
import { designTokens } from "@/lib/design-tokens";

export function SearchPage() {
  const [searchValue, setSearchValue] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [results, setResults] = useState([]);

  const handleSearch = async () => {
    setIsLoading(true);
    // API 호출 시뮬레이션
    await new Promise((resolve) => setTimeout(resolve, 1000));
    setResults([]); // 빈 결과 시뮬레이션
    setIsLoading(false);
  };

  return (
    <PageTemplate header={<AuthHeader />}>
      <div className="max-w-4xl mx-auto p-4">
        {/* 메인페이지와 일관된 헤더 */}
        <div className="text-center mb-8">
          <h1 className="text-3xl md:text-4xl font-bold bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent mb-4">
            검색하기
          </h1>
          <p className="text-gray-600">궁금한 내용을 검색해보세요</p>
        </div>

        {/* 모바일 최적화 검색박스 */}
        <div className="mb-6">
          <SearchBox
            value={searchValue}
            onChange={setSearchValue}
            onSearch={handleSearch}
            placeholder="검색어를 입력하세요..."
            className="w-full"
          />
        </div>

        {/* 로딩 상태 */}
        {isLoading && (
          <Loading type="card" message="검색 중..." className="mb-6" />
        )}

        {/* 빈 결과 상태 */}
        {!isLoading && searchValue && results.length === 0 && (
          <EmptySearch
            searchTerm={searchValue}
            onReset={() => setSearchValue("")}
          />
        )}
      </div>
    </PageTemplate>
  );
}
```

### 2. SSR 안전 텍스트 에디터 사용하기

```typescript
"use client";

import React, { useState } from "react";
import {
  Button, // Atoms
  Editor,
  FormField,
  Card, // Molecules
  PageTemplate, // Templates
} from "@/components";

export function CreatePostPage() {
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    // 게시글 저장 로직
    console.log({ title, content });
  };

  return (
    <PageTemplate>
      <div className="max-w-4xl mx-auto p-4">
        <h1 className="text-3xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent mb-6">
          새 글 작성하기
        </h1>

        <Card className="p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            <FormField
              label="제목"
              name="title"
              value={title}
              onChange={setTitle}
              placeholder="게시글 제목을 입력하세요"
              required
            />

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                내용
              </label>
              {/* SSR 안전 에디터 - 자동으로 로딩 UI 표시 */}
              <Editor value={content} onChange={setContent} />
            </div>

            <Button type="submit" size="full">
              게시글 저장
            </Button>
          </form>
        </Card>
      </div>
    </PageTemplate>
  );
}
```

### 3. 모바일 최적화 로그인 페이지 만들기

```typescript
"use client";

import React, { useState } from "react";
import {
  Button, // Atoms
  FormField,
  CTACard,
  Loading, // Molecules
  AuthTemplate, // Templates
} from "@/components";
import { Heart } from "lucide-react";

export function LoginPage() {
  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    // 로그인 API 호출 시뮬레이션
    await new Promise((resolve) => setTimeout(resolve, 2000));
    setIsLoading(false);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* 메인페이지와 일관된 브랜드 로고 */}
        <div className="text-center mb-8">
          <div className="w-16 h-16 mx-auto mb-4 bg-gradient-to-r from-pink-500 to-purple-600 rounded-2xl flex items-center justify-center">
            <Heart className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent">
            비밀로그
          </h1>
          <p className="text-gray-600 mt-2">
            나만의 롤링페이퍼를 만들어 보세요
          </p>
        </div>

        {/* 모바일 최적화 로그인 폼 */}
        <div className="bg-white/80 backdrop-blur-sm rounded-lg border-0 shadow-lg p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
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

            <FormField
              label="비밀번호"
              name="password"
              type="password"
              value={formData.password}
              onChange={(value) =>
                setFormData((prev) => ({ ...prev, password: value }))
              }
              placeholder="비밀번호를 입력하세요"
              required
            />

            <Button
              type="submit"
              size="full"
              disabled={isLoading}
              className="h-12"
            >
              {isLoading ? (
                <Loading type="button" message="로그인 중..." />
              ) : (
                "로그인"
              )}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
```

### 3. 모바일 퍼스트 Design Token 활용하기

```typescript
import { designTokens, getColor, getGradient } from "@/lib/design-tokens";

// 메인페이지와 일관된 스타일 적용
const mobileCardStyles = {
  // Pink-Purple 그라디언트 배경
  background: `linear-gradient(to right, ${designTokens.colors.primary[500]}, ${designTokens.colors.secondary[600]})`,
  color: "white",

  // 모바일 최적화 타이포그래피
  fontSize: designTokens.typography.fontSize.base, // 16px 최소 크기
  lineHeight: designTokens.typography.lineHeight.normal, // 1.5

  // 터치 친화적 간격
  padding: designTokens.spacing[6], // 24px
  minHeight: designTokens.touchTarget.recommended, // 48px
  borderRadius: designTokens.borderRadius.lg, // 8px

  // 모바일 특화 그림자
  boxShadow: designTokens.boxShadow.card,
};

// 그라디언트 유틸리티 함수 사용
const gradientButton = {
  background: `linear-gradient(to right, ${getColor(
    "primary",
    "500"
  )}, ${getColor("secondary", "600")})`,
  color: "white",
  padding: `${designTokens.spacing[3]} ${designTokens.spacing[6]}`,
  borderRadius: designTokens.borderRadius.lg,
  minHeight: designTokens.touchTarget.minimum, // 44px
};

// Tailwind CSS와 Design Token 조합 (메인페이지 스타일)
export function MobileCard({ children }: { children: React.ReactNode }) {
  return (
    <div
      className="bg-white/80 backdrop-blur-sm shadow-lg hover:shadow-xl transition-all duration-300 rounded-lg"
      style={{
        padding: designTokens.spacing[6],
        minHeight: designTokens.touchTarget.comfortable, // 56px
      }}
    >
      {children}
    </div>
  );
}

// 모바일 터치 검증
import { validateTouchTarget } from "@/lib/design-tokens";

const buttonHeight = "48px";
if (validateTouchTarget(buttonHeight)) {
  console.log("터치 타겟 크기가 적절합니다!"); // true - 44px 이상
}
```

## 🔧 SSR (서버 사이드 렌더링) 호환성

이 컴포넌트 시스템은 **Next.js SSR과 완전 호환**되도록 설계되었습니다.

### 🎯 SSR 안전 컴포넌트

**클라이언트 전용 컴포넌트 (Dynamic Import 사용)**

- `Editor` - Quill.js 기반 텍스트 에디터
  - Next.js `dynamic()` 함수를 사용하여 클라이언트에서만 로드
  - 로딩 중 스켈레톤 UI 표시
  - `document` 객체 접근 문제 해결

### 🚀 브라우저 특화 기능 처리

**동적 로딩 패턴**

```typescript
import dynamic from "next/dynamic";

// SSR 안전 에디터 로딩
const Editor = dynamic(() => import("@/components/molecules/editor"), {
  ssr: false,
  loading: () => <EditorLoading />,
});

// 사용 예시
<Editor value={content} onChange={setContent} />;
```

**브라우저 환경 체크**

```typescript
useEffect(() => {
  if (typeof window === "undefined") return;

  // 브라우저에서만 실행되는 코드
  const initBrowserOnlyFeature = async () => {
    const module = await import("browser-only-library");
    // 초기화 로직
  };

  initBrowserOnlyFeature();
}, []);
```

## 📱 모바일 퍼스트 디자인 원칙

이 컴포넌트 시스템은 **모바일 퍼스트** 접근법을 기반으로 구축되었습니다.

### 🎯 핵심 원칙

**터치 최적화**

- **최소 터치 타겟**: 모든 상호작용 요소는 최소 44px × 44px 보장
- **권장 터치 타겟**: 48px × 48px (편안한 사용)
- **터치 피드백**: `active:scale-[0.98]`, `touch-manipulation` 속성 적용
- **충분한 간격**: 버튼 간 최소 8px 이상 간격 유지

**모바일 특화 컴포넌트**

- `BottomSheetCard` - 모바일 바텀시트 UI
- `PullToRefreshLoader` - 당겨서 새로고침
- `MobileNav` - 터치 친화적 네비게이션
- `SafeArea` 대응 - iOS 노치/홈바 영역 고려

**반응형 우선순위**

1. **Mobile First** (320px~): 기본 디자인 기준
2. **Mobile Large** (480px~): 큰 모바일 화면
3. **Tablet** (768px~): 태블릿 확장
4. **Desktop** (1024px+): 데스크톱 추가 기능

## 🎨 메인페이지 기준 디자인 일관성

모든 컴포넌트는 **메인페이지의 Pink-Purple-Indigo 그라디언트 테마**를 기준으로 디자인되었습니다.

### 🌈 색상 체계

**그라디언트 테마**

```typescript
// 메인 그라디언트
bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50  // 배경
bg-gradient-to-r from-pink-500 to-purple-600              // 버튼
bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 // 제목

// 기능별 그라디언트
from-pink-500 to-red-500      // Feature 1
from-purple-500 to-indigo-500 // Feature 2
from-green-500 to-teal-500    // Feature 3
from-orange-500 to-yellow-500 // Feature 4
```

**카드 스타일 통일**

- 기본: `bg-white/80 backdrop-blur-sm shadow-lg hover:shadow-xl`
- 브랜드 일관성: 모든 카드에 동일한 스타일 적용
- 호버 효과: 그림자 강화 및 스케일 애니메이션

### 🎯 시각적 일관성 체크포인트

- ✅ **색상**: 메인페이지 그라디언트 팔레트 사용
- ✅ **타이포그래피**: 그라디언트 제목, 일관된 본문 색상
- ✅ **간격**: Design Token 기반 일관된 여백
- ✅ **그림자**: 통일된 shadow-lg 스타일
- ✅ **모서리**: 일관된 rounded-lg 적용
- ✅ **애니메이션**: 부드러운 트랜지션 효과

---

이 문서는 프로젝트의 아토믹 디자인 패턴 적용에 대한 가이드입니다. 질문이나 개선사항이 있으면 언제든 문의해 주세요! 🚀
