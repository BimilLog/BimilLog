"use client";

import React, { useEffect, useRef, useState } from "react";
import dynamic from "next/dynamic";
import { Spinner } from "../atoms";

interface EditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

// Quill 에디터 컴포넌트를 dynamic import로 로드
const QuillEditor: React.FC<EditorProps> = ({
  value,
  onChange,
  placeholder = "내용을 입력하세요...",
}) => {
  const editorRef = useRef<HTMLDivElement>(null);
  const quillRef = useRef<any>(null);
  const isInitializing = useRef(false);
  const [isReady, setIsReady] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const initQuill = async () => {
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
        console.log("Quill 에디터 초기화를 시작합니다...");

        // Quill을 동적으로 import
        const { default: Quill } = await import("quill");

        // Quill 2.0에 맞는 CSS를 동적으로 로드
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

        // CSS 로드 대기
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

        // CSS가 완전히 적용될 때까지 추가 대기
        await new Promise((resolve) => setTimeout(resolve, 300));

        // Quill 인스턴스 생성 - Quill 2.0 호환성을 위한 안전한 설정
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
          // Quill 2.0에서 안전한 기본 포맷들만 사용
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

        const quill = quillRef.current;

        // 내용 변경 이벤트 리스너
        quill.on("text-change", () => {
          try {
            const content = quill.getSemanticHTML
              ? quill.getSemanticHTML()
              : quill.root.innerHTML;
            onChange(content);
          } catch (err) {
            console.error("Error getting content:", err);
            onChange(quill.root.innerHTML);
          }
        });

        // 초기 값 설정
        if (value) {
          try {
            const delta = quill.clipboard.convert({ html: value });
            quill.setContents(delta, "silent");
          } catch (err) {
            console.error("Error setting initial content:", err);
            quill.root.innerHTML = value;
          }
        }

        // SVG 아이콘 문제 해결을 위한 추가 처리
        setTimeout(() => {
          const toolbar = editorRef.current?.querySelector(".ql-toolbar");
          if (toolbar) {
            // 잘못된 텍스트 노드 제거
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
        console.log("Quill 에디터가 성공적으로 초기화되었습니다.");
      } catch (error) {
        console.error("Quill 로드 실패:", error);
        setError(
          error instanceof Error ? error.message : "에디터 로드에 실패했습니다."
        );
        setIsReady(true);
      } finally {
        isInitializing.current = false;
      }
    };

    initQuill();

    // 컴포넌트 언마운트 시 정리
    return () => {
      if (quillRef.current) {
        try {
          quillRef.current.off("text-change");
          quillRef.current = null;
        } catch (err) {
          console.error("Error cleaning up Quill:", err);
        }
      }
    };
  }, [onChange, placeholder]);

  // value prop 변경 시 에디터 내용 업데이트
  useEffect(() => {
    if (quillRef.current && isReady && !error) {
      try {
        const currentContent = quillRef.current.getSemanticHTML
          ? quillRef.current.getSemanticHTML()
          : quillRef.current.root.innerHTML;

        if (value !== currentContent) {
          const delta = quillRef.current.clipboard.convert({ html: value });
          quillRef.current.setContents(delta, "silent");
        }
      } catch (err) {
        console.error("Error updating content:", err);
      }
    }
  }, [value, isReady, error]);

  // 에러 발생 시 폴백 에디터 렌더링
  if (error) {
    return (
      <div className="w-full">
        <div className="min-h-[200px] border border-gray-200 rounded-lg bg-white">
          <div className="p-3 bg-gray-50 border-b rounded-t-lg">
            <p className="text-sm text-gray-600">
              간단 편집기 (에디터 로드 실패)
            </p>
          </div>
          <textarea
            className="w-full h-48 p-4 border-0 resize-none focus:outline-none"
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
      <div
        ref={editorRef}
        className="bg-white min-h-[200px] rounded-lg"
        style={{
          fontSize: "14px",
          lineHeight: "1.5",
        }}
      />
      {!isReady && (
        <div className="absolute inset-0 bg-white bg-opacity-90 flex items-center justify-center rounded-lg z-10">
          <div className="flex flex-col items-center gap-2">
            <Spinner size="md" />
            <p className="text-sm text-gray-500">에디터 준비 중...</p>
          </div>
        </div>
      )}
    </div>
  );
};

// 로딩 컴포넌트
const EditorLoading = () => (
  <div className="relative min-h-[200px] bg-white rounded-lg border border-gray-200 flex items-center justify-center">
    <div className="flex flex-col items-center gap-2">
      <Spinner size="md" />
      <p className="text-sm text-gray-500">에디터 로딩 중...</p>
    </div>
  </div>
);

// Dynamic import로 SSR 문제 해결
const Editor = dynamic(() => Promise.resolve(QuillEditor), {
  ssr: false,
  loading: () => <EditorLoading />,
});

export default Editor;
