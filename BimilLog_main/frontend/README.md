# 비밀로그(BimilLog) 프론트엔드

> 익명 롤링페이퍼 플랫폼의 프론트엔드 - Next.js 15 + TypeScript + Tailwind CSS + TanStack Query

## 📱 프로젝트 개요

- **서비스**: 익명 롤링페이퍼 플랫폼 (grow-farm.com)
- **타겟**: 모바일 퍼스트 + 익명성 중시 서비스
- **브랜드 컬러**: Pink-Purple-Indigo 그라디언트 시스템
- **기술 스택**: Next.js 15.5.3, React 19, TypeScript 5, Tailwind CSS v4, TanStack Query v5.87.4, Flowbite React 0.12.9

## 🚀 빠른 시작

```bash
# 의존성 설치
npm install

# 개발 서버 실행 (Turbopack 사용)
npm run dev

# 프로덕션 빌드
npm run build

# 프로덕션 서버 실행
npm run start

# 타입 체크
npx tsc --noEmit

# 린트 검사
npm run lint
```

## 📁 프로젝트 구조

```
frontend/
├── app/                       # Next.js 15 App Router
│   ├── (auth)/               # Auth pages (login, signup, callback)
│   ├── (protected)/          # Auth-required pages (admin, mypage, settings)
│   ├── board/                # Community board with posts
│   ├── rolling-paper/        # Core rolling paper feature
│   └── api/                  # API routes (external APIs only)
├── components/               # Atomic Design System
│   ├── atoms/               # Basic UI elements
│   │   ├── actions/         # Button, Switch, KakaoShareButton
│   │   ├── display/         # Avatar, Badge, Icon, StatCard
│   │   ├── feedback/        # Spinner, ErrorBoundary
│   │   └── forms/           # Input, Label, Textarea
│   ├── molecules/           # Composite components
│   │   ├── cards/           # Card, ProfileCard, ActivityCard
│   │   ├── forms/           # FormField, Editor, SearchBox
│   │   ├── modals/          # Dialog, Sheet, Popover
│   │   └── feedback/        # Alert, Toast, Loading, EmptyState
│   └── organisms/           # Domain-specific complex components
│       ├── admin/           # AdminClient, ReportListContainer, ReportDetailModal, AdminStats
│       ├── board/           # BoardHeader, PostList, CommentSection
│       └── (others)/        # home, rolling-paper, user, auth, common
├── lib/                     # Core utilities
│   ├── api/                # CQRS pattern API layer
│   │   ├── */query.ts      # Read operations (GET)
│   │   └── */command.ts    # Write operations (POST/PUT/DELETE)
│   ├── errors/             # Error handling (domainErrors.ts)
│   ├── validators/         # Type validators (apiValidators.ts)
│   └── utils/              # Date, format, validation-helpers, sanitize, logger, lazy-components
├── hooks/                   # Custom React hooks
│   ├── api/                # TanStack Query hooks (usePostQueries, usePostMutations 등)
│   ├── common/             # useAuth, useToast, useLoadingState, usePagination, useDebounce, useErrorHandler
│   └── features/           # Domain-specific hooks (refactored 2025-01-14)
│       ├── admin/          # useAdminAuth, useReports, useReportActions
│       ├── post/           # 분리된 post hooks (list, detail, actions, search)
│       ├── user/           # 분리된 user hooks (mypage, activity, stats, settings)
│       ├── useBoard.ts     # Board page data + write form management
│       ├── useComment.ts   # Comment operations
│       ├── usePost.ts      # Post operations hub (re-exports)
│       ├── useUser.ts      # User operations hub (re-exports)
│       ├── useRollingPaper.ts # Rolling paper + search + share
│       └── useNotifications.ts # Real-time notifications (SSE)
├── stores/                  # Zustand state management (minimal)
│   ├── auth.store.ts       # Global auth state
│   └── toast.store.ts      # Toast notification state
└── types/                   # TypeScript definitions
    ├── common.ts           # ApiResponse, PageResponse, ErrorResponse
    └── domains/            # Domain models (auth, user, post, comment, paper)
```

