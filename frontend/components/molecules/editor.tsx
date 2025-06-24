"use client";

import React, { useEffect, useRef, useState } from "react";
import dynamic from "next/dynamic";
import { Spinner } from "../atoms";

interface EditorProps {
  value: string;
  onChange: (value: string) => void;
}

// Quill 에디터 컴포넌트를 dynamic import로 로드
const QuillEditor: React.FC<EditorProps> = ({ value, onChange }) => {
  const editorRef = useRef<HTMLDivElement>(null);
  const quillRef = useRef<any>(null);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    const initQuill = async () => {
      if (
        typeof window === "undefined" ||
        !editorRef.current ||
        quillRef.current
      ) {
        return;
      }

      try {
        // Quill을 동적으로 import
        const { default: Quill } = await import("quill");

        // CSS를 동적으로 로드
        if (!document.querySelector('link[href*="quill.snow.css"]')) {
          const link = document.createElement("link");
          link.rel = "stylesheet";
          link.href = "https://cdn.quilljs.com/1.3.6/quill.snow.css";
          document.head.appendChild(link);
        }

        // 짧은 지연을 두어 CSS가 로드되도록 함
        await new Promise((resolve) => setTimeout(resolve, 100));

        quillRef.current = new Quill(editorRef.current, {
          theme: "snow",
          modules: {
            toolbar: [
              [{ header: "1" }, { header: "2" }, { font: [] }],
              [{ size: [] }],
              ["bold", "italic", "underline", "strike", "blockquote"],
              [
                { list: "ordered" },
                { list: "bullet" },
                { indent: "-1" },
                { indent: "+1" },
              ],
              ["link", "image"],
              ["clean"],
            ],
          },
        });

        const quill = quillRef.current;

        quill.on("text-change", () => {
          onChange(quill.root.innerHTML);
        });

        setIsReady(true);
      } catch (error) {
        console.error("Quill 로드 실패:", error);
        setIsReady(true); // 에러가 있어도 로딩 상태를 해제
      }
    };

    initQuill();
  }, [onChange]);

  useEffect(() => {
    if (
      quillRef.current &&
      isReady &&
      quillRef.current.root.innerHTML !== value
    ) {
      const delta = quillRef.current.clipboard.convert({ html: value });
      quillRef.current.setContents(delta, "silent");
    }
  }, [value, isReady]);

  return <div ref={editorRef} className="bg-white min-h-[200px]" />;
};

// 로딩 컴포넌트
const EditorLoading = () => (
  <div className="min-h-[200px] bg-white rounded-lg border flex items-center justify-center">
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
