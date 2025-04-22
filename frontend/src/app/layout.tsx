import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "농장 키우기",
  description: "농장 키우기 웹 애플리케이션",
};

// 네비게이션 컴포넌트
const Navigation = () => {
  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-dark">
      <div className="container px-5">
        {/* 로고 */}
        <a className="navbar-brand" href="/">
          농장 키우기
        </a>

        {/* 햄버거 메뉴 (모바일) */}
        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#navbarSupportedContent"
          aria-controls="navbarSupportedContent"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span className="navbar-toggler-icon"></span>
        </button>

        {/* 네비게이션 메뉴 */}
        <div className="collapse navbar-collapse" id="navbarSupportedContent">
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
              <a className="nav-link" href="/board">
                자유게시판
              </a>
            </li>
            <li className="nav-item">
              <a className="nav-link" href="/about">
                내 농장 가기
              </a>
            </li>
            <li className="nav-item">
              <a className="nav-link" href="/contact">
                문의하기
              </a>
            </li>
            <li className="nav-item">
              <a className="nav-link" href="/pricing">
                로그인
              </a>
            </li>
            <li className="nav-item">
              <a className="nav-link" href="/faq">
                로그아웃
              </a>
            </li>
            <li className="nav-item dropdown">
              <a
                className="nav-link dropdown-toggle"
                id="navbarDropdownBlog"
                href="#"
                role="button"
                data-bs-toggle="dropdown"
                aria-expanded="false"
              >
                예비 버튼
              </a>
              <ul
                className="dropdown-menu dropdown-menu-end"
                aria-labelledby="navbarDropdownBlog"
              >
                <li>
                  <a className="dropdown-item" href="/blog">
                    블로그 홈
                  </a>
                </li>
                <li>
                  <a className="dropdown-item" href="/blog/post">
                    블로그 포스트
                  </a>
                </li>
              </ul>
            </li>
            <li className="nav-item dropdown">
              <a
                className="nav-link dropdown-toggle"
                id="navbarDropdownPortfolio"
                href="#"
                role="button"
                data-bs-toggle="dropdown"
                aria-expanded="false"
              >
                예비 버튼
              </a>
              <ul
                className="dropdown-menu dropdown-menu-end"
                aria-labelledby="navbarDropdownPortfolio"
              >
                <li>
                  <a className="dropdown-item" href="/portfolio">
                    포트폴리오 개요
                  </a>
                </li>
                <li>
                  <a className="dropdown-item" href="/portfolio/item">
                    포트폴리오 항목
                  </a>
                </li>
              </ul>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  );
};

// 푸터 컴포넌트
const Footer = () => {
  return (
    <footer className="bg-dark py-4 mt-auto">
      <div className="container px-5">
        <div className="row align-items-center justify-content-between flex-column flex-sm-row">
          <div className="col-auto">
            <div className="small m-0 text-white">
              Copyright &copy; 농장 키우기 {new Date().getFullYear()}
            </div>
          </div>
          <div className="col-auto">
            <a className="link-light small" href="/privacy">
              개인정보처리방침
            </a>
            <span className="text-white mx-1">&middot;</span>
            <a className="link-light small" href="/terms">
              이용약관
            </a>
            <span className="text-white mx-1">&middot;</span>
            <a className="link-light small" href="/contact">
              개발자소개
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className="h-100">
      <head>
        <link
          href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.5.0/font/bootstrap-icons.css"
          rel="stylesheet"
        />
      </head>
      <body className="d-flex flex-column h-100">
        <Navigation />
        <div className="flex-grow-1 d-flex flex-column">{children}</div>
        <Footer />
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
      </body>
    </html>
  );
}
