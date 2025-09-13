/**
 * 도메인별 에러 정의
 * 각 도메인에서 발생할 수 있는 에러 타입과 메시지를 정의합니다.
 */

export enum ErrorDomain {
  AUTH = 'AUTH',
  USER = 'USER',
  POST = 'POST',
  COMMENT = 'COMMENT',
  PAPER = 'PAPER',
  NOTIFICATION = 'NOTIFICATION',
  ADMIN = 'ADMIN',
  NETWORK = 'NETWORK',
  UNKNOWN = 'UNKNOWN'
}

export enum ErrorCode {
  // Auth errors
  AUTH_EXPIRED = 'AUTH_EXPIRED',
  AUTH_INVALID_TOKEN = 'AUTH_INVALID_TOKEN',
  AUTH_UNAUTHORIZED = 'AUTH_UNAUTHORIZED',
  AUTH_DUPLICATE_LOGIN = 'AUTH_DUPLICATE_LOGIN',

  // User errors
  USER_NOT_FOUND = 'USER_NOT_FOUND',
  USER_DUPLICATE_NICKNAME = 'USER_DUPLICATE_NICKNAME',
  USER_UPDATE_FAILED = 'USER_UPDATE_FAILED',

  // Post errors
  POST_NOT_FOUND = 'POST_NOT_FOUND',
  POST_CREATE_FAILED = 'POST_CREATE_FAILED',
  POST_UPDATE_FAILED = 'POST_UPDATE_FAILED',
  POST_DELETE_FAILED = 'POST_DELETE_FAILED',
  POST_PERMISSION_DENIED = 'POST_PERMISSION_DENIED',

  // Comment errors
  COMMENT_CREATE_FAILED = 'COMMENT_CREATE_FAILED',
  COMMENT_DELETE_FAILED = 'COMMENT_DELETE_FAILED',
  COMMENT_NOT_FOUND = 'COMMENT_NOT_FOUND',

  // Paper errors
  PAPER_POSITION_OCCUPIED = 'PAPER_POSITION_OCCUPIED',
  PAPER_MESSAGE_FAILED = 'PAPER_MESSAGE_FAILED',
  PAPER_NOT_FOUND = 'PAPER_NOT_FOUND',

  // Notification errors
  NOTIFICATION_CONNECTION_FAILED = 'NOTIFICATION_CONNECTION_FAILED',
  NOTIFICATION_SUBSCRIBE_FAILED = 'NOTIFICATION_SUBSCRIBE_FAILED',

  // Network errors
  NETWORK_ERROR = 'NETWORK_ERROR',
  TIMEOUT_ERROR = 'TIMEOUT_ERROR',

  // Unknown
  UNKNOWN_ERROR = 'UNKNOWN_ERROR'
}

export interface DomainError {
  domain: ErrorDomain;
  code: ErrorCode;
  message: string;
  userMessage: string;
  recoverable: boolean;
  retryable: boolean;
}

