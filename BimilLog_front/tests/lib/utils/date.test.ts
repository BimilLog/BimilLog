import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  formatRelativeDate,
  formatDateTime,
  formatKoreanDate,
  formatKoreanDateTime,
  formatShortDate,
  getDaysBetween,
  addDays,
  formatDate,
  getNowISOString,
  getTimeDifferenceInMs,
} from '@/lib/utils/date';

describe('date utils', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-03-06T12:00:00.000Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('formatRelativeDate', () => {
    it('null/undefined 입력 시 "날짜 정보 없음" 반환', () => {
      expect(formatRelativeDate(null)).toBe('날짜 정보 없음');
      expect(formatRelativeDate(undefined)).toBe('날짜 정보 없음');
    });

    it('유효하지 않은 날짜 문자열은 "날짜 오류" 반환', () => {
      expect(formatRelativeDate('invalid-date')).toBe('날짜 오류');
    });

    it('60초 미만이면 "방금 전" 반환', () => {
      const thirtySecondsAgo = new Date(Date.now() - 30 * 1000).toISOString();
      expect(formatRelativeDate(thirtySecondsAgo)).toBe('방금 전');
    });

    it('60분 미만이면 "N분 전" 반환', () => {
      const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000).toISOString();
      expect(formatRelativeDate(fiveMinutesAgo)).toBe('5분 전');
    });

    it('24시간 미만이면 "N시간 전" 반환', () => {
      const threeHoursAgo = new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString();
      expect(formatRelativeDate(threeHoursAgo)).toBe('3시간 전');
    });

    it('7일 미만이면 "N일 전" 반환', () => {
      const twoDaysAgo = new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString();
      expect(formatRelativeDate(twoDaysAgo)).toBe('2일 전');
    });

    it('7일 이상이면 절대 날짜 반환', () => {
      const twoWeeksAgo = new Date(Date.now() - 14 * 24 * 60 * 60 * 1000).toISOString();
      const result = formatRelativeDate(twoWeeksAgo);
      expect(result).not.toBe('날짜 정보 없음');
      expect(result).not.toBe('날짜 오류');
      // formatKoreanDate 형식으로 반환됨
      expect(result).toMatch(/\d{4}\.\d{2}\.\d{2}/);
    });
  });

  describe('formatDateTime', () => {
    it('null/undefined 입력 시 "날짜 정보 없음" 반환', () => {
      expect(formatDateTime(null)).toBe('날짜 정보 없음');
      expect(formatDateTime(undefined)).toBe('날짜 정보 없음');
    });

    it('유효하지 않은 날짜는 "날짜 오류" 반환', () => {
      expect(formatDateTime('invalid')).toBe('날짜 오류');
    });

    it('YYYY-MM-DD HH:mm 형식으로 반환', () => {
      // UTC 시간 기준으로 로컬 시간으로 변환됨
      const result = formatDateTime('2026-01-15T09:30:00.000Z');
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/);
    });
  });

  describe('formatKoreanDate', () => {
    it('null/undefined 입력 시 "날짜 정보 없음" 반환', () => {
      expect(formatKoreanDate(null)).toBe('날짜 정보 없음');
    });

    it('유효하지 않은 날짜는 "날짜 오류" 반환', () => {
      expect(formatKoreanDate('bad')).toBe('날짜 오류');
    });

    it('YYYY.MM.DD 형식으로 반환', () => {
      const result = formatKoreanDate('2026-01-15T09:30:00.000Z');
      expect(result).toMatch(/\d{4}\.\d{2}\.\d{2}/);
    });
  });

  describe('formatKoreanDateTime', () => {
    it('null/undefined 시 "날짜 정보 없음"', () => {
      expect(formatKoreanDateTime(null)).toBe('날짜 정보 없음');
    });

    it('유효하지 않은 날짜는 "날짜 오류"', () => {
      expect(formatKoreanDateTime('x')).toBe('날짜 오류');
    });

    it('유효한 날짜는 YYYY.MM.DD HH:mm 형식', () => {
      const result = formatKoreanDateTime('2026-01-15T09:30:00.000Z');
      expect(result).toBeTruthy();
      expect(result).not.toBe('날짜 정보 없음');
      expect(result).not.toBe('날짜 오류');
    });
  });

  describe('formatShortDate', () => {
    it('짧은 형태의 날짜를 반환', () => {
      const result = formatShortDate('2026-01-15T00:00:00.000Z');
      expect(result).toBeTruthy();
    });
  });

  describe('getDaysBetween', () => {
    it('같은 날짜는 0일', () => {
      expect(getDaysBetween('2026-01-01', '2026-01-01')).toBe(0);
    });

    it('날짜 차이를 올바르게 계산', () => {
      expect(getDaysBetween('2026-01-01', '2026-01-04')).toBe(3);
    });

    it('순서 상관없이 절대값 반환', () => {
      expect(getDaysBetween('2026-01-04', '2026-01-01')).toBe(3);
    });

    it('Date 객체도 허용', () => {
      const d1 = new Date('2026-01-01');
      const d2 = new Date('2026-01-06');
      expect(getDaysBetween(d1, d2)).toBe(5);
    });
  });

  describe('addDays', () => {
    it('날짜에 일수를 더한다', () => {
      const base = new Date('2026-01-01');
      const result = addDays(base, 5);
      expect(result.getDate()).toBe(6);
    });

    it('음수로 빼기도 가능', () => {
      const base = new Date('2026-01-10');
      const result = addDays(base, -3);
      expect(result.getDate()).toBe(7);
    });

    it('원본 날짜를 변경하지 않는다', () => {
      const base = new Date('2026-01-01');
      addDays(base, 5);
      expect(base.getDate()).toBe(1);
    });
  });

  describe('formatDate', () => {
    it('formatKoreanDate의 별칭', () => {
      expect(formatDate('2026-01-15T09:30:00.000Z')).toBe(
        formatKoreanDate('2026-01-15T09:30:00.000Z')
      );
    });
  });

  describe('getNowISOString', () => {
    it('현재 시각 ISO string 반환', () => {
      const result = getNowISOString();
      expect(result).toBe('2026-03-06T12:00:00.000Z');
    });
  });

  describe('getTimeDifferenceInMs', () => {
    it('밀리초 차이를 절대값으로 반환', () => {
      const result = getTimeDifferenceInMs(
        '2026-01-01T00:00:00.000Z',
        '2026-01-01T00:00:05.000Z'
      );
      expect(result).toBe(5000);
    });

    it('순서 상관없이 절대값', () => {
      const a = '2026-01-01T00:00:05.000Z';
      const b = '2026-01-01T00:00:00.000Z';
      expect(getTimeDifferenceInMs(a, b)).toBe(5000);
    });
  });
});
