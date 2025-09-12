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
├── app/                     # Next.js 15 App Router
│   ├── (public)/           # 인증 불필요 페이지 (예정)
│   ├── (auth)/             # 인증 관련 페이지
│   ├── (protected)/        # 인증 필수 페이지
│   └── api/                # API 라우트
├── components/             # 아토믹 디자인 시스템
│   ├── atoms/              # 기본 UI 요소
│   ├── molecules/          # 복합 컴포넌트
│   └── organisms/          # 복잡한 섹션
├── lib/                    # 유틸리티 및 헬퍼
│   └── api/               # API 클라이언트 (CQRS 패턴)
├── hooks/                  # 커스텀 React 훅
├── stores/                 # Zustand 상태 관리
└── types/                  # TypeScript 타입 정의
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
// ✅ 권장: 메인 export
import { Button, Card, AuthHeader } from "@/components";

// ✅ 호환성: 레거시 경로
import { Button } from "@/components/ui/button";

// ✅ 직접: 아토믹 레벨
import { Button } from "@/components/atoms/button";
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

### Core Hooks
- `useAuth` - 인증 상태 관리
- `useToast` - 토스트 알림
- `useApi` - API 호출 헬퍼

### State Management
- `useLoadingState` - 로딩 상태 관리
- `usePagination` - 페이지네이션
- `usePasswordModal` - 비밀번호 모달

### Utility Hooks
- `useDebounce` - 디바운스
- `useLocalStorage` - 로컬스토리지
- `useMediaQuery` - 미디어 쿼리

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

## 🔄 현재 리팩토링 상황 (2025-01-20)

### ✅ 완료된 작업
- 백업 파일 정리 및 디렉토리 구조 통합
- 컴포넌트 중복 제거 (EmptyState, Spinner)
- organisms/index.ts 생성 및 컴포넌트 export 정리
- hooks/index.ts 전체 훅 export 추가
- API 레이어 CQRS 패턴 적용

### 🔜 예정된 작업
- Route 실제 마이그레이션 ((public) 그룹으로 이동)
- API 참조 업데이트 (약 30개 파일)
- 타입 시스템 중앙화
- 성능 최적화 (dynamic import 확대)

## 📚 참고 자료

- [Next.js 15 문서](https://nextjs.org/docs)
- [Tailwind CSS v4](https://tailwindcss.com)
- [Atomic Design](https://atomicdesign.bradfrost.com/)
- [React 19 새 기능](https://react.dev)

---

**Last Updated**: 2025-01-20