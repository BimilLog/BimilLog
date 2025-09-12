import { AuthTokens } from "@/types/domains/auth";

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
      console.error("Failed to save tokens:", error);
    }
  }

  static getTokens(): AuthTokens | null {
    try {
      const tokenData = localStorage.getItem(this.TOKEN_STORAGE_KEY);
      if (!tokenData) return null;
      
      const parsed = JSON.parse(tokenData);
      
      if (this.isTokenExpired(parsed)) {
        this.clearTokens();
        return null;
      }
      
      return parsed;
    } catch (error) {
      console.error("Failed to get tokens:", error);
      return null;
    }
  }

  static clearTokens(): void {
    try {
      localStorage.removeItem(this.TOKEN_STORAGE_KEY);
    } catch (error) {
      console.error("Failed to clear tokens:", error);
    }
  }

  static isTokenExpired(tokenData: any): boolean {
    if (!tokenData.expiresIn || !tokenData.savedAt) return false;
    
    const now = Date.now();
    const expiryTime = tokenData.savedAt + (tokenData.expiresIn * 1000);
    
    return now >= expiryTime;
  }

  static isAccessTokenValid(): boolean {
    const tokens = this.getTokens();
    return tokens?.accessToken && !this.isTokenExpired(tokens);
  }
}

export const tokenManager = TokenManager;