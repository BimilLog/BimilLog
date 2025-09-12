import type { RollingPaperMessage, VisitMessage } from "@/types/domains/paper";

/**
 * 롤링페이퍼 좌표 시스템 유틸리티
 * 
 * 백엔드: 1-based 좌표 (x: 1~6/4, y: 1~10)
 * 프론트엔드: 0-based 좌표 (x: 0~5/3, y: 0~9)
 */

export interface CoordinateConfig {
  isMobile: boolean;
  maxPages?: number;
}

/**
 * 좌표 시스템 설정값 가져오기
 */
export function getCoordinateConfig(isMobile: boolean) {
  const pageWidth = isMobile ? 4 : 6; // 페이지당 가로 칸 수
  const pageHeight = 10; // 페이지당 세로 칸 수 (고정)
  const maxPages = isMobile ? 3 : 2; // 최대 페이지 수
  const canvasWidth = pageWidth * maxPages; // 전체 캔버스 가로 크기
  const canvasHeight = pageHeight; // 전체 캔버스 세로 크기
  
  return {
    pageWidth,
    pageHeight,
    maxPages,
    canvasWidth,
    canvasHeight,
    totalPages: maxPages,
  };
}

/**
 * 프론트엔드 좌표를 백엔드 좌표로 변환 (0-based → 1-based)
 */
export function frontendToBackend(x: number, y: number) {
  return { x: x + 1, y: y + 1 };
}

/**
 * 백엔드 좌표를 프론트엔드 좌표로 변환 (1-based → 0-based)
 */
export function backendToFrontend(x: number, y: number) {
  return { x: x - 1, y: y - 1 };
}

/**
 * 좌표로부터 페이지 번호 계산
 */
export function getPageFromCoords(x: number, y: number, pageWidth: number): number {
  return Math.floor(x / pageWidth) + 1;
}

/**
 * 좌표를 그리드 내 위치로 변환
 */
export function getGridPosFromCoords(x: number, y: number, pageWidth: number) {
  const gridX = x % pageWidth;
  const gridY = y;
  return { gridX, gridY };
}

/**
 * 페이지와 그리드 위치를 좌표로 변환
 */
export function getCoordsFromPageAndGrid(
  page: number,
  gridX: number,
  gridY: number,
  pageWidth: number
) {
  const x = (page - 1) * pageWidth + gridX;
  const y = gridY;
  return { x, y };
}

/**
 * 메시지 위치 Map 생성 (성능 최적화)
 */
export function createMessagePositionMap(
  messages: (RollingPaperMessage | VisitMessage)[]
) {
  const map = new Map<string, RollingPaperMessage | VisitMessage>();
  messages.forEach((message) => {
    const key = `${message.x},${message.y}`;
    map.set(key, message);
  });
  return map;
}

/**
 * 특정 위치에 메시지가 있는지 확인
 */
export function isPositionOccupied(
  x: number,
  y: number,
  messagePositionMap: Map<string, RollingPaperMessage | VisitMessage>
): boolean {
  if (typeof x !== 'number' || typeof y !== 'number') return false;
  if (x < 0 || y < 0) return false;
  
  // 프론트엔드 좌표를 백엔드 좌표로 변환하여 비교
  const { x: backendX, y: backendY } = frontendToBackend(x, y);
  const key = `${backendX},${backendY}`;
  return messagePositionMap.has(key);
}

/**
 * 특정 위치의 메시지 가져오기
 */
export function getMessageAt(
  x: number,
  y: number,
  messagePositionMap: Map<string, RollingPaperMessage | VisitMessage>
): RollingPaperMessage | VisitMessage | null {
  if (typeof x !== 'number' || typeof y !== 'number') return null;
  if (x < 0 || y < 0) return null;
  
  // 프론트엔드 좌표를 백엔드 좌표로 변환하여 조회
  const { x: backendX, y: backendY } = frontendToBackend(x, y);
  const key = `${backendX},${backendY}`;
  return messagePositionMap.get(key) || null;
}

/**
 * 빈 위치 찾기
 */
export function findEmptyPosition(
  canvasWidth: number,
  canvasHeight: number,
  messagePositionMap: Map<string, RollingPaperMessage | VisitMessage>
): { x: number; y: number } | null {
  for (let y = 0; y < canvasHeight; y++) {
    for (let x = 0; x < canvasWidth; x++) {
      if (!isPositionOccupied(x, y, messagePositionMap)) {
        return { x, y };
      }
    }
  }
  return null;
}

/**
 * 특정 위치 주변의 빈 위치 찾기
 */
export function findNearbyEmptyPosition(
  centerX: number,
  centerY: number,
  canvasWidth: number,
  canvasHeight: number,
  messagePositionMap: Map<string, RollingPaperMessage | VisitMessage>,
  radius: number = 3
): { x: number; y: number } | null {
  for (let r = 1; r <= radius; r++) {
    for (let dy = -r; dy <= r; dy++) {
      for (let dx = -r; dx <= r; dx++) {
        if (Math.abs(dx) === r || Math.abs(dy) === r) {
          const x = centerX + dx;
          const y = centerY + dy;
          if (
            x >= 0 &&
            x < canvasWidth &&
            y >= 0 &&
            y < canvasHeight &&
            !isPositionOccupied(x, y, messagePositionMap)
          ) {
            return { x, y };
          }
        }
      }
    }
  }
  // 인근에 없으면 아무데나
  return findEmptyPosition(canvasWidth, canvasHeight, messagePositionMap);
}