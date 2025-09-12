import { BaseEntity, UserInfo, PageResponse } from './common';

// Post Types
export interface Post extends BaseEntity {
  userId: number;
  userName: string | null;
  title: string;
  content: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  isNotice: boolean;
  postCacheFlag?: string;
}

export interface SimplePost {
  id: number;
  userId: number;
  userName: string;
  title: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  createdAt: string;
  isNotice: boolean;
  postCacheFlag?: string;
}

export interface CreatePostRequest {
  userName: string | null;
  title: string;
  content: string;
  password?: number;
}

export interface UpdatePostRequest {
  title?: string;
  content?: string;
  password?: number;
}

// Comment Types
export interface Comment extends BaseEntity {
  postId: number;
  userId?: number;
  userName: string | null;
  content: string;
  likeCount: number;
  parentId?: number;
  userLike?: boolean;
  isDeleted?: boolean;
  depth?: number;
}

export interface SimpleComment {
  id: number;
  postId: number;
  userId?: number;
  userName: string;
  content: string;
  likeCount: number;
  createdAt: string;
  userLike?: boolean;
}

export interface CreateCommentRequest {
  postId: number;
  content: string;
  parentId?: number;
  password?: number;
}

export interface UpdateCommentRequest {
  content: string;
  password?: number;
}

// Paper Types (Rolling Paper)
export interface Paper extends BaseEntity {
  userId: number;
  userName: string;
  paperUrl: string;
  messageCount: number;
  isPublic: boolean;
}

export interface PaperMessage extends BaseEntity {
  paperId: number;
  position: number;
  content: string;
  color: string;
  fontSize: number;
  fontFamily: string;
  authorName?: string;
  isAnonymous: boolean;
}

// Notification Types
export interface Notification extends BaseEntity {
  userId: number;
  type: NotificationType;
  title: string;
  message: string;
  relatedId?: number;
  isRead: boolean;
}

export type NotificationType = 
  | 'COMMENT'
  | 'LIKE'
  | 'REPLY'
  | 'MENTION'
  | 'PAPER_MESSAGE'
  | 'SYSTEM';

// Auth Types
export interface LoginRequest {
  code: string;
  state?: string;
}

export interface SignUpRequest {
  userName: string;
  uuid: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

// User Types
export interface User extends UserInfo {
  id: number;
  userId: number;
  role: UserRole;
  paperUrl?: string;
  createdAt: string;
  lastLoginAt?: string;
}

export type UserRole = 'USER' | 'ADMIN' | 'MODERATOR';

// Search Types
export type SearchType = 'TITLE' | 'TITLE_CONTENT' | 'AUTHOR';

export interface SearchRequest {
  type: SearchType;
  keyword: string;
  page?: number;
  size?: number;
}

// Report Types
export interface Report extends BaseEntity {
  reporterId?: number;
  targetType: 'POST' | 'COMMENT' | 'USER';
  targetId: number;
  reason: string;
  description?: string;
  status: ReportStatus;
  handledBy?: number;
  handledAt?: string;
}

export type ReportStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'REJECTED';