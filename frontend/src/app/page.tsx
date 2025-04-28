"use client";

import Link from "next/link";
import useAuthStore from "@/util/authStore";
import { useState } from "react";
import { KakaoFriendDTO, KakaoFriendListDTO } from "@/components/types/schema";

export default function Home() {
  const { user } = useAuthStore();
  const [friends, setFriends] = useState<KakaoFriendDTO[]>([]);
  const [showModal, setShowModal] = useState(false);

  const handleLogin = () => {
    window.location.href = `${process.env.NEXT_PUBLIC_KAKAO_AUTH_URL}?response_type=code&client_id=${process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID}&redirect_uri=${process.env.NEXT_PUBLIC_KAKAO_REDIRECT_URI}`;
  };

  const handleFetchFriends = async () => {
    setShowModal(true);
    try {
      const response = await fetch(
        `http://localhost:8080/user/friendlist?offset=0`,
        { method: "POST", credentials: "include" }
      );
      if (!response.ok) throw new Error("친구 목록 불러오기 실패");
      const data: KakaoFriendListDTO = await response.json();
      setFriends(data.elements);
    } catch (error) {
      console.error(error);
    }
  };

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
                <div className="d-grid gap-3 d-sm-flex justify-content-sm-center justify-content-xl-start">
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
                    농장 키우기 사용법
                  </Link>
                  {user && (
                    <button
                      onClick={handleFetchFriends}
                      className="btn btn-outline-light btn-lg px-15"
                    >
                      카카오톡 친구 불러오기
                    </button>
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
                </div>
              </div>
            </div>
            <div className="col-lg-4 mb-5">
              <div className="card h-100 shadow border-0">
                <div className="card-body p-4">
                  <h5 className="card-title mb-3">주간 인기글</h5>
                </div>
              </div>
            </div>
            <div className="col-lg-4 mb-5">
              <div className="card h-100 shadow border-0">
                <div className="card-body p-4">
                  <h5 className="card-title mb-3">명예의 전당</h5>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
      {showModal && (
        <div className="modal show d-block" tabIndex={-1}>
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">카카오톡 친구 목록</h5>
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Close"
                  onClick={() => setShowModal(false)}
                ></button>
              </div>
              <div className="modal-body">
                {friends.length > 0 ? (
                  <ul className="list-group">
                    {friends.map((friend) => (
                      <li
                        key={friend.id}
                        className="list-group-item d-flex align-items-center"
                      >
                        <img
                          src={friend.profileThumbnailImage}
                          alt={friend.profileNickname}
                          width={40}
                          height={40}
                          className="rounded-circle me-2"
                        />
                        {friend.profileNickname}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p>친구 목록이 없습니다.</p>
                )}
              </div>
              <div className="modal-footer">
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
        </div>
      )}
    </main>
  );
}
