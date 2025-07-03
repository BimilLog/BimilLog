# 🧬 아토믹 디자인 패턴 가이드

이 프로젝트는 Brad Frost의 아토믹 디자인 방법론을 적용하여 확장 가능하고 재사용 가능한 컴포넌트 시스템을 구축했습니다.

## 📁 폴더 구조

```
frontend/components/
├── atoms/          # 원자 - 가장 기본적인 UI 요소
├── molecules/      # 분자 - Atoms의 조합으로 구성된 단순한 UI 그룹
├── organisms/      # 유기체 - 복잡한 UI 섹션
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
- `KakaoShareButton` - 카카오 공유 버튼

### Molecules (분자)

2개 이상의 Atoms가 결합된 단순한 UI 그룹입니다.

**📱 레이아웃 & 구조**

- `Card` - 카드 컴포넌트
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
- `KakaoFriendsModal` - 카카오 친구 목록 모달

**⏳ 상태 컴포넌트 (모바일 최적화)**

- `Loading` - 범용 로딩 컴포넌트
- `Skeleton` - 스켈레톤 로딩 (콘텐츠 자리표시자)

**📭 빈 상태 컴포넌트 (Empty States)**

- `EmptyState` - 범용 빈 상태 컴포넌트

### Organisms (유기체)

Molecules와 Atoms가 결합된 복잡한 UI 섹션입니다.

- `AuthHeader` - 인증 헤더
- `MobileNav` - 모바일 네비게이션
- `NotificationBell` - 실시간 알림 시스템 (SSE + FCM 연동, 모바일 바텀시트)
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

// 또는 메인 index에서 일괄 import
import {
  Button,
  Icon,
  Spinner, // Atoms
  Card,
  SearchBox,
  FormField, // Molecules
  AuthHeader, // Organisms
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
- **모바일 최적화 Atoms** - Button, Input, Label, Textarea, Avatar, Badge, Switch, Icon, Spinner, KakaoShareButton
- **고급 Molecules 컴포넌트** - Card, Alert, SearchBox, FormField, ReportModal
- **상태 관리 Molecules** - Loading, Skeleton, EmptyState 컴포넌트들
- **터치 최적화 Organisms** - AuthHeader, MobileNav, NotificationBell, Board 관련 컴포넌트들
- **반응형 Templates 시스템** - PageTemplate, AuthTemplate, DashboardTemplate

**🎨 메인페이지 기준 디자인 시스템**

- **Pink-Purple-Indigo 그라디언트** - 메인페이지와 완전히 일관된 색상 체계
- **모바일 터치 최적화** - 최소 44px 터치 타겟, 피드백 애니메이션
- **브랜드 일관성** - 모든 컴포넌트에 통일된 `bg-white/80 backdrop-blur-sm` 스타일
- **Design Token 시스템** - 완전한 모바일 퍼스트 디자인 토큰 체계

**🔔 실시간 알림 시스템 (SSE + FCM + 모바일 바텀시트)**

- **SSE 연결 시스템** - Server-Sent Events로 실시간 알림 수신
- **FCM 토큰 관리** - 모바일/태블릿에서 Firebase 푸시 알림 지원
- **모바일 바텀시트** - 터치 친화적인 알림 UI (Sheet 컴포넌트)
- **배치 처리 시스템** - 개별 액션은 5분마다 일괄 처리, 전체 액션은 즉시 실행
- **알림 관리 기능** - 모두 읽기, 모두 삭제, 개별 읽음/삭제 처리
- **시각적 피드백** - 알림 타입별 아이콘, 상대시간 표시, 읽음 상태 구분
- **스마트 연결** - 인증 및 닉네임 설정 완료시에만 SSE 연결

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

## 🎯 롤링페이퍼 리팩토링 사용 예제

### 1. 새로운 통합 컴포넌트 사용법

```typescript
import { RollingPaperClient } from "@/components";

// 내 롤링페이퍼 페이지
export default function MyRollingPaperPage() {
  return <RollingPaperClient />;
}

