import type { RollingPaperMessage, VisitMessage } from '@/types/domains/paper';

/**
 * 롤링페이퍼 그리드 설정
 */
export const GRID_CONFIG = {
  ROWS: 10,
  COLS_MOBILE: 4,
  COLS_PC: 6,
} as const;

/**
 * 페이지당 메시지 개수
 */
export const MESSAGES_PER_PAGE = {
  MOBILE: GRID_CONFIG.ROWS * GRID_CONFIG.COLS_MOBILE, // 40개
  PC: GRID_CONFIG.ROWS * GRID_CONFIG.COLS_PC, // 60개
} as const;

/**
 * 그리드 좌표 타입
 */
export interface GridPosition {
  x: number; // 1-based (1~6 또는 1~4)
  y: number; // 1-based (1~10)
}

/**
 * DB 인덱스 타입
 */
export interface DBIndex {
  rowIndex: number; // 0-based (0~9)
  colIndex: number; // 0-based (0~5 또는 0~3)
}

/**
 * UI 좌표(1-based)를 DB 인덱스(0-based)로 변환
 */
export function gridPositionToDBIndex(position: GridPosition): DBIndex {
  return {
    rowIndex: position.y - 1,
    colIndex: position.x - 1,
  };
}

/**
 * DB 인덱스(0-based)를 UI 좌표(1-based)로 변환
 */
export function dbIndexToGridPosition(index: DBIndex): GridPosition {
  return {
    x: index.colIndex + 1,
    y: index.rowIndex + 1,
  };
}

/**
 * 디바이스가 모바일인지 확인
 */
export function isMobileDevice(): boolean {
  if (typeof window === 'undefined') return false;
  return window.innerWidth < 768;
}

/**
 * 현재 디바이스의 그리드 열 개수 반환
 */
export function getGridColumns(): number {
  return isMobileDevice() ? GRID_CONFIG.COLS_MOBILE : GRID_CONFIG.COLS_PC;
}

/**
 * 현재 디바이스의 페이지당 메시지 개수 반환
 */
export function getMessagesPerPage(): number {
  return isMobileDevice() ? MESSAGES_PER_PAGE.MOBILE : MESSAGES_PER_PAGE.PC;
}

/**
 * 메시지 배열을 2차원 그리드로 변환
 */
export function createMessageGrid<T extends { x: number; y: number }>(
  messages: T[],
  cols: number = getGridColumns()
): (T | null)[][] {
  const grid: (T | null)[][] = [];
  const rows = GRID_CONFIG.ROWS;

  // 빈 그리드 초기화
  for (let i = 0; i < rows; i++) {
    grid[i] = new Array(cols).fill(null);
  }

  // 메시지를 그리드에 배치
  messages.forEach(message => {
    const { rowIndex, colIndex } = gridPositionToDBIndex({
      x: message.x,
      y: message.y,
    });

    if (
      rowIndex >= 0 &&
      rowIndex < rows &&
      colIndex >= 0 &&
      colIndex < cols
    ) {
      grid[rowIndex][colIndex] = message;
    }
  });

  return grid;
}

/**
 * 전체 페이지 수 계산
 * PC: 2페이지, 모바일: 3페이지로 고정
 */
export function calculateTotalPages(): number {
  return isMobileDevice() ? 3 : 2;
}

/**
 * 페이지와 그리드 내 위치에서 실제 전체 좌표 계산
 * 예: 2페이지의 (1,2) -> PC에서 실제 좌표 (7,2)
 * 입력: 0-based gridX/gridY, 출력: 1-based x/y
 */
export function getAbsoluteCoords(
  page: number,
  gridX: number,
  gridY: number
): GridPosition {
  const cols = getGridColumns();
  const baseX = (page - 1) * cols;
  return {
    x: baseX + gridX + 1, // 1-based로 변환
    y: gridY + 1, // 1-based로 변환
  };
}

/**
 * 절대 좌표에서 페이지와 페이지 내 좌표 계산
 */
export function getPageAndGridPosition(absoluteX: number, absoluteY: number): {
  page: number;
  gridX: number;
  gridY: number;
} {
  const cols = getGridColumns();
  const page = Math.floor((absoluteX - 1) / cols) + 1;
  const gridX = ((absoluteX - 1) % cols) + 1;

  return {
    page,
    gridX,
    gridY: absoluteY,
  };
}


/**
 * 특정 위치에 메시지가 있는지 확인
 */
export function isPositionOccupied<T extends { x: number; y: number }>(
  messages: T[],
  position: GridPosition
): boolean {
  return messages.some(msg => msg.x === position.x && msg.y === position.y);
}

/**
 * 빈 위치 찾기 (메시지 작성 가능한 위치)
 */
export function findEmptyPositions<T extends { x: number; y: number }>(
  messages: T[],
  page: number = 1
): GridPosition[] {
  const emptyPositions: GridPosition[] = [];
  const cols = getGridColumns();
  const rows = GRID_CONFIG.ROWS;

  const startCol = (page - 1) * cols + 1;
  const endCol = startCol + cols - 1;

  for (let y = 1; y <= rows; y++) {
    for (let x = startCol; x <= endCol; x++) {
      const position = { x, y };
      if (!isPositionOccupied(messages, position)) {
        emptyPositions.push(position);
      }
    }
  }

  return emptyPositions;
}

/**
 * 롤링페이퍼 공유 URL 생성
 */
export function getRollingPaperShareUrl(nickname: string): string {
  if (typeof window === 'undefined') {
    return `https://grow-farm.com/rolling-paper/${encodeURIComponent(nickname)}`;
  }
  return `${window.location.origin}/rolling-paper/${encodeURIComponent(nickname)}`;
}

/**
 * 롤링페이퍼 공유 데이터 생성
 */
export function getRollingPaperShareData(nickname: string, messageCount: number) {
  return {
    title: `${nickname}님의 롤링페이퍼`,
    text: `${nickname}님에게 익명으로 따뜻한 메시지를 남겨보세요! 현재 ${messageCount}개의 메시지가 있어요`,
    url: getRollingPaperShareUrl(nickname),
  };
}