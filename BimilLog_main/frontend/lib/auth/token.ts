import { AuthTokens } from "@/types/domains/auth";
import { logger } from "@/lib/utils";

export class TokenManager {
  private static readonly TOKEN_STORAGE_KEY = "bimillog_tokens";
  
  static saveTokens(tokens: AuthTokens): void {
    try {
      const tokenData = {
        ...tokens,
        savedAt: Date.now()
      };
      localStorage.setItem(this.TOKEN_STORAGE_KEY, JSON.stringify(tokenData));
    } catch (error) {
      logger.error("Failed to save tokens:", error);
    }
  }

  static getTokens(): AuthTokens | null {
    try {
      const tokenData = localStorage.getItem(this.TOKEN_STORAGE_KEY);
      if (!tokenData) return null;

      const parsed = JSON.parse(tokenData);

      // 토큰 만료 시 자동으로 정리하는 이중 체크 패턴
      // 1차: 만료 여부 확인
      if (this.isTokenExpired(parsed)) {
        // 2차: 만료된 토큰 자동 삭제로 메모리 정리
        this.clearTokens();
        return null;
      }

      return parsed;
    } catch (error) {
      logger.error("Failed to get tokens:", error);
      return null;
    }
  }

  static clearTokens(): void {
    try {
      localStorage.removeItem(this.TOKEN_STORAGE_KEY);
    } catch (error) {
      logger.error("Failed to clear tokens:", error);
    }
  }

  static isTokenExpired(tokenData: AuthTokens & { savedAt?: number }): boolean {
    // 만료 시간 정보나 저장 시간이 없으면 만료되지 않은 것으로 처리
    if (!tokenData.expiresIn || !tokenData.savedAt) return false;

    const now = Date.now();
    // 토큰 만료 시간 계산: 저장 시점 + (만료 시간(초) * 1000으로 밀리초 변환)
    const expiryTime = tokenData.savedAt + (tokenData.expiresIn * 1000);

    // 현재 시간이 만료 시간보다 크거나 같으면 만료됨
    return now >= expiryTime;
  }

  static isAccessTokenValid(): boolean {
    const tokens = this.getTokens();
    // 토큰 존재 여부와 만료 여부를 동시에 검사하는 이중 체크
    // Boolean 변환으로 undefined/null을 false로 변환
    return !!(tokens?.accessToken && !this.isTokenExpired(tokens));
  }
}

export const tokenManager = TokenManager;