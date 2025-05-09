export default function Manual() {
  return (
    <main className="flex-shrink-0">
      {/* Header */}
      <header className="py-1 bg-white">
        <div className="container px-5">
          <div className="row justify-content-center">
            <div className="col-lg-8 col-xxl-6">
              <div className="text-center my-5">
                <h1 className="fw-bolder mb-3">농장 키우기 사용법</h1>
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* About section one */}
      <section className="py-5 bg-light" id="scroll-target">
        <div className="container px-5 my-5">
          <div className="row gx-5 align-items-center">
            <div className="col-lg-6">
              <img
                className="img-fluid rounded mb-5 mb-lg-0"
                src="/farmimage2.jpg"
                alt="쪽지 심기 이미지"
              />
            </div>
            <div className="col-lg-6">
              <h2 className="fw-bolder">
                1. 친구의 농장에 익명으로 쪽지를 심어보세요!
              </h2>
              <p className="lead fw-normal text-muted mb-0">
                로그인을 하지 않아도 농장 이름으로 친구의 농장에 찾아갈 수
                있어요. 익명의 쪽지를 원하는 농작물로 감싸서 친구의 농장에
                심어보세요! 쪽지 내용은 암호화 되어 저장됩니다!
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* About section two */}
      <section className="py-5">
        <div className="container px-5 my-5">
          <div className="row gx-5 align-items-center">
            <div className="col-lg-6 order-first order-lg-last">
              <img
                className="img-fluid rounded mb-5 mb-lg-0"
                src="/farmimage3.jpg"
                alt="쪽지 보기 이미지"
              />
            </div>
            <div className="col-lg-6">
              <h2 className="fw-bolder">
                2. 농장을 만들어서 친구에게 쪽지를 받으세요!
              </h2>
              <p className="lead fw-normal text-muted mb-0">
                카카오 로그인을 하고 농장을 만들면, 친구들이 농장에 찾아올 수
                있어요. 친구들은 익명으로 여러분의 농장에 쪽지를 심을 거에요!
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* About section three */}
      <section className="py-5 bg-light" id="scroll-target">
        <div className="container px-5 my-5">
          <div className="row gx-5 align-items-center">
            <div className="col-lg-6">
              <img
                className="img-fluid rounded mb-5 mb-lg-0"
                src="/farmimage4.jpg"
                alt="게시판 이미지"
              />
            </div>
            <div className="col-lg-6">
              <h2 className="fw-bolder">3. 게시판에 글과 댓글을 남겨보세요!</h2>
              <p className="lead fw-normal text-muted mb-0">
                농장을 만들면 게시판에 글과 댓글을 남길 수 있고 자신의 농장을
                홍보할 수 있어요!
              </p>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
