"use client";

import React, { useEffect, useRef } from "react";
import Quill from "quill";
import "quill/dist/quill.snow.css";

interface EditorProps {
  value: string;
  onChange: (value: string) => void;
}

const Editor: React.FC<EditorProps> = ({ value, onChange }) => {
  const editorRef = useRef<HTMLDivElement>(null);
  const quillRef = useRef<Quill | null>(null);

  useEffect(() => {
    if (editorRef.current && !quillRef.current) {
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
    }
  }, [onChange]);

  useEffect(() => {
    if (quillRef.current && quillRef.current.root.innerHTML !== value) {
      const delta = quillRef.current.clipboard.convert({ html: value });
      quillRef.current.setContents(delta, "silent");
    }
  }, [value]);

  return <div ref={editorRef} className="bg-white" />;
};

export default Editor;
