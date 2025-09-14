import { ImageResponse } from "@vercel/og";
import { NextRequest } from "next/server";

export const runtime = "edge";

export async function GET(req: NextRequest) {
  try {
    const { searchParams } = new URL(req.url);
    const title = searchParams.get("title") || "비밀로그";
    const author = searchParams.get("author") || "";
    const type = searchParams.get("type") || "default";
    const description = searchParams.get("description") || "";
    const date = searchParams.get("date") || "";
    const tags = searchParams.get("tags") || "";

    // 타입별 디자인 설정
    const getDesignByType = () => {
      switch (type) {
        case "post":
          return {
            bgGradient: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
            accentColor: "#fbbf24",
            subtitle: "커뮤니티 게시글",
            icon: "📝",
          };
        case "paper":
          return {
            bgGradient: "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)",
            accentColor: "#10b981",
            subtitle: "익명 롤링페이퍼",
            icon: "💌",
          };
        case "search":
          return {
            bgGradient: "linear-gradient(135deg, #0ea5e9 0%, #6366f1 100%)",
            accentColor: "#fbbf24",
            subtitle: "검색 결과",
            icon: "🔍",
          };
        case "profile":
          return {
            bgGradient: "linear-gradient(135deg, #10b981 0%, #14b8a6 100%)",
            accentColor: "#f59e0b",
            subtitle: "사용자 프로필",
            icon: "👤",
          };
        case "error":
          return {
            bgGradient: "linear-gradient(135deg, #ef4444 0%, #f97316 100%)",
            accentColor: "#fbbf24",
            subtitle: "오류 페이지",
            icon: "⚠️",
          };
        default:
          return {
            bgGradient: "linear-gradient(135deg, #667eea 0%, #f093fb 100%)",
            accentColor: "#fbbf24",
            subtitle: "익명으로 소통하는 새로운 공간",
            icon: "🔒",
          };
      }
    };

    const design = getDesignByType();

    return new ImageResponse(
      (
        <div
          style={{
            height: "100%",
            width: "100%",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            background: design.bgGradient,
            position: "relative",
            fontFamily: "system-ui, -apple-system, sans-serif",
          }}
        >
          {/* 배경 패턴 */}
          <div
            style={{
              position: "absolute",
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              backgroundImage: `radial-gradient(circle at 1px 1px, rgba(255,255,255,0.1) 1px, transparent 1px)`,
              backgroundSize: "40px 40px",
            }}
          />

          {/* 메인 콘텐츠 컨테이너 */}
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              backgroundColor: "rgba(255, 255, 255, 0.95)",
              borderRadius: "24px",
              padding: "60px",
              boxShadow: "0 20px 60px rgba(0, 0, 0, 0.3)",
              maxWidth: "1000px",
              width: "90%",
              position: "relative",
            }}
          >
            {/* 로고/브랜드 */}
            <div
              style={{
                display: "flex",
                alignItems: "center",
                marginBottom: "30px",
              }}
            >
              <div
                style={{
                  fontSize: "32px",
                  fontWeight: 700,
                  background: design.bgGradient,
                  backgroundClip: "text",
                  color: "transparent",
                }}
              >
                비밀로그
              </div>
            </div>

            {/* 서브타이틀 */}
            <div
              style={{
                fontSize: "24px",
                color: "#6b7280",
                marginBottom: "30px",
              }}
            >
              {design.subtitle}
            </div>

            {/* 타이틀 */}
            <div
              style={{
                fontSize: title.length > 30 ? "48px" : "56px",
                fontWeight: 700,
                color: "#1f2937",
                textAlign: "center",
                lineHeight: 1.2,
                maxWidth: "800px",
                marginBottom: author ? "20px" : "0",
              }}
            >
              {title}
            </div>

            {/* 작성자 */}
            {author && (
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  marginTop: "20px",
                  padding: "12px 24px",
                  backgroundColor: "rgba(107, 114, 128, 0.1)",
                  borderRadius: "100px",
                }}
              >
                <div
                  style={{
                    fontSize: "20px",
                    color: "#6b7280",
                  }}
                >
                  {author}
                </div>
              </div>
            )}

            {/* 하단 URL */}
            <div
              style={{
                position: "absolute",
                bottom: "30px",
                fontSize: "18px",
                color: "#9ca3af",
                letterSpacing: "0.5px",
              }}
            >
              grow-farm.com
            </div>
          </div>
        </div>
      ),
      {
        width: 1200,
        height: 630,
      }
    );
  } catch (e) {
    console.error("OG Image generation failed:", e);
    return new Response(`Failed to generate image`, {
      status: 500,
    });
  }
}
