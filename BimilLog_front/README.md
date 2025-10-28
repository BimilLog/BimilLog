# BimilLog Frontend

> 익명 롤링페이퍼 플랫폼의 프론트엔드


## 🎨 디자인 특징

### 브랜드 컬러

Pink-Purple-Indigo 그라디언트 시스템

```css
/* 메인 그라디언트 */
bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600

/* 배경 그라디언트 */
bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50
```

### UI/UX 디자인

- **모바일 퍼스트**: 모바일 화면에 최적화된 레이아웃 (320px~)
- **반응형 디자인**: 모바일/태블릿/데스크톱 대응 (Breakpoints: `md:768px`, `lg:1024px`)
- **터치 최적화**: 최소 터치 타겟 44px × 44px, 권장 48px × 48px
- **Atomic Design**: 체계적인 컴포넌트 구조 (Atoms → Molecules → Organisms)
- **카드 스타일**: `bg-white/80 backdrop-blur-sm` 글래스모피즘 효과
- **인터랙션**: `active:scale-[0.98]` 터치 피드백 애니메이션

## 🛠️ 기술 스택

### Core
- **Next.js** 15.5.3 (App Router)
- **React** 19.0.0
- **TypeScript** 5.x

### UI/UX
- **Tailwind CSS** v4
- **Flowbite React** 0.12.9
- **Radix UI** (Headless Components)
- **Lucide React** (Icons)

### 상태 관리
- **TanStack Query** 5.87.4 (서버 상태)
- **Zustand** 5.0.8 (클라이언트 상태)

### 기타
- **Firebase** 11.9.1 (FCM 푸시 알림)
- **Quill** 2.0.3 (리치 텍스트 에디터)
- **Next PWA** 5.6.0 (PWA 지원)

## 🔗 관련 링크

- **메인 프로젝트**: [BimilLog Repository](../)
- **웹사이트**: [grow-farm.com](https://grow-farm.com)
- **백엔드**: Spring Boot 3.4.4 (포트 8080)
