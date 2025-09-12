# GrowFarm 개발 가이드

> 비밀로그 프로젝트의 개발 패턴, 컴포넌트 구조, 성능 최적화 가이드

## 빠른 참조

### 프로젝트 핵심 정보

- **서비스**: 롤링페이퍼 + 커뮤니티 게시판
- **타겟**: 모바일 퍼스트 (익명 메시지 플랫폼)
- **브랜드**: Pink-Purple-Indigo 그라디언트
- **스택**: Next.js 15.2.4 + TypeScript + Tailwind + Spring Boot

### 디자인 시스템 핵심

```css
/* 메인 브랜드 그라디언트 */
bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600  /* 제목 */
bg-gradient-to-r from-pink-500 to-purple-600                /* 버튼 */
bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50   /* 배경 */

/* 카드 기본 스타일 */
bg-white/80 backdrop-blur-sm border-0 shadow-lg hover:shadow-xl transition-shadow rounded-lg

/* 터치 최적화 버튼 */
min-h-[44px] px-4 active:scale-[0.98] transition-transform
```

---

## 아토믹 디자인 구조

### 사용 패턴

```typescript
// 권장: 메인 export에서 일괄 import
import { Button, Card, AuthHeader, HomeHero } from "@/components";

// 호환성: 기존 UI 경로 지원
import { Button } from "@/components/ui/button";

// 직접: 아토믹 레벨별 import
import { Button } from "@/components/atoms/button";
import { HomeHero } from "@/components/organisms/home/HomeHero";
```

### 컴포넌트 분류 기준

```typescript
// Atoms: 더 이상 쪼갤 수 없는 기본 UI
Button, Input, Label, Avatar, Badge, Icon;

// Molecules: 2-3개 Atoms 조합, 특정 기능
Card, Dialog, SearchBox, FormField, AdFitBanner;

// Organisms: 복잡한 섹션, 비즈니스 로직 포함
AuthHeader, HomeHero, BoardSearch, NotificationBell;
```

---

## 📱 모바일 퍼스트 개발

### 반응형 브레이크포인트

```css
/* 기본 (모바일) */    320px ~ 767px
/* 태블릿 */          768px ~ 1023px
/* 데스크톱 */        1024px+

/* Tailwind 클래스 */
기본값                /* 모바일 */
md:클래스             /* 태블릿+ */
lg:클래스             /* 데스크톱+ */
```

### 터치 최적화 원칙

```typescript
// 최소 터치 타겟: 44px × 44px
// 권장 터치 타겟: 48px × 48px
// 편안한 터치 타겟: 56px × 56px

// 터치 피드백 필수
className = "active:scale-[0.98] transition-transform touch-manipulation";

// 충분한 간격 (최소 8px)
className = "space-y-2 gap-4";
```

---

## 🪝 커스텀 훅 패턴

### 데이터 관리 훅

```typescript
const useDataFetch = (id: string) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchData = async () => {
    // API 호출 로직
  };

  useEffect(() => {
    if (id) fetchData();
  }, [id]);

  return { data, loading, error, refetch: fetchData };
};
```

### 액션 관리 훅

```typescript
const useActions = (id: string, onRefresh: () => void) => {
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleCreate = async (data: any) => {
    setIsSubmitting(true);
    try {
      await api.create(data);
      await onRefresh();
    } finally {
      setIsSubmitting(false);
    }
  };

  return { handleCreate, isSubmitting };
};
```

---

## 컴포넌트 개발 패턴

### 새 컴포넌트 생성 템플릿

```typescript
// components/atoms/new-component.tsx
interface NewComponentProps {
  variant?: "default" | "primary";
  size?: "sm" | "md" | "lg";
  className?: string;
  children: React.ReactNode;
}

export const NewComponent: React.FC<NewComponentProps> = ({
  variant = "default",
  size = "md",
  className = "",
  children,
}) => {
  const baseStyles = "기본-스타일";
  const variantStyles = {
    default: "기본-변형-스타일",
    primary: "주요-변형-스타일",
  };
  const sizeStyles = {
    sm: "작은-크기-스타일",
    md: "중간-크기-스타일",
    lg: "큰-크기-스타일",
  };

  return (
    <div
      className={`${baseStyles} ${variantStyles[variant]} ${sizeStyles[size]} ${className}`}
    >
      {children}
    </div>
  );
};
```

### Props 타입 정의 패턴

