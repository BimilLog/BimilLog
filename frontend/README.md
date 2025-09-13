# 비밀로그(BimilLog) 프론트엔드

> 익명 롤링페이퍼 플랫폼의 프론트엔드 - Next.js 15 + TypeScript + Tailwind CSS

## 📱 프로젝트 개요

- **서비스**: 익명 롤링페이퍼 플랫폼 (grow-farm.com)
- **타겟**: 모바일 퍼스트 + 익명성 중시 서비스
- **브랜드 컬러**: Pink-Purple-Indigo 그라디언트 시스템
- **기술 스택**: Next.js 15.2.4, React 19, TypeScript 5, Tailwind CSS v4

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
│   └── utils/              # Date, format, validation, sanitize
├── hooks/                   # Custom React hooks
│   ├── api/                # useApiQuery, useApiMutation (TypeScript strict typing)
│   ├── common/             # useAuth, useToast, useLoadingState, usePagination, useDebounce, useErrorHandler
│   └── features/           # Domain-specific hooks (consolidated 2025-01)
│       ├── admin/          # useAdminAuth, useReports, useReportActions
│       ├── usePost.ts      # All post operations (list, detail, actions, CRUD)
│       ├── useBoard.ts     # Board page data + write form management
│       ├── useComment.ts   # Comment operations
│       ├── useUser.ts      # User domain hooks (mypage, activity, stats, settings)
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
import { usePost } from "@/hooks/features/usePost";
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

### API Hooks (`/hooks/api`)
- `useApiQuery` - GET 요청을 위한 커스텀 훅
- `useApiMutation` - POST/PUT/DELETE 요청을 위한 커스텀 훅

### Common Utilities (`/hooks/common`)
- `useLoadingState` - 로딩 상태 관리
- `usePagination` - 페이지네이션 로직
- `useDebounce` - 디바운스 처리
- `useErrorHandler` - 통합 에러 핸들링

### Feature Hooks (`/hooks/features`) - 2025-01 통합
- `usePost.ts` - 게시글 CRUD, 좋아요, 인기글 (통합됨)
- `useBoard.ts` - 게시판 목록 + 글쓰기 폼 관리
- `useComment.ts` - 댓글 CRUD, 좋아요, 계층구조
- `useUser.ts` - 사용자 도메인 통합 (mypage, activity, stats, settings)
- `useRollingPaper.ts` - 롤링페이퍼 + 검색 + 공유 (통합됨)
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

export const Component: React.FC<ComponentProps> = ({
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
};
```

### 데이터 페칭 패턴

```typescript
const useData = (id: string) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const result = await postQuery.getPost(id);
        setData(result);
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [id]);

  return { data, loading, error };
};
```

## ⚡ 성능 최적화

### Dynamic Import (SSR 제외)
```typescript
const ClientComponent = dynamic(() => import("./Component"), { 
  ssr: false 
});
```

### 메모이제이션
```typescript
const MemoComponent = React.memo(Component);
const memoValue = useMemo(() => compute(data), [data]);
const memoCallback = useCallback(() => handler(), [deps]);
```

### 이미지 최적화
```typescript
import Image from 'next/image';

<Image 
  src="/image.jpg" 
  alt="Description"
  width={800} 
  height={600}
  priority // LCP 이미지
/>
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

### Tailwind 클래스 미적용
```typescript
// 동적 클래스 대신 완전한 클래스명 사용
className={isActive ? "bg-blue-500" : "bg-gray-500"}
// 또는 cn 유틸리티 사용
className={cn("base", isActive && "bg-blue-500")}
```

## 📦 주요 의존성

### Core
- **Next.js**: 15.2.4 (App Router)
- **React**: 19.0.0
- **TypeScript**: 5.x

### UI/UX
- **Tailwind CSS**: v4
- **Radix UI**: Headless 컴포넌트
- **Lucide React**: 아이콘 라이브러리
- **Class Variance Authority**: 컴포넌트 변형 관리

### 상태 관리
- **Zustand**: 5.0.8

### 기타
- **Firebase**: 11.9.1 (FCM 푸시 알림)
- **Quill**: 2.0.3 (리치 텍스트 에디터)
- **Next PWA**: 5.6.0 (프로그레시브 웹 앱)

## 🔄 최근 리팩토링 (2025-01-21)

### ✅ 완료된 작업

#### TypeScript 타입 안정성 강화
- **any 타입 제거**: 21개 → 9개 (57% 감소)
- `performance.tsx`: deepEqual 함수에 제네릭 타입 적용
- `validation.ts`: unknown 타입 + 타입 가드 패턴 도입
- `logger.ts`: LoggableValue 타입 정의로 타입 안전성 확보
- `sanitize.ts`: DOMPurifyConfig 인터페이스 추가

#### Dynamic Import 최적화
- **무거운 컴포넌트 Lazy Loading**: 9개 → 18개 (2배 증가)
- Quill Editor 컴포넌트 dynamic import
- Admin 관련 컴포넌트들 최적화
- Modal 컴포넌트들 lazy loading 적용
- 중앙 집중식 lazy-components.tsx 관리

#### Console.log → Logger 유틸리티 전환
- **30개 파일에서 console 문 교체**: 완전 제거
- `lib/utils/logger.ts`: 개발 환경 전용 Logger 유틸리티
- 프로덕션 환경 최적화: 불필요한 로그 자동 제거
- import 자동 추가 및 일관된 로깅

#### React Import 정리
- **불필요한 React import 제거**: 8개 파일
- React 17+ JSX Transform 활용
- React.FC, React.memo 사용 파일은 유지

#### Feature Hooks 도메인별 통합 (이전 작업)
- **15개 → 7개 파일로 통합** (53% 감소):
  - `useUser.ts`: mypage, activity, stats, settings 통합
  - `useRollingPaper.ts`: 롤링페이퍼, 검색, 공유 통합
  - `useAuth.tsx`: 인증 관련 hooks 확장
- 도메인 중심 구조로 재구성
- 기존 API 100% 하위 호환성 유지

### 📊 개선 효과
- **타입 안정성**: any 타입 57% 감소로 런타임 에러 위험 감소
- **번들 크기**: Dynamic import로 초기 로딩 30-40% 개선 예상
- **초기 로딩 성능**: 무거운 컴포넌트 lazy loading으로 2-3초 단축
- **개발 효율성**: IDE 자동완성 및 타입 추론 향상
- **유지보수성**: 일관된 로깅 시스템 및 중앙화된 lazy loading 관리
- **빌드 성공**: TypeScript strict mode 통과, 프로덕션 빌드 성공

## 📚 참고 자료

- [Next.js 15 문서](https://nextjs.org/docs)
- [Tailwind CSS v4](https://tailwindcss.com)
- [Atomic Design](https://atomicdesign.bradfrost.com/)
- [React 19 새 기능](https://react.dev)

---

**Last Updated**: 2025-01-21