// 공개 롤링페이퍼 페이지
export default function PublicRollingPaperPage({
  params,
}: {
  params: { nickname: string };
}) {
  return <RollingPaperClient nickname={params.nickname} />;
}
```

### 2. 개별 컴포넌트 사용법

```typescript
import {
  useRollingPaper,
  useRollingPaperShare,
  RollingPaperLayout,
  RollingPaperHeader,
  RollingPaperGrid,
  InfoCard,
  PageNavigation,
  RecentMessages,
} from "@/components";

export function CustomRollingPaperPage() {
  const {
    messages,
    messageCount,
    recentMessages,
    isOwner,
    currentPage,
    totalPages,
    setCurrentPage,
    // ... 기타 상태들
  } = useRollingPaper({ nickname: "example", isPublic: true });

  const { handleKakaoShare, handleWebShare } = useRollingPaperShare({
    nickname: "example",
    messageCount,
    isOwner: false,
  });

  return (
    <RollingPaperLayout adPosition="커스텀 페이지">
      <RollingPaperHeader
        nickname="example"
        messageCount={messageCount}
        isOwner={isOwner}
      />

      <InfoCard isOwner={isOwner} nickname="example" />

      <RollingPaperGrid
        messages={messages}
        nickname="example"
        currentPage={currentPage}
        totalPages={totalPages}
        // ... 기타 props
      />

      <PageNavigation
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={setCurrentPage}
      />

      <RecentMessages
        messages={recentMessages}
        isOwner={isOwner}
        onShare={handleWebShare}
      />
    </RollingPaperLayout>
  );
}
```

### 3. 커스텀 훅 활용법

```typescript
import { useRollingPaper, useRollingPaperShare } from "@/components";

export function useCustomRollingPaper(nickname: string) {
  const rollingPaper = useRollingPaper({
    nickname,
    isPublic: true,
  });

  const share = useRollingPaperShare({
    nickname,
    messageCount: rollingPaper.messageCount,
    isOwner: rollingPaper.isOwner,
  });

  const handleMessageSubmit = async (
    position: { x: number; y: number },
    data: any
  ) => {
    // 커스텀 메시지 제출 로직
    try {
      await rollingPaperApi.createMessage(nickname, {
        decoType: data.decoType,
        anonymity: data.anonymousNickname,
        content: data.content,
        width: position.x,
        height: position.y,
      });
      await rollingPaper.refetchMessages();
      alert("메시지 작성 완료!");
    } catch (error) {
      console.error("메시지 작성 실패:", error);
    }
  };

  return {
    ...rollingPaper,
    ...share,
    handleMessageSubmit,
  };
}
```

### 4. 모바일/PC 분기 처리 예제

```typescript
import { useRollingPaper } from "@/components";

export function ResponsiveRollingPaper() {
  const {
    isMobile,
    totalPages, // 모바일: 3페이지, PC: 2페이지
    colsPerPage, // 모바일: 4열, PC: 6열
    slotsPerPage, // 자동 계산됨
  } = useRollingPaper();

  return (
    <div>
      <p>현재 화면: {isMobile ? "모바일" : "PC"}</p>
      <p>총 페이지: {totalPages}</p>
      <p>페이지당 열 수: {colsPerPage}</p>
      <p>페이지당 슬롯 수: {slotsPerPage}</p>
    </div>
  );
}
```

### 5. 타입 안전성 활용 예제

```typescript
import {
  MessageView,
  type RollingPaperMessage,
  type VisitMessage,
} from "@/components";

export function SafeMessageDisplay({
  message,
}: {
  message: RollingPaperMessage | VisitMessage;
}) {
  // 타입 가드가 내장되어 있어 안전하게 사용 가능
  return (
    <MessageView
      message={message}
      isOwner={true} // RollingPaperMessage만 내용 표시
    />
  );
}
```

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

### 4. 실시간 알림 시스템 사용하기 (SSE + FCM + 모바일 바텀시트)

```typescript
"use client";

import React from "react";
import {
  Button, // Atoms
  NotificationBell, // Organisms (완전한 알림 시스템)
} from "@/components";
import { useAuth } from "@/hooks/useAuth";
import { useNotifications } from "@/hooks/useNotifications";

