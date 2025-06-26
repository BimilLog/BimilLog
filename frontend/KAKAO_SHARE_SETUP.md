# 카카오톡 공유 기능 설정 가이드 (신규 - 간단한 방식)

## 1. 카카오 개발자 센터 설정

### 1.1 카카오 앱 등록

1. [카카오 개발자 센터](https://developers.kakao.com/)에 접속
2. 내 애플리케이션 → 애플리케이션 추가하기
3. 앱 이름과 회사명 입력하여 앱 생성

### 1.2 플랫폼 설정

1. 내 애플리케이션 → 앱 설정 → 플랫폼
2. "Web" 플랫폼 추가
3. 사이트 도메인 등록:
   - 개발환경: `http://localhost:3000`
   - 운영환경: 실제 도메인 (예: `https://yoursite.com`)

### 1.3 JavaScript 키 확인

1. 내 애플리케이션 → 앱 키
2. "JavaScript 키" 복사

### 1.4 카카오링크 활성화

1. 카카오 개발자 센터 → 내 애플리케이션 → 제품 설정 → 카카오링크
2. "활성화 설정" → ON
3. "Web" 플랫폼에서 도메인 등록 확인

## 2. 환경 변수 설정

프론트엔드 루트 디렉토리에 `.env` 파일 생성:

```bash
# 카카오 JavaScript 앱 키
NEXT_PUBLIC_KAKAO_JAVA_SCRIPT_KEY=your_kakao_javascript_app_key_here
```

**주의**: `your_kakao_javascript_app_key_here`를 실제 JavaScript 키로 교체해야 합니다.

## 3. 구현 방식

이 구현체는 [카카오 공식 문서](https://developers.kakao.com/docs/latest/ko/kakaotalk-share/js-link#default-template-msg-sample-default)의 **피드 A형 템플릿**을 사용하는 간단한 방식입니다:

- `Kakao.Share.sendDefault()` 사용 (공식 권장 방식)
- 피드 A형 템플릿으로 통일
- 자동 대체 공유 (카카오톡 공유 실패 시)

## 4. 사용 방법

### 4.1 컴포넌트 방식 (권장)

```tsx
import { KakaoShareButton } from "@/components/atoms/kakao-share-button";

// 서비스 전체 공유
<KakaoShareButton
  type="service"
  variant="outline"
  size="lg"
/>

// 롤링페이퍼 공유
<KakaoShareButton
  type="rollingPaper"
  userName="홍길동"
  messageCount={5}
  variant="default"
/>

// 게시글 공유
<KakaoShareButton
  type="post"
  postId={123}
  title="게시글 제목"
  author="작성자"
  content="게시글 내용"
  likes={10}
  size="sm"
/>
```

### 4.2 직접 함수 호출 방식

```tsx
import {
  shareService,
  shareRollingPaper,
  sharePost,
  fallbackShare,
} from "@/lib/kakao-share";

const handleShareService = async () => {
  const success = await shareService();
  if (!success) {
    // 자동으로 대체 공유 방법 실행됨 (Web Share API 또는 클립보드 복사)
  }
};

const handleShareRollingPaper = async () => {
  const success = await shareRollingPaper("홍길동", 5);
  // 카카오톡 공유 성공 또는 실패 여부 반환
};

const handleSharePost = async () => {
  const success = await sharePost(123, "제목", "작성자", "내용", 10);
  // 카카오톡 공유 성공 또는 실패 여부 반환
};
```

## 5. 구현된 공유 기능

### 5.1 서비스 공유 (`shareService`)

- **용도**: 비밀로그 서비스 전체 홍보
- **위치**: 메인 페이지, 방문 페이지
- **내용**: 서비스 소개 및 시작하기 버튼

### 5.2 롤링페이퍼 공유 (`shareRollingPaper`)

- **용도**: 개인 롤링페이퍼 공유
- **매개변수**: `userName` (필수), `messageCount` (옵션)
- **내용**: 사용자명과 메시지 개수 포함

### 5.3 게시글 공유 (`sharePost`)

- **용도**: 게시판 글 공유
- **매개변수**: `postId`, `title`, `author`, `content` (필수), `likes` (옵션)
- **내용**: 게시글 정보와 추천수 포함

## 6. 자동 대체 공유 시스템

카카오톡 공유가 실패할 경우 자동으로 대체 방법을 제공합니다:

1. **카카오톡 공유 시도**
2. **실패 시 Web Share API 사용** (모바일 지원)
3. **Web Share API 실패 시 클립보드 복사**
4. **클립보드 복사 실패 시 수동 복사 프롬프트**

## 7. 문제 해결

### 7.1 카카오 SDK 초기화 실패

```
Error: ❌ 카카오 SDK 초기화에 실패했습니다.
```

**해결 방법**:

1. `.env` 파일에서 `NEXT_PUBLIC_KAKAO_JAVA_SCRIPT_KEY` 확인
2. 카카오 개발자 센터에서 JavaScript 키 재확인
3. 개발 서버 재시작 (`npm run dev` 다시 실행)
4. 브라우저 캐시 삭제 후 새로고침

### 7.2 도메인 에러

```
KakaoError: 등록되지 않은 도메인입니다.
```

**해결 방법**:

1. 카카오 개발자 센터 → 앱 설정 → 플랫폼에서 도메인 확인
2. 로컬 개발: `http://localhost:3000` 등록 필수
3. 운영 환경: 실제 도메인 등록 필수

### 7.3 카카오링크 비활성화

```
KakaoError: 카카오링크가 비활성화되어 있습니다.
```

**해결 방법**:

1. 카카오 개발자 센터 → 제품 설정 → 카카오링크
2. "활성화 설정" → ON으로 변경

## 8. 테스트 방법

### 8.1 로컬 개발 환경

1. 개발 서버 실행: `npm run dev`
2. `http://localhost:3000` 접속
3. 메인 페이지에서 "카카오톡 공유" 버튼 클릭
4. 카카오톡 앱이 열리면 성공

### 8.2 모바일 테스트

1. 개발 서버를 네트워크에서 접근 가능하도록 설정
2. 모바일에서 해당 URL 접속
3. 카카오톡 공유 테스트
4. Web Share API 동작 확인

## 9. 커스터마이징

### 9.1 공유 내용 수정

`frontend/lib/kakao-share.ts` 파일에서 각 함수의 `title`, `description`, `imageUrl` 수정:

```typescript
// 예: 서비스 공유 내용 수정
window.Kakao.Share.sendDefault({
  objectType: "feed",
  content: {
    title: "💕 비밀로그 - 익명 롤링페이퍼 서비스", // 제목 수정
    description: "친구들에게 익명으로 따뜻한 메시지를 받아보세요!", // 설명 수정
    imageUrl: "your-custom-image-url", // 이미지 URL 수정
    // ...
  },
  // ...
});
```

### 9.2 버튼 스타일 수정

```tsx
<KakaoShareButton
  type="service"
  variant="default" // "default" | "outline" | "ghost"
  size="lg" // "default" | "sm" | "lg"
  className="custom-class"
/>
```

## 10. 기술적 세부사항

### 10.1 사용하는 카카오 API

```javascript
// 실제 사용되는 API (피드 A형)
Kakao.Share.sendDefault({
  objectType: "feed",
  content: {
    title: "제목",
    description: "설명",
    imageUrl: "이미지 URL",
    link: {
      mobileWebUrl: "모바일 URL",
      webUrl: "웹 URL",
    },
  },
  buttons: [
    {
      title: "버튼 텍스트",
      link: {
        mobileWebUrl: "모바일 URL",
        webUrl: "웹 URL",
      },
    },
  ],
});
```

### 10.2 장점

- **간단함**: 복잡한 DOM 조작 없음
- **안전함**: React와의 충돌 없음
- **자동 대체**: 카카오톡 공유 실패시 자동으로 다른 방법 제공
- **공식 방식**: 카카오 공식 문서 기준 구현

---

이 가이드는 카카오 공식 문서의 [피드 A형 템플릿](https://developers.kakao.com/docs/latest/ko/kakaotalk-share/js-link#default-template-msg-sample-default)을 기반으로 작성되었습니다.
