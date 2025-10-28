# BimilLog

비밀로그 프로젝트 - 익명 롤링페이퍼 플랫폼

## 프로젝트 구조

이 프로젝트는 **독립적인 마이크로서비스 구조**로 구성되어 있습니다.

```
BimilLog/
├── BimilLog_main/     # 메인 애플리케이션
│   ├── backend/       # Spring Boot 백엔드
│   └── frontend/      # Next.js 프론트엔드
│
└── bimilLog_chat/     # 채팅 서비스
    └── src/           # Spring Boot 백엔드
```

## 모듈 설명

### BimilLog_main (메인 서비스)
- **포트**: 8080
- **기술 스택**:
  - 백엔드: Spring Boot 3.4.4, Java 21
  - 프론트엔드: Next.js 15
  - 데이터베이스: MySQL, Redis
  - 인증: OAuth 2.0 (Kakao), JWT
- **주요 기능**:
  - 회원 관리 (카카오 로그인)
  - 익명 게시판 (post, comment)
  - 롤링페이퍼 (paper, message)
  - 실시간 알림 (FCM, SSE)
  - 관리자 기능
- **아키텍처**: Hexagonal Architecture (포트 & 어댑터 패턴)

### bimilLog_chat (채팅 서비스)
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