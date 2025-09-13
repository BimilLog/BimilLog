"use client";

// Re-export all post domain hooks from separated files
export * from './post/usePostList';
export * from './post/usePostDetail';
export * from './post/usePostActions';
export * from './post/usePostSearch';

// Re-export types for backward compatibility
export type { CommentWithReplies } from './post/usePostDetail';