# BimilLog v2.7

> 익명으로 마음을 전하는 SNS

## 📖 프로젝트 소개

**BimilLog**는 익명성을 기반으로 한 감성 커뮤니케이션 플랫폼입니다. 사용자들은 자신의 정체를 드러내지 않고 진솔한 마음을 롤링페이퍼에 담아 전달할 수 있으며, 익명 게시판을 통해 자유롭게 소통할 수 있습니다.

### 🎯 서비스 특징

- **익명 롤링페이퍼**: 그리드 기반의 롤링페이퍼에 익명 메시지를 남기고, 링크를 통해 공유할 수 있습니다.
- **익명 게시판**: 익명으로 게시글과 댓글을 작성하며 자유롭게 소통합니다.
- **실시간 알림**: FCM과 SSE를 통해 새로운 메시지와 댓글을 실시간으로 받아볼 수 있습니다.
- **소셜 로그인**: 간편한 소셜 로그인으로 10초안에 가입할 수 있습니다.
- **플레이스토어 지원**: 플레이스토어에서 다운받을 수 있습니다.

### 🌐 서비스 URL

- **웹사이트**: [grow-farm.com](https://grow-farm.com)

## 🏗️ 프로젝트 구조

```
BimilLog/
├── BimilLog_core/     # 메인 백엔드 (Spring Boot)
├── BimilLog_front/    # 프론트엔드 (Next.js)
├── BimilLog_android/  # 플레이스토어 (TWA 래핑)
├── BimilLog_alarm/    # 알림 서버 (Spring Boot) (개발 예정)
└── BimilLog_chat/     # 채팅 서비스 (Spring Boot) (개발 예정)
```

## 개발 과정

### 개발자
- 백엔드 개발자 정재익 개인 프로젝트

### 개발 연혁
- 2025-03-21 개발 시작
- 2025-05-11 테스트 서버 운영
- 2025-07-01 버전 1.0.0 배포 운영 시작
- 2025-10-12 버전 2.0.0 배포
- 2025-11-29 버전 2.2.0 배포 
- 2025-12-02 플레이스토어 앱 출시
- 2026-01-04 버전 2.4.0 배포
- 2026-01-26 버전 2.6.0 배포

## 모듈 설명

### BimilLog_core (메인 백엔드)
- **기술 스택**:
  - Spring Boot 3.4.4, Java 21
  - MySQL, Redis
  - OAuth 2.0 (Kakao), JWT
- **주요 기능**:
  - 회원 관리 (소셜 로그인)
  - 익명 게시판
  - 익명 롤링페이퍼
  - 실시간 알림 (알림 서버로 이동 예정)
  - 관리자 기능
  - 친구 기능

### BimilLog_front (프론트엔드)
- **기술 스택**:
  - Next.js 15.5.3, React 19, TypeScript
  - TanStack Query, Zustand
  - Atomic Design 패턴
- **주요 기능**:
  - 모바일 최적화, 반응형 UI/UX
  - 실시간 알림 (FCM, SSE)
  - PWA 지원

### BimilLog_android (안드로이드)
- **기술 스택**:
  - Android 8.7.3, Kotlin 1.9.20
- **주요 기능**:
  - 안드로이드 모니터링 (firebase, crashlytics)
  - 앱 전용 FCM
  - TWA 래핑

### BimilLog_alarm (알람 서비스) (개발 예정)
- **예정 기술 스택**:
  - 백엔드: Spring Boot 3.4.4, Java 21
  - 메시지 큐: Redis Stream Or RabbitMQ
  - 통신: OpenFeign OR GRPC(메인 서비스와 통신)
- **주요 기능**:
  - 채팅 및 코어의 알림 중앙 관리

### BimilLog_chat (채팅 서비스) (개발 예정)
- **예정 기술 스택**:
  - 백엔드: Spring Boot 3.4.4, Java 21
  - 데이터베이스: Mysql OR MongoDB
  - 통신: OpenFeign OR GRPC(메인 서비스와 통신)
- **주요 기능**:
  - 익명 채팅, 단체 채팅, 개인 채팅 등