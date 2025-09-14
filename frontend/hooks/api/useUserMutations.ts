import { useMutation, useQueryClient } from '@tanstack/react-query';
import { mutationKeys, queryKeys } from '@/lib/tanstack-query/keys';
import { userCommand } from '@/lib/api';
import { useToast } from '@/hooks';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores';

/**
 * 사용자명 변경
 */
export const useUpdateUsername = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const { user, setUser } = useAuthStore();

  return useMutation({
    mutationKey: mutationKeys.user.updateUsername,
    mutationFn: (username: string) => userCommand.updateUserName(username),
    onSuccess: (response, username) => {
      if (response.success) {
        // 사용자 정보 업데이트
        if (user) {
          setUser({ ...user, userName: username });
        }
        // 캐시 무효화
        queryClient.invalidateQueries({ queryKey: queryKeys.user.all });
        queryClient.invalidateQueries({ queryKey: queryKeys.auth.all });
        showToast({ type: 'success', message: '사용자명이 변경되었습니다.' });
      }
    },
    onError: (error) => {
      showToast({ type: 'error', message: '사용자명 변경에 실패했습니다.' });
    },
  });
};

/**
 * 사용자 설정 업데이트
 */
export const useUpdateSettings = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationKey: mutationKeys.user.updateSettings,
    mutationFn: userCommand.updateSettings,
    onSuccess: (response) => {
      if (response.success) {
        // 설정 캐시 무효화
        queryClient.invalidateQueries({ queryKey: queryKeys.user.settings() });
        showToast({ type: 'success', message: '설정이 저장되었습니다.' });
      }
    },
    onError: (error) => {
      showToast({ type: 'error', message: '설정 저장에 실패했습니다.' });
    },
  });
};

/**
 * 사용자 신고
 */
export const useReportUser = () => {
  const { showToast } = useToast();

  return useMutation({
    mutationKey: mutationKeys.user.report,
    mutationFn: userCommand.submitReport,
    onSuccess: (response) => {
      if (response.success) {
        showToast({ type: 'success', message: '신고가 접수되었습니다.' });
      }
    },
    onError: (error) => {
      showToast({ type: 'error', message: '신고 처리에 실패했습니다.' });
    },
  });
};

/**
 * 회원 탈퇴
 */
export const useWithdraw = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const router = useRouter();
  const { logout } = useAuthStore();

  return useMutation({
    mutationKey: mutationKeys.user.withdraw,
    mutationFn: userCommand.withdraw,
    onSuccess: (response) => {
      if (response.success) {
        // 모든 캐시 초기화
        queryClient.clear();
        // 인증 상태 초기화
        logout();
        showToast({ type: 'success', message: '회원 탈퇴가 완료되었습니다.' });
        router.push('/');
      }
    },
    onError: (error) => {
      showToast({ type: 'error', message: '회원 탈퇴에 실패했습니다.' });
    },
  });
};