```typescript
// 기본 Props
interface BaseProps {
  className?: string;
  children?: React.ReactNode;
}

// 이벤트 핸들러 Props
interface ActionProps {
  onSubmit?: () => void;
  onCancel?: () => void;
  onClick?: (event: React.MouseEvent) => void;
}

// 데이터 Props
interface DataProps {
  data: SomeType[];
  loading?: boolean;
  error?: string;
}

// 조합
interface ComponentProps extends BaseProps, ActionProps, DataProps {
  // 추가 props
}
```

---

## 스타일링 가이드

### 일관된 스타일 패턴

```typescript
// 1. 카드형 컴포넌트
const cardStyles =
  "bg-white/80 backdrop-blur-sm border-0 shadow-lg hover:shadow-xl transition-shadow rounded-lg p-6";

// 2. 그라디언트 제목
const titleStyles =
  "text-3xl font-bold bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent";

// 3. 기본 버튼
const buttonStyles =
  "px-4 py-2 min-h-[44px] rounded-lg font-medium transition-all duration-200 active:scale-[0.98]";

// 4. 입력 필드
const inputStyles =
  "w-full px-3 py-2 min-h-[44px] border-2 border-gray-200 rounded-lg focus:border-purple-400 focus:outline-none";
```

### 조건부 스타일링

```typescript
import { cn } from "@/lib/utils";

// 1. 간단한 조건
className={`base-styles ${isActive ? "active-styles" : "inactive-styles"}`}

// 2. 복잡한 조건 (cn 유틸리티 사용)
const styles = cn(
  "base-styles",
  variant === "primary" && "primary-styles",
  size === "large" && "large-styles",
  isDisabled && "disabled-styles",
  className
);
```

---

## ⚡ 성능 최적화

### SSR 문제 해결

```typescript
// 클라이언트 전용 컴포넌트
import dynamic from "next/dynamic";

const ClientOnlyComponent = dynamic(() => import("./ClientOnlyComponent"), {
  ssr: false,
  loading: () => <Loading />,
});
```

### 메모이제이션 패턴

```typescript
// 1. 컴포넌트 메모이제이션
const MemoizedComponent = React.memo(Component, (prevProps, nextProps) => {
  // 비교 로직 (선택적)
  return prevProps.id === nextProps.id;
});

// 2. 값 메모이제이션
const expensiveValue = useMemo(() => {
  return computeExpensiveValue(data);
}, [data]);

// 3. 콜백 메모이제이션
const memoizedCallback = useCallback(
  (id: string) => {
    handleAction(id);
  },
  [dependency]
);
```

### 배치 처리 패턴 (알림 시스템)

```typescript
const useBatchProcessor = () => {
  const batchQueue = useRef([]);

  const addToBatch = (action: Action) => {
    batchQueue.current.push(action);
  };

  const processBatch = async () => {
    if (batchQueue.current.length === 0) return;

    const actions = batchQueue.current.splice(0);
    await api.batchProcess(actions);
  };

  // 5분마다 자동 처리
  useEffect(() => {
    const interval = setInterval(processBatch, 5 * 60 * 1000);
    return () => clearInterval(interval);
  }, []);

  return { addToBatch, processBatch };
};
```

---

## 🔐 권한 관리 패턴

### 컴포넌트 레벨 권한 체크

```typescript
const usePermissions = (item: Post | Comment) => {
  const { user, isAuthenticated } = useAuth();

  const canModify = () => {
    if (!item) return false;

    // 익명 글/댓글은 비회원만 수정 가능
    if (item.userName === "익명" || item.userName === null) {
      return !isAuthenticated;
    }

    // 회원 글/댓글은 본인만 수정 가능
    return isAuthenticated && user?.userName === item.userName;
  };

  const canView = () => {
    // 모든 사용자 볼 수 있음
    return true;
  };

  return { canModify: canModify(), canView: canView() };
};

// 사용 예시
const PostActions = ({ post }: { post: Post }) => {
  const { canModify } = usePermissions(post);

  return (
    <div>
      {canModify && (
        <>
          <Button onClick={handleEdit}>수정</Button>
          <Button onClick={handleDelete}>삭제</Button>
        </>
      )}
    </div>
  );
};
```

---

## 🌐 API 호출 패턴

### API 응답 타입 정의

```typescript
interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}

interface PaginatedResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}
```

### API 호출 훅 패턴

```typescript
const useApi = <T>(endpoint: string) => {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await api.get<ApiResponse<T>>(endpoint);

      if (response.success) {
        setData(response.data || null);
      } else {
        setError(response.error || "Unknown error");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Network error");
    } finally {
      setLoading(false);
    }
  };

  return { data, loading, error, refetch: fetchData };
};
```

---

## 📋 개발 체크리스트

### 새 기능 개발 시

- [ ] 모바일 우선으로 디자인했는가?
- [ ] 아토믹 디자인 원칙을 따랐는가?
- [ ] 44px 이상 터치 타겟을 보장했는가?
- [ ] 브랜드 컬러(Pink-Purple-Indigo)를 사용했는가?
- [ ] TypeScript 타입을 정확히 정의했는가?
- [ ] 에러 처리를 적절히 했는가?
- [ ] 로딩 상태를 표시했는가?
- [ ] 접근성(a11y)을 고려했는가?

### 컴포넌트 리팩토링 시

- [ ] 200줄 이상의 긴 컴포넌트인가?
- [ ] useState가 5개 이상인가?
- [ ] useEffect가 3개 이상인가?
- [ ] 복잡한 비즈니스 로직이 포함되어 있는가?
- [ ] 모바일/PC 분기가 복잡한가?

### 코드 리뷰 시

- [ ] 컴포넌트가 단일 책임 원칙을 따르는가?
- [ ] 재사용 가능한 구조인가?
- [ ] 일관된 네이밍 컨벤션을 사용했는가?
- [ ] 성능 최적화를 적용했는가?
- [ ] 메모리 누수 가능성은 없는가?

---

## 🐛 일반적인 문제 해결

### Hydration 에러

```typescript
// 문제: SSR과 클라이언트 렌더링 불일치
// 해결: dynamic import 사용
const ClientComponent = dynamic(() => import("./Component"), { ssr: false });
```

### 무한 리렌더링

```typescript
// 문제: dependency 배열 누락
useEffect(() => {
  fetchData();
}, []); // 빈 배열로 한 번만 실행

// 문제: 객체/배열 참조 변경
const stableRef = useRef(complexObject);
useEffect(() => {
  // logic
}, [stableRef.current]);
```

### 상태 업데이트 안됨

```typescript
// 문제: 불변성 위반
// 잘못된 방법
state.push(newItem);
setState(state);

// 올바른 방법
setState((prev) => [...prev, newItem]);
```

### CSS 스타일 적용 안됨

```typescript
// Tailwind 클래스명 확인
// 동적 클래스명 (Tailwind가 인식 못함)
className={`bg-${color}-500`}

// 완전한 클래스명 사용
className={color === 'blue' ? 'bg-blue-500' : 'bg-red-500'}
```

---

## 📚 유용한 유틸리티

### 클래스명 조합 유틸리티

```typescript
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// 사용 예시
const buttonClass = cn(
  "base-button-styles",
  variant === "primary" && "primary-styles",
  size === "large" && "large-styles",
  className
);
```

### 디바운스 훅

```typescript
const useDebounce = (value: string, delay: number) => {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => clearTimeout(handler);
  }, [value, delay]);

  return debouncedValue;
};

// 사용 예시
const [searchTerm, setSearchTerm] = useState("");
const debouncedSearchTerm = useDebounce(searchTerm, 300);

useEffect(() => {
  if (debouncedSearchTerm) {
    performSearch(debouncedSearchTerm);
  }
}, [debouncedSearchTerm]);
```

### 로컬스토리지 훅

```typescript
const useLocalStorage = <T>(key: string, initialValue: T) => {
  const [storedValue, setStoredValue] = useState<T>(() => {
    if (typeof window === "undefined") return initialValue;

    try {
      const item = window.localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch (error) {
      return initialValue;
    }
  });

  const setValue = (value: T | ((val: T) => T)) => {
    try {
      const valueToStore =
        value instanceof Function ? value(storedValue) : value;
      setStoredValue(valueToStore);

      if (typeof window !== "undefined") {
        window.localStorage.setItem(key, JSON.stringify(valueToStore));
      }
    } catch (error) {
      console.error(`Error saving to localStorage:`, error);
    }
  };

  return [storedValue, setValue] as const;
};
```

---

**💡 이 가이드는 프로젝트 발전에 따라 지속적으로 업데이트됩니다.**
**🚀 효율적인 개발을 위해 참고하시고, 개선 사항이 있으면 언제든 말씀해 주세요!**
