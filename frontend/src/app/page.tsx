import Link from "next/link";

export default function Home() {
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
                  <Link
                    className="btn btn-primary btn-lg px-4 me-sm-3"
                    href="#features"
                  >
                    카카오 로그인
                  </Link>
                  <Link
                    className="btn btn-outline-light btn-lg px-4"
                    href="/manual"
                  >
                    농장 키우기 사용법
                  </Link>
                </div>
              </div>
            </div>
            <div className="col-xl-5 col-xxl-6 d-none d-xl-block text-center">
              <img
                className="img-fluid rounded-3 my-5"
                src="https://dummyimage.com/600x400/343a40/6c757d"
                alt="..."
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
    </main>
  );
}
