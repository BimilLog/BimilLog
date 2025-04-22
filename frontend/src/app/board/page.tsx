"use client";

import { useEffect, useState } from "react";
import { SimplePostDTO } from "@/components/types/schema";
import Pagination from "@/components/Pagination";
import { formatDateTime } from "@/util/date";
import Link from "next/link";

// 로딩 스피너 컴포넌트
const LoadingSpinner = ({ small = false }: { small?: boolean }) => (
  <div className={`text-center py-${small ? 3 : 4}`}>
    <div
      className={`spinner-border ${
        small ? "spinner-border-sm" : ""
      } text-primary`}
      role="status"
    >
      <span className="visually-hidden">로딩중...</span>
    </div>
  </div>
);

// 게시글 목록 아이템 컴포넌트
const PostItem = ({ post }: { post: SimplePostDTO }) => (
  <tr key={post.postId}>
    <td>
      <Link
        href={`/board/${post.postId}`}
        className="text-decoration-none text-dark"
      >
        {post.title}
        {post.commentCount > 0 && (
          <span className="text-primary ms-1">[{post.commentCount}]</span>
        )}
      </Link>
    </td>
    <td>{post.farmName}</td>
    <td>{formatDateTime(post.createdAt).split(" ")[0]}</td>
    <td>{post.views}</td>
  </tr>
);

// 인기 게시글 아이템 컴포넌트
const PopularPostItem = ({ post }: { post: SimplePostDTO }) => (
  <li key={post.postId} className="border-bottom pb-2 mb-2">
    <Link
      href={`/board/${post.postId}`}
      className="text-decoration-none text-dark"
    >
      <div className="d-flex justify-content-between align-items-center">
        <span className="text-truncate" style={{ maxWidth: "250px" }}>
          {post.title}
        </span>
        <span className="badge bg-light text-dark">{post.views}</span>
      </div>
    </Link>
  </li>
);

export default function BoardPage() {
  // 페이지 상태관리
  const [currentPage, setCurrentPage] = useState<number>(1);
  const [totalPages, setTotalPages] = useState<number>(5);
  const [loading, setLoading] = useState<boolean>(false);
  const [posts, setPosts] = useState<SimplePostDTO[]>([]);
  const [featuredPosts, setFeaturedPosts] = useState<SimplePostDTO[]>([]);
  const [pageSize, setPageSize] = useState<number>(10);

  // 게시글 데이터 불러오기 (페이지별)
  useEffect(() => {
    const fetchPosts = async () => {
      setLoading(true);
      try {
        // 실제 API 호출
        const response = await fetch(
          `http://localhost:8080/board?page=${currentPage - 1}&size=${pageSize}`
        );
        const data = await response.json();

        // 일반 게시글 및 페이지 정보 설정
        setPosts(data.posts.content);
        setTotalPages(data.posts.totalPages);

        // 인기 게시글 설정
        setFeaturedPosts(data.featuredPosts);
        setLoading(false);
      } catch (error) {
        console.error("Posts 불러오기 오류:", error);
        setLoading(false);

        // 오류 메시지 표시
        alert("게시글을 불러오는데 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    };

    fetchPosts();
  }, [currentPage, pageSize]); // currentPage 또는 pageSize가 변경될 때마다 실행

  // 페이지 변경 핸들러
  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    // 페이지 상단으로 스크롤
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  // 페이지 사이즈 변경 핸들러
  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newSize = parseInt(e.target.value, 10);
    setPageSize(newSize);
    setCurrentPage(1); // 페이지 크기가 변경되면 첫 페이지로 돌아감
  };

  return (
    <main className="flex-shrink-0">
      {/* 공간 띄우기 용 */}
      <header className="py-1 bg-white">
        <div className="text-center my-3"></div>
      </header>

      {/* 게시판 본문 */}
      <div className="container px-5">
        <div className="row">
          {/* 게시글 목록 */}
          <div className="col-lg-8">
            {/* 게시판 검색 및 버튼 영역 */}
            <div className="row mb-3">
              <div className="col-md-6">
                <div className="input-group mb-3">
                  <input
                    type="text"
                    className="form-control"
                    placeholder="검색어를 입력하세요"
                    aria-label="검색어를 입력하세요"
                  />
                  <button className="btn btn-dark" type="button">
                    검색
                  </button>
                </div>
              </div>
              <div className="col-md-6 text-end">
                <select
                  className="form-select"
                  value={pageSize}
                  onChange={handlePageSizeChange}
                  style={{ width: "auto", display: "inline-block" }}
                >
                  <option value={10}>10개 보기</option>
                  <option value={30}>30개 보기</option>
                  <option value={50}>50개 보기</option>
                </select>
              </div>
            </div>

            {/* 게시글 목록 테이블 */}
            <div className="card mb-4">
              <div className="card-body p-0">
                <div className="table-responsive">
                  <table className="table table-hover mb-0">
                    <thead className="bg-light">
                      <tr>
                        <th style={{ width: "60%" }}>제목</th>
                        <th style={{ width: "15%" }}>농장</th>
                        <th style={{ width: "15%" }}>작성일</th>
                        <th style={{ width: "10%" }}>조회수</th>
                      </tr>
                    </thead>
                    <tbody>
                      {loading ? (
                        <tr>
                          <td colSpan={4} className="text-center">
                            <LoadingSpinner />
                          </td>
                        </tr>
                      ) : posts.length === 0 ? (
                        <tr>
                          <td colSpan={4} className="text-center py-4">
                            게시글이 없습니다.
                          </td>
                        </tr>
                      ) : (
                        posts.map((post) => (
                          <PostItem key={post.postId} post={post} />
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            {/* 페이지네이션과 글쓰기 버튼 */}
            <div className="d-flex justify-content-between align-items-center">
              <div className="flex-grow-1">
                <Pagination
                  currentPage={currentPage}
                  totalPages={totalPages}
                  onPageChange={handlePageChange}
                  pageRangeDisplayed={5}
                />
              </div>
              <div>
                <Link href="/board/write" className="btn btn-outline-secondary">
                  글쓰기
                </Link>
              </div>
            </div>
          </div>

          {/* 사이드바 */}
          <div className="col-lg-4">
            {/* 인기 게시글 */}
            <div className="card mb-4">
              <div className="card-header">인기 게시글</div>
              <div className="card-body">
                <ul className="list-unstyled mb-0">
                  {loading ? (
                    <LoadingSpinner small />
                  ) : featuredPosts.length === 0 ? (
                    <li className="text-center py-3">
                      인기 게시글이 없습니다.
                    </li>
                  ) : (
                    featuredPosts.map((post) => (
                      <PopularPostItem key={post.postId} post={post} />
                    ))
                  )}
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
