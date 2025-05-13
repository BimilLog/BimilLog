export default function Manual() {
  return (
    <main className="flex-shrink-0">
      {/* Header */}
      <header className="py-1 bg-white">
        <div className="container px-5">
          <div className="row justify-content-center">
            <div className="col-lg-8 col-xxl-6">
              <div className="text-center my-5">
                <h1 className="fw-bolder mb-3">네 마음을 심어줘 메뉴얼</h1>
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
                1. 친구의 농장에 마음을 심어보세요!
              </h2>
              <p className="lead fw-normal text-muted mb-0">
                친구의 농장을 꾸며줄 수 있어요. 마음을 담은 메시지로 친구의 농장을 가꿔보세요!
                자신이 누군지 드러내지 않아도 됩니다. 메시지는 암호화 되어 저장되고 오로지 친구만 확인할 수 있어요.
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
                2. 농장을 만들어 마음을 받아보세요!
              </h2>
              <p className="lead fw-normal text-muted mb-0">
                간단한 카카오 로그인으로 농장을 만들고 농장 홍보를 통해 친구들을 부를 수 있어요!
                친구들은 여러분의 농장에 마음을 심을거에요! 그 마음은 오로지 당신만 확인할 수 있어요!
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
                  src="/farmimage4.png"
                  alt="친구목록 이미지"
              />
            </div>
            <div className="col-lg-6">
              <h2 className="fw-bolder">3. 친구의 농장에 찾아가 보아요!</h2>
              <p className="lead fw-normal text-muted mb-0">
                친구 목록 불러오기를 통해 농장을 만든 카톡 친구들을 확인할 수 있어요!
                간편하게 친구의 농장에 찾아가보아요!
              </p>
            </div>
          </div>
        </div>
      </section>


      {/* About section four */}
      <section className="py-5">
        <div className="container px-5 my-5">
          <div className="row gx-5 align-items-center">
            <div className="col-lg-6 order-first order-lg-last">
              <img
                  className="img-fluid rounded mb-5 mb-lg-0"
                  src="/farmimage5.jpg"
                  alt="게시판 이미지"
              />
            </div>
            <div className="col-lg-6">
              <h2 className="fw-bolder">
                4. 게시판에 글과 댓글을 남겨보세요
              </h2>
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
