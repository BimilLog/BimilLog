"use client";

// Re-export all user domain hooks from separated files
export * from './user/useUserStats';
export * from './user/useUserActivity';
export * from './user/useUserSettings';
export * from './user/useMyPage';

// Re-export types for backward compatibility
export type { UserStats } from './user/useUserStats';