// 에러 코드별 메시지 매핑
export const errorMessages: Record<ErrorCode, { message: string; userMessage: string }> = {
  // Auth
  [ErrorCode.AUTH_EXPIRED]: {
    message: 'Authentication token has expired',
    userMessage: '로그인이 만료되었습니다. 다시 로그인해주세요.'
  },
  [ErrorCode.AUTH_INVALID_TOKEN]: {
    message: 'Invalid authentication token',
    userMessage: '인증 정보가 올바르지 않습니다. 다시 로그인해주세요.'
  },
  [ErrorCode.AUTH_UNAUTHORIZED]: {
    message: 'Unauthorized access',
    userMessage: '접근 권한이 없습니다.'
  },
  [ErrorCode.AUTH_DUPLICATE_LOGIN]: {
    message: 'Duplicate login detected',
    userMessage: '다른 기기에서 로그인되었습니다. 다시 로그인해주세요.'
  },

  // User
  [ErrorCode.USER_NOT_FOUND]: {
    message: 'User not found',
    userMessage: '사용자를 찾을 수 없습니다.'
  },
  [ErrorCode.USER_DUPLICATE_NICKNAME]: {
    message: 'Nickname already exists',
    userMessage: '이미 사용 중인 닉네임입니다.'
  },
  [ErrorCode.USER_UPDATE_FAILED]: {
    message: 'Failed to update user information',
    userMessage: '사용자 정보 업데이트에 실패했습니다.'
  },

  // Post
  [ErrorCode.POST_NOT_FOUND]: {
    message: 'Post not found',
    userMessage: '게시글을 찾을 수 없습니다.'
  },
  [ErrorCode.POST_CREATE_FAILED]: {
    message: 'Failed to create post',
    userMessage: '게시글 작성에 실패했습니다.'
  },
  [ErrorCode.POST_UPDATE_FAILED]: {
    message: 'Failed to update post',
    userMessage: '게시글 수정에 실패했습니다.'
  },
  [ErrorCode.POST_DELETE_FAILED]: {
    message: 'Failed to delete post',
    userMessage: '게시글 삭제에 실패했습니다.'
  },
  [ErrorCode.POST_PERMISSION_DENIED]: {
    message: 'No permission to modify this post',
    userMessage: '이 게시글을 수정할 권한이 없습니다.'
  },

  // Comment
  [ErrorCode.COMMENT_CREATE_FAILED]: {
    message: 'Failed to create comment',
    userMessage: '댓글 작성에 실패했습니다.'
  },
  [ErrorCode.COMMENT_DELETE_FAILED]: {
    message: 'Failed to delete comment',
    userMessage: '댓글 삭제에 실패했습니다.'
  },
  [ErrorCode.COMMENT_NOT_FOUND]: {
    message: 'Comment not found',
    userMessage: '댓글을 찾을 수 없습니다.'
  },

  // Paper
  [ErrorCode.PAPER_POSITION_OCCUPIED]: {
    message: 'Position already occupied',
    userMessage: '이미 다른 메시지가 있는 위치입니다. 다른 위치를 선택해주세요.'
  },
  [ErrorCode.PAPER_MESSAGE_FAILED]: {
    message: 'Failed to write message',
    userMessage: '메시지 작성에 실패했습니다.'
  },
  [ErrorCode.PAPER_NOT_FOUND]: {
    message: 'Rolling paper not found',
    userMessage: '롤링페이퍼를 찾을 수 없습니다.'
  },

  // Notification
  [ErrorCode.NOTIFICATION_CONNECTION_FAILED]: {
    message: 'Failed to connect to notification service',
    userMessage: '알림 서비스 연결에 실패했습니다.'
  },
  [ErrorCode.NOTIFICATION_SUBSCRIBE_FAILED]: {
    message: 'Failed to subscribe to notifications',
    userMessage: '알림 구독에 실패했습니다.'
  },

  // Network
  [ErrorCode.NETWORK_ERROR]: {
    message: 'Network connection error',
    userMessage: '네트워크 연결에 문제가 있습니다. 인터넷 연결을 확인해주세요.'
  },
  [ErrorCode.TIMEOUT_ERROR]: {
    message: 'Request timeout',
    userMessage: '요청 시간이 초과되었습니다. 다시 시도해주세요.'
  },

  // Unknown
  [ErrorCode.UNKNOWN_ERROR]: {
    message: 'Unknown error occurred',
    userMessage: '알 수 없는 오류가 발생했습니다.'
  }
};

// 에러 코드로부터 도메인 추출
export function getDomainFromCode(code: ErrorCode): ErrorDomain {
  if (code.startsWith('AUTH_')) return ErrorDomain.AUTH;
  if (code.startsWith('USER_')) return ErrorDomain.USER;
  if (code.startsWith('POST_')) return ErrorDomain.POST;
  if (code.startsWith('COMMENT_')) return ErrorDomain.COMMENT;
  if (code.startsWith('PAPER_')) return ErrorDomain.PAPER;
  if (code.startsWith('NOTIFICATION_')) return ErrorDomain.NOTIFICATION;
  if (code.startsWith('NETWORK_')) return ErrorDomain.NETWORK;
  return ErrorDomain.UNKNOWN;
}

// 에러 생성 헬퍼
export function createDomainError(
  code: ErrorCode,
  customMessage?: string,
  recoverable = false,
  retryable = false
): DomainError {
  const domain = getDomainFromCode(code);
  const messages = errorMessages[code] || errorMessages[ErrorCode.UNKNOWN_ERROR];

  return {
    domain,
    code,
    message: customMessage || messages.message,
    userMessage: messages.userMessage,
    recoverable,
    retryable
  };
}