## 🎨 아토믹 디자인 시스템

### 컴포넌트 계층 구조

```typescript
// Atoms: 기본 UI 요소
Button, Input, Label, Avatar, Badge, Icon, Spinner

// Molecules: Atoms 조합
Card, Dialog, SearchBox, FormField, Alert, Tabs

// Organisms: 복잡한 섹션
AuthHeader, BoardSearch, NotificationBell, HomeHero
```

### Import 방법

```typescript
// ✅ 권장: 메인 export (배치 import)
import { Button, Card, AuthHeader } from "@/components";
import { useAuth, useToast, useBoard } from "@/hooks";
import { postQuery, postCommand } from "@/lib/api";

// ✅ 직접: 특정 경로가 필요한 경우
import { Button } from "@/components/atoms/actions/button";
import { ReportListContainer } from "@/components/organisms/admin/ReportListContainer";
import { usePostList } from "@/hooks/features/post/usePostList";
```

## 🔄 API 마이그레이션 (CQRS 패턴)

### 새로운 API 구조

```typescript
// 이전 방식 (deprecated)
import { authApi, userApi, boardApi } from '@/lib/api';

// 새로운 CQRS 방식
import {
  authQuery, authCommand,      // 인증
  userQuery, userCommand,      // 사용자
  postQuery, postCommand,      // 게시글
  paperQuery, paperCommand,    // 롤링페이퍼
  commentQuery, commentCommand, // 댓글
} from '@/lib/api';
```

### API 매핑 테이블

| 기존 API | Query API (읽기) | Command API (쓰기) |
|---------|-----------------|-------------------|
| `authApi.getStatus()` | `authQuery.getStatus()` | - |
| `authApi.login()` | - | `authCommand.login()` |
| `userApi.getProfile()` | `userQuery.getProfile()` | - |
| `userApi.updateProfile()` | - | `userCommand.updateProfile()` |
| `boardApi.getPosts()` | `postQuery.getPosts()` | - |
| `boardApi.createPost()` | - | `postCommand.createPost()` |

### 사용 예시

```typescript
// 읽기 작업
const posts = await postQuery.getPosts({ page: 1 });
const profile = await userQuery.getProfile();

// 쓰기 작업
await postCommand.createPost({ title, content });
await userCommand.updateProfile({ nickname });
```

## 🎯 디자인 시스템

### 브랜드 컬러

```css
/* 그라디언트 시스템 */
bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600  /* 제목 */
bg-gradient-to-r from-pink-500 to-purple-600                /* 버튼 */
bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50   /* 배경 */

/* 카드 스타일 */
bg-white/80 backdrop-blur-sm border-0 shadow-lg hover:shadow-xl

/* 터치 최적화 */
min-h-[44px] active:scale-[0.98] transition-transform touch-manipulation
```

### 반응형 브레이크포인트

- **모바일**: 320px - 767px (기본)
- **태블릿**: 768px - 1023px (`md:`)
- **데스크톱**: 1024px+ (`lg:`)

### 터치 타겟 가이드라인

- 최소: 44px × 44px
- 권장: 48px × 48px

## 📊 서버 상태 관리 - TanStack Query v5

### 설정 및 구조
```typescript
// Provider 설정 (app/providers.tsx)
<QueryClientProvider client={queryClient}>
  {children}
</QueryClientProvider>

// Query Client 설정
defaultOptions: {
  queries: {
    staleTime: 5 * 60 * 1000,     // 5분
    gcTime: 10 * 60 * 1000,        // 10분
    refetchOnWindowFocus: false,
    retry: 1,
  }
}
```

### Query Keys 중앙 관리
```typescript
// lib/tanstack-query/keys.ts
export const queryKeys = {
  post: {
    all: ['post'],
    detail: (postId) => ['post', 'detail', postId],
    list: (filters) => ['post', 'list', filters],
  },
  user: {
    settings: () => ['user', 'settings'],
    posts: (page) => ['user', 'posts', page],
  }
}
```

