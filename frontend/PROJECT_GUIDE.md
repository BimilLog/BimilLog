# 🌱 GrowFarm 프로젝트 종합 가이드

> 비밀로그 (롤링페이퍼 + 커뮤니티) 프로젝트의 전체 설계 방법론 및 개발 가이드

## 📋 목차

- [1. 프로젝트 개요](#1-프로젝트-개요)
- [2. 기술 스택](#2-기술-스택)
- [3. 아키텍처 설계](#3-아키텍처-설계)
- [4. 디자인 시스템](#4-디자인-시스템)
- [5. 컴포넌트 구조](#5-컴포넌트-구조)
- [6. 개발 패턴](#6-개발-패턴)
- [7. 성능 최적화](#7-성능-최적화)
- [8. 개발 워크플로우](#8-개발-워크플로우)

---

## 1. 프로젝트 개요

### 🎯 핵심 서비스

- **롤링페이퍼**: 익명 메시지 작성/공유 플랫폼
- **커뮤니티 게시판**: 사용자 소통 공간
- **카카오 연동**: 간편 로그인 및 공유

### 👥 타겟 사용자

- **주요**: 모바일 사용자 (모바일 퍼스트)
- **보조**: 데스크톱 사용자
- **특징**: 익명성을 중시하는 젊은 사용자층

### 🎨 브랜드 정체성

- **컬러**: Pink-Purple-Indigo 그라디언트
- **톤**: 따뜻하고 친근한 감성
- **키워드**: 익명, 마음전달, 소통, 따뜻함

---

## 2. 기술 스택

### 📱 Frontend

```yaml
Framework: Next.js 14 (App Router)
Language: TypeScript
Styling: Tailwind CSS
UI Components: 아토믹 디자인 + shadcn/ui 기반
State Management: React Hooks + Context
Testing: (미정)
```

### 🖥️ Backend

```yaml
Framework: Spring Boot (Java)
Database: (확인 필요)
Authentication: 카카오 OAuth
Real-time: SSE (Server-Sent Events)
Push Notification: FCM (Firebase Cloud Messaging)
```

### 🔧 개발 도구

```yaml
Package Manager: npm
Code Quality: ESLint + Prettier
Version Control: Git
Deployment: (확인 필요)
```

---

## 3. 아키텍처 설계

### 📁 프로젝트 구조

```
grow_farm/
├── frontend/                 # Next.js 앱
│   ├── app/                 # App Router 페이지들
│   │   ├── admin/          # 관리자 페이지
│   │   ├── auth/           # 인증 페이지
│   │   ├── board/          # 게시판 페이지
│   │   ├── rolling-paper/  # 롤링페이퍼 페이지
│   │   └── ...
│   ├── components/         # 아토믹 디자인 컴포넌트
│   │   ├── atoms/          # 기본 UI 요소
│   │   ├── molecules/      # 조합된 컴포넌트
│   │   ├── organisms/      # 복잡한 섹션
│   │   └── index.ts        # Export 관리
│   ├── hooks/              # React 커스텀 훅
│   ├── lib/                # 유틸리티 및 설정
│   └── types/              # TypeScript 타입 정의
└── backend/                # Spring Boot 앱
    └── src/main/java/jaeik/growfarm/
        ├── controller/     # REST API 컨트롤러
        ├── service/        # 비즈니스 로직
        ├── entity/         # 데이터 모델
        └── ...
```

### 🔄 데이터 플로우

```
사용자 입력 → React Component → Hook → API Call → Spring Controller → Service → Database
                     ↑                                                           ↓
              State Update ← Hook ← API Response ← Controller ← Service ← Database
```

### 🌐 페이지 구조

```
/ (홈페이지)
├── /login (로그인)
├── /board (게시판)
│   ├── /board/write (글작성)
│   └── /board/post/[id] (글상세)
├── /rolling-paper (내 롤링페이퍼)
├── /rolling-paper/[nickname] (공개 롤링페이퍼)
├── /visit (롤링페이퍼 방문)
├── /mypage (마이페이지)
└── /admin (관리자)
```

---

## 4. 디자인 시스템

### 🎨 컬러 팔레트

```css
/* 메인 브랜드 그라디언트 */
Primary Gradient: from-pink-600 via-purple-600 to-indigo-600
Background Gradient: from-pink-50 via-purple-50 to-indigo-50
Button Gradient: from-pink-500 to-purple-600

/* 기능별 그라디언트 */
Feature 1: from-pink-500 to-red-500      /* 익명 메시지 */
Feature 2: from-orange-500 to-yellow-500 /* 카카오 연동 */
Feature 3: from-purple-500 to-indigo-500 /* 다양한 디자인 */
Feature 4: from-green-500 to-teal-500    /* 커뮤니티 */

/* 카드 및 표면 */
Card Base: bg-white/80 backdrop-blur-sm
Card Shadow: shadow-lg hover:shadow-xl
```

### 📱 모바일 퍼스트 원칙

```css
/* 터치 타겟 최소 크기 */
Minimum: 44px × 44px
Recommended: 48px × 48px
Comfortable: 56px × 56px

/* 반응형 브레이크포인트 */
Mobile: 320px ~ 767px (기본)
Tablet: 768px ~ 1023px
Desktop: 1024px+

/* 타이포그래피 */
Mobile Base: 16px (최소 크기)
Line Height: 1.5 (가독성 최적화)
```

### 🧩 컴포넌트 스타일 원칙

```css
/* 일관된 카드 스타일 */
.card-base {
  @apply bg-white/80 backdrop-blur-sm border-0 shadow-lg hover:shadow-xl transition-shadow rounded-lg;
}

/* 일관된 그라디언트 제목 */
.gradient-title {
  @apply bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent;
}

/* 터치 최적화 버튼 */
.touch-button {
  @apply min-h-[44px] px-4 active:scale-[0.98] transition-transform;
}
```

---

## 5. 컴포넌트 구조

### 🧬 아토믹 디자인 적용

#### **Atoms (원자)**

```typescript
// 기본 UI 요소 - 단일 책임
Button, Input, Label, Textarea, Avatar, Badge, Icon, Spinner
KakaoShareButton, Switch

// 사용 예시
<Button variant="gradient" size="lg">
  내 롤링페이퍼 만들기
</Button>
```

#### **Molecules (분자)**

```typescript
// 조합된 컴포넌트 - 특정 기능
Card, Dialog, SearchBox, FormField, Alert, Tabs
AdFitBanner, ResponsiveAdFitBanner, Loading, EmptyState

// 사용 예시
<Card className="card-base">
  <CardHeader>
    <CardTitle className="gradient-title">제목</CardTitle>
  </CardHeader>
  <CardContent>내용</CardContent>
</Card>
```

#### **Organisms (유기체)**

```typescript
// 복잡한 섹션 - 비즈니스 로직 포함
AuthHeader, NotificationBell, BoardSearch, PostList
HomeHero, HomeFeatures, HomeFooter

// 사용 예시
<HomeHero
  isAuthenticated={isAuthenticated}
  onOpenFriendsModal={handleOpenFriendsModal}
/>
```

### 📦 Export 구조

```typescript
// 메인 Export - 추천 방식
import { Button, Card, AuthHeader } from "@/components";

// 호환성 Export - 기존 지원
import { Button } from "@/components/ui/button";

// 아토믹 직접 Import
import { Button } from "@/components/atoms/button";
import { HomeHero } from "@/components/organisms/home/HomeHero";
```

### 🎯 컴포넌트 명명 규칙

```typescript
// Atoms: 명사형
Button, Input, Label, Avatar;

// Molecules: 기능 중심
SearchBox, FormField, AlertDialog, AdFitBanner;

// Organisms: 섹션 중심
AuthHeader, HomeHero, BoardSearch, PostList;

// Pages: 페이지명 + Client/Page
HomeClient, BoardClient, PostDetailPage;
```

---

## 6. 개발 패턴

### 🪝 Hook 패턴

```typescript
// 1. 데이터 관리 Hook
const usePostDetail = (id: string) => {
  // 상태, 로딩, 에러, 데이터 fetch 등
  return { post, loading, error, fetchPost };
};

// 2. 액션 관리 Hook
const useCommentActions = (postId: string, onRefresh: () => void) => {
  // CRUD 액션들
  return { create, update, delete, like };
};

// 3. UI 상태 Hook
const useModal = () => {
  // 모달 열림/닫힘 상태
  return { isOpen, open, close };
};
```

### 📱 모바일/PC 분기 패턴

```typescript
// 1. CSS 클래스 분기
<div className="flex flex-col sm:flex-row gap-4">
  <div className="sm:hidden">모바일 전용</div>
  <div className="hidden sm:block">PC 전용</div>
</div>;

// 2. 컴포넌트 분기
{
  isMobile ? <MobileComponent /> : <DesktopComponent />;
}

// 3. Hook에서 분기
const useResponsive = () => {
  const [isMobile, setIsMobile] = useState(false);
  // window.innerWidth 체크 로직
  return { isMobile, isTablet, isDesktop };
};
```

### 🔐 권한 관리 패턴

```typescript
// 1. 컴포넌트 레벨
const canModify = () => {
  if (post.userName === "익명") return !isAuthenticated;
  return isAuthenticated && user?.userName === post.userName;
};

// 2. 조건부 렌더링
{
  canModify() && <Button onClick={handleDelete}>삭제</Button>;
}

// 3. Hook에서 권한 체크
const usePermissions = (item: Post | Comment) => {
  return { canEdit, canDelete, canView };
};
```

### 🎨 스타일링 패턴

```typescript
// 1. 일관된 카드 스타일
className =
  "bg-white/80 backdrop-blur-sm border-0 shadow-lg hover:shadow-xl transition-shadow rounded-lg";

// 2. 그라디언트 제목
className =
  "text-3xl font-bold bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent";

// 3. 터치 최적화 버튼
className = "min-h-[44px] px-4 active:scale-[0.98] transition-transform";

// 4. 모바일 퍼스트 여백
className = "px-4 py-8 md:px-8 md:py-16";
```

---

## 7. 성능 최적화

### ⚡ Next.js 최적화

```typescript
// 1. Dynamic Import (SSR 문제 해결)
const Editor = dynamic(() => import("@/components/molecules/editor"), {
  ssr: false,
  loading: () => <EditorLoading />,
});

// 2. Image 최적화
import Image from "next/image";
<Image
  src="/logo.png"
  alt="비밀로그"
  width={40}
  height={40}
  priority // LCP 최적화
/>;

// 3. 폰트 최적화
import { Inter } from "next/font/google";
const inter = Inter({ subsets: ["latin"] });
```

### 🔄 상태 관리 최적화

```typescript
// 1. 배치 처리 (알림 시스템)
const useBatchActions = () => {
  const queue = useRef([]);

  const addToQueue = (action) => {
    queue.current.push(action);
  };

  const processBatch = async () => {
    // 5분마다 일괄 처리
  };
};

// 2. 메모이제이션
const MemoizedComponent = React.memo(Component);
const memoizedValue = useMemo(() => computation, [deps]);
const memoizedCallback = useCallback(() => handler, [deps]);

// 3. 조건부 렌더링 최적화
{
  isVisible && <ExpensiveComponent />;
}
```

### 📱 모바일 최적화

```typescript
// 1. 터치 이벤트 최적화
<div className="touch-manipulation select-none">
  <button className="active:scale-[0.98] transition-transform">
    터치 최적화 버튼
  </button>
</div>

// 2. 뷰포트 최적화
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />

// 3. 레이지 로딩
const LazyComponent = lazy(() => import("./Component"));
<Suspense fallback={<Loading />}>
  <LazyComponent />
</Suspense>
```

---

## 8. 개발 워크플로우

### 🛠️ 새 컴포넌트 개발 과정

#### 1단계: 아토믹 레벨 결정

```typescript
// 질문: 이 컴포넌트는 어떤 레벨인가?
// - 더 쪼갤 수 없는 기본 요소? → Atoms
// - 2-3개 Atoms의 조합? → Molecules
// - 복잡한 비즈니스 로직을 포함? → Organisms
```

#### 2단계: 컴포넌트 생성

```typescript
// atoms/new-button.tsx
interface NewButtonProps {
  variant?: "default" | "gradient";
  size?: "sm" | "md" | "lg";
  children: React.ReactNode;
  onClick?: () => void;
}

export const NewButton: React.FC<NewButtonProps> = ({
  variant = "default",
  size = "md",
  children,
  onClick,
}) => {
  const baseStyles = "transition-all duration-200 rounded-lg font-medium";
  const variantStyles = {
    default: "bg-gray-200 text-gray-800 hover:bg-gray-300",
    gradient:
      "bg-gradient-to-r from-pink-500 to-purple-600 text-white hover:from-pink-600 hover:to-purple-700",
  };
  const sizeStyles = {
    sm: "px-3 py-1.5 text-sm min-h-[36px]",
    md: "px-4 py-2 text-base min-h-[44px]",
    lg: "px-6 py-3 text-lg min-h-[48px]",
  };

  return (
    <button
      className={`${baseStyles} ${variantStyles[variant]} ${sizeStyles[size]} active:scale-[0.98]`}
      onClick={onClick}
    >
      {children}
    </button>
  );
};
```

#### 3단계: Export 추가

```typescript
// components/atoms/index.ts
export { NewButton } from "./new-button";

// components/index.ts
export { NewButton } from "./atoms/new-button";
```

#### 4단계: 사용 예시 작성

```typescript
// 기본 사용
<NewButton variant="gradient" size="lg" onClick={handleClick}>
  내 롤링페이퍼 만들기
</NewButton>

// 조건부 사용
<NewButton
  variant={isActive ? "gradient" : "default"}
  size={isMobile ? "md" : "lg"}
>
  {isLoading ? "로딩 중..." : "확인"}
</NewButton>
```

### 🔧 기존 컴포넌트 리팩토링 과정

#### 1단계: 복잡도 분석

```typescript
// 체크리스트:
// □ 파일이 200줄 이상인가?
// □ useState가 5개 이상인가?
// □ useEffect가 3개 이상인가?
// □ props가 10개 이상인가?
// □ 모바일/PC 분기가 복잡한가?
```

#### 2단계: 로직 분리

```typescript
// 커스텀 훅으로 분리
const useComponentLogic = () => {
  // 상태 관리 로직
  // 이벤트 핸들러
  // 사이드 이펙트
  return { state, actions, handlers };
};

// 컴포넌트는 UI만 담당
const Component = () => {
  const { state, actions, handlers } = useComponentLogic();
  return <div>UI만 담당</div>;
};
```

#### 3단계: UI 컴포넌트 분리

```typescript
// 긴 JSX를 작은 컴포넌트로 분리
const ComponentHeader = ({ title, actions }) => <header>...</header>;
const ComponentContent = ({ data }) => <main>...</main>;
const ComponentFooter = ({ onSubmit }) => <footer>...</footer>;

const MainComponent = () => (
  <div>
    <ComponentHeader />
    <ComponentContent />
    <ComponentFooter />
  </div>
);
```

### 📝 코딩 컨벤션

#### TypeScript

```typescript
// 1. 인터페이스 명명
interface ComponentProps {
  // 컴포넌트 Props
  title: string;
  onSubmit: () => void;
}

interface ApiResponse<T> {
  // API 응답 타입
  success: boolean;
  data?: T;
  error?: string;
}

// 2. 컴포넌트 정의
export const Component: React.FC<ComponentProps> = ({ title, onSubmit }) => {
  // 구현
};

// 3. 훅 정의
export const useCustomHook = (param: string) => {
  // 구현
  return { data, loading, error };
};
```

#### 스타일링

```typescript
// 1. Tailwind 클래스 순서
// Layout → Flexbox/Grid → Spacing → Sizing → Typography → Colors → Effects
className =
  "flex flex-col gap-4 p-4 w-full text-lg text-gray-800 bg-white shadow-lg rounded-lg";

// 2. 조건부 스타일링
const buttonStyles = cn(
  "base-styles",
  variant === "primary" && "primary-styles",
  size === "large" && "large-styles",
  className
);

// 3. 반응형 우선순위
className = "text-sm md:text-base lg:text-lg"; // 모바일 → 태블릿 → 데스크톱
```

### 🚀 배포 및 테스트

#### 개발 환경 체크리스트

```bash
# 1. 코드 품질 체크
npm run lint      # ESLint 검사
npm run type-check # TypeScript 검사

# 2. 빌드 테스트
npm run build     # 프로덕션 빌드
npm run start     # 프로덕션 모드 실행

# 3. 성능 체크
# - Lighthouse 점수 확인
# - Core Web Vitals 측정
# - 모바일 성능 테스트
```

#### 브라우저 호환성

```typescript
// 지원 브라우저
// - Chrome 90+
// - Safari 14+
// - Firefox 88+
// - Samsung Internet 14+

// PWA 기능
// - 웹 앱 설치
// - 오프라인 지원 (Service Worker)
// - 푸시 알림 (FCM)
```

---

## 💡 개발 팁 및 베스트 프랙티스

### 🎯 효율적인 개발을 위한 체크리스트

#### 새로운 기능 개발 시

- [ ] 모바일 우선으로 디자인했는가?
- [ ] 아토믹 디자인 원칙을 따랐는가?
- [ ] 재사용 가능한 컴포넌트로 만들었는가?
- [ ] TypeScript 타입을 정확히 정의했는가?
- [ ] 접근성(a11y)을 고려했는가?
- [ ] 성능 최적화를 적용했는가?

#### 코드 리뷰 시

- [ ] 컴포넌트가 단일 책임 원칙을 따르는가?
- [ ] 비즈니스 로직이 UI와 분리되어 있는가?
- [ ] 일관된 네이밍 컨벤션을 사용했는가?
- [ ] 적절한 에러 처리가 되어 있는가?
- [ ] 메모리 누수 가능성은 없는가?

### 🔍 디버깅 가이드

#### 일반적인 문제들

```typescript
// 1. Hydration 에러 (SSR)
// 해결: dynamic import 사용
const ClientOnlyComponent = dynamic(() => import("./Component"), {
  ssr: false,
});

// 2. 무한 리렌더링
// 해결: dependency 배열 점검
useEffect(() => {
  // logic
}, [dependency]); // dependency 확인 필수

// 3. 상태 업데이트 안됨
// 해결: 불변성 유지
setState((prev) => ({ ...prev, newValue })); // 새 객체 생성

// 4. CSS 스타일 적용 안됨
// 해결: Tailwind 클래스명 확인, 우선순위 점검
className = "!important-style"; // 강제 적용 시
```

### 🎨 디자인 일관성 유지

#### 색상 사용 가이드

```css
/* 메인 브랜드 컬러 - 핵심 액션에만 사용 */
bg-gradient-to-r from-pink-500 to-purple-600

/* 보조 컬러 - 일반 버튼, 링크 */
text-purple-600 hover:text-purple-700

/* 중립 컬러 - 텍스트, 배경 */
text-gray-600, bg-gray-50

/* 상태 컬러 - 피드백 */
text-green-600 (성공), text-red-600 (에러), text-yellow-600 (경고)
```

#### 컴포넌트 스타일 가이드

```typescript
// 카드형 컴포넌트 기본 스타일
const cardBaseStyle =
  "bg-white/80 backdrop-blur-sm border-0 shadow-lg hover:shadow-xl transition-shadow rounded-lg";

// 버튼 기본 스타일
const buttonBaseStyle =
  "px-4 py-2 min-h-[44px] rounded-lg font-medium transition-all duration-200 active:scale-[0.98]";

// 입력 필드 기본 스타일
const inputBaseStyle =
  "w-full px-3 py-2 min-h-[44px] border-2 border-gray-200 rounded-lg focus:border-purple-400 focus:outline-none";
```

---

## 📚 참고 자료

### 공식 문서

- [Next.js Documentation](https://nextjs.org/docs)
- [React Documentation](https://react.dev)
- [Tailwind CSS](https://tailwindcss.com/docs)
- [TypeScript Handbook](https://www.typescriptlang.org/docs)

### 디자인 시스템

- [Atomic Design by Brad Frost](https://atomicdesign.bradfrost.com)
- [Material Design Guidelines](https://material.io/design)
- [Apple Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines)

### 성능 최적화

- [Web.dev Performance](https://web.dev/performance)
- [Next.js Performance](https://nextjs.org/docs/advanced-features/measuring-performance)
- [React Performance](https://react.dev/learn/render-and-commit)

---

**📝 이 문서는 프로젝트 발전에 따라 지속적으로 업데이트됩니다.**
**💬 질문이나 개선 제안이 있으시면 언제든 말씀해 주세요!**
