import { ImageResponse } from "next/og";
import { logger } from '@/lib/utils/logger';

export const runtime = "edge";

export async function GET(request: Request) {
  try {
    // URL에서 쿼리 파라미터 추출
    const { searchParams } = new URL(request.url);

    // 제목과 작성자 정보 가져오기 (길이 제한으로 안전성 확보)
    const title = searchParams.get("title")?.slice(0, 100);
    const author = searchParams.get("author")?.slice(0, 50);

    // OG 이미지 생성 (1200x630 크기의 이미지 반환)
    return new ImageResponse(
      (
        <div
          style={{
            // 전체 컨테이너 스타일 (보라색 그라데이션 배경)
            height: "100%",
            width: "100%",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            backgroundColor: "#fff",
            fontFamily: '"Pretendard"',
            backgroundImage:
              "linear-gradient(to bottom right, #E0E7FF 25%, #F3E8FF 75%)",
          }}
        >
          <div
            style={{
              // 메인 컨텐츠 카드 스타일 (반투명 배경과 그림자)
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              textAlign: "center",
              padding: "40px",
              border: "2px solid #C4B5FD",
              borderRadius: "16px",
              backgroundColor: "rgba(255, 255, 255, 0.8)",
              boxShadow: "0 10px 25px -5px rgba(0, 0, 0, 0.1)",
            }}
          >
            <div
              style={{
                // 제목 텍스트 스타일 (큰 글씨, 진한 보라색)
                fontSize: 60,
                fontWeight: 700,
                color: "#4C1D95",
                marginBottom: "20px",
                lineHeight: 1.3,
                maxHeight: "2.6em",
                overflow: "hidden",
                textOverflow: "ellipsis",
              }}
            >
              {title || "비밀로그"}
            </div>
            {author && (
              <div
                style={{
                  // 작성자 텍스트 스타일 (작은 글씨, 연한 보라색)
                  fontSize: 32,
                  color: "#6D28D9",
                }}
              >
                - {author} -
              </div>
            )}
            <div
              style={{
                // 브랜드 로고 영역 (우측 하단 고정)
                position: "absolute",
                bottom: 30,
                right: 40,
                display: "flex",
                alignItems: "center",
                fontSize: 24,
                color: "#5B21B6",
              }}
            >
              <div style={{ marginRight: "8px", width: "24px", height: "24px", display: "flex", alignItems: "center", justifyContent: "center" }}>
                {/* 비밀로그 아이콘 (눈 가리기 모양) */}
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" stroke="#7c3aed" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                  <path d="m2 2 20 20" stroke="#7c3aed" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                  <path d="M10.5 16.5a5 5 0 0 0 7-7l-2-2a5 5 0 0 0-7 7l2 2Z" stroke="#7c3aed" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                  <path d="m17 17-2.5-2.5" stroke="#7c3aed" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </div>
              <span>비밀로그</span>
            </div>
          </div>
        </div>
      ),
      {
        // OG 이미지 표준 크기 (SNS 공유 최적화)
        width: 1200,
        height: 630,
      }
    );
  } catch (e: unknown) {
    // 에러 처리: 이미지 생성 실패 시 JSON 에러 응답 반환
    const errorMessage = e instanceof Error ? e.message : "Unknown error";
    logger.error(`OG Image generation failed: ${errorMessage}`);

    return new Response(
      JSON.stringify({
        error: "Failed to generate OG image",
        details: errorMessage,
        timestamp: new Date().toISOString()
      }),
      {
        status: 500,
        headers: { "Content-Type": "application/json" }
      }
    );
  }
}