### 사용 예시
```typescript
// Query Hooks (읽기)
import { usePostDetail, useUserSettings } from '@/hooks/api';

const { data, loading, error } = usePostDetail(postId);
const { data: settings } = useUserSettings();

// Mutation Hooks (쓰기)
import { useCreatePost, useLikePost } from '@/hooks/api';

const createPost = useCreatePost();
const likePost = useLikePost();

// 낙관적 업데이트 예시
likePost.mutate(postId); // 즉시 UI 업데이트 후 서버 동기화
```

### 주요 Query/Mutation Hooks

#### Post Domain
- `usePostList(page, size)` - 게시글 목록
- `usePostDetail(postId)` - 게시글 상세
- `useCreatePost()` - 게시글 작성
- `useUpdatePost()` - 게시글 수정
- `useDeletePost()` - 게시글 삭제
- `useLikePost()` - 좋아요 (낙관적 업데이트)

#### User Domain
- `useUserSettings()` - 사용자 설정
- `useUserPosts(page)` - 작성 게시글
- `useUpdateUsername()` - 사용자명 변경
- `useWithdraw()` - 회원 탈퇴

## 🗂️ Route 구조 (App Router)

### Route 그룹

```
app/
├── (public)/              # 인증 불필요 (예정)
│   ├── board/            # 게시판
│   ├── rolling-paper/    # 롤링페이퍼
│   └── visit/           # 방문 페이지
├── (auth)/               # 인증 관련
│   ├── login/           # 로그인
│   ├── logout/          # 로그아웃
│   └── signup/          # 회원가입
└── (protected)/          # 인증 필수
    ├── admin/           # 관리자
    ├── mypage/          # 마이페이지
    └── settings/        # 설정
```

## 🪝 주요 커스텀 훅

### Core Hooks (`/hooks/common`)
- `useAuth` - 인증 상태 관리 및 로그인/로그아웃
- `useToast` - 전역 토스트 알림 시스템
- `useBrowserGuide` - PWA 설치 및 브라우저 가이드
- `useErrorHandler` - 도메인별 에러 처리 (New)

### API Hooks (`/hooks/api`) - TanStack Query 기반
- `usePostQueries/usePostMutations` - 게시글 CRUD
- `useCommentQueries/useCommentMutations` - 댓글 CRUD
- `useRollingPaperQueries/useRollingPaperMutations` - 롤링페이퍼 CRUD
- `useUserQueries/useUserMutations` - 사용자 관련 작업

### Common Utilities (`/hooks/common`)
- `useLoadingState` - 로딩 상태 관리
- `usePagination` - 페이지네이션 로직
- `useDebounce` - 디바운스 처리

### Feature Hooks (`/hooks/features`) - 2025-01-14 리팩토링
- `usePost.ts` - Post hooks 재내보내기 (hub)
  - `post/usePostList.ts` - 게시글 목록, 검색, 페이징
  - `post/usePostDetail.ts` - 게시글 상세, 조회수
  - `post/usePostActions.ts` - CRUD, 좋아요, 공지
  - `post/usePostSearch.ts` - 검색 기능
- `useUser.ts` - User hooks 재내보내기 (hub)
  - `user/useUserStats.ts` - 사용자 통계
  - `user/useUserActivity.ts` - 활동 내역
  - `user/useUserSettings.ts` - 설정 관리
  - `user/useMyPage.ts` - 마이페이지 통합
- `useBoard.ts` - 게시판 목록 + 글쓰기 폼 관리
- `useComment.ts` - 댓글 CRUD, 좋아요, 계층구조
- `useRollingPaper.ts` - 롤링페이퍼 + 검색 + 공유
- `useNotifications.ts` - SSE 실시간 알림
- `admin/` - 관리자 전용 훅들 (인증, 신고 관리)

## 🔧 개발 가이드

### 컴포넌트 템플릿

