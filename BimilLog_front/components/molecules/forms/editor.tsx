"use client";

import React, { useEffect, useRef, useState } from "react";

import dynamic from "next/dynamic";
import DOMPurify from "dompurify";

import { Spinner } from "@/components";

import { logger } from "@/lib/utils";

interface EditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

/**
 * Quill 에디터 컴포넌트 - 게시글 작성 시 사용하는 리치 텍스트 에디터.
 * SSR 이슈 방지를 위해 dynamic import 사용.
 * Quill 2.0 호환 + 안정성 강화한 초기화 로직 포함.
 *
 * B-7-008: Quill CSS 는 globals.css 에서 npm 경로(@import "quill/dist/quill.snow.css")로 로드.
 *   더 이상 jsdelivr CDN 동적 로드를 시도하지 않으므로 외부 차단망에서도 정상 동작.
 */
const QuillEditor: React.FC<EditorProps> = ({
  value,
  onChange,
  placeholder = "내용을 입력하세요",
}) => {
  // DOM 요소 및 Quill 인스턴스 참조
  const editorRef = useRef<HTMLDivElement>(null);
  const quillRef = useRef<unknown>(null);

  // 중복 초기화 방지를 위한 플래그
  const isInitializing = useRef(false);

  // 초기 value 설정 여부 추적
  const isInitialValueSet = useRef(false);

  // onChange 를 ref 로 관리하여 클로저 문제 방지
  const onChangeRef = useRef(onChange);
  const placeholderRef = useRef(placeholder);
  const initialValueRef = useRef(value);

  // 에디터 상태 관리
  const [isReady, setIsReady] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // onChange 가 변경될 때마다 ref 업데이트
  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  useEffect(() => {
    placeholderRef.current = placeholder;
  }, [placeholder]);

  useEffect(() => {
    initialValueRef.current = value;
  }, [value]);

  useEffect(() => {
    /**
     * Quill 에디터 초기화 함수
     * 복잡한 초기화 과정이 필요한 이유:
     * 1. SSR 환경에서 window 객체 접근 방지
     * 2. 중복 초기화 방지
     * 3. Quill 2.0 버전의 CSS 로드 보장 (B-7-008 이후 globals.css 가 담당)
     * 4. 브라우저 호환성 문제 해결
     */
    const initQuill = async () => {
      // 초기화 조건 체크: 서버사이드/DOM 미존재/중복 초기화 방지
      if (
        typeof window === "undefined" ||
        !editorRef.current ||
        quillRef.current ||
        isInitializing.current
      ) {
        return;
      }

      try {
        isInitializing.current = true;
        logger.log("Quill 에디터 초기화를 시작합니다.");

        // Quill 라이브러리 동적 import (번들 크기 최적화)
        const { default: Quill } = await import("quill");

        // 에디터 높이/스크롤바 커스터마이징 CSS 한 번만 추가
        const styleId = "quill-editor-height-fix";
        if (!document.querySelector(`#${styleId}`)) {
          const style = document.createElement("style");
          style.id = styleId;
          style.textContent = `
            .ql-container {
              font-size: 14px;
            }
            .ql-editor {
              max-height: 300px;
              overflow-y: auto;
              min-height: 200px;
            }
            .ql-editor::-webkit-scrollbar {
              width: 8px;
            }
            .ql-editor::-webkit-scrollbar-track {
              background: rgba(0, 0, 0, 0.05);
              border-radius: 4px;
            }
            .ql-editor::-webkit-scrollbar-thumb {
              background: rgba(0, 0, 0, 0.25);
              border-radius: 4px;
            }
            .ql-editor::-webkit-scrollbar-thumb:hover {
              background: rgba(0, 0, 0, 0.4);
            }
            /* WCAG 2.4.7 Focus Visible (AA) — 다크 모드 포커스 가시성 */
            .ql-container.ql-snow:focus-within {
              outline: 2px solid var(--color-postal-navy, #1F3A68);
              outline-offset: 2px;
            }
          `;
          document.head.appendChild(style);
        }

        // 기존 툴바가 있으면 제거 (중복 방지)
        const existingToolbar = editorRef.current?.querySelector(".ql-toolbar");
        if (existingToolbar) {
          logger.log("기존 툴바 제거 중...");
          existingToolbar.remove();
        }

        /**
         * Quill 인스턴스 생성
         * toolbar 와 formats 를 Quill 2.0 에 맞게 안전하게 설정.
         * 커뮤니티 게시글 작성에 필요한 기본적인 서식만 포함.
         */
        quillRef.current = new Quill(editorRef.current, {
          theme: "snow",
          placeholder: placeholderRef.current,
          modules: {
            toolbar: [
              [{ header: [1, 2, false] }],
              ["bold", "italic", "underline", "strike"],
              [{ color: [] }, { background: [] }],
              [{ list: "ordered" }, { list: "bullet" }],
              [{ align: [] }],
              ["blockquote", "code-block"],
              ["link"],
              ["clean"],
            ],
          },
          // XSS 방지를 위해 안전한 포맷만 허용
          formats: [
            "header",
            "bold",
            "italic",
            "underline",
            "strike",
            "color",
            "background",
            "list",
            "align",
            "blockquote",
            "code-block",
            "link",
          ],
        });

        const quill = quillRef.current as {
          on: (event: string, handler: () => void) => void;
          getSemanticHTML?: () => string;
          root: { innerHTML: string };
          clipboard: { convert: (options: { html: string }) => unknown };
          setContents: (delta: unknown, source: string) => void;
          off?: (event: string, handler?: () => void) => void;
        };

        /**
         * 텍스트 변경 이벤트 리스너 설정
         * getSemanticHTML() 메서드가 있으면 우선 사용,
         * 없을 경우 innerHTML 로 대체 (Quill 버전 호환성)
         */
        quill.on("text-change", () => {
          try {
            const raw = quill.getSemanticHTML
              ? quill.getSemanticHTML()
              : quill.root.innerHTML;
            onChangeRef.current(DOMPurify.sanitize(raw));
            // 사용자가 타이핑을 시작하면 초기 value 설정 완료로 표시 (이후 외부 동기화 방지)
            isInitialValueSet.current = true;
          } catch (err) {
            logger.error("Error getting content:", err);
            onChangeRef.current(DOMPurify.sanitize(quill.root.innerHTML));
            isInitialValueSet.current = true;
          }
        });

        // 기존 내용이 있는 경우 에디터에 설정 (초기 1회만)
        const initialValue = initialValueRef.current;
        if (initialValue) {
          try {
            const delta = quill.clipboard.convert({ html: initialValue });
            quill.setContents(delta, "silent");
            isInitialValueSet.current = true;
          } catch (err) {
            logger.error("Error setting initial content:", err);
            quill.root.innerHTML = initialValue;
            isInitialValueSet.current = true;
          }
        }

        /**
         * SVG 아이콘 렌더링 문제 해결
         * Quill 의 SVG 아이콘이 텍스트로 잘못 렌더링되는 경우가 있어
         * 툴바 버튼에서 잘못된 텍스트 노드를 제거.
         */
        setTimeout(() => {
          const toolbar = editorRef.current?.querySelector(".ql-toolbar");
          if (toolbar) {
            const buttons = toolbar.querySelectorAll("button");
            buttons.forEach((button) => {
              const textNodes = Array.from(button.childNodes).filter(
                (node) =>
                  node.nodeType === Node.TEXT_NODE &&
                  node.textContent?.includes("viewBox")
              );
              textNodes.forEach((node) => node.remove());
            });
          }
        }, 100);

        setIsReady(true);
        setError(null);
        logger.log("Quill 에디터가 성공적으로 초기화되었습니다.");
      } catch (error) {
        logger.error("Quill 로드 실패:", error);
        setError(
          error instanceof Error ? error.message : "에디터 로드에 실패했습니다."
        );
        setIsReady(true);
      } finally {
        isInitializing.current = false;
      }
    };

    initQuill();

    const quillInstance = quillRef.current;
    const editorElement = editorRef.current;

    // 컴포넌트 언마운트 시 메모리 누수 방지를 위한 정리
    return () => {
      if (quillInstance) {
        try {
          (quillInstance as { off: (event: string) => void }).off("text-change");
          // DOM 정리 - 툴바 제거
          const toolbar = editorElement?.querySelector(".ql-toolbar");
          toolbar?.remove();
        } catch (err) {
          logger.error("Error cleaning up Quill:", err);
        }
      }
    };
    // 초기 마운트 시에만 Quill 을 초기화하고, 이후에는 재초기화하지 않음
  }, []);

  /**
   * 외부에서 value prop 이 변경되었을 때 에디터 내용 동기화
   * 초기 value 설정 이후에는 사용자 입력만 반영하기 위해 동기화하지 않음.
   * 임시저장 복원 등 외부 변경은 컴포넌트 리마운트로 처리.
   */
  useEffect(() => {
    if (!quillRef.current || !isReady || error || isInitialValueSet.current) {
      return;
    }

    if (value) {
      try {
        const quill = quillRef.current as {
          clipboard: { convert: (options: { html: string }) => unknown };
          setContents: (delta: unknown, source: string) => void;
        };
        const delta = quill.clipboard.convert({ html: value });
        quill.setContents(delta, "silent");
        isInitialValueSet.current = true;
        logger.log("외부에서 value 가 변경되어 에디터 내용을 동기화했습니다.");
      } catch (err) {
        logger.error("Error updating content:", err);
      }
    }
  }, [value, isReady, error]);

  /**
   * 에러 발생 시 대체 에디터 렌더링
   * Quill 로드 실패 시에도 기본 텍스트 입력은 가능하도록.
   */
  if (error) {
    return (
      <div className="w-full">
        <div className="h-[400px] border border-ink-soft rounded-lg bg-paper-50 dark:bg-postal-navy/20 flex flex-col">
          <div className="p-3 bg-paper-aged border-b border-ink-soft rounded-t-lg">
            <p className="text-sm text-brand-muted">
              간단 작성기 (에디터 로드 실패)
            </p>
          </div>
          <textarea
            className="w-full flex-1 p-4 border-0 resize-none bg-transparent text-brand-primary focus:outline-none focus:ring-2 focus:ring-postal-navy/40"
            placeholder={placeholder}
            value={value.replace(/<[^>]*>/g, "")} // HTML 태그 제거
            onChange={(e) => onChange(e.target.value)}
          />
        </div>
        <p className="text-xs text-stamp-red mt-1 break-keep">
          고급 작성기를 로드할 수 없어 간단 작성기로 전환하였습니다.
        </p>
      </div>
    );
  }

  return (
    <div className="w-full relative">
      {/* Quill 이 마운트될 DOM 요소 — 토큰 기반 배경/보더 + 다크 모드 */}
      <div
        ref={editorRef}
        className="bg-paper-50 dark:bg-postal-navy/20 h-[400px] rounded-lg border border-ink-soft dark:border-postal-navy/40 text-sm leading-relaxed flex flex-col"
      />
      {/* 에디터 초기화 중 로딩 오버레이 */}
      {!isReady && (
        <div className="absolute inset-0 bg-paper-50/90 dark:bg-postal-navy/40 flex items-center justify-center rounded-lg z-10">
          <div className="flex flex-col items-center gap-2">
            <Spinner size="md" />
            <p className="text-sm text-brand-secondary">에디터 준비 중...</p>
          </div>
        </div>
      )}
    </div>
  );
};

/**
 * 에디터 로딩 중 표시되는 컴포넌트
 * dynamic import 대기 시간 동안 사용자에게 로딩 상태를 보여줌.
 */
const EditorLoading = () => (
  <div className="relative h-[400px] bg-paper-50 dark:bg-postal-navy/20 rounded-lg border border-ink-soft dark:border-postal-navy/40 flex items-center justify-center">
    <div className="flex flex-col items-center gap-2">
      <Spinner size="md" />
      <p className="text-sm text-brand-secondary">에디터 로딩 중...</p>
    </div>
  </div>
);

/**
 * 메인 에디터 컴포넌트 (Dynamic Import)
 * SSR 환경에서 window 객체 접근 문제를 방지하기 위해
 * 클라이언트 사이드에서만 로드되도록 설정.
 */
const Editor = dynamic(() => Promise.resolve(QuillEditor), {
  ssr: false,
  loading: () => <EditorLoading />,
});

export default Editor;
