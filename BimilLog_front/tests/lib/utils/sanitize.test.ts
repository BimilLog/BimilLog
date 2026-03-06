import { describe, it, expect } from 'vitest';
import {
  sanitizeUrl,
  escapeHtml,
  stripHtmlTags,
} from '@/lib/utils/sanitize';

// sanitizeHtml은 DOMPurify 의존 → window가 필요하므로 별도 테스트 가능
// 여기서는 SSR-safe 함수들만 테스트

describe('sanitize utils', () => {
  describe('sanitizeUrl', () => {
    it('빈 문자열이면 빈 문자열 반환', () => {
      expect(sanitizeUrl('')).toBe('');
    });

    it('javascript: URL을 차단', () => {
      expect(sanitizeUrl('javascript:alert(1)')).toBe('');
      expect(sanitizeUrl('JAVASCRIPT:alert(1)')).toBe('');
      expect(sanitizeUrl('JavaScript:void(0)')).toBe('');
    });

    it('data: URL을 차단 (이미지 제외)', () => {
      expect(sanitizeUrl('data:text/html,<script>alert(1)</script>')).toBe('');
      expect(sanitizeUrl('data:application/json,{}')).toBe('');
    });

    it('data:image/ URL은 허용', () => {
      const imageUrl = 'data:image/png;base64,abc123';
      expect(sanitizeUrl(imageUrl)).toBe(imageUrl);
    });

    it('일반 URL은 그대로 반환', () => {
      const url = 'https://example.com/page';
      expect(sanitizeUrl(url)).toBe(url);
    });

    it('상대 경로도 그대로 반환', () => {
      expect(sanitizeUrl('/board/post/1')).toBe('/board/post/1');
    });
  });

  describe('escapeHtml', () => {
    it('& 문자를 이스케이프', () => {
      expect(escapeHtml('A & B')).toBe('A &amp; B');
    });

    it('< > 문자를 이스케이프', () => {
      expect(escapeHtml('<script>')).toBe('&lt;script&gt;');
    });

    it('따옴표를 이스케이프', () => {
      expect(escapeHtml('"hello"')).toBe('&quot;hello&quot;');
      expect(escapeHtml("'hello'")).toBe('&#39;hello&#39;');
    });

    it('/ 문자를 이스케이프', () => {
      expect(escapeHtml('a/b')).toBe('a&#x2F;b');
    });

    it('일반 텍스트는 그대로', () => {
      expect(escapeHtml('Hello World 123')).toBe('Hello World 123');
    });

    it('복합 XSS 패턴을 안전하게 이스케이프', () => {
      const xss = '<script>alert("xss")</script>';
      const result = escapeHtml(xss);
      expect(result).not.toContain('<');
      expect(result).not.toContain('>');
      expect(result).toContain('&lt;');
    });
  });

  describe('stripHtmlTags', () => {
    it('빈 문자열이면 빈 문자열 반환', () => {
      expect(stripHtmlTags('')).toBe('');
    });

    it('HTML 태그를 제거하고 텍스트만 반환', () => {
      expect(stripHtmlTags('<p>Hello <strong>World</strong></p>')).toBe('Hello World');
    });

    it('&nbsp;를 공백으로 변환', () => {
      expect(stripHtmlTags('Hello&nbsp;World')).toBe('Hello World');
    });

    it('HTML 엔티티를 디코딩', () => {
      expect(stripHtmlTags('&lt;tag&gt;')).toBe('<tag>');
      expect(stripHtmlTags('&amp;')).toBe('&');
      expect(stripHtmlTags('&quot;hello&quot;')).toBe('"hello"');
      expect(stripHtmlTags('&#39;test&#39;')).toBe("'test'");
    });

    it('앞뒤 공백을 제거', () => {
      expect(stripHtmlTags('  <p>Hello</p>  ')).toBe('Hello');
    });
  });
});
