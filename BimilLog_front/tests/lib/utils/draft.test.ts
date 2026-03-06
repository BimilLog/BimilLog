import { describe, it, expect, beforeEach, vi } from 'vitest';
import {
  getDrafts,
  getDraft,
  getPostDraft,
  saveDraft,
  deleteDraft,
  deletePostDraft,
  clearDrafts,
  getDraftMetadata,
  hasDraft,
  getDraftCount,
} from '@/lib/utils/draft';

describe('draft utils', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  describe('getDrafts', () => {
    it('빈 상태에서 빈 배열 반환', () => {
      expect(getDrafts()).toEqual([]);
    });

    it('만료된 임시저장을 자동 필터링', () => {
      const expiredDraft = {
        id: 'draft_1',
        type: 'post',
        title: '만료됨',
        content: '내용',
        createdAt: '2020-01-01T00:00:00Z',
        updatedAt: '2020-01-01T00:00:00Z',
        expiresAt: '2020-02-01T00:00:00Z', // 이미 만료
      };
      localStorage.setItem('bimillog_drafts', JSON.stringify([expiredDraft]));
      expect(getDrafts()).toHaveLength(0);
    });

    it('유효한 임시저장만 반환', () => {
      const futureDate = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString();
      const validDraft = {
        id: 'draft_1',
        type: 'post',
        title: '유효',
        content: '내용',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        expiresAt: futureDate,
      };
      localStorage.setItem('bimillog_drafts', JSON.stringify([validDraft]));
      expect(getDrafts()).toHaveLength(1);
    });
  });

  describe('saveDraft', () => {
    it('새 임시저장을 생성하고 ID를 반환', () => {
      const id = saveDraft({ type: 'post', title: '제목', content: '내용' });
      expect(id).toBeTruthy();
      expect(id).toContain('draft_');
      expect(getDrafts()).toHaveLength(1);
    });

    it('동일한 postId의 임시저장을 업데이트', () => {
      const id1 = saveDraft({ type: 'post', title: '원래', content: '원래 내용', postId: 10 });
      const id2 = saveDraft({ type: 'post', title: '수정', content: '수정 내용', postId: 10 });

      expect(id1).toBe(id2); // 같은 ID
      expect(getDrafts()).toHaveLength(1);
      expect(getDraft(id1)!.title).toBe('수정');
    });

    it('작성 모드 임시저장은 postId 없는 것끼리 업데이트', () => {
      saveDraft({ type: 'post', title: 'A', content: 'A' });
      saveDraft({ type: 'post', title: 'B', content: 'B' });
      expect(getDrafts()).toHaveLength(1);
      expect(getDrafts()[0].title).toBe('B');
    });
  });

  describe('getDraft', () => {
    it('ID로 임시저장을 찾는다', () => {
      const id = saveDraft({ type: 'post', title: 'T', content: 'C' });
      expect(getDraft(id)).toBeTruthy();
      expect(getDraft(id)!.title).toBe('T');
    });

    it('없으면 null', () => {
      expect(getDraft('none')).toBeNull();
    });
  });

  describe('getPostDraft', () => {
    it('작성 모드: postId 없는 최신 임시저장', () => {
      saveDraft({ type: 'post', title: '게시글', content: '내용' });
      const draft = getPostDraft();
      expect(draft).toBeTruthy();
      expect(draft!.title).toBe('게시글');
    });

    it('수정 모드: 특정 postId의 임시저장', () => {
      saveDraft({ type: 'post', title: '수정글', content: '내용', postId: 5 });
      const draft = getPostDraft(5);
      expect(draft).toBeTruthy();
      expect(draft!.title).toBe('수정글');
    });

    it('없으면 null', () => {
      expect(getPostDraft(999)).toBeNull();
    });
  });

  describe('deleteDraft', () => {
    it('ID로 삭제', () => {
      const id = saveDraft({ type: 'post', title: 'T', content: 'C' });
      expect(deleteDraft(id)).toBe(true);
      expect(getDrafts()).toHaveLength(0);
    });

    it('존재하지 않으면 false', () => {
      expect(deleteDraft('nope')).toBe(false);
    });
  });

  describe('deletePostDraft', () => {
    it('작성 모드: postId 없는 임시저장 삭제', () => {
      saveDraft({ type: 'post', title: 'T', content: 'C' });
      expect(deletePostDraft()).toBe(true);
      expect(getDrafts()).toHaveLength(0);
    });

    it('수정 모드: 특정 postId 임시저장 삭제', () => {
      saveDraft({ type: 'post', title: 'T', content: 'C', postId: 10 });
      expect(deletePostDraft(10)).toBe(true);
      expect(getDrafts()).toHaveLength(0);
    });

    it('없으면 false', () => {
      expect(deletePostDraft(999)).toBe(false);
    });
  });

  describe('clearDrafts', () => {
    it('모든 임시저장과 메타데이터를 삭제', () => {
      saveDraft({ type: 'post', title: 'T', content: 'C' });
      clearDrafts();
      expect(getDrafts()).toHaveLength(0);
      expect(getDraftMetadata()).toBeNull();
    });
  });

  describe('getDraftMetadata', () => {
    it('임시저장이 없으면 null', () => {
      expect(getDraftMetadata()).toBeNull();
    });

    it('임시저장 후 메타데이터가 존재', () => {
      saveDraft({ type: 'post', title: 'T', content: 'C' });
      const meta = getDraftMetadata();
      expect(meta).toBeTruthy();
      expect(meta!.autoSave).toBe(true);
      expect(meta!.version).toBe(1);
      expect(meta!.lastSaved).toBeTruthy();
    });
  });

  describe('hasDraft / getDraftCount', () => {
    it('hasDraft: 임시저장 존재 여부', () => {
      expect(hasDraft()).toBe(false);
      saveDraft({ type: 'post', title: 'T', content: 'C' });
      expect(hasDraft()).toBe(true);
    });

    it('getDraftCount: 임시저장 개수', () => {
      expect(getDraftCount()).toBe(0);
      saveDraft({ type: 'post', title: 'T', content: 'C' });
      saveDraft({ type: 'post', title: 'T2', content: 'C2', postId: 1 });
      expect(getDraftCount()).toBe(2);
    });
  });
});
