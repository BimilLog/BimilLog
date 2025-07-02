# 카카오 서비스 설정 가이드 (공유 기능 + 광고 기능)

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

---

# 카카오 AdFit 광고 설정 가이드

## 1. AdFit 광고 단위 등록

### 1.1 AdFit 개발자 센터 접속

1. [AdFit 개발자 센터](https://adfit.kakao.com/)에 접속
2. 카카오 계정으로 로그인

### 1.2 매체 등록

1. 매체 관리 → 매체 등록
2. 매체 정보 입력:
   - 매체명: 프로젝트 이름
   - 매체 URL: 실제 서비스 도메인
   - 카테고리: 적절한 카테고리 선택

### 1.3 광고 단위 생성

1. 광고 단위 관리 → 광고 단위 등록
2. 모바일용 광고 단위:
   - 크기: 320x50, 320x100, 또는 300x250
   - 광고 위치: 상단/하단/중간 설정
3. PC용 광고 단위:
   - 크기: 728x90, 300x250, 또는 반응형
   - 광고 위치: 사이드바/콘텐츠 내부 설정

### 1.4 광고 단위 ID 복사

생성된 각 광고 단위의 ID를 복사합니다.

## 2. 환경 변수 설정

프론트엔드 루트 디렉토리에 `.env` 파일에 추가:

```bash
# 카카오 JavaScript 앱 키 (기존)
NEXT_PUBLIC_KAKAO_JAVA_SCRIPT_KEY=your_kakao_javascript_app_key_here

# 카카오 AdFit 광고 단위 ID (신규 추가)
NEXT_PUBLIC_MOBILE_AD=your_mobile_ad_unit_id_here
NEXT_PUBLIC_PC_AD=your_pc_ad_unit_id_here
```

**주의**: 실제 광고 단위 ID로 교체해야 합니다.

## 3. CSP (Content Security Policy) 설정

`next.config.ts`에서 카카오 광고 도메인이 허용되도록 설정되어 있습니다:

```typescript
// 이미 설정 완료됨
"script-src": "https://t1.daumcdn.net https://*.daumcdn.net",
"connect-src": "https://analytics.ad.daum.net https://kaat.daum.net https://kuid-provider.ds.kakao.com",
"img-src": "https://*.daumcdn.net https://t1.daumcdn.net",
"frame-src": "https://*.daumcdn.net https://analytics.ad.daum.net"
```

## 4. 광고 컴포넌트 사용법

### 4.1 기본 AdFit 배너

```tsx
import { AdFitBanner, AD_SIZES, getAdUnit } from "@/components/molecules/adfit-banner";

// 모바일 광고
<AdFitBanner
  adUnit={getAdUnit("MOBILE_BANNER")!}
  width={AD_SIZES.BANNER_320x50.width}
  height={AD_SIZES.BANNER_320x50.height}
  onAdFail={() => console.log("광고 로딩 실패")}
/>

// PC 광고
<AdFitBanner
  adUnit={getAdUnit("PC_BANNER")!}
  width={AD_SIZES.BANNER_728x90.width}
  height={AD_SIZES.BANNER_728x90.height}
/>
```

### 4.2 반응형 AdFit 배너 (권장)

```tsx
import { ResponsiveAdFitBanner } from "@/components/molecules/responsive-adfit-banner";

<ResponsiveAdFitBanner
  mobileAdUnit={getAdUnit("MOBILE_BANNER")!}
  pcAdUnit={getAdUnit("PC_BANNER")!}
  onAdFail={() => console.log("광고 로딩 실패")}
  className="my-4"
/>;
```

## 5. 사용 가능한 광고 크기

```typescript
export const AD_SIZES = {
  BANNER_320x50: { width: 320, height: 50 }, // 모바일 상단/하단
  BANNER_320x100: { width: 320, height: 100 }, // 모바일 중간
  BANNER_300x250: { width: 300, height: 250 }, // 모바일/PC 사각형
  BANNER_728x90: { width: 728, height: 90 }, // PC 상단/하단
} as const;
```

## 6. 광고 차단 및 오류 처리

### 6.1 자동 처리되는 경우

- 광고 차단기로 인한 스크립트 로딩 실패
- AdFit 서버에서 광고가 없는 경우 (NO-AD)
- CSP 정책에 의한 차단

### 6.2 오류 콜백 활용

```tsx
<AdFitBanner
  adUnit={adUnit}
  width={320}
  height={50}
  onAdFail={() => {
    // 광고 실패 시 대체 콘텐츠 표시
    console.log("광고를 표시할 수 없습니다.");
    // 분석 이벤트 전송 등
  }}
/>
```

## 7. 문제 해결

### 7.1 광고가 표시되지 않는 경우

1. **환경변수 확인**: `.env` 파일의 광고 단위 ID 확인
2. **도메인 등록**: AdFit에서 서비스 도메인이 올바르게 등록되었는지 확인
3. **개발자 도구**: 브라우저 콘솔에서 오류 메시지 확인
4. **광고 차단기**: 광고 차단기 비활성화 후 테스트

### 7.2 CSP 오류

```
Refused to load the script 'https://t1.daumcdn.net/kas/static/ba.min.js'
because it violates the following Content Security Policy directive
```

**해결됨**: CSP 설정이 이미 업데이트되어 해결되었습니다.

### 7.3 NO-AD 응답

```javascript
// AdFit에서 광고가 없을 때 자동으로 처리됨
console.log("AdFit 광고 로딩 실패:", element);
```

이는 정상적인 동작이며, 광고 인벤토리가 부족하거나 타겟팅 조건에 맞지 않을 때 발생합니다.

## 8. 성능 최적화

### 8.1 지연 로딩

AdFit 스크립트는 자동으로 비동기 로딩됩니다:

```typescript
script.async = true;
script.src = "https://t1.daumcdn.net/kas/static/ba.min.js";
```

### 8.2 중복 로딩 방지

같은 페이지에서 여러 광고를 사용할 때 스크립트 중복 로딩을 방지합니다:

```typescript
if (
  document.querySelector('script[src*="t1.daumcdn.net/kas/static/ba.min.js"]')
) {
  // 이미 로드된 경우 재사용
}
```

## 9. 테스트 환경

### 9.1 로컬 개발 환경

- AdFit에서 `localhost:3000` 도메인 등록 필요
- 개발 환경에서도 실제 광고가 표시됨

### 9.2 스테이징/프로덕션

- 실제 도메인 등록 후 테스트
- 광고 수익은 실제 서비스에서만 발생

---

## 통합 환경변수 설정 예시

```bash
# 카카오 공유 기능
NEXT_PUBLIC_KAKAO_JAVA_SCRIPT_KEY=abcdef1234567890abcdef1234567890

# 카카오 AdFit 광고
NEXT_PUBLIC_MOBILE_AD=DAN-1234567890abcdef
NEXT_PUBLIC_PC_AD=DAN-abcdef1234567890

# 기타 환경설정
NEXT_PUBLIC_API_URL=https://grow-farm.com/api
```
