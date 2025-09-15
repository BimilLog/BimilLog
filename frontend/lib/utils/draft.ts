/**
 * 게시글 임시저장 관리 유틸리티
 * localStorage를 활용한 자동저장 및 복구 기능
 */

export interface Draft {
  id: string;
  type: 'post' | 'comment';
  title?: string;
  content: string;
  postId?: number; // 수정 모드일 때 사용
  parentId?: number; // 댓글일 때 사용
  createdAt: string;
  updatedAt: string;
  expiresAt: string; // 30일 후 자동 삭제
}

export interface DraftMetadata {
  lastSaved: string;
  autoSave: boolean;
  version: number;
}

const DRAFT_KEY = 'bimillog_drafts';
const DRAFT_METADATA_KEY = 'bimillog_draft_metadata';
const DRAFT_EXPIRY_DAYS = 30;
const AUTO_SAVE_INTERVAL = 5 * 60 * 1000; // 5분

/**
 * 임시저장 목록 가져오기
 */
export function getDrafts(): Draft[] {
  if (typeof window === 'undefined') return [];

  try {
    const drafts = localStorage.getItem(DRAFT_KEY);
    if (!drafts) return [];

    const parsedDrafts = JSON.parse(drafts) as Draft[];

    // 만료된 임시저장 필터링
    const now = new Date();
    const validDrafts = parsedDrafts.filter(draft => {
      const expiryDate = new Date(draft.expiresAt);
      return expiryDate > now;
    });

    // 필터링 후 저장
    if (validDrafts.length !== parsedDrafts.length) {
      localStorage.setItem(DRAFT_KEY, JSON.stringify(validDrafts));
    }

    return validDrafts;
  } catch (error) {
    console.error('Failed to get drafts:', error);
    return [];
  }
}

/**
 * 특정 임시저장 가져오기
 */
export function getDraft(id: string): Draft | null {
  const drafts = getDrafts();
  return drafts.find(d => d.id === id) || null;
}

/**
 * 게시글용 임시저장 가져오기
 */
export function getPostDraft(postId?: number): Draft | null {
  const drafts = getDrafts();

  if (postId) {
    // 수정 모드: 특정 postId의 임시저장 찾기
    return drafts.find(d => d.type === 'post' && d.postId === postId) || null;
  } else {
    // 작성 모드: postId가 없는 최신 게시글 임시저장 찾기
    return drafts
      .filter(d => d.type === 'post' && !d.postId)
      .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())[0] || null;
  }
}

/**
 * 임시저장 추가/업데이트
 */
export function saveDraft(draft: Omit<Draft, 'id' | 'createdAt' | 'updatedAt' | 'expiresAt'>): string {
  if (typeof window === 'undefined') return '';

  try {
    const drafts = getDrafts();
    const now = new Date();
    const expiresAt = new Date(now.getTime() + DRAFT_EXPIRY_DAYS * 24 * 60 * 60 * 1000);

    let existingDraft: Draft | undefined;

    if (draft.type === 'post' && draft.postId) {
      // 수정 모드: 동일한 postId 찾기
      existingDraft = drafts.find(d => d.type === 'post' && d.postId === draft.postId);
    } else if (draft.type === 'post' && !draft.postId) {
      // 작성 모드: postId가 없는 게시글 임시저장 찾기
      existingDraft = drafts.find(d => d.type === 'post' && !d.postId);
    }

    if (existingDraft) {
      // 기존 임시저장 업데이트
      existingDraft.title = draft.title;
      existingDraft.content = draft.content;
      existingDraft.updatedAt = now.toISOString();

      localStorage.setItem(DRAFT_KEY, JSON.stringify(drafts));
      updateMetadata();
      return existingDraft.id;
    } else {
      // 새 임시저장 생성
      const newDraft: Draft = {
        ...draft,
        id: `draft_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`,
        createdAt: now.toISOString(),
        updatedAt: now.toISOString(),
        expiresAt: expiresAt.toISOString(),
      };

      drafts.push(newDraft);
      localStorage.setItem(DRAFT_KEY, JSON.stringify(drafts));
      updateMetadata();
      return newDraft.id;
    }
  } catch (error) {
    console.error('Failed to save draft:', error);
    return '';
  }
}

/**
 * 임시저장 삭제
 */
export function deleteDraft(id: string): boolean {
  if (typeof window === 'undefined') return false;

  try {
    const drafts = getDrafts();
    const filtered = drafts.filter(d => d.id !== id);

    if (filtered.length === drafts.length) return false;

    localStorage.setItem(DRAFT_KEY, JSON.stringify(filtered));
    return true;
  } catch (error) {
    console.error('Failed to delete draft:', error);
    return false;
  }
}

/**
 * 게시글용 임시저장 삭제
 */
export function deletePostDraft(postId?: number): boolean {
  if (typeof window === 'undefined') return false;

  try {
    const drafts = getDrafts();
    let filtered;

    if (postId) {
      // 수정 모드: 특정 postId의 임시저장 삭제
      filtered = drafts.filter(d => !(d.type === 'post' && d.postId === postId));
    } else {
      // 작성 모드: postId가 없는 게시글 임시저장 삭제
      filtered = drafts.filter(d => !(d.type === 'post' && !d.postId));
    }

    if (filtered.length === drafts.length) return false;

    localStorage.setItem(DRAFT_KEY, JSON.stringify(filtered));
    return true;
  } catch (error) {
    console.error('Failed to delete post draft:', error);
    return false;
  }
}

/**
 * 모든 임시저장 삭제
 */
export function clearDrafts(): void {
  if (typeof window === 'undefined') return;

  try {
    localStorage.removeItem(DRAFT_KEY);
    localStorage.removeItem(DRAFT_METADATA_KEY);
  } catch (error) {
    console.error('Failed to clear drafts:', error);
  }
}

/**
 * 메타데이터 업데이트
 */
function updateMetadata(): void {
  if (typeof window === 'undefined') return;

  try {
    const metadata: DraftMetadata = {
      lastSaved: new Date().toISOString(),
      autoSave: true,
      version: 1,
    };

    localStorage.setItem(DRAFT_METADATA_KEY, JSON.stringify(metadata));
  } catch (error) {
    console.error('Failed to update metadata:', error);
  }
}

/**
 * 메타데이터 가져오기
 */
export function getDraftMetadata(): DraftMetadata | null {
  if (typeof window === 'undefined') return null;

  try {
    const metadata = localStorage.getItem(DRAFT_METADATA_KEY);
    return metadata ? JSON.parse(metadata) : null;
  } catch (error) {
    console.error('Failed to get metadata:', error);
    return null;
  }
}

/**
 * 자동저장 간격
 */
export const AUTO_SAVE_DELAY = AUTO_SAVE_INTERVAL;

/**
 * 임시저장이 있는지 확인
 */
export function hasDraft(postId?: number): boolean {
  return !!getPostDraft(postId);
}

/**
 * 임시저장 개수 가져오기
 */
export function getDraftCount(): number {
  return getDrafts().length;
}