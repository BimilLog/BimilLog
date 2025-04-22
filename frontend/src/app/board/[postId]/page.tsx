export default function PostPage() {
  return (
    <main className="flex-shrink-0">
      {/* Page Content*/}
      <div className="container px-5 my-5">
        <div className="col-lg-0">
          <article className="card bg-white">
            <header className="mb-4 card bg-light">
              <h1 className="fw-bolder pt-4 pb-2 text-center">
                Welcome to Blog Post!
              </h1>
              <div className="fw-bold text-xl-end mx-3">Valerie Luna</div>
              <div className="text-muted fst-italic mb-2 text-xl-end mx-3">
                January 1, 2023
              </div>
            </header>
            {/* Post content*/}
            <section className="mb-3 px-4">
              <p className="fs-5 mb-4">
                Science is an enterprise that should be cherished as an activity
                of the free human mind. Because it transforms who we are, how we
                live, and it gives us an understanding of our place in the
                universe.
              </p>
              <p className="fs-5 mb-4">
                The universe is large and old, and the ingredients for life as
                we know it are everywhere, so there's no reason to think that
                Earth would be unique in that regard. Whether of not the life
                became intelligent is a different question, and we'll see if we
                find that.
              </p>
              <p className="fs-5 mb-4">
                If you get asteroids about a kilometer in size, those are large
                enough and carry enough energy into our system to disrupt
                transportation, communication, the food chains, and that can be
                a really bad day on Earth.
              </p>
              <p className="fs-5 mb-4">
                For me, the most fascinating interface is Twitter. I have odd
                cosmic thoughts every day and I realized I could hold them to
                myself or share them with people who might be interested.
              </p>
              <p className="fs-5 mb-4">
                Venus has a runaway greenhouse effect. I kind of want to know
                what happened there because we're twirling knobs here on Earth
                without knowing the consequences of it. Mars once had running
                water. It's bone dry today. Something bad happened there as
                well.
              </p>
            </section>
            {/* 글 추천 버튼 */}
            <div className="d-flex justify-content-center mb-4">
              <button className="btn btn-outline-primary me-2">
                <i className="bi bi-hand-thumbs-up"></i> 추천 (0)
              </button>
              <button className="btn btn-outline-secondary">
                <i className="bi bi-share"></i> 공유하기
              </button>
            </div>
          </article>
        </div>
        {/* Comments section*/}
        <section>
          <div className="card bg-light mt-4">
            <div className="card-header">
              <h5 className="mb-0">댓글</h5>
            </div>
            <div className="card-body">
              {/* Comment form*/}
              <form className="mb-4">
                <textarea
                  className="form-control"
                  rows={3}
                  placeholder="댓글을 입력하세요..."
                ></textarea>
                <div className="d-flex justify-content-end mt-2">
                  <button type="submit" className="btn btn-outline-secondary">
                    댓글 작성
                  </button>
                </div>
              </form>
              {/* Comment with nested comments*/}
              <div className="d-flex mb-4">
                {/* Parent comment*/}
                <div className="ms-3 w-100">
                  <div className="d-flex justify-content-between">
                    <div className="fw-bold">Commenter Name</div>
                    <small className="text-muted">2023.01.01 12:34</small>
                  </div>
                  <p>
                    If you're going to lead a space frontier, it has to be
                    government; it'll never be private enterprise. Because the
                    space frontier is dangerous, and it's expensive, and it has
                    unquantified risks.
                  </p>
                  <div className="d-flex justify-content-between align-items-center">
                    <button className="btn btn-sm btn-outline-primary">
                      <i className="bi bi-hand-thumbs-up"></i> 추천 (3)
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
