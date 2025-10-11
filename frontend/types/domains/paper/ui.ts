import { RollingPaperMessage, VisitMessage } from '@/types/domains/paper';

/**
 * 롤링페이퍼 UI 상태 타입 정의
 */

// ===== Grid Related Types =====

/**
 * 그리드 위치 타입 (0-based)
 * UI와 백엔드 모두 0-based 좌표 사용
 */
export interface GridPosition {
  x: number; // 0-based (0~11, PC 2페이지=12칸, 모바일 3페이지=12칸)
  y: number; // 0-based (0~9)
}

/**
 * 메시지 그리드 타입
 * 2차원 배열로 표현되는 롤링페이퍼 그리드
 */
export type MessageGrid<T = RollingPaperMessage | VisitMessage> = (T | null)[][];

// ===== UI State Types =====

/**
 * 롤링페이퍼 뷰 상태
 */
export interface RollingPaperViewState {
  currentPage: number;
  isMessageListOpen: boolean;
  highlightedPosition: GridPosition | null;
  selectedMessages: number[];
}

/**
 * 메시지 폼 데이터
 */
export interface MessageFormData {
  content: string;
  anonymity: string;
  decoType: string;
}

/**
 * 롤링페이퍼 공유 옵션
 */
export interface ShareOptions {
  platform: 'kakao' | 'web' | 'clipboard';
  nickname: string;
  messageCount: number;
  url?: string;
}

// ===== Props Types =====

/**
 * 롤링페이퍼 컨테이너 Props
 */
export interface RollingPaperContainerProps {
  nickname?: string;
}

/**
 * 롤링페이퍼 뷰 Props
 */
import type { Toast } from "@/components";

export interface RollingPaperViewProps {
  targetNickname: string;
  isPublic: boolean;
  isOwner: boolean;
  isMobile: boolean;
  messages: (RollingPaperMessage | VisitMessage)[];
  messageCount: number;
  recentMessages: RollingPaperMessage[];
  totalPages: number;
  currentPage: number;
  setCurrentPage: (page: number) => void;
  isMessageListOpen: boolean;
  setIsMessageListOpen: (open: boolean) => void;
  highlightedPosition: GridPosition | null;
  clearHighlight: () => void;
  getMessageAt: (x: number, y: number) => RollingPaperMessage | VisitMessage | null;
  getCoordsFromPageAndGrid: (page: number, gridX: number, gridY: number) => GridPosition;
  handleWebShare: () => Promise<void>;
  handleMessageSubmit: (position: GridPosition, data: unknown) => Promise<void>;
  handleMessageClick: (message: RollingPaperMessage | VisitMessage) => void;
  refetchMessages: () => Promise<void>;
  toasts: Toast[];
  removeToast: (id: string) => void;
  showSuccess: (title: string, message: string) => void;
  showError: (title: string, message: string) => void;
}

// ===== Event Handler Types =====

/**
 * 메시지 작성 핸들러 파라미터
 */
export interface CreateMessageParams {
  userName: string;
  content: string;
  anonymity: string;
  decoType: string;
  x: number;
  y: number;
}

/**
 * 메시지 삭제 핸들러 파라미터
 */
export interface DeleteMessageParams {
  messageId: number;
}

// ===== Filter/Sort Types =====

/**
 * 메시지 필터 옵션
 */
export interface MessageFilterOptions {
  decoType?: string;
  dateRange?: {
    start: Date;
    end: Date;
  };
}

/**
 * 메시지 정렬 옵션
 */
export interface MessageSortOptions {
  field: 'createdAt' | 'position';
  order: 'asc' | 'desc';
}