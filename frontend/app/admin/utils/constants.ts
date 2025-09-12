export const ADMIN_TABS = {
  REPORTS: "reports",
  STATS: "stats",
} as const;

export const FILTER_OPTIONS = [
  { value: "all", label: "전체 유형" },
  { value: "POST", label: "게시글" },
  { value: "COMMENT", label: "댓글" },
  { value: "ERROR", label: "오류" },
  { value: "IMPROVEMENT", label: "개선사항" },
] as const;

export const PAGE_SIZES = {
  DEFAULT: 20,
  SMALL: 10,
  LARGE: 50,
} as const;

export const DEBOUNCE_DELAYS = {
  SEARCH: 300,
  FILTER: 0,
} as const;

export const TOUCH_TARGET_SIZE = 48;

export const BREAKPOINTS = {
  MOBILE: 640,
  TABLET: 768,
  DESKTOP: 1024,
} as const;

export const MESSAGES = {
  ERROR: {
    FETCH_REPORTS: "신고 목록을 불러오는 중 오류가 발생했습니다.",
    BAN_USER: "사용자 제재 중 오류가 발생했습니다.",
    WITHDRAW_USER: "사용자 탈퇴 처리 중 오류가 발생했습니다.",
    NO_TARGET_ID: "대상 ID가 없습니다.",
    PERMISSION_DENIED: "권한이 없습니다.",
  },
  SUCCESS: {
    BAN_USER: "사용자가 성공적으로 제재되었습니다.",
    WITHDRAW_USER: "사용자가 성공적으로 탈퇴 처리되었습니다.",
  },
  CONFIRM: {
    BAN_USER: "정말로 이 사용자를 제재하시겠습니까? 이 작업은 되돌릴 수 없습니다.",
    WITHDRAW_USER: "정말로 이 사용자를 강제 탈퇴시키시겠습니까? 이 작업은 되돌릴 수 없습니다.",
  },
} as const;