# 아토믹 디자인 패턴 가이드

이 프로젝트는 Brad Frost의 아토믹 디자인 방법론을 적용한 확장 가능한 컴포넌트 시스템입니다.

## 폴더 구조

```
frontend/components/
├── atoms/          # 원자 - 기본 UI 요소 (Button, Input, Label...)
├── molecules/      # 분자 - Atoms 조합 (Card, Dialog, Search...)
├── organisms/      # 유기체 - 복잡한 섹션 (AuthHeader, BoardSearch...)
├── index.ts        # 메인 export 파일
└── ui.ts          # 호환성을 위한 re-export 파일
```

## 사용 방법

### 새로운 아토믹 구조 사용 (권장)

```typescript
// 아토믹 구조를 직접 사용
import { Button, Input } from "@/components/atoms/button";
import { Card, SearchBox } from "@/components/molecules/card";
import { AuthHeader } from "@/components/organisms/auth-header";

// 또는 메인 index에서 일괄 import
import { Button, Card, AuthHeader } from "@/components";
```

### 기존 UI 경로 호환성

```typescript
// 기존 방식 (여전히 작동함)
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";

// 새로운 방식 (권장)
import { Button } from "@/components/atoms/button";
import { Card } from "@/components/molecules/card";
```

## 컴포넌트 개발 가이드라인

### Atoms 개발 원칙

- 단일 책임 원칙을 따름
- 독립적으로 동작 가능
- Props로 다양한 variant, size, color 등을 받아 유연하게 활용

### Molecules 개발 원칙

- 2개 이상의 Atoms 조합으로 구성
- 특정 기능을 수행하는 최소 단위
- 기본적인 비즈니스 로직 포함 가능

### Organisms 개발 원칙

- 페이지의 주요 섹션을 구성
- 상태 관리와 비즈니스 로직 포함
- API 호출 및 데이터 처리

## 디자인 토큰 시스템

완전한 디자인 토큰 시스템이 구현되어 있습니다. `@/lib/design-tokens`에서 사용할 수 있습니다.

```typescript
import { designTokens, getColor, getSpacing } from "@/lib/design-tokens";

// 직접 사용
const primaryColor = designTokens.colors.primary[500];
const baseSpacing = designTokens.spacing[4];
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

## 🔄 마이그레이션 가이드

1. **컴포넌트 분석**: 기존 컴포넌트의 아토믹 레벨 결정
2. **적절한 폴더로 이동**: atoms, molecules, organisms 중 선택
3. **Import 경로 업데이트**: 새로운 경로로 import 구문 변경
4. **Index 파일 업데이트**: 해당 레벨의 index.ts에 export 추가

## 사용 권장 사항

1. **새로운 컴포넌트**: 아토믹 구조를 사용하여 개발
2. **기존 컴포넌트**: 점진적으로 새로운 구조로 마이그레이션
3. **스타일링**: Design Token 시스템 활용
4. **페이지 개발**: Organisms를 활용한 일관된 레이아웃

## 📚 참고 자료

- [Atomic Design by Brad Frost](https://atomicdesign.bradfrost.com/)
- [React Component Patterns](https://kentcdodds.com/blog/compound-components-with-react-hooks)
- [Design Systems](https://www.designsystems.com/)
