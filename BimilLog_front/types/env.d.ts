declare namespace NodeJS {
  interface ProcessEnv {
    // API URLs
    NEXT_PUBLIC_API_URL: string;
    NEXT_PUBLIC_KAKAO_REDIRECT_URI: string;
    
    // Kakao OAuth
    NEXT_PUBLIC_KAKAO_AUTH_URL: string;
    NEXT_PUBLIC_KAKAO_CLIENT_ID: string;
    NEXT_PUBLIC_KAKAO_JAVA_SCRIPT_KEY: string;
    
    // AdFit
    NEXT_PUBLIC_MOBILE_AD: string;
    NEXT_PUBLIC_PC_AD: string;
    
    // Next.js
    NODE_ENV: 'development' | 'production' | 'test';
  }
}

interface Window {
  gtag?: (...args: unknown[]) => void;
}