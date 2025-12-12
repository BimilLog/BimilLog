# BimilLog v2.0

> 익명으로 마음을 전하는 롤링페이퍼 플랫폼

## 📖 프로젝트 소개

**BimilLog**는 익명성을 기반으로 한 감성 커뮤니케이션 플랫폼입니다. 사용자들은 자신의 정체를 드러내지 않고 진솔한 마음을 롤링페이퍼에 담아 전달할 수 있으며, 익명 게시판을 통해 자유롭게 소통할 수 있습니다.

### 🎯 서비스 특징

- **익명 롤링페이퍼**: 그리드 기반의 롤링페이퍼에 익명 메시지를 남기고, 링크를 통해 공유할 수 있습니다.
- **익명 게시판**: 익명으로 게시글과 댓글을 작성하며 자유롭게 소통합니다.
- **실시간 알림**: FCM과 SSE를 통해 새로운 메시지와 댓글을 실시간으로 받아볼 수 있습니다.
- **카카오 소셜 로그인**: 간편한 카카오 로그인으로 빠르게 서비스를 이용할 수 있습니다.
- **모바일 최적화**: 모바일 퍼스트 디자인으로 언제 어디서나 편리하게 사용 가능합니다.

### 🌐 서비스 URL

- **웹사이트**: [grow-farm.com](https://grow-farm.com)

## 🏗️ 프로젝트 구조

```
BimilLog/
├── BimilLog_core/     # 메인 백엔드 (Spring Boot)
├── BimilLog_front/    # 프론트엔드 (Next.js)
└── BimilLog_chat/     # 채팅 서비스 (Spring Boot)
```

## 모듈 설명

### BimilLog_core (메인 백엔드)
- **기술 스택**:
  - Spring Boot 3.4.4, Java 21
  - MySQL, Redis
  - OAuth 2.0 (Kakao), JWT
- **주요 기능**:
  - 회원 관리 (카카오 로그인)
  - 익명 게시판 (post, comment)
  - 롤링페이퍼 (paper, message)
  - 실시간 알림 (FCM, SSE)
  - 관리자 기능
- **아키텍처**: Hexagonal Architecture (포트 & 어댑터 패턴)

### BimilLog_front (프론트엔드)
- **기술 스택**:
  - Next.js 15.5.3, React 19, TypeScript
  - TanStack Query, Zustand
  - Atomic Design 패턴
- **주요 기능**:
  - 반응형 UI/UX
  - 실시간 알림 (FCM, SSE)
  - PWA 지원

### BimilLog_chat (채팅 서비스)
- **기술 스택**:
  - 백엔드: Spring Boot 3.4.4, Java 21
  - 데이터베이스: MySQL
  - 통신: OpenFeign (메인 서비스와 통신)
- **주요 기능**:
  - 실시간 채팅
  - 메인 서비스와 API 통신