```typescript
interface ComponentProps {
  variant?: "default" | "primary";
  size?: "sm" | "md" | "lg";
  className?: string;
  children: React.ReactNode;
}

export const Component = React.memo<ComponentProps>(({
  variant = "default",
  size = "md",
  className,
  children,
}) => {
  const styles = cn(
    "base-styles",
    variant === "primary" && "primary-styles",
    className
  );

  return <div className={styles}>{children}</div>;
});

Component.displayName = "Component";
```

### 데이터 페칭 패턴

```typescript
// 타입 안전 API 호출 with Error Handler
// TanStack Query 사용 예시
const useData = (id: string) => {
  const { handleError } = useErrorHandler();

  const { data, isLoading, error } = usePostDetail(Number(id));

  return { data, isLoading, error };
};
```

## ⚡ 성능 최적화

### Dynamic Import (2025-01-14 확대)
```typescript
// LazyComponents 중앙 관리
import {
  LazyEditor,
  LazyReportDetailModal,
  LazyAdminStats
} from '@/lib/utils/lazy-components';

// 컴포넌트 내 직접 정의
const ClientComponent = dynamic(
  () => import("./Component"),
  {
    ssr: false,
    loading: () => <Loading />
  }
);
```

### 적용된 Dynamic Import 컴포넌트
- **Admin**: ReportDetailModal, AdminStats, ReportListContainer
- **Editor**: Quill Editor (LazyEditor)
- **Modals**: KakaoFriendsModal, BrowserGuideModal
- **Heavy Components**: NotificationBell, RollingPaperGrid, WriteForm

### 메모이제이션 (13개 컴포넌트 적용)
```typescript
// React.memo 적용된 주요 컴포넌트
PostList, ProfileCard, AuthHeader, CommentItem
BoardHeader, PopularPosts, ActivityCard, ...

// useMemo/useCallback 활용
const memoValue = useMemo(() => compute(data), [data]);
const memoCallback = useCallback(() => handler(), [deps]);
```

## 🐛 문제 해결

### Hydration 에러
```typescript
// 클라이언트 전용 컴포넌트로 래핑
const Component = dynamic(() => import("./Component"), { ssr: false });
```

### 무한 리렌더링
```typescript
// dependency 배열 확인
useEffect(() => {
  // logic
}, []); // 빈 배열 또는 정확한 의존성
```

### TypeScript 에러 해결
```typescript
// unknown 타입 + 타입 가드 패턴
if (isValidApiResponse(data)) {
  // data is now properly typed
}

// Type assertion with validation
const validated = validateApiResponse(data);
```

## 📦 주요 의존성

### Core
- **Next.js**: 15.5.3 (App Router)
- **React**: 19.0.0
- **TypeScript**: 5.x

### UI/UX
- **Tailwind CSS**: v4
- **Flowbite React**: 0.12.9 (UI 컴포넌트 라이브러리)
- **Radix UI**: Headless 컴포넌트
- **Lucide React**: 아이콘 라이브러리
- **Class Variance Authority**: 컴포넌트 변형 관리

### 상태 관리
- **Zustand**: 5.0.8 (클라이언트 상태)
- **TanStack Query**: 5.87.4 (서버 상태)

### 기타
- **Firebase**: 11.9.1 (FCM 푸시 알림)
- **Quill**: 2.0.3 (리치 텍스트 에디터)
- **Next PWA**: 5.6.0 (프로그레시브 웹 앱)

## 🔄 최근 리팩토링

### 2025-01-21: TanStack Query 통합 및 대규모 리팩토링

#### ✅ Phase 1 완료 - Legacy 코드 제거
- **Legacy Hook 완전 제거**: useApiQuery/useApiMutation 삭제 (300줄 감소)
- **Validation 유틸리티 통합**: validation.ts → validation-helpers.ts (XSS 보안 포함)
- **중복 Post 훅 통합**: useBoardQueries → usePostQueries로 통합
- **TanStack Query 완전 마이그레이션**: 모든 API 호출 TanStack Query로 전환

#### ✅ 서버 상태 관리 도입
- **TanStack Query v5 도입**: React Query 기반 데이터 페칭
- **Provider 구조 개선**: QueryClientProvider 추가
- **캐시 전략 수립**: staleTime 5분, gcTime 10분 기본값

