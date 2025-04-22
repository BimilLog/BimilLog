"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import useAuthStore from "@/util/authStore";

/**
 * 네비게이션 컴포넌트
 * 로그인 상태에 따라 다른 메뉴를 보여줍니다.
 */

// 카카오 로그인 처리 함수
const handleLogin = () => {
  // 환경 변수를 활용한 카카오 로그인 URL 생성
  const authUrl = process.env.NEXT_PUBLIC_KAKAO_AUTH_URL;
  const clientId = process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID;
  const redirectUri = process.env.NEXT_PUBLIC_KAKAO_REDIRECT_URI;

  // 카카오 로그인 페이지로 이동
  window.location.href = `${authUrl}?response_type=code&client_id=${clientId}&redirect_uri=${redirectUri}`;
};

const Navigation = () => {
  const { user, logout } = useAuthStore();
  const [isMounted, setIsMounted] = useState(false);

  // 컴포넌트가 마운트된 후에만 클라이언트 사이드 코드가 실행되도록 함
  useEffect(() => {
    setIsMounted(true);
  }, []);

  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-dark">
      <div className="container px-5">
        {/* 로고 */}
        <Link className="navbar-brand" href="/">
          농장 키우기
        </Link>

        {/* 햄버거 메뉴 (모바일) */}
        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#navbarSupportedContent"
          aria-controls="navbarSupportedContent"
          aria-expanded="false"
          aria-label="Toggle navigation"
          suppressHydrationWarning={true}
        >
          <span className="navbar-toggler-icon"></span>
        </button>

        {/* 네비게이션 메뉴 */}
        <div
          className="collapse navbar-collapse"
          id="navbarSupportedContent"
          suppressHydrationWarning={true}
        >
          {/* 데스크탑 검색바 */}
          <div
            className="d-none d-lg-block mx-lg-3 flex-grow-1"
            style={{ maxWidth: "400px" }}
          >
            <form className="d-flex" role="search">
              <div className="input-group input-group-sm">
                <input
                  type="search"
                  className="form-control form-control-sm"
                  placeholder="농장 이름을 입력하세요"
                  aria-label="농장 검색"
                  style={{ height: "31px" }}
                />
                <button
                  className="btn btn-secondary btn-sm d-flex align-items-center justify-content-center"
                  type="submit"
                  style={{
                    height: "31px",
                    minWidth: "80px",
                    fontSize: "0.8rem",
                  }}
                >
                  농장 가기
                </button>
              </div>
            </form>
          </div>

          {/* 모바일 검색바 */}
          <form className="d-flex d-lg-none my-3 w-100" role="search">
            <div className="input-group input-group-sm">
              <input
                type="search"
                className="form-control form-control-sm"
                placeholder="농장 이름을 입력하세요"
                aria-label="농장 검색"
              />
              <button
                className="btn btn-secondary btn-sm d-flex align-items-center justify-content-center"
                type="submit"
                style={{ minWidth: "80px", fontSize: "0.8rem" }}
              >
                농장 가기
              </button>
            </div>
          </form>

          {/* 메뉴 아이템들 */}
          <ul className="navbar-nav ms-auto mb-2 mb-lg-0">
            <li className="nav-item">
              <Link className="nav-link" href="/board">
                자유게시판
              </Link>
            </li>

            {/* 로그인 상태에 따른 메뉴 분기 */}
            {user ? (
              <>
                <li className="nav-item">
                  <Link className="nav-link" href="/farm">
                    내 농장 가기
                  </Link>
                </li>
                <li className="nav-item">
                  <Link className="nav-link" href="/ask">
                    문의하기
                  </Link>
                </li>
                <li className="nav-item">
                  <Link className="nav-link" href="/mypage">
                    마이페이지
                  </Link>
                </li>
                <li className="nav-item">
                  <button
                    className="nav-link"
                    onClick={logout}
                    style={{
                      background: "none",
                      border: "none",
                      cursor: "pointer",
                    }}
                  >
                    로그아웃
                  </button>
                </li>
              </>
            ) : (
              <li className="nav-item">
                <button
                  className="nav-link"
                  onClick={handleLogin}
                  style={{
                    background: "none",
                    border: "none",
                    cursor: "pointer",
                  }}
                >
                  로그인
                </button>
              </li>
            )}
          </ul>
        </div>
      </div>
    </nav>
  );
};

export default Navigation;
