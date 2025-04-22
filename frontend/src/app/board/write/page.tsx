"use client";

import Link from "next/link";
import { useState } from "react";

export default function WritePage() {
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");

  // 제목 변경 핸들러
  const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setTitle(e.target.value);
  };

  // 내용 변경 핸들러
  const handleContentChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContent(e.target.value);
  };

  // 글 작성 완료 핸들러
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // 여기에 API 호출 로직 추가 예정
    console.log({ title, content });
    alert("글 작성이 완료되었습니다.");
    // 작성 완료 후 게시판 목록으로 이동 (실제 구현 시 추가)
  };

  return (
    <main className="flex-shrink-0">
      <div className="container px-5 my-5">
        <div className="col-lg-0">
          <form onSubmit={handleSubmit}>
            <article className="card bg-white">
              <header className="mb-4 card bg-light">
                <div className="p-3">
                  <input
                    type="text"
                    className="form-control form-control-lg"
                    placeholder="제목을 입력하세요"
                    value={title}
                    onChange={handleTitleChange}
                    required
                  />
                </div>
              </header>
              {/* 내용 텍스트 에어리어 */}
              <section className="mb-3 px-4">
                <textarea
                  className="form-control"
                  placeholder="내용을 입력하세요"
                  rows={15}
                  value={content}
                  onChange={handleContentChange}
                  required
                ></textarea>
              </section>

              {/* 버튼 영역 */}
              <div className="d-flex justify-content-end p-3">
                <Link href="/board" className="btn btn-outline-secondary me-2">
                  취소
                </Link>
                <button type="submit" className="btn btn-outline-primary">
                  작성 완료
                </button>
              </div>
            </article>
          </form>
        </div>
      </div>
    </main>
  );
}
