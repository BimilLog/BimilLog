"use client";

import { useEffect, useState } from "react";
import { SimplePostDTO } from "@/components/types/schema";
import Pagination from "@/components/Pagination";
import { formatDateTime } from "@/util/date";
import Link from "next/link";
import useAuthStore from "@/util/authStore";

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
  <tr>
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
  <li className="border-bottom pb-2 mb-2">
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

// API 기본 URL
const API_BASE = "http://localhost:8080";

export default function BoardPage() {
  // 페이지 상태관리
  const [currentPage, setCurrentPage] = useState<number>(1);
  const [totalPages, setTotalPages] = useState<number>(5);
  const [loading, setLoading] = useState<boolean>(false);
  const [posts, setPosts] = useState<SimplePostDTO[]>([]);
  const [featuredPosts, setFeaturedPosts] = useState<SimplePostDTO[]>([]);
  const [pageSize, setPageSize] = useState<number>(10);
  const [searchType, setSearchType] = useState<string>("제목");
  const [searchKeyword, setSearchKeyword] = useState<string>("");
  const [isSearchMode, setIsSearchMode] = useState<boolean>(false);
  const { user } = useAuthStore();

  // 일반 게시글 목록 불러오기
  const fetchPosts = async (page = currentPage) => {
    setLoading(true);
    try {
      const url = `${API_BASE}/board?page=${page - 1}&size=${pageSize}`;

      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(
          `게시글을 불러오는데 실패했습니다 (${response.status})`
        );
      }

      const data = await response.json();
      setPosts(data.posts.content);
      setTotalPages(data.posts.totalPages);
      setFeaturedPosts(data.featuredPosts);
    } catch (error) {
      console.error("게시글 불러오기 오류:", error);
      alert("게시글을 불러오는데 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setLoading(false);
    }
  };

  // 검색 결과 불러오기
  const fetchSearch = async (page = currentPage) => {
    setLoading(true);
    try {
      const url = `${API_BASE}/board/search?type=${searchType}&query=${encodeURIComponent(
        searchKeyword
      )}&page=${page - 1}&size=${pageSize}`;

      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`검색에 실패했습니다 (${response.status})`);
      }

      const data = await response.json();
      setPosts(data.content);
      setTotalPages(data.totalPages);
    } catch (error) {
      console.error("검색 오류:", error);
      alert("검색 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setLoading(false);
    }
  };

  // 초기 로딩 및 페이지/사이즈 변경 시 데이터 불러오기
  useEffect(() => {
    if (isSearchMode) {
      fetchSearch(currentPage);
    } else {
      fetchPosts(currentPage);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentPage, pageSize, isSearchMode]);

  // 핸들러 함수들
  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setPageSize(parseInt(e.target.value, 10));
    setCurrentPage(1);
  };

  // 검색 실행 핸들러
  const handleSearch = () => {
    const trimmed = searchKeyword.trim();
    if (trimmed) {
      setIsSearchMode(true);
      setCurrentPage(1);
      fetchSearch(1); // 검색 버튼 클릭 시 직접 API 호출
    } else {
      setIsSearchMode(false);
      fetchPosts(1);
    }
  };

  // 플레이스홀더 텍스트 반환
  const getPlaceholderText = () => {
    const placeholders: Record<string, string> = {
      제목: "제목으로 검색",
      작성자: "농장 이름으로 검색",
      제목내용: "제목+내용으로 검색",
    };
    return placeholders[searchType] || "검색어를 입력하세요";
  };

  return (
    <main className="flex-shrink-0">
      <header className="py-1 bg-white">
        <div className="text-center my-3"></div>
      </header>

      <div className="container px-5">
        <div className="row">
          {/* 게시글 목록 */}
          <div className="col-lg-8">
            {/* 검색 및 설정 영역 */}
            <div className="row mb-3">
              <div className="col-md-8">
                <div className="input-group mb-3">
                  <select
                    className="form-select flex-shrink-1"
                    style={{ maxWidth: "140px", minWidth: "140px" }}
                    value={searchType}
                    onChange={(e) => setSearchType(e.target.value)}
                  >
                    <option value="제목">제목 검색</option>
                    <option value="작성자">농장 검색</option>
                    <option value="제목내용">제목+내용 검색</option>
                  </select>
                  <input
                    type="text"
                    className="form-control"
                    placeholder={getPlaceholderText()}
                    value={searchKeyword}
                    onChange={(e) => setSearchKeyword(e.target.value)}
                    onKeyPress={(e) => e.key === "Enter" && handleSearch()}
                  />
                  <button
                    className="btn btn-dark"
                    type="button"
                    onClick={handleSearch}
                  >
                    검색
                  </button>
                </div>
              </div>
              <div className="col-md-4 text-end">
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

            {/* 검색 결과 표시 */}
            {isSearchMode && (
              <div className="mb-3">
                <div className="alert alert-light">
                  <strong>&ldquo;{searchKeyword}&rdquo;</strong>에 대한 검색 결과입니다.
                </div>
              </div>
            )}

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
                {user && (
                  <Link
                    href="/board/write"
                    className="btn btn-outline-secondary"
                  >
                    글쓰기
                  </Link>
                )}
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
