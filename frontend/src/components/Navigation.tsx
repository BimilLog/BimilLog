"use client";

import Link from "next/link";
import { useState, FormEvent } from "react";
import useAuthStore from "@/util/authStore";
import { useRouter } from "next/navigation";

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
  const [searchFarm, setSearchFarm] = useState(""); // 농장 검색어 상태
  const [isSearching, setIsSearching] = useState(false); // 검색 중 상태
  const router = useRouter();

  // 농장 검색 처리 함수
  const handleFarmSearch = async (e: FormEvent) => {
    e.preventDefault();

    // 검색어가 비어있으면 처리하지 않음
    if (!searchFarm.trim()) {
      alert("농장 이름을 입력해주세요");
      return;
    }

    setIsSearching(true);

    try {
      // 농장 존재 여부 확인을 위한 API 호출
      const response = await fetch(
        `https://grow-farm.com/api/farm/${encodeURIComponent(searchFarm.trim())}`,
        {
          method: "GET",
          credentials: "include",
        }
      );

      if (response.ok) {
        // 농장이 존재하면 해당 농장 페이지로 이동
        router.push(`/farm/${encodeURIComponent(searchFarm.trim())}`);
      } else {
        // 서버에서 오류 응답이 오면 존재하지 않는 농장
        alert("존재하지 않는 농장입니다");
      }
    } catch (error) {
      console.error("농장 검색 중 오류 발생:", error);
      alert("농장 검색 중 오류가 발생했습니다");
    } finally {
      setIsSearching(false);
    }
  };

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
            <form className="d-flex" role="search" onSubmit={handleFarmSearch}>
              <div className="input-group input-group-sm">
                <input
                  type="search"
                  className="form-control form-control-sm"
                  placeholder="농장 이름을 입력하세요"
                  aria-label="농장 검색"
                  style={{ height: "31px" }}
                  value={searchFarm}
                  onChange={(e) => setSearchFarm(e.target.value)}
                  disabled={isSearching}
                />
                <button
                  className="btn btn-secondary btn-sm d-flex align-items-center justify-content-center"
                  type="submit"
                  style={{
                    height: "31px",
                    minWidth: "80px",
                    fontSize: "0.8rem",
                  }}
                  disabled={isSearching}
                >
                  {isSearching ? "검색 중..." : "농장 가기"}
                </button>
              </div>
            </form>
          </div>

          {/* 모바일 검색바 */}
          <form
            className="d-flex d-lg-none my-3 w-100"
            role="search"
            onSubmit={handleFarmSearch}
          >
            <div className="input-group input-group-sm">
              <input
                type="search"
                className="form-control form-control-sm"
                placeholder="농장 이름을 입력하세요"
                aria-label="농장 검색"
                value={searchFarm}
                onChange={(e) => setSearchFarm(e.target.value)}
                disabled={isSearching}
              />
              <button
                className="btn btn-secondary btn-sm d-flex align-items-center justify-content-center"
                type="submit"
                style={{ minWidth: "80px", fontSize: "0.8rem" }}
                disabled={isSearching}
              >
                {isSearching ? "검색 중..." : "농장 가기"}
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
                  <Link className="nav-link" href={`/farm/${user?.farmName}`}>
                    내농장가기
                  </Link>
                </li>
                <li className="nav-item">
                  <Link className="nav-link" href="/ask">
                    건의하기
                  </Link>
                </li>
                <li className="nav-item">
                  <Link className="nav-link" href="/mypage">
                    마이페이지
                  </Link>
                </li>
                {/* 관리자 메뉴 - 관리자 권한이 있는 경우에만 표시 */}
                {user.role === "ADMIN" && (
                  <li className="nav-item">
                    <Link className="nav-link text-danger" href="/admin">
                      관리자
                    </Link>
                  </li>
                )}
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
