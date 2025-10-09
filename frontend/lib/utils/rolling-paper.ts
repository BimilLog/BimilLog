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
  x: number; // 0-based (0~5 PC, 0~3 Mobile, 최대 0~11)
  y: number; // 0-based (0~9)
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
export function getGridColumns(isMobile?: boolean): number {
  const mobile = isMobile !== undefined ? isMobile : isMobileDevice();
  return mobile ? GRID_CONFIG.COLS_MOBILE : GRID_CONFIG.COLS_PC;
}

/**
 * 현재 디바이스의 페이지당 메시지 개수 반환
 */
export function getMessagesPerPage(isMobile?: boolean): number {
  const mobile = isMobile !== undefined ? isMobile : isMobileDevice();
  return mobile ? MESSAGES_PER_PAGE.MOBILE : MESSAGES_PER_PAGE.PC;
}

/**
 * 메시지 배열을 2차원 그리드로 변환
 * 전체 좌표 범위(x: 0~11, y: 0~9)를 처리하도록 수정
 */
export function createMessageGrid<T extends { x: number; y: number }>(
  messages: T[]
): (T | null)[][] {
  // 전체 좌표 범위를 처리하기 위해 최대 열 개수 사용 (12열: x=0~11)
  const maxCols = 12; // PC 2페이지(0~11), 모바일 3페이지(0~11)
  const grid: (T | null)[][] = [];
  const rows = GRID_CONFIG.ROWS;

  // 빈 그리드 초기화 (12열로 초기화)
  for (let i = 0; i < rows; i++) {
    grid[i] = new Array(maxCols).fill(null);
  }

  // 메시지를 그리드에 배치 (이미 0-based)
  messages.forEach(message => {
    const rowIndex = message.y;
    const colIndex = message.x;

    if (
      rowIndex >= 0 &&
      rowIndex < rows &&
      colIndex >= 0 &&
      colIndex < maxCols
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
export function calculateTotalPages(isMobile: boolean): number {
  return isMobile ? 3 : 2;
}

/**
 * 페이지와 그리드 내 위치에서 실제 전체 좌표 계산
 * 예: 2페이지의 (1,2) -> PC에서 실제 좌표 (7,2)
 * 입력: 0-based gridX/gridY, 출력: 0-based x/y
 */
export function getAbsoluteCoords(
  page: number,
  gridX: number,
  gridY: number,
  isMobile: boolean
): GridPosition {
  const cols = getGridColumns(isMobile);
  const baseX = (page - 1) * cols;
  return {
    x: baseX + gridX, // 0-based 유지
    y: gridY, // 0-based 유지
  };
}

/**
 * 절대 좌표에서 페이지와 페이지 내 좌표 계산
 * 입력: 0-based absoluteX/absoluteY, 출력: page(1-based), gridX/gridY(0-based)
 */
export function getPageAndGridPosition(absoluteX: number, absoluteY: number, isMobile?: boolean): {
  page: number;
  gridX: number;
  gridY: number;
} {
  const cols = getGridColumns(isMobile);
  const page = Math.floor(absoluteX / cols) + 1;
  const gridX = absoluteX % cols;

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
  page: number = 1,
  isMobile?: boolean
): GridPosition[] {
  const emptyPositions: GridPosition[] = [];
  const cols = getGridColumns(isMobile);
  const rows = GRID_CONFIG.ROWS;

  const startCol = (page - 1) * cols;
  const endCol = startCol + cols;

  for (let y = 0; y < rows; y++) {
    for (let x = startCol; x < endCol; x++) {
      const position = { x, y };
      if (!isPositionOccupied(messages, position)) {
        emptyPositions.push(position);
      }
    }
  }

  return emptyPositions;
}

/**
 * 맨해튼 거리 계산 (그리드 기반 거리)
 */
function calculateManhattanDistance(pos1: GridPosition, pos2: GridPosition): number {
  return Math.abs(pos1.x - pos2.x) + Math.abs(pos1.y - pos2.y);
}

/**
 * 특정 위치에서 가장 가까운 빈 위치 찾기
 * 맨해튼 거리 기준으로 정렬하여 반환
 */
export function findNearestEmptyPositions<T extends { x: number; y: number }>(
  messages: T[],
  fromPosition: GridPosition,
  count: number = 3
): GridPosition[] {
  const allEmptyPositions: GridPosition[] = [];
  const maxCols = 12; // 전체 좌표 범위 (0~11)
  const rows = GRID_CONFIG.ROWS;

  // 전체 좌표 범위에서 빈 위치 찾기
  for (let y = 0; y < rows; y++) {
    for (let x = 0; x < maxCols; x++) {
      const position = { x, y };
      if (!isPositionOccupied(messages, position)) {
        allEmptyPositions.push(position);
      }
    }
  }

  // 맨해튼 거리 기준으로 정렬
  const sortedPositions = allEmptyPositions
    .map(pos => ({
      position: pos,
      distance: calculateManhattanDistance(fromPosition, pos)
    }))
    .sort((a, b) => a.distance - b.distance)
    .slice(0, count)
    .map(item => item.position);

  return sortedPositions;
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