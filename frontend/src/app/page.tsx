"use client";

import Link from "next/link";
import useAuthStore from "@/util/authStore";
import { useState, useEffect } from "react";
import { KakaoFriendDTO, SimplePostDTO } from "@/components/types/schema";
import fetchClient from "@/util/fetchClient";
import LoadingSpinner from "@/components/LoadingSpinner";
import Pagination from "@/components/Pagination";

const API_BASE = "http://localhost:8080";

// 게시글 타입 정의
interface PostType {
  _RealtimePopular?: boolean;
  is_RealtimePopular?: boolean;
  _WeeklyPopular?: boolean;
  is_WeeklyPopular?: boolean;
  _HallOfFame?: boolean;
  is_HallOfFame?: boolean;
  postId?: number;
  title?: string;
  content?: string;
  farmName?: string;
  createdAt?: string;
  likes?: number;
  views?: number;
  commentCount?: number;
  userId?: number;
}

// 인기 게시글 아이템 컴포넌트
const PopularPostItem = ({ post }: { post: SimplePostDTO }) => (
  <div className="mb-3">
    <Link
      href={`/board/${post.postId}`}
      className="text-decoration-none text-dark"
    >
      <div className="d-flex justify-content-between align-items-center">
        <span className="text-truncate" style={{ maxWidth: "250px" }}>
          {post.title}
          {post.commentCount > 0 && (
            <span className="text-primary ms-1">[{post.commentCount}]</span>
          )}
        </span>
        <span className="badge bg-light text-dark">👍 {post.likes}</span>
      </div>
    </Link>
  </div>
);

