import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { userQuery } from '@/lib/api';

/**
 * 사용자 설정 조회
 */
export const useUserSettings = () => {
  return useQuery({
    queryKey: queryKeys.user.settings(),
    queryFn: userQuery.getSettings,
    staleTime: 30 * 60 * 1000, // 30분
  });
};

/**
 * 사용자가 작성한 게시글 목록
 */
export const useUserPosts = (page: number = 0, size: number = 10) => {
  return useQuery({
    queryKey: queryKeys.user.posts(page),
    queryFn: () => userQuery.getUserPosts(page, size),
    staleTime: 3 * 60 * 1000,
  });
};

/**
 * 사용자가 작성한 댓글 목록
 */
export const useUserComments = (page: number = 0, size: number = 10) => {
  return useQuery({
    queryKey: queryKeys.user.comments(page),
    queryFn: () => userQuery.getUserComments(page, size),
    staleTime: 3 * 60 * 1000,
  });
};

/**
 * 사용자가 좋아요한 게시글 목록
 */
export const useUserLikedPosts = (page: number = 0, size: number = 10) => {
  return useQuery({
    queryKey: queryKeys.user.likePosts(page),
    queryFn: () => userQuery.getUserLikedPosts(page, size),
    staleTime: 3 * 60 * 1000,
  });
};

/**
 * 사용자가 좋아요한 댓글 목록
 */
export const useUserLikedComments = (page: number = 0, size: number = 10) => {
  return useQuery({
    queryKey: queryKeys.user.likeComments(page),
    queryFn: () => userQuery.getUserLikedComments(page, size),
    staleTime: 3 * 60 * 1000,
  });
};

/**
 * 친구 목록 조회
 */
export const useUserFriendList = (offset: number = 0, limit: number = 10) => {
  return useQuery({
    queryKey: queryKeys.user.friendList(),
    queryFn: () => userQuery.getFriendList(offset, limit),
    staleTime: 10 * 60 * 1000, // 10분
    gcTime: 30 * 60 * 1000, // 30분
  });
};

/**
 * 사용자명 중복 확인
 */
export const useCheckUsername = (username: string) => {
  return useQuery({
    queryKey: ['user', 'checkUsername', username],
    queryFn: () => userQuery.checkUserName(username),
    enabled: !!username && username.trim().length > 0,
    staleTime: 60 * 1000, // 1분
  });
};