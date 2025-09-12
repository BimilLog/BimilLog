# 비밀로그(BimilLog) 프론트엔드 개발 가이드

> 익명 롤링페이퍼 플랫폼의 아토믹 디자인 시스템 & 개발 패턴

## 🔧 현재 리팩토링 상황 (2025-01-20)

### ✅ 완료된 작업
- **구조 정리**: 백업 파일 제거, util/ → lib/ 통합, layouts/ → organisms/ 이동  
- **아키텍처 분석**: Server Components vs Client Components 분석, API 레이어 현황 파악
- **일관성 확보**: import 경로 통합, .gitignore 백업 패턴 추가

### 🔄 진행 중인 작업  
- **컴포넌트 구조 최적화**: atoms 서브디렉토리 구조 정리 중
- **중복 해결**: EmptyState 컴포넌트 중복 문제 (atoms vs molecules)

### 🔜 예정된 작업
- **API 레이어 개편**: CQRS 패턴 반영한 Query/Command 분리
- **타입 시스템 강화**: types/ 디렉토리 활용도 증대, 중앙화된 타입 정의  
- **상태 관리 전략**: Context API 확장 또는 새로운 상태 관리 솔루션 검토

---

## 📱 프로젝트 정체성

- **서비스**: 익명 롤링페이퍼 플랫폼 (grow-farm.com)
- **타겟**: 모바일 퍼스트 + 익명성 중시
- **브랜드**: Pink-Purple-Indigo 그라디언트
- **스택**: Next.js 15.2.4 + TypeScript + Tailwind + Spring Boot

## 🎨 핵심 디자인 스타일

```css
/* 브랜드 그라디언트 */
bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600  /* 제목 */
bg-gradient-to-r from-pink-500 to-purple-600                /* 버튼 */
bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50   /* 배경 */

/* 표준 카드 스타일 */
bg-white/80 backdrop-blur-sm border-0 shadow-lg hover:shadow-xl transition-shadow rounded-lg

/* 터치 최적화 버튼 */
min-h-[44px] px-4 active:scale-[0.98] transition-transform touch-manipulation
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

## 📁 아토믹 디자인 구조

```
frontend/components/
├── atoms/          # 원자 - 기본 UI 요소 (Button, Input, Label...)
├── molecules/      # 분자 - Atoms 조합 (Card, Dialog, Search...)
├── organisms/      # 유기체 - 복잡한 섹션 (AuthHeader, BoardSearch...)
├── index.ts        # 메인 export 파일
└── ui.ts          # 호환성을 위한 re-export 파일
```

### 컴포넌트 Import 방법

```typescript
// ✅ 권장: 메인 export에서 일괄 import
import { Button, Card, AuthHeader } from "@/components";

// ✅ 호환성: 기존 UI 경로 지원
import { Button } from "@/components/ui/button";

// ✅ 직접: 아토믹 레벨별 import
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

## 🪝 표준 훅 패턴

### 데이터 관리 훅

```typescript
const useData = (id: string) => {
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

## 🔧 컴포넌트 개발 템플릿

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

    // 익명 글/댓글: 비회원만 수정 가능
    if (item.userName === "익명" || item.userName === null) {
      return !isAuthenticated;
    }

    // 회원 글/댓글: 본인만 수정 가능
    return isAuthenticated && user?.userName === item.userName;
  };

  return { canModify: canModify() };
};
```

## ⚡ 성능 최적화 패턴

```typescript
// SSR 문제 해결
const ClientOnly = dynamic(() => import("./Component"), { ssr: false });

// 메모이제이션
const MemoComponent = React.memo(Component);
const memoValue = useMemo(() => compute(data), [data]);
const memoCallback = useCallback(() => handler, [deps]);
```

## 🌐 API 호출 패턴

```typescript
interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}

// 사용 예시
const { data, loading, error } = useApi<PostType>("/api/posts");
```

## 🔧 자주 사용하는 유틸리티

```typescript
// 클래스명 조합 (lib/utils.ts)
import { cn } from "@/lib/utils";
const styles = cn("base", isActive && "active", className);

// 디바운스
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

// 로컬스토리지
const useLocalStorage = <T>(key: string, initialValue: T) => {
  // 구현 로직
  return [value, setValue] as const;
};
```

## 🧪 주요 컴포넌트

### Atoms
- `Button`, `Input`, `Label`, `Textarea` - 폼 컨트롤
- `Avatar`, `Badge`, `Icon`, `Spinner` - 미디어 & 콘텐츠
- `KakaoShareButton` - 소셜 공유

### Molecules
- `Card`, `Alert`, `Tabs` - 레이아웃 & 구조
- `Dialog`, `Sheet`, `Popover` - 인터랙티브
- `Editor`, `SearchBox`, `FormField` - 콘텐츠 & 폼
- `ResponsiveAdFitBanner`, `AdFitBanner` - 광고

### Organisms
- `AuthHeader`, `MobileNav`, `NotificationBell` - 네비게이션
- `BoardSearch`, `PostList`, `BoardPagination` - 게시판
- `HomeHero`, `HomeFeatures`, `HomeFooter` - 홈페이지

## 📋 개발 체크리스트

### 새 기능 개발 시
- [ ] 모바일 우선 디자인
- [ ] 44px+ 터치 타겟 보장
- [ ] Pink-Purple-Indigo 브랜드 컬러 사용
- [ ] 아토믹 디자인 원칙 적용
- [ ] TypeScript 타입 정확히 정의
- [ ] 에러 처리 및 로딩 상태 표시

### 리팩토링 대상
- [ ] 200줄+ 긴 컴포넌트
- [ ] 5개+ useState 사용
- [ ] 3개+ useEffect 사용
- [ ] 복잡한 비즈니스 로직 포함

## 🐛 빠른 문제 해결

```typescript
// Hydration 에러
const Component = dynamic(() => import("./Component"), { ssr: false });

// 무한 리렌더링
useEffect(() => { /* logic */ }, []); // dependency 배열 확인

// 상태 업데이트 안됨
setState(prev => ({ ...prev, newValue })); // 불변성 유지

// Tailwind 클래스 적용 안됨
className={isActive ? "bg-blue-500" : "bg-gray-500"} // 완전한 클래스명 사용
```

## 🔄 마이그레이션 가이드

1. **컴포넌트 분석**: 기존 컴포넌트의 아토믹 레벨 결정
2. **적절한 폴더로 이동**: atoms, molecules, organisms 중 선택
3. **Import 경로 업데이트**: 새로운 경로로 import 구문 변경
4. **Index 파일 업데이트**: 해당 레벨의 index.ts에 export 추가

## 사용 권장 사항

1. **새로운 컴포넌트**: 아토믹 구조를 사용하여 개발
2. **기존 컴포넌트**: 점진적으로 새로운 구조로 마이그레이션
3. **스타일링**: 브랜드 컬러와 표준 패턴 활용
4. **페이지 개발**: Organisms를 활용한 일관된 레이아웃

## 📚 참고 자료

- [Atomic Design by Brad Frost](https://atomicdesign.bradfrost.com/)
- [React Component Patterns](https://kentcdodds.com/blog/compound-components-with-react-hooks)
- [Design Systems](https://www.designsystems.com/)

---

**💡 이 가이드는 프로젝트 발전에 따라 지속적으로 업데이트됩니다.**