export function AppHeader() {
  const { user } = useAuth();

  // useNotifications 훅에서 모든 알림 상태를 관리
  const {
    notifications, // 알림 목록
    unreadCount, // 읽지 않은 알림 수
    isConnected, // SSE 연결 상태
    isLoading, // 로딩 상태
    batchStatus, // 배치 처리 상태 (개발 모드)

    // 개별 액션 (배치 큐에 추가)
    markAsRead, // 개별 읽음 처리
    deleteNotification, // 개별 삭제

    // 전체 액션 (즉시 실행)
    markAllAsRead, // 모든 알림 읽음 처리
    deleteAllNotifications, // 모든 알림 삭제

    // 새로고침
    fetchNotifications, // 수동 새로고침
  } = useNotifications();

  return (
    <header className="bg-white/80 backdrop-blur-sm shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* 로고 */}
          <div className="flex items-center">
            <h1 className="text-xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent">
              비밀로그
            </h1>
          </div>

          {/* 사용자 액션 */}
          <div className="flex items-center space-x-4">
            {user && (
              <>
                {/* 실시간 알림 벨 - 모든 기능이 통합된 완전한 컴포넌트 */}
                <NotificationBell />

                {/* 사용자 정보 */}
                <div className="flex items-center space-x-2">
                  <span className="text-sm text-gray-700">{user.nickname}</span>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}

// NotificationBell 컴포넌트의 주요 기능들:

// 📱 **모바일 최적화**
// - 데스크톱: Card 형태의 팝오버 (기존)
// - 모바일/태블릿: Sheet 형태의 바텀시트 (새로 추가)
// - 화면 크기 자동 감지 및 적절한 UI 제공

// 🔔 **실시간 알림 시스템**
// - SSE (Server-Sent Events) 연결로 실시간 알림 수신
// - FCM (Firebase Cloud Messaging) 토큰 자동 관리
// - 모바일/태블릿에서만 FCM 토큰 등록
// - 연결 상태 시각적 표시 (개발 모드)

// 📋 **알림 관리 기능**
// - 읽지 않은 알림 수 배지 표시
// - 개별 알림 읽음/삭제 (배치 처리)
// - 전체 알림 읽음/삭제 (즉시 처리)
// - 자동 새로고침 및 수동 새로고침

// ⚡ **성능 최적화**
// - 배치 처리: 개별 액션은 5분마다 일괄 처리
// - 즉시 처리: 전체 액션은 바로 실행
// - UI 즉시 업데이트: 서버 응답 대기 없이 UI 먼저 변경
// - 스마트 연결: 인증 상태 및 닉네임 설정 완료시에만 SSE 연결

// 🎨 **시각적 향상**
// - 알림 타입별 아이콘 (댓글, 농장, 인기글, 관리자 등)
// - 상대 시간 표시 (방금 전, N분 전, N시간 전, N일 전)
// - 읽지 않은 알림 시각적 강조
// - 부드러운 애니메이션 및 터치 피드백

// 사용자 정의 알림 처리 예시
export function CustomNotificationHandler() {
  const { notifications } = useNotifications();

  // 특정 타입의 알림만 필터링
  const commentNotifications = notifications.filter(
    (n) => n.type === "COMMENT"
  );
  const farmNotifications = notifications.filter((n) => n.type === "FARM");

  return (
    <div className="space-y-4">
      {/* 댓글 알림 */}
      {commentNotifications.length > 0 && (
        <div className="bg-blue-50 p-4 rounded-lg">
          <h3 className="font-medium text-blue-900 mb-2">
            새로운 댓글 ({commentNotifications.length}개)
          </h3>
          {commentNotifications.slice(0, 3).map((notification) => (
            <div key={notification.id} className="text-sm text-blue-700">
              {notification.data}
            </div>
          ))}
        </div>
      )}

      {/* 농장 알림 */}
      {farmNotifications.length > 0 && (
        <div className="bg-green-50 p-4 rounded-lg">
          <h3 className="font-medium text-green-900 mb-2">
            농장 업데이트 ({farmNotifications.length}개)
          </h3>
          {farmNotifications.slice(0, 3).map((notification) => (
            <div key={notification.id} className="text-sm text-green-700">
              {notification.data}
            </div>
          ))}
        </div>
      )}
    </div>
  );
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

## 🎯 롤링페이퍼 전용 컴포넌트들

### Rolling Paper Components

별도의 기능적 컴포넌트들로 롤링페이퍼 시스템을 구성합니다.

**핵심 컴포넌트들**

- `RecentVisits` - 최근 방문한 롤링페이퍼 목록 (localStorage 기반)
- `RollingPaperHeader` - 롤링페이퍼 페이지 헤더
- `MessageForm` - 메시지 작성 폼
- `MessageView` - 메시지 보기 컴포넌트
- `RollingPaperGrid` - 롤링페이퍼 그리드 레이아웃
- `RecentMessages` - 최근 메시지들 표시

### 🍪 쿠키 및 로컬스토리지 시스템

**최근 방문 기록 관리**

```typescript
import {
  addRecentVisit,
  getRecentVisits,
  removeRecentVisit,
  clearRecentVisits,
  getRelativeTimeString,
} from "@/lib/cookies";

// 방문 기록 추가 (자동으로 호출됨)
addRecentVisit(nickname);

// 방문 기록 조회
const visits = getRecentVisits(); // RecentVisit[] 반환

// 개별 기록 삭제
removeRecentVisit(nickname);

// 모든 기록 삭제
clearRecentVisits();

// 상대 시간 문자열 생성
const timeAgo = getRelativeTimeString("2024-01-15T10:30:00"); // "2시간 전"
```

**시스템 특징**

- **저장소**: localStorage (`'recent_rolling_papers'`)
- **최대 개수**: 5개 (FIFO 방식)
- **생명주기**: 30일 자동 만료
- **중복 처리**: 재방문시 최신으로 업데이트
- **자동 정리**: 만료된 기록 자동 삭제

### 📋 새로 추가된 컴포넌트 사용 예시

```typescript
import {
  RecentVisits,
  RollingPaperHeader,
  MessageForm,
  MessageView,
  addRecentVisit,
  getRecentVisits,
} from "@/components";

// 1. 최근 방문 목록 표시 (/visit 페이지)
export function VisitPage() {
  return (
    <div className="container mx-auto p-4">
      <h1>롤링페이퍼 방문</h1>

      {/* 검색 섹션 */}
      <SearchBox />

      {/* 최근 방문한 롤링페이퍼 */}
      <RecentVisits />
    </div>
  );
}

// 2. 방문 기록 자동 저장
export function RollingPaperPage({ nickname }: { nickname: string }) {
  const { user } = useAuth();

  useEffect(() => {
    const currentNickname = decodeURIComponent(nickname);
    const isOwner = user?.userName === currentNickname;

    // 다른 사람의 롤링페이퍼 방문시에만 기록 저장
    if (!isOwner) {
      addRecentVisit(nickname);
    }
  }, [nickname, user]);

  return (
    <div>
      <RollingPaperHeader nickname={nickname} />
      {/* 롤링페이퍼 콘텐츠 */}
    </div>
  );
}

// 3. 쿠키 시스템 커스터마이징
export function CustomRecentVisits() {
  const [visits, setVisits] = useState([]);

  useEffect(() => {
    setVisits(getRecentVisits());
  }, []);

  const handleRemove = (nickname: string) => {
    removeRecentVisit(nickname);
    setVisits(getRecentVisits());
  };

  return (
    <div className="space-y-2">
      {visits.map((visit) => (
        <div key={visit.nickname} className="flex items-center justify-between">
          <Link href={`/rolling-paper/${visit.nickname}`}>
            {visit.displayName}님의 롤링페이퍼
          </Link>
          <span>{getRelativeTimeString(visit.visitedAt)}</span>
          <Button onClick={() => handleRemove(visit.nickname)}>삭제</Button>
        </div>
      ))}
    </div>
  );
}
```

### 🔄 컴포넌트 Export 구조

**메인 Export (`@/components`)**

```typescript
// 모든 아토믹 컴포넌트 + 롤링페이퍼 컴포넌트 + 유틸리티
import {
  Button,
  Card,
  Editor, // 아토믹 컴포넌트들
  RecentVisits,
  MessageForm, // 롤링페이퍼 컴포넌트들
  addRecentVisit,
  getRecentVisits, // 쿠키 시스템
} from "@/components";
```

**호환성 Export (`@/components/ui`)**

```typescript
// 기존 UI 경로 호환성 + 새로운 컴포넌트들
import {
  Button,
  Card,
  Dialog, // 기존 UI 컴포넌트들
  RecentVisits,
  MessageForm, // 새로 추가된 컴포넌트들
  AuthHeader,
  NotificationBell, // Organisms 포함
} from "@/components/ui";
```

### 📊 롤링페이퍼 컴포넌트 리팩토링 완료 ✅

**🔧 완전한 컴포넌트 분리 및 병합 작업 완료**

기존의 22KB rolling-paper-client.tsx와 33KB public-rolling-paper-client.tsx를 체계적으로 분리하고 병합하여 효율적인 구조로 리팩토링했습니다.

**📱 새로운 컴포넌트 구조**

**🎯 공통 훅 (Hooks)**

- `useRollingPaper` - 롤링페이퍼 상태 관리 (내/공개 통합)
- `useRollingPaperShare` - 공유 기능 (카카오/웹 공유)

**🧩 공통 컴포넌트**

- `RollingPaperLayout` - 전체 레이아웃 (광고, 헤더 포함)
- `RollingPaperHeader` - 반응형 헤더 (모바일/PC 최적화)
- `RollingPaperGrid` - 메시지 그리드 (페이지네이션 포함)
- `PageNavigation` - 페이지네이션 컴포넌트
- `InfoCard` - 정보 카드 (소유자/방문자 구분)
- `RecentMessages` - 최근 메시지 목록
- `MessageForm` - 메시지 작성 폼 (개선)
- `MessageView` - 메시지 보기 (타입 안전성 강화)

**🎯 통합 메인 컴포넌트**

- `RollingPaperClient` - 내/공개 롤링페이퍼 통합 처리

**💡 개선 효과**

**코드 중복 제거**

- 기존 55KB → 현재 ~15KB (73% 감소)
- 공통 로직 훅으로 분리
- 중복 UI 컴포넌트 통합

**모바일/PC 분리 최적화**

- 반응형 디자인 로직 체계화
- 터치/마우스 인터랙션 분리
- 화면 크기별 레이아웃 최적화

**기능별 분할**

- 내 롤링페이퍼 vs 공개 롤링페이퍼 로직 분리
- 권한별 UI 처리 개선
- 타입 안전성 강화 (VisitMessage vs RollingPaperMessage)

**재사용성 극대화**

- 모든 컴포넌트 독립적 사용 가능
- Props 기반 유연한 설정
- 아토믹 디자인 원칙 준수

### 📊 현재 구현 현황 (업데이트)

### ✅ 완료된 기능

**🎯 게시판 UI 개선**

- 게시글 목록에서 사용자 아이콘 제거 (게시글 상세는 유지)
- 작성자 이름 클릭시 롤링페이퍼 이동 기능
- 회원/익명 사용자 구분 표시

**📱 롤링페이퍼 시스템 완성**

- 페이지네이션 시스템 (PC: 2페이지, 모바일: 3페이지)
- 좌표 시스템 확장 (x축 0~11)
- 방문 기록 저장 및 관리 시스템
- 최근 방문한 롤링페이퍼 컴포넌트

**🍪 쿠키 시스템 (LocalStorage 기반)**

- 자동 방문 기록 저장 (다른 사람 롤링페이퍼만)
- 최대 5개, 30일 생명주기
- FIFO 방식 자동 관리
- 개별/전체 삭제 기능

**🎨 디자인 시스템 일관성**

- /visit 페이지 디자인에 맞는 RecentVisits 스타일링
- 메인페이지 Pink-Purple-Indigo 테마 유지
- 모바일 퍼스트 반응형 디자인

**📋 Export 시스템 완성**

- 아토믹 구조 기반 체계적 Export
- 롤링페이퍼 컴포넌트들 통합
- 쿠키 유틸리티 함수들 포함
- 기존 호환성 완전 보장

---

이 문서는 프로젝트의 아토믹 디자인 패턴 적용에 대한 가이드입니다. 질문이나 개선사항이 있으면 언제든 문의해 주세요! 🚀