export default function Home() {
  const { user } = useAuthStore();
  const [friends, setFriends] = useState<KakaoFriendDTO[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const [loadingFriends, setLoadingFriends] = useState(false);

  // 인기글 상태관리
  const [realtimePosts, setRealtimePosts] = useState<SimplePostDTO[]>([]);
  const [weeklyPosts, setWeeklyPosts] = useState<SimplePostDTO[]>([]);
  const [famePosts, setFamePosts] = useState<SimplePostDTO[]>([]);
  const [realtimeLoading, setRealtimeLoading] = useState<boolean>(false);
  const [weeklyLoading, setWeeklyLoading] = useState<boolean>(false);
  const [fameLoading, setFameLoading] = useState<boolean>(false);

  const handleLogin = () => {
    window.location.href = `${process.env.NEXT_PUBLIC_KAKAO_AUTH_URL}?response_type=code&client_id=${process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID}&redirect_uri=${process.env.NEXT_PUBLIC_KAKAO_REDIRECT_URI}`;
  };

  const handleFetchFriends = async () => {
    setShowModal(true);
    fetchFriendsByPage(1);
  };

  const fetchFriendsByPage = async (page: number) => {
    setLoadingFriends(true);
    try {
      // 페이지가 1부터 시작하지만 offset은 0부터 시작하므로 계산
      const offset = (page - 1) * 10;
      const response = await fetchClient(
        `${API_BASE}/user/friendlist?offset=${offset}`,
        {
          method: "POST",
        }
      );

      if (!response.ok) {
        const errorData = await response.json().catch(() => null);
        console.log("에러 데이터:", errorData);

        // KAKAO_FRIEND_CONSENT_FAIL 오류 처리
        if (
          errorData?.errorCode === "KAKAO_FRIEND_CONSENT_FAIL" ||
          (errorData?.status === 401 &&
            errorData?.message?.includes("카카오 친구 추가 동의을 해야 합니다"))
        ) {
          const confirm = window.confirm(
            "카카오톡 친구 목록 제공 동의를 하지 않았습니다. 친구 목록 제공 동의를 하시겠습니까?"
          );

          if (confirm) {
            const authUrl = process.env.NEXT_PUBLIC_KAKAO_AUTH_URL;
            const clientId = process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID;
            const redirectUri = process.env.NEXT_PUBLIC_KAKAO_REDIRECT_URI;

            window.location.href = `${authUrl}?response_type=code&client_id=${clientId}&redirect_uri=${redirectUri}&scope=friends`;
            return;
          } else {
            setShowModal(false);
            return;
          }
        }

        throw new Error("친구 목록 불러오기 실패");
      }

      const data = await response.json();

      console.log("받은 데이터:", data);

      // Page<KakaoFriendListDTO> 형태 처리
      if (data.content && Array.isArray(data.content.elements)) {
        setFriends(data.content.elements);
        setTotalCount(data.content.total_count || 0);
        setTotalPages(Math.ceil((data.content.total_count || 0) / 10));
      } else if (data.elements) {
        // 기존 형태 대응
        setFriends(data.elements);
        setTotalCount(data.total_count || 0);
        console.log("총 친구 수:", data.total_count);
        setTotalPages(Math.ceil((data.total_count || 0) / 10));
      }

      setCurrentPage(page);
    } catch (error) {
      console.error(error);
    } finally {
      setLoadingFriends(false);
    }
  };

  const handlePageChange = (page: number) => {
    fetchFriendsByPage(page);
  };

  const handleVisitFarm = async (farmName: string) => {
    try {
      const response = await fetchClient(`${API_BASE}/farm/${farmName}`);
      if (response.ok) {
        window.location.href = `/farm/${farmName}`;
      } else {
        alert("농장을 방문할 수 없습니다.");
      }
    } catch (error) {
      console.error("농장 방문 오류:", error);
      alert("농장 방문 중 오류가 발생했습니다.");
    }
  };

  // 실시간 인기글 불러오기
  const fetchRealtimePosts = async () => {
    setRealtimeLoading(true);
    try {
      const response = await fetchClient(`${API_BASE}/board/realtime`);
      if (!response.ok) {
        throw new Error("실시간 인기글을 불러오는데 실패했습니다.");
      }
      const data = await response.json();
      // _RealtimePopular 플래그가 true인 게시글만 필터링
      const filteredPosts = Array.isArray(data)
        ? data.filter((post: PostType) => post._RealtimePopular === true)
        : Array.isArray(data?.content)
        ? data.content.filter(
            (post: PostType) =>
              post.is_RealtimePopular === true || post._RealtimePopular === true
          )
        : [];
      setRealtimePosts(filteredPosts);
    } catch (error) {
      console.error("실시간 인기글 불러오기 오류:", error);
      setRealtimePosts([]);
    } finally {
      setRealtimeLoading(false);
    }
  };

  // 주간 인기글 불러오기
  const fetchWeeklyPosts = async () => {
    setWeeklyLoading(true);
    try {
      const response = await fetchClient(`${API_BASE}/board/weekly`);
      if (!response.ok) {
        throw new Error("주간 인기글을 불러오는데 실패했습니다.");
      }
      const data = await response.json();
      // _WeeklyPopular 플래그가 true인 게시글만 필터링
      const filteredPosts = Array.isArray(data)
        ? data.filter((post: PostType) => post._WeeklyPopular === true)
        : Array.isArray(data?.content)
        ? data.content.filter(
            (post: PostType) =>
              post.is_WeeklyPopular === true || post._WeeklyPopular === true
          )
        : [];
      setWeeklyPosts(filteredPosts);
    } catch (error) {
      console.error("주간 인기글 불러오기 오류:", error);
      setWeeklyPosts([]);
    } finally {
      setWeeklyLoading(false);
    }
  };

  // 명예의 전당 불러오기
  const fetchFamePosts = async () => {
    setFameLoading(true);
    try {
      const response = await fetchClient(`${API_BASE}/board/fame`);
      if (!response.ok) {
        throw new Error("명예의 전당을 불러오는데 실패했습니다.");
      }
      const data = await response.json();
      // _HallOfFame 플래그가 true인 게시글만 필터링
      const filteredPosts = Array.isArray(data)
        ? data.filter((post: PostType) => post._HallOfFame === true)
        : Array.isArray(data?.content)
        ? data.content.filter(
            (post: PostType) =>
              post.is_HallOfFame === true || post._HallOfFame === true
          )
        : [];
      setFamePosts(filteredPosts);
    } catch (error) {
      console.error("명예의 전당 불러오기 오류:", error);
      setFamePosts([]);
    } finally {
      setFameLoading(false);
    }
  };

  // 페이지 로드 시 인기글 데이터 불러오기
  useEffect(() => {
    fetchRealtimePosts();
    fetchWeeklyPosts();
    fetchFamePosts();
  }, []);

  return (
    <main className="flex-shrink-0">
      {/* Header */}
      <header className="bg-dark py-5">
        <div className="container px-5">
          <div className="row gx-5 align-items-center justify-content-center">
            <div className="col-lg-8 col-xl-7 col-xxl-6">
              <div className="my-5 text-center text-xl-start">
                <h1 className="display-5 fw-bolder text-white mb-2">
                  익명 메시지로 마음을 담아 친구의 농장을 꾸며보세요!
                </h1>
                <p className="lead fw-normal text-white-50 mb-4">
                  카카오 로그인으로 내 농장을 만들어 친구로부터 메시지를
                  받아보세요!
                </p>
                <div className="d-grid gap-3 d-sm-flex justify-content-sm-center justify-content-xl-start position-relative">
                  {!user && (
                    <div
                      onClick={handleLogin}
                      className="d-inline-block me-sm-3"
                      style={{
                        transition: "transform 0.2s ease-in-out",
                        cursor: "pointer",
                      }}
                      onMouseOver={(e) =>
                        (e.currentTarget.style.transform = "scale(1.05)")
                      }
                      onMouseOut={(e) =>
                        (e.currentTarget.style.transform = "scale(1)")
                      }
                    >
                      <img
                        src="/kakao_login_large_narrow.png"
                        alt="카카오 로그인"
                        className="img-fluid"
                        style={{
                          maxHeight: "50px",
                          width: "auto",
                          minWidth: "130px",
                        }}
                      />
                    </div>
                  )}
                  <Link
                    className="btn btn-outline-light btn-lg px-15"
                    href="/manual"
                  >
                    네 마음을 심어줘 사용법
                  </Link>
                  {user && (
                    <button
                      onClick={handleFetchFriends}
                      className="btn btn-outline-light btn-lg px-15"
                    >
                      카카오톡 친구 불러오기
                    </button>
                  )}

                  {/* 모달을 버튼 아래에 배치 */}
                  {showModal && (
                    <div
                      className="position-absolute start-0 top-100 mt-2"
                      style={{
                        width: "100%",
                        maxWidth: "500px",
                        zIndex: 9999,
                        backgroundColor: "white",
                        boxShadow: "0 0 20px rgba(0,0,0,0.3)",
                        borderRadius: "5px",
                      }}
                    >
                      <div className="modal-content border">
                        <div
                          className="modal-header"
                          style={{
                            backgroundColor: "#444444",
                            borderRadius: "5px 5px 0 0",
                          }}
                        >
                          <h5 className="modal-title w-100 text-center text-white">
                            카카오톡 친구 목록
                          </h5>
                        </div>
                        <div className="modal-body p-0">
                          {loadingFriends ? (
                            <div className="text-center py-4">
                              <LoadingSpinner width={50} height={50} />
                            </div>
                          ) : friends.length > 0 ? (
                            <>
                              <div className="table-responsive">
                                <table className="table table-hover mb-0">
                                  <thead style={{ backgroundColor: "#f8f9fa" }}>
                                    <tr className="text-center">
                                      <th
                                        className="py-3"
                                        style={{ width: "40%" }}
                                      >
                                        친구
                                      </th>
                                      <th
                                        className="py-3 text-start"
                                        style={{ width: "30%" }}
                                      >
                                        농장 이름
                                      </th>
                                      <th
                                        className="py-3"
                                        style={{ width: "30%" }}
                                      >
                                        친구 {totalCount}명
                                      </th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {friends.map((friend) => (
                                      <tr key={friend.id}>
                                        <td className="align-middle py-3">
                                          <div className="d-flex align-items-center justify-content-center">
                                            <img
                                              src={
                                                friend.profile_thumbnail_image
                                              }
                                              alt={friend.profile_nickname}
                                              width={40}
                                              height={40}
                                              className="rounded-circle me-3"
                                            />
                                            {friend.profile_nickname}
                                          </div>
                                        </td>
                                        <td className="align-middle py-3 text-start ps-4">
                                          {friend.farmName || "-"}
                                        </td>
                                        <td className="align-middle py-3 text-center">
                                          <button
                                            className="btn btn-primary btn-sm px-3"
                                            onClick={() =>
                                              handleVisitFarm(friend.farmName)
                                            }
                                            disabled={!friend.farmName}
                                          >
                                            농장 가기
                                          </button>
                                        </td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                              </div>

                              <div className="p-3">
                                {totalPages > 1 && (
                                  <Pagination
                                    currentPage={currentPage}
                                    totalPages={totalPages}
                                    onPageChange={handlePageChange}
                                  />
                                )}
                              </div>
                            </>
                          ) : (
                            <p className="text-center py-3">
                              친구 목록이 없습니다.
                            </p>
                          )}
                        </div>
                        <div className="modal-footer justify-content-end">
                          <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={() => setShowModal(false)}
                          >
                            닫기
                          </button>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
            <div className="col-xl-5 col-xxl-6 d-none d-xl-block text-center">
              <img
                className="img-fluid rounded-3 my-5"
                src="/farmImage.jpeg"
                alt="농장 이미지"
              />
            </div>
          </div>
        </div>
      </header>
      {/* Blog preview section */}
      <section className="py-5">
        <div className="container px-5 my-5">
          <div className="row gx-5 justify-content-center">
            <div className="col-lg-8 col-xl-6">
              <div className="text-center">
                <h2 className="fw-bolder">인기글</h2>
                <p className="lead fw-normal text-muted mb-5">
                  인기글을 확인해 보세요
                </p>
              </div>
            </div>
          </div>
          <div className="row gx-5">
            <div className="col-lg-4 mb-5">
              <div className="card h-100 shadow border-0">
                <div className="card-body p-4">
                  <h5 className="card-title mb-3">실시간 인기글</h5>
                  {realtimeLoading ? (
                    <div className="text-center py-4">
                      <LoadingSpinner width={50} height={50} />
                    </div>
                  ) : realtimePosts.length === 0 ? (
                    <p className="text-center text-muted py-3">
                      실시간 인기글이 없습니다.
                    </p>
                  ) : (
                    <div>
                      {realtimePosts.slice(0, 5).map((post) => (
                        <PopularPostItem key={post.postId} post={post} />
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
            <div className="col-lg-4 mb-5">
              <div className="card h-100 shadow border-0">
                <div className="card-body p-4">
                  <h5 className="card-title mb-3">주간 인기글</h5>
                  {weeklyLoading ? (
                    <div className="text-center py-4">
                      <LoadingSpinner width={50} height={50} />
                    </div>
                  ) : weeklyPosts.length === 0 ? (
                    <p className="text-center text-muted py-3">
                      주간 인기글이 없습니다.
                    </p>
                  ) : (
                    <div>
                      {weeklyPosts.slice(0, 5).map((post) => (
                        <PopularPostItem key={post.postId} post={post} />
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
            <div className="col-lg-4 mb-5">
              <div className="card h-100 shadow border-0">
                <div className="card-body p-4">
                  <h5 className="card-title mb-3">명예의 전당</h5>
                  {fameLoading ? (
                    <div className="text-center py-4">
                      <LoadingSpinner width={50} height={50} />
                    </div>
                  ) : famePosts.length === 0 ? (
                    <p className="text-center text-muted py-3">
                      명예의 전당이 비었습니다.
                    </p>
                  ) : (
                    <div>
                      {famePosts.slice(0, 5).map((post) => (
                        <PopularPostItem key={post.postId} post={post} />
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
