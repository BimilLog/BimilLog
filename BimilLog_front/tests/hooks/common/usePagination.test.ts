import { renderHook, act } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { usePagination } from '@/hooks/common/usePagination';

describe('usePagination', () => {
  it('기본값으로 초기화', () => {
    const { result } = renderHook(() => usePagination());
    expect(result.current.currentPage).toBe(0);
    expect(result.current.pageSize).toBe(30);
    expect(result.current.totalItems).toBe(0);
    expect(result.current.totalPages).toBe(1); // 최소 1
  });

  it('초기값을 전달하여 초기화', () => {
    const { result } = renderHook(() => usePagination({
      initialPage: 2,
      pageSize: 10,
      totalItems: 100,
    }));
    expect(result.current.currentPage).toBe(2);
    expect(result.current.pageSize).toBe(10);
    expect(result.current.totalItems).toBe(100);
    expect(result.current.totalPages).toBe(10);
  });

  describe('페이지 이동', () => {
    it('goToPage: 유효한 범위 내로 제한', () => {
      const { result } = renderHook(() => usePagination({ totalItems: 100, pageSize: 10 }));

      act(() => result.current.goToPage(5));
      expect(result.current.currentPage).toBe(5);

      act(() => result.current.goToPage(-1));
      expect(result.current.currentPage).toBe(0);

      act(() => result.current.goToPage(100));
      expect(result.current.currentPage).toBe(9); // 마지막 페이지
    });

    it('nextPage: 다음 페이지로 이동', () => {
      const { result } = renderHook(() => usePagination({ totalItems: 50, pageSize: 10 }));

      act(() => result.current.nextPage());
      expect(result.current.currentPage).toBe(1);
    });

    it('nextPage: 마지막 페이지에서는 이동하지 않음', () => {
      const { result } = renderHook(() => usePagination({ totalItems: 10, pageSize: 10 }));

      act(() => result.current.nextPage());
      expect(result.current.currentPage).toBe(0); // 유일한 페이지
    });

    it('previousPage: 이전 페이지로 이동', () => {
      const { result } = renderHook(() => usePagination({ initialPage: 3, totalItems: 100, pageSize: 10 }));

      act(() => result.current.previousPage());
      expect(result.current.currentPage).toBe(2);
    });

    it('previousPage: 첫 페이지에서는 이동하지 않음', () => {
      const { result } = renderHook(() => usePagination());

      act(() => result.current.previousPage());
      expect(result.current.currentPage).toBe(0);
    });

    it('firstPage: 첫 페이지로 이동', () => {
      const { result } = renderHook(() => usePagination({ initialPage: 5, totalItems: 100, pageSize: 10 }));

      act(() => result.current.firstPage());
      expect(result.current.currentPage).toBe(0);
    });

    it('lastPage: 마지막 페이지로 이동', () => {
      const { result } = renderHook(() => usePagination({ totalItems: 100, pageSize: 10 }));

      act(() => result.current.lastPage());
      expect(result.current.currentPage).toBe(9);
    });
  });

  describe('hasNextPage / hasPreviousPage', () => {
    it('첫 페이지에서는 hasPreviousPage=false', () => {
      const { result } = renderHook(() => usePagination({ totalItems: 100, pageSize: 10 }));
      expect(result.current.hasPreviousPage).toBe(false);
      expect(result.current.hasNextPage).toBe(true);
    });

    it('마지막 페이지에서는 hasNextPage=false', () => {
      const { result } = renderHook(() => usePagination({ initialPage: 9, totalItems: 100, pageSize: 10 }));
      expect(result.current.hasNextPage).toBe(false);
      expect(result.current.hasPreviousPage).toBe(true);
    });
  });

  describe('offset', () => {
    it('현재 페이지에 맞는 offset 계산', () => {
      const { result } = renderHook(() => usePagination({ initialPage: 2, pageSize: 10 }));
      expect(result.current.offset).toBe(20);
    });
  });

  describe('pageRange', () => {
    it('총 페이지가 5 이하면 전체 페이지 반환', () => {
      const { result } = renderHook(() => usePagination({ totalItems: 30, pageSize: 10 }));
      expect(result.current.pageRange).toEqual([0, 1, 2]);
    });

    it('현재 페이지 중심으로 최대 5개 범위', () => {
      const { result } = renderHook(() => usePagination({ initialPage: 5, totalItems: 200, pageSize: 10 }));
      expect(result.current.pageRange).toHaveLength(5);
      expect(result.current.pageRange).toContain(5);
    });
  });

  describe('setPageSize', () => {
    it('페이지 크기 변경 시 첫 페이지로 이동', () => {
      const { result } = renderHook(() => usePagination({ initialPage: 5, totalItems: 100, pageSize: 10 }));

      act(() => result.current.setPageSize(20));
      expect(result.current.pageSize).toBe(20);
      expect(result.current.currentPage).toBe(0);
    });
  });

  describe('setTotalItems', () => {
    it('totalItems 변경 시 totalPages 재계산', () => {
      const { result } = renderHook(() => usePagination({ pageSize: 10 }));

      act(() => result.current.setTotalItems(50));
      expect(result.current.totalItems).toBe(50);
      expect(result.current.totalPages).toBe(5);
    });
  });
});
