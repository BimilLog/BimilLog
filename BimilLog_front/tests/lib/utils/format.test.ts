import { describe, it, expect } from 'vitest';
import {
  calculateActivityScore,
  getActivityLevel,
  getNextLevelThreshold,
  formatNumber,
  stripHtml,
  getInitials,
} from '@/lib/utils/format';

describe('format utils', () => {
  describe('calculateActivityScore', () => {
    it('각 항목의 가중치를 올바르게 적용', () => {
      const stats = {
        totalPosts: 10,      // 10 * 5 = 50
        totalComments: 20,   // 20 * 2 = 40
        totalLikedPosts: 5,  // 5 * 1 = 5
        totalLikedComments: 3, // 3 * 1 = 3
      };
      expect(calculateActivityScore(stats)).toBe(98);
    });

    it('모든 값이 0이면 0 반환', () => {
      expect(calculateActivityScore({
        totalPosts: 0,
        totalComments: 0,
        totalLikedPosts: 0,
        totalLikedComments: 0,
      })).toBe(0);
    });
  });

  describe('getActivityLevel', () => {
    it('0~24점은 "새싹"', () => {
      expect(getActivityLevel(0).level).toBe('새싹');
      expect(getActivityLevel(24).level).toBe('새싹');
    });

    it('25~99점은 "초보자"', () => {
      expect(getActivityLevel(25).level).toBe('초보자');
      expect(getActivityLevel(99).level).toBe('초보자');
    });

    it('100~249점은 "활발함"', () => {
      expect(getActivityLevel(100).level).toBe('활발함');
      expect(getActivityLevel(249).level).toBe('활발함');
    });

    it('250~499점은 "고수"', () => {
      expect(getActivityLevel(250).level).toBe('고수');
      expect(getActivityLevel(499).level).toBe('고수');
    });

    it('500점 이상은 "전설급"', () => {
      expect(getActivityLevel(500).level).toBe('전설급');
      expect(getActivityLevel(1000).level).toBe('전설급');
    });

    it('각 레벨에 color가 존재', () => {
      expect(getActivityLevel(0).color).toBeTruthy();
      expect(getActivityLevel(500).color).toBeTruthy();
    });
  });

  describe('getNextLevelThreshold', () => {
    it('0~24 → 25', () => {
      expect(getNextLevelThreshold(0)).toBe(25);
      expect(getNextLevelThreshold(24)).toBe(25);
    });

    it('25~99 → 100', () => {
      expect(getNextLevelThreshold(25)).toBe(100);
      expect(getNextLevelThreshold(99)).toBe(100);
    });

    it('100~249 → 250', () => {
      expect(getNextLevelThreshold(100)).toBe(250);
    });

    it('250~499 → 500', () => {
      expect(getNextLevelThreshold(250)).toBe(500);
    });

    it('500 이상은 다음 500 단위 + 500', () => {
      expect(getNextLevelThreshold(500)).toBe(1000);
      expect(getNextLevelThreshold(999)).toBe(1500);
    });
  });

  describe('formatNumber', () => {
    it('숫자를 로케일 형식으로 변환', () => {
      expect(formatNumber(1000)).toBe('1,000');
      expect(formatNumber(1234567)).toBe('1,234,567');
    });

    it('작은 숫자는 그대로', () => {
      expect(formatNumber(0)).toBe('0');
      expect(formatNumber(999)).toBe('999');
    });
  });

  describe('stripHtml', () => {
    it('HTML 태그를 제거', () => {
      expect(stripHtml('<p>Hello</p>')).toContain('Hello');
    });

    it('<br> 태그를 줄바꿈으로 변환', () => {
      expect(stripHtml('Hello<br>World')).toContain('Hello\nWorld');
    });

    it('</p> 태그를 줄바꿈으로 변환', () => {
      const result = stripHtml('<p>First</p><p>Second</p>');
      expect(result).toContain('First');
      expect(result).toContain('Second');
    });

    it('연속 줄바꿈 3개 이상을 2개로 정리', () => {
      const result = stripHtml('<p>A</p><br><br><br><p>B</p>');
      expect(result).not.toContain('\n\n\n');
    });
  });

  describe('getInitials', () => {
    it('빈 문자열이면 빈 문자열 반환', () => {
      expect(getInitials('')).toBe('');
      expect(getInitials('  ')).toBe('');
    });

    it('한글 이름은 앞 2글자', () => {
      expect(getInitials('홍길동')).toBe('홍길');
    });

    it('영문 이름은 각 단어 첫 글자 대문자', () => {
      expect(getInitials('John Doe')).toBe('JD');
    });

    it('단일 영문 단어는 첫 2글자 대문자', () => {
      expect(getInitials('John')).toBe('JO');
    });

    it('한자 이름도 앞 2글자', () => {
      expect(getInitials('李白')).toBe('李白');
    });
  });
});
