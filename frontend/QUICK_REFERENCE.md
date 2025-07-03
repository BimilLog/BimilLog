# ⚡ 빠른 참조 가이드

> 개발 작업 시 즉시 참조할 수 있는 핵심 패턴 및 정보

## 🎯 프로젝트 정체성

- **서비스**: 롤링페이퍼(익명 메시지) + 커뮤니티 게시판
- **타겟**: 모바일 퍼스트 + 익명성 중시
- **브랜드**: Pink-Purple-Indigo 그라디언트

## 🎨 핵심 스타일

```css
/* 브랜드 그라디언트 */
bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600  /* 제목 */
bg-gradient-to-r from-pink-500 to-purple-600                /* 버튼 */
bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50   /* 배경 */

/* 표준 카드 */
bg-white/80 backdrop-blur-sm border-0 shadow-lg hover:shadow-xl transition-shadow rounded-lg

/* 터치 버튼 */
min-h-[44px] px-4 active:scale-[0.98] transition-transform
```

## 🧬 컴포넌트 Import

```typescript
// ✅ 권장: 메인 export
import { Button, Card, AuthHeader } from "@/components";

// ✅ 호환성: 기존 경로
import { Button } from "@/components/ui/button";
```

## 📱 반응형 원칙

```css
/* 브레이크포인트 */
320px-767px   /* 모바일 (기본) */
768px-1023px  /* 태블릿 (md:) */
1024px+       /* 데스크톱 (lg:) */

/* 터치 타겟 */
최소 44px × 44px
권장 48px × 48px
```

## 🪝 표준 훅 패턴

```typescript
// 데이터 훅
const useData = (id: string) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchData = async () => {
    /* API 호출 */
  };

  return { data, loading, error, refetch: fetchData };
};

// 액션 훅
const useActions = (onRefresh: () => void) => {
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleAction = async (data: any) => {
    setIsSubmitting(true);
    try {
      await api.action(data);
      await onRefresh();
    } finally {
      setIsSubmitting(false);
    }
  };

  return { handleAction, isSubmitting };
};
```

## 🎯 컴포넌트 템플릿

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
  className = "",
  children,
}) => {
  const styles = cn(
    "base-styles",
    variant === "primary" && "primary-styles",
    size === "lg" && "large-styles",
    className
  );

  return <div className={styles}>{children}</div>;
};
```

## 🔐 권한 체크 패턴

```typescript
const usePermissions = (item: Post | Comment) => {
  const { user, isAuthenticated } = useAuth();

  const canModify = () => {
    if (!item) return false;

    // 익명: 비회원만 수정 가능
    if (item.userName === "익명" || item.userName === null) {
      return !isAuthenticated;
    }

    // 회원: 본인만 수정 가능
    return isAuthenticated && user?.userName === item.userName;
  };

  return { canModify: canModify() };
};
```

## ⚡ 성능 패턴

```typescript
// SSR 문제 해결
const ClientOnly = dynamic(() => import("./Component"), { ssr: false });

// 메모이제이션
const MemoComponent = React.memo(Component);
const memoValue = useMemo(() => compute(data), [data]);
const memoCallback = useCallback(() => handler, [deps]);
```

## 🌐 API 패턴

```typescript
interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}

// 사용
const { data, loading, error } = useApi<PostType>("/api/posts");
```

## 📋 개발 체크리스트

### 새 기능

- [ ] 모바일 우선 디자인
- [ ] 44px+ 터치 타겟
- [ ] Pink-Purple-Indigo 컬러
- [ ] 아토믹 디자인 적용
- [ ] TypeScript 타입 정의

### 리팩토링 대상

- [ ] 200줄+ 컴포넌트
- [ ] 5개+ useState
- [ ] 3개+ useEffect
- [ ] 복잡한 비즈니스 로직

## 🐛 빠른 문제 해결

```typescript
// Hydration 에러
const Component = dynamic(() => import("./Component"), { ssr: false });

// 무한 리렌더링
useEffect(() => { /* logic */ }, []); // deps 확인

// 상태 업데이트 안됨
setState(prev => ({ ...prev, newValue })); // 불변성 유지

// Tailwind 안됨
className={isActive ? "bg-blue-500" : "bg-gray-500"} // 완전한 클래스명
```

## 📁 프로젝트 구조

```
frontend/
├── app/                    # 페이지 (App Router)
├── components/             # 아토믹 컴포넌트
│   ├── atoms/             # 기본 UI (Button, Input...)
│   ├── molecules/         # 조합 UI (Card, Dialog...)
│   └── organisms/         # 복잡 UI (Header, Hero...)
├── hooks/                 # 커스텀 훅
├── lib/                   # 유틸리티
└── types/                 # 타입 정의
```

## 🔧 자주 사용하는 유틸리티

```typescript
// 클래스명 조합
const styles = cn("base", isActive && "active", className);

// 디바운스
const debouncedValue = useDebounce(value, 300);

// 로컬스토리지
const [value, setValue] = useLocalStorage("key", defaultValue);
```

---

**💡 이 파일은 개발 중 빠른 참조용입니다. 상세한 내용은 DEVELOPMENT_GUIDE.md를 참고하세요.**
