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
              <div style={{ marginRight: "8px", width: "24px", height: "24px", display: "flex", alignItems: "center", justifyContent: "center" }}>
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
        width: 1200,
        height: 630,
      }
    );
  } catch (e: unknown) {
    const errorMessage = e instanceof Error ? e.message : "Unknown error";
    console.error(`OG Image generation failed: ${errorMessage}`);
    
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
