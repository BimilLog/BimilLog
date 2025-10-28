# BimilLog

비밀로그 프로젝트 - 익명 롤링페이퍼 플랫폼

## 프로젝트 구조

이 프로젝트는 **독립적인 마이크로서비스 구조**로 구성되어 있습니다.

```
BimilLog/
├── BimilLog_core/     # 메인 백엔드 (Spring Boot)
├── BimilLog_front/    # 프론트엔드 (Next.js)
├── BimilLog_chat/     # 채팅 서비스 (Spring Boot)
├── .github/           # CI/CD workflows
└── BimilLog_main/     # Docs & Terraform
    ├── docs/          # 문서
    └── terraform/     # 인프라 코드
```

## 모듈 설명

### BimilLog_core (메인 백엔드)
- **포트**: 8080
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
- **포트**: 3000
- **기술 스택**:
  - Next.js 15.5.3, React 19, TypeScript
  - TanStack Query, Zustand
  - Atomic Design 패턴
- **주요 기능**:
  - 반응형 UI/UX
  - 실시간 알림 (FCM, SSE)
  - PWA 지원

### BimilLog_chat (채팅 서비스)
- **포트**: 8081
- **기술 스택**:
  - 백엔드: Spring Boot 3.4.4, Java 21
  - 데이터베이스: MySQL
  - 통신: OpenFeign (메인 서비스와 통신)
- **주요 기능**:
  - 실시간 채팅
  - 메인 서비스와 API 통신

## 실행 방법

### 사전 요구사항
- Java 21
- MySQL
- Redis (메인 서비스만)
- Node.js 18+ (프론트엔드)