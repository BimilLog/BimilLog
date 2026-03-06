import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useToastStore } from '@/stores/toast.store';

const resetStore = () => {
  useToastStore.setState({ toasts: [] });
};

describe('useToastStore', () => {
  beforeEach(() => {
    resetStore();
    vi.clearAllMocks();
  });

  // 1. 초기 상태
  describe('초기 상태', () => {
    it('toasts가 빈 배열이다', () => {
      const { toasts } = useToastStore.getState();
      expect(toasts).toEqual([]);
    });
  });

  // 2. addToast
  describe('addToast', () => {
    it('토스트를 추가하면 toasts 배열에 포함된다', () => {
      const id = useToastStore.getState().addToast('success', '성공 메시지');

      const { toasts } = useToastStore.getState();
      expect(toasts).toHaveLength(1);
      expect(toasts[0].id).toBe(id);
      expect(toasts[0].type).toBe('success');
      expect(toasts[0].title).toBe('성공 메시지');
    });

    it('반환값이 string 타입의 id이다', () => {
      const id = useToastStore.getState().addToast('info', '알림');

      expect(typeof id).toBe('string');
      expect(id.length).toBeGreaterThan(0);
    });

    it('description을 전달하면 토스트에 포함된다', () => {
      useToastStore.getState().addToast('info', '제목', '설명 텍스트');

      const toast = useToastStore.getState().toasts[0];
      expect(toast.description).toBe('설명 텍스트');
    });

    it('description을 전달하지 않으면 undefined이다', () => {
      useToastStore.getState().addToast('info', '제목');

      const toast = useToastStore.getState().toasts[0];
      expect(toast.description).toBeUndefined();
    });

    it('success 타입의 기본 duration은 4000이다', () => {
      useToastStore.getState().addToast('success', '성공');

      expect(useToastStore.getState().toasts[0].duration).toBe(4000);
    });

    it('error 타입의 기본 duration은 6000이다', () => {
      useToastStore.getState().addToast('error', '에러');

      expect(useToastStore.getState().toasts[0].duration).toBe(6000);
    });

    it('warning 타입의 기본 duration은 5000이다', () => {
      useToastStore.getState().addToast('warning', '경고');

      expect(useToastStore.getState().toasts[0].duration).toBe(5000);
    });

    it('info 타입의 기본 duration은 4000이다', () => {
      useToastStore.getState().addToast('info', '정보');

      expect(useToastStore.getState().toasts[0].duration).toBe(4000);
    });

    it('feedback 타입의 기본 duration은 8000이다', () => {
      useToastStore.getState().addToast('feedback', '피드백');

      expect(useToastStore.getState().toasts[0].duration).toBe(8000);
    });

    it('neutral 타입의 기본 duration은 3000이다', () => {
      useToastStore.getState().addToast('neutral', '중립');

      expect(useToastStore.getState().toasts[0].duration).toBe(3000);
    });

    it('커스텀 duration이 기본값을 오버라이드한다', () => {
      useToastStore.getState().addToast('success', '성공', undefined, 10000);

      expect(useToastStore.getState().toasts[0].duration).toBe(10000);
    });

    it('action을 전달하면 토스트에 포함된다', () => {
      const action = { label: '확인', onClick: vi.fn() };
      useToastStore.getState().addToast('feedback', '피드백', '설명', undefined, action);

      expect(useToastStore.getState().toasts[0].action).toEqual(action);
    });

    it('undoAction을 전달하면 토스트에 포함된다', () => {
      const undoAction = vi.fn();
      useToastStore.getState().addToast('info', '실행 취소', '설명', undefined, undefined, undoAction);

      expect(useToastStore.getState().toasts[0].undoAction).toBe(undoAction);
    });
  });

  // 3. removeToast
  describe('removeToast', () => {
    it('특정 id의 토스트를 제거한다', () => {
      const id = useToastStore.getState().addToast('success', '삭제 대상');
      useToastStore.getState().addToast('info', '유지 대상');

      useToastStore.getState().removeToast(id);

      const { toasts } = useToastStore.getState();
      expect(toasts).toHaveLength(1);
      expect(toasts[0].title).toBe('유지 대상');
    });

    it('존재하지 않는 id를 제거해도 에러가 발생하지 않는다', () => {
      useToastStore.getState().addToast('success', '토스트');

      expect(() => {
        useToastStore.getState().removeToast('non-existent-id');
      }).not.toThrow();

      expect(useToastStore.getState().toasts).toHaveLength(1);
    });
  });

  // 4. clearAllToasts
  describe('clearAllToasts', () => {
    it('모든 토스트를 제거한다', () => {
      useToastStore.getState().addToast('success', '토스트1');
      useToastStore.getState().addToast('error', '토스트2');
      useToastStore.getState().addToast('info', '토스트3');

      useToastStore.getState().clearAllToasts();

      expect(useToastStore.getState().toasts).toEqual([]);
    });

    it('토스트가 없는 상태에서 호출해도 에러가 발생하지 않는다', () => {
      expect(() => {
        useToastStore.getState().clearAllToasts();
      }).not.toThrow();

      expect(useToastStore.getState().toasts).toEqual([]);
    });
  });

  // 5. 편의 메서드
  describe('편의 메서드', () => {
    it('showSuccess는 type이 success인 토스트를 추가한다', () => {
      const id = useToastStore.getState().showSuccess('성공!', '완료되었습니다');

      const toast = useToastStore.getState().toasts[0];
      expect(typeof id).toBe('string');
      expect(toast.type).toBe('success');
      expect(toast.title).toBe('성공!');
      expect(toast.description).toBe('완료되었습니다');
      expect(toast.duration).toBe(4000);
    });

    it('showSuccess에 커스텀 duration을 전달할 수 있다', () => {
      useToastStore.getState().showSuccess('성공', undefined, 2000);

      expect(useToastStore.getState().toasts[0].duration).toBe(2000);
    });

    it('showError는 type이 error인 토스트를 추가한다', () => {
      const id = useToastStore.getState().showError('오류 발생', '다시 시도해주세요');

      const toast = useToastStore.getState().toasts[0];
      expect(typeof id).toBe('string');
      expect(toast.type).toBe('error');
      expect(toast.title).toBe('오류 발생');
      expect(toast.description).toBe('다시 시도해주세요');
      expect(toast.duration).toBe(6000);
    });

    it('showWarning은 type이 warning인 토스트를 추가한다', () => {
      useToastStore.getState().showWarning('주의');

      const toast = useToastStore.getState().toasts[0];
      expect(toast.type).toBe('warning');
      expect(toast.duration).toBe(5000);
    });

    it('showInfo는 type이 info인 토스트를 추가한다', () => {
      useToastStore.getState().showInfo('안내 메시지');

      const toast = useToastStore.getState().toasts[0];
      expect(toast.type).toBe('info');
      expect(toast.duration).toBe(4000);
    });

    it('showFeedback은 type이 feedback인 토스트를 추가한다', () => {
      const action = { label: '확인', onClick: vi.fn() };
      useToastStore.getState().showFeedback('피드백', '내용', action);

      const toast = useToastStore.getState().toasts[0];
      expect(toast.type).toBe('feedback');
      expect(toast.action).toEqual(action);
      expect(toast.duration).toBe(8000);
    });

    it('showNeutral은 type이 neutral인 토스트를 추가한다', () => {
      useToastStore.getState().showNeutral('일반 메시지');

      const toast = useToastStore.getState().toasts[0];
      expect(toast.type).toBe('neutral');
      expect(toast.duration).toBe(3000);
    });

    it('showWithUndo는 undoAction이 포함된 info 토스트를 추가한다', () => {
      const undoFn = vi.fn();
      useToastStore.getState().showWithUndo('삭제됨', '항목이 삭제되었습니다', undoFn);

      const toast = useToastStore.getState().toasts[0];
      expect(toast.type).toBe('info');
      expect(toast.title).toBe('삭제됨');
      expect(toast.description).toBe('항목이 삭제되었습니다');
      expect(toast.undoAction).toBe(undoFn);
    });

    it('showWithUndo에 커스텀 duration을 전달할 수 있다', () => {
      useToastStore.getState().showWithUndo('삭제됨', '설명', vi.fn(), 15000);

      expect(useToastStore.getState().toasts[0].duration).toBe(15000);
    });

    it('showToast는 객체 형태의 옵션으로 토스트를 추가한다', () => {
      const id = useToastStore.getState().showToast({
        type: 'error',
        message: '네트워크 오류',
        description: '서버에 연결할 수 없습니다',
        duration: 7000,
      });

      const toast = useToastStore.getState().toasts[0];
      expect(typeof id).toBe('string');
      expect(toast.type).toBe('error');
      expect(toast.title).toBe('네트워크 오류');
      expect(toast.description).toBe('서버에 연결할 수 없습니다');
      expect(toast.duration).toBe(7000);
    });

    it('showToast에서 duration을 생략하면 타입별 기본값이 적용된다', () => {
      useToastStore.getState().showToast({
        type: 'success',
        message: '저장 완료',
      });

      expect(useToastStore.getState().toasts[0].duration).toBe(4000);
    });

    it('showAdvancedToast는 모든 옵션을 지원한다', () => {
      const action = { label: '보기', onClick: vi.fn() };
      const undoAction = vi.fn();

      useToastStore.getState().showAdvancedToast({
        type: 'feedback',
        title: '고급 토스트',
        description: '상세 설명',
        duration: 12000,
        action,
        undoAction,
      });

      const toast = useToastStore.getState().toasts[0];
      expect(toast.type).toBe('feedback');
      expect(toast.title).toBe('고급 토스트');
      expect(toast.description).toBe('상세 설명');
      expect(toast.duration).toBe(12000);
      expect(toast.action).toEqual(action);
      expect(toast.undoAction).toBe(undoAction);
    });
  });

  // 6. 여러 토스트 동시 관리
  describe('여러 토스트 동시 관리', () => {
    it('토스트 3개를 추가하면 배열 길이가 3이다', () => {
      useToastStore.getState().addToast('success', '첫 번째');
      useToastStore.getState().addToast('error', '두 번째');
      useToastStore.getState().addToast('info', '세 번째');

      expect(useToastStore.getState().toasts).toHaveLength(3);
    });

    it('중간 토스트를 제거하면 나머지가 유지된다', () => {
      const id1 = useToastStore.getState().addToast('success', '첫 번째');
      const id2 = useToastStore.getState().addToast('error', '두 번째');
      const id3 = useToastStore.getState().addToast('info', '세 번째');

      useToastStore.getState().removeToast(id2);

      const { toasts } = useToastStore.getState();
      expect(toasts).toHaveLength(2);
      expect(toasts[0].id).toBe(id1);
      expect(toasts[0].title).toBe('첫 번째');
      expect(toasts[1].id).toBe(id3);
      expect(toasts[1].title).toBe('세 번째');
    });

    it('각 토스트는 고유한 id를 가진다', () => {
      const id1 = useToastStore.getState().addToast('success', 'A');
      const id2 = useToastStore.getState().addToast('success', 'B');
      const id3 = useToastStore.getState().addToast('success', 'C');

      expect(id1).not.toBe(id2);
      expect(id2).not.toBe(id3);
      expect(id1).not.toBe(id3);
    });

    it('토스트 추가 순서가 배열 순서와 일치한다', () => {
      useToastStore.getState().addToast('success', '첫 번째');
      useToastStore.getState().addToast('error', '두 번째');
      useToastStore.getState().addToast('warning', '세 번째');

      const { toasts } = useToastStore.getState();
      expect(toasts[0].title).toBe('첫 번째');
      expect(toasts[1].title).toBe('두 번째');
      expect(toasts[2].title).toBe('세 번째');
    });
  });
});
