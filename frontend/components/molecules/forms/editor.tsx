"use client";

import React, { useEffect, useRef, useState } from "react";
import dynamic from "next/dynamic";
import { Spinner } from "@/components";
import { logger } from "@/lib/utils";

interface EditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

/**
 * Quill 에디터 컴포넌트 - 게시글 작성 시 사용되는 리치 텍스트 에디터
 * SSR 이슈 방지를 위해 dynamic import 사용
 * Quill 2.0 호환성 및 안정성을 위한 복합적 초기화 로직 포함
 */
const QuillEditor: React.FC<EditorProps> = ({
  value,
  onChange,
  placeholder = "내용을 입력하세요...",
}) => {
  // DOM 요소 및 Quill 인스턴스 참조
  const editorRef = useRef<HTMLDivElement>(null);
  const quillRef = useRef<unknown>(null);

  // 중복 초기화 방지를 위한 플래그
  const isInitializing = useRef(false);

  // 에디터 상태 관리
  const [isReady, setIsReady] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    /**
     * Quill 에디터 초기화 함수
     * 복잡한 초기화 과정이 필요한 이유:
     * 1. SSR 환경에서 window 객체 접근 방지
     * 2. 중복 초기화 방지
     * 3. Quill 2.0 버전의 CSS 동적 로딩
     * 4. 브라우저 호환성 문제 해결
     */
    const initQuill = async () => {
      // 초기화 조건 체크: 서버사이드/DOM 미준비/중복 초기화 방지
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
        logger.log("Quill 에디터 초기화를 시작합니다...");

        // Quill 라이브러리를 동적으로 import (번들 크기 최적화)
        const { default: Quill } = await import("quill");

        /**
         * CSS 동적 로딩 함수
         * Quill 2.0은 CSS가 별도 로딩되어야 하므로 수동 로딩
         * 중복 로딩 방지 및 에러 핸들링 포함
         */
        const loadCSS = (href: string, id: string) => {
          return new Promise<void>((resolve, reject) => {
            if (document.querySelector(`#${id}`)) {
              resolve();
              return;
            }

            const link = document.createElement("link");
            link.id = id;
            link.rel = "stylesheet";
            link.href = href;
            link.onload = () => resolve();
            link.onerror = () =>
              reject(new Error(`Failed to load CSS: ${href}`));
            document.head.appendChild(link);
          });
        };

        // Quill CSS 파일들을 병렬로 로드
        await Promise.all([
          loadCSS(
            "https://cdn.jsdelivr.net/npm/quill@2.0.3/dist/quill.core.css",
            "quill-core-css"
          ),
          loadCSS(
            "https://cdn.jsdelivr.net/npm/quill@2.0.3/dist/quill.snow.css",
            "quill-snow-css"
          ),
        ]);

        // 에디터 높이 고정을 위한 커스텀 CSS 추가
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
              background: #f1f1f1;
              border-radius: 4px;
            }
            .ql-editor::-webkit-scrollbar-thumb {
              background: #888;
              border-radius: 4px;
            }
            .ql-editor::-webkit-scrollbar-thumb:hover {
              background: #555;
            }
          `;
          document.head.appendChild(style);
        }

        // CSS 스타일 적용 대기 (렌더링 완료 보장)
        await new Promise((resolve) => setTimeout(resolve, 300));

        // 기존 툴바가 있으면 제거 (중복 방지)
        const existingToolbar = editorRef.current.querySelector('.ql-toolbar');
        if (existingToolbar) {
          logger.log("기존 툴바 제거 중...");
          existingToolbar.remove();
        }

        /**
         * Quill 인스턴스 생성
         * toolbar와 formats를 Quill 2.0에 맞게 안전하게 설정
         * 커뮤니티 게시글 작성에 필요한 기본적인 서식만 포함
         */
        quillRef.current = new Quill(editorRef.current, {
          theme: "snow",
          placeholder: placeholder,
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
          // XSS 방지를 위해 안전한 포맷들만 허용
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
         * getSemanticHTML() 메서드 사용을 우선하되,
         * 없을 경우 innerHTML로 폴백 (Quill 버전 호환성)
         */
        quill.on("text-change", () => {
          try {
            const content = quill.getSemanticHTML
              ? quill.getSemanticHTML()
              : quill.root.innerHTML;
            onChange(content);
          } catch (err) {
            logger.error("Error getting content:", err);
            onChange(quill.root.innerHTML);
          }
        });

        // 기존 내용이 있는 경우 에디터에 설정
        if (value) {
          try {
            const delta = quill.clipboard.convert({ html: value });
            quill.setContents(delta, "silent");
          } catch (err) {
            logger.error("Error setting initial content:", err);
            quill.root.innerHTML = value;
          }
        }

        /**
         * SVG 아이콘 렌더링 문제 해결
         * Quill이 SVG 아이콘을 텍스트로 잘못 렌더링하는 경우가 있어
         * 툴바 버튼에서 잘못된 텍스트 노드를 제거
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

    // 컴포넌트 언마운트 시 메모리 누수 방지를 위한 정리
    return () => {
      if (quillRef.current) {
        try {
          (quillRef.current as { off: (event: string) => void }).off("text-change");
          // DOM 정리 - 툴바 제거
          if (editorRef.current) {
            const toolbar = editorRef.current.querySelector('.ql-toolbar');
            toolbar?.remove();
          }
        } catch (err) {
          logger.error("Error cleaning up Quill:", err);
        }
      }
    };
  }, [onChange, placeholder]);

  /**
   * 외부에서 value prop이 변경되었을 때 에디터 내용 동기화
   * 무한 루프 방지를 위해 실제 내용이 다를 때만 업데이트
   */
  useEffect(() => {
    if (quillRef.current && isReady && !error) {
      try {
        const quill = quillRef.current as {
          getSemanticHTML?: () => string;
          root: { innerHTML: string };
          clipboard: { convert: (options: { html: string }) => unknown };
          setContents: (delta: unknown, source: string) => void;
        };
        const currentContent = quill.getSemanticHTML
          ? quill.getSemanticHTML()
          : quill.root.innerHTML;

        if (value !== currentContent) {
          const delta = quill.clipboard.convert({ html: value });
          quill.setContents(delta, "silent");
        }
      } catch (err) {
        logger.error("Error updating content:", err);
      }
    }
  }, [value, isReady, error]);

  /**
   * 에러 발생 시 폴백 에디터 렌더링
   * Quill 로드 실패 시에도 기본 텍스트 입력이 가능하도록 함
   * HTML 태그는 제거하여 플레인 텍스트로 편집
   */
  if (error) {
    return (
      <div className="w-full">
        <div className="h-[400px] border border-gray-200 rounded-lg bg-white flex flex-col">
          <div className="p-3 bg-gray-50 border-b rounded-t-lg">
            <p className="text-sm text-brand-muted">
              간단 편집기 (에디터 로드 실패)
            </p>
          </div>
          <textarea
            className="w-full flex-1 p-4 border-0 resize-none focus:outline-none"
            placeholder={placeholder}
            value={value.replace(/<[^>]*>/g, "")} // HTML 태그 제거
            onChange={(e) => onChange(e.target.value)}
          />
        </div>
        <p className="text-xs text-red-500 mt-1">
          고급 편집기를 로드할 수 없어 간단 편집기로 전환되었습니다.
        </p>
      </div>
    );
  }

  return (
    <div className="w-full relative">
      {/* Quill 에디터가 마운트될 DOM 요소 */}
      <div
        ref={editorRef}
        className="bg-white h-[400px] rounded-lg border border-gray-200"
        style={{
          fontSize: "14px",
          lineHeight: "1.5",
          display: "flex",
          flexDirection: "column",
        }}
      />
      {/* 에디터 초기화 중 로딩 오버레이 */}
      {!isReady && (
        <div className="absolute inset-0 bg-white bg-opacity-90 flex items-center justify-center rounded-lg z-10">
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
 * dynamic import 대기 시간 동안 사용자에게 로딩 상태를 보여줌
 */
const EditorLoading = () => (
  <div className="relative h-[400px] bg-white rounded-lg border border-gray-200 flex items-center justify-center">
    <div className="flex flex-col items-center gap-2">
      <Spinner size="md" />
      <p className="text-sm text-brand-secondary">에디터 로딩 중...</p>
    </div>
  </div>
);

/**
 * 메인 에디터 컴포넌트 (Dynamic Import)
 * SSR 환경에서 window 객체 접근 문제를 방지하기 위해
 * 클라이언트 사이드에서만 로드되도록 설정
 */
const Editor = dynamic(() => Promise.resolve(QuillEditor), {
  ssr: false,
  loading: () => <EditorLoading />,
});

export default Editor;
