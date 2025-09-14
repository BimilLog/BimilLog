import { ImageResponse } from "next/og";
import { logger } from '@/lib/utils/logger';

export const runtime = "edge";

// Type definitions
interface OGParams {
  title?: string;
  author?: string;
}

interface ErrorResponse {
  error: string;
  details: string;
  timestamp: string;
}

// Style constants
const STYLES = {
  container: {
    height: "100%",
    width: "100%",
    display: "flex",
    flexDirection: "column" as const,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#fff",
    fontFamily: '"Pretendard"',
    backgroundImage: "linear-gradient(to bottom right, #E0E7FF 25%, #F3E8FF 75%)",
  },
  card: {
    display: "flex",
    flexDirection: "column" as const,
    alignItems: "center",
    justifyContent: "center",
    textAlign: "center" as const,
    padding: "40px",
    border: "2px solid #C4B5FD",
    borderRadius: "16px",
    backgroundColor: "rgba(255, 255, 255, 0.8)",
    boxShadow: "0 10px 25px -5px rgba(0, 0, 0, 0.1)",
  },
  title: {
    fontSize: 60,
    fontWeight: 700,
    color: "#4C1D95",
    marginBottom: "20px",
    lineHeight: 1.3,
    maxHeight: "2.6em",
    overflow: "hidden",
    textOverflow: "ellipsis",
  },
  author: {
    fontSize: 32,
    color: "#6D28D9",
  },
  logo: {
    position: "absolute" as const,
    bottom: 30,
    right: 40,
    display: "flex",
    alignItems: "center",
    fontSize: 24,
    color: "#5B21B6",
  },
  logoIcon: {
    marginRight: "8px",
    width: "24px",
    height: "24px",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
} as const;

const COLORS = {
  primary: "#7c3aed",
  stroke: "#7c3aed",
} as const;

const OG_CONFIG = {
  width: 1200,
  height: 630,
  defaultTitle: "비밀로그",
  maxTitleLength: 100,
  maxAuthorLength: 50,
} as const;

// Helper functions
function validateAndSanitizeParams(searchParams: URLSearchParams): OGParams {
  const title = searchParams.get("title")?.slice(0, OG_CONFIG.maxTitleLength) || undefined;
  const author = searchParams.get("author")?.slice(0, OG_CONFIG.maxAuthorLength) || undefined;

  return { title, author };
}

function createErrorResponse(errorMessage: string): Response {
  const errorResponse: ErrorResponse = {
    error: "Failed to generate OG image",
    details: errorMessage,
    timestamp: new Date().toISOString(),
  };

  return new Response(JSON.stringify(errorResponse), {
    status: 500,
    headers: { "Content-Type": "application/json" },
  });
}

function BimilLogIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M9.88 9.88a3 3 0 1 0 4.24 4.24"
        stroke={COLORS.stroke}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="m2 2 20 20"
        stroke={COLORS.stroke}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M10.5 16.5a5 5 0 0 0 7-7l-2-2a5 5 0 0 0-7 7l2 2Z"
        stroke={COLORS.stroke}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="m17 17-2.5-2.5"
        stroke={COLORS.stroke}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export async function GET(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const { title, author } = validateAndSanitizeParams(searchParams);

    return new ImageResponse(
      (
        <div style={STYLES.container}>
          <div style={STYLES.card}>
            <div style={STYLES.title}>
              {title || OG_CONFIG.defaultTitle}
            </div>
            {author && (
              <div style={STYLES.author}>
                - {author} -
              </div>
            )}
            <div style={STYLES.logo}>
              <div style={STYLES.logoIcon}>
                <BimilLogIcon />
              </div>
              <span>비밀로그</span>
            </div>
          </div>
        </div>
      ),
      {
        width: OG_CONFIG.width,
        height: OG_CONFIG.height,
      }
    );
  } catch (e: unknown) {
    const errorMessage = e instanceof Error ? e.message : "Unknown error occurred";
    logger.error("OG Image generation failed:", { error: errorMessage, url: request.url });

    return createErrorResponse(errorMessage);
  }
}