import { ImageResponse } from "next/og";

export const runtime = "edge";

export async function GET(request: Request) {
  try {
    const { searchParams } = new URL(request.url);

    const title = searchParams.get("title")?.slice(0, 100);
    const author = searchParams.get("author")?.slice(0, 50);

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
            backgroundColor: "#fff",
            fontFamily: '"Pretendard"',
            backgroundImage:
              "linear-gradient(to bottom right, #E0E7FF 25%, #F3E8FF 75%)",
          }}
        >
          <div
            style={{
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
                  fontSize: 32,
                  color: "#6D28D9",
                }}
              >
                - {author} -
              </div>
            )}
            <div
              style={{
                position: "absolute",
                bottom: 30,
                right: 40,
                display: "flex",
                alignItems: "center",
                fontSize: 24,
                color: "#5B21B6",
              }}
            >
              <span style={{ marginRight: "8px" }}>🤫</span>
              <span>비밀로그</span>
            </div>
          </div>
        </div>
      ),
      {
        width: 1200,
        height: 630,
      }
    );
  } catch (e: any) {
    console.error(`OG Image generation failed: ${e.message}`);
    return new Response("Failed to generate OG image", { status: 500 });
  }
}