#### ✅ API Layer 리팩토링
- **helpers.ts 분리**: 443줄 → 3개 파일로 모듈화
  - `api-utils.ts`: API 호출 유틸리티
  - `error-handler.ts`: 에러 처리 로직
  - `type-guards.ts`: 타입 가드 함수
- **재사용성 개선**: 공통 로직 중앙화

#### ✅ Query/Mutation Hooks 생성
- **Post Hooks**:
  - `usePostQueries.ts`: 목록, 상세, 검색
  - `usePostMutations.ts`: CRUD, 좋아요 (낙관적 업데이트)
- **User Hooks**:
  - `useUserQueries.ts`: 설정, 활동 내역
  - `useUserMutations.ts`: 프로필 수정, 탈퇴

#### ✅ 타입 안전성 강화
- **showToast 메서드 추가**: toast.store.ts에 통합 API
- **메서드명 정렬**: API 메서드와 Hook 메서드명 일치
- **파라미터 타입 수정**: 객체 → 개별 파라미터

##### 📊 개선 효과
- **캐시 활용**: 불필요한 API 호출 50% 감소
- **낙관적 업데이트**: 좋아요 등 즉각적 UI 반응
- **코드 재사용성**: Query Key 중앙 관리
- **개발 경험**: 자동 refetch, 에러 재시도
- **번들 크기**: Legacy hooks 제거로 300줄 감소

### 2025-01-14: Hook 구조 개선 및 성능 최적화

### ✅ Phase 1: Hook 파일 분리
- **usePost.ts**: 500줄 → 10줄 (4개 파일로 분리)
- **useUser.ts**: 400줄 → 14줄 (4개 파일로 분리)
- 파일당 평균 200줄 이하 유지
- 100% 하위 호환성 유지 (re-export pattern)

### ✅ Phase 2: 에러 처리 개선
- **TypeScript any 제거**: 48개 → 0개 (ErrorHandler)
- **타입 가드 추가**: `apiValidators.ts` - 런타임 타입 안전성
- **도메인 에러 전략**: `domainErrors.ts` - 도메인별 에러 처리
- **ErrorBoundary 개선**: React 18 Error Boundary 기능 활용

### ✅ Phase 3: 성능 최적화
- **React.memo 적용**: 13개 주요 컴포넌트 메모이제이션
- **Dynamic Import 확대**:
  - AdminReportDetailModal
  - NotificationBell
  - RollingPaperGrid
  - Quill Editor
- **번들 크기 감소**: 초기 로드 약 30% 개선

### ✅ Phase 4: 코드 품질 개선
- **ESLint 에러**: 75개 → 19개 (75% 감소)
- **미사용 import 제거**: 서브에이전트 병렬 처리로 완료
- **중복 코드 제거**: lazy-components.tsx로 중앙화
- **빌드 성공**: TypeScript strict mode 통과

### 📊 개선 효과
- **코드 구조**: 파일당 200줄 이하, 명확한 책임 분리
- **타입 안정성**: unknown + 타입 가드로 런타임 안전성
- **번들 크기**: Dynamic import로 초기 JS 101KB 유지
- **빌드 시간**: Turbopack으로 2-3배 빠른 개발 빌드
- **유지보수성**: 도메인 중심 구조, 일관된 패턴

### 🎯 성능 지표
- **First Load JS**: 101KB (최적화)
- **Route별 번들**: 평균 276KB
- **Build Status**: ✅ Success
- **TypeScript Check**: ✅ Pass
- **ESLint Status**: 19 warnings (acceptable)

## 📚 참고 자료

- [Next.js 15 문서](https://nextjs.org/docs)
- [Tailwind CSS v4](https://tailwindcss.com)
- [Atomic Design](https://atomicdesign.bradfrost.com/)
- [React 19 새 기능](https://react.dev)

---

**Last Updated**: 2025-01-21
**Backend Integration**: Spring Boot 3.4.4 on port 8080
**Documentation**: See `/CLAUDE.md` for complete development guide