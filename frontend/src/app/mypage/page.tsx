export default function MyPage() {
  return (
    <main className="flex-shrink-0">
      <div className="container px-5 py-5">
        <div className="row">
          {/* 프로필 섹션 */}
          <div className="col-md-4">
            <div className="card mb-4">
              <div className="card-body text-center">
                {/* 프로필 이미지 */}
                <img
                  src="https://dummyimage.com/150x150/6c757d/dee2e6.jpg"
                  alt="프로필 이미지"
                  className="img-fluid rounded-circle mb-4 px-4"
                />

                {/* 사용자 정보 */}
                <h5 className="fw-bold mb-1">사용자 닉네임</h5>
                <p className="text-muted mb-1">카카오ID: kakao_12345678</p>
                <p className="text-muted mb-3">농장 이름: 행복한농장</p>

                {/* 프로필 관리 버튼 */}
                <div className="d-grid gap-2 mb-2">
                  <button className="btn btn-secondary">
                    <i className="bi bi-pencil-square me-1"></i>농장 이름 변경
                  </button>
                  <button className="btn btn-secondary text-danger">
                    <i className="bi bi-person-x me-1"></i>회원 탈퇴
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* 내 활동 섹션 */}
          <div className="col-md-8">
            <div className="card">
              <div className="card-header">
                <h5 className="mb-0">내 활동</h5>
              </div>
              <div className="card-body">
                <div className="row mb-0">
                  <div className="col-md-6 mb-3">
                    <div className="d-grid">
                      <button className="btn btn-primary py-3">
                        내가 쓴 글 보기
                      </button>
                    </div>
                  </div>
                  <div className="col-md-6 mb-3">
                    <div className="d-grid">
                      <button className="btn btn-primary py-3">
                        내가 쓴 댓글 보기
                      </button>
                    </div>
                  </div>
                  <div className="col-md-6 mb-3">
                    <div className="d-grid">
                      <button className="btn btn-primary py-3">
                        추천한 글 보기
                      </button>
                    </div>
                  </div>
                  <div className="col-md-6 mb-3">
                    <div className="d-grid">
                      <button className="btn btn-primary py-3">
                        추천한 댓글 보기
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
