export default function AskPage() {
  return (
    <main className="flex-shrink-0">
      {/* Page content*/}
      <section className="py-5">
        <div className="container px-5">
          {/* Contact form*/}
          <div className="bg-light rounded-3 py-5 px-4 px-md-5 mb-5">
            <div className="row gx-5 justify-content-center">
              <div className="col-lg-8 col-xl-6">
                <form id="contactForm" data-sb-form-api-token="API_TOKEN">
                  {/* 신고 유형 선택 */}
                  <div className="form-floating mb-3">
                    <select
                      className="form-select"
                      id="reportType"
                      aria-label="신고 유형 선택"
                    >
                      <option value="BUG">버그 신고</option>
                      <option value="IMPROVEMENT">개선 사항</option>
                    </select>
                    <label htmlFor="reportType">신고 유형</label>
                  </div>

                  {/* 제목 */}
                  <div className="form-floating mb-3">
                    <input
                      className="form-control"
                      id="name"
                      type="text"
                      placeholder="제목을 입력하세요"
                      data-sb-validations="required"
                    />
                    <label htmlFor="name">제목</label>
                    <div
                      className="invalid-feedback"
                      data-sb-feedback="name:required"
                    >
                      A name is required.
                    </div>
                  </div>

                  {/* 내용 */}
                  <div className="form-floating mb-3">
                    <textarea
                      className="form-control"
                      id="message"
                      placeholder="내용을 입력하세요"
                      style={{ height: "10rem" }}
                      data-sb-validations="required"
                    ></textarea>
                    <label htmlFor="message">내용</label>
                    <div
                      className="invalid-feedback"
                      data-sb-feedback="message:required"
                    >
                      A message is required.
                    </div>
                  </div>

                  {/* Submit a success message*/}
                  {/**/}
                  {/* This is what your users will see when the form*/}
                  {/* has successfully submitted*/}
                  <div className="d-none" id="submitSuccessMessage">
                    <div className="text-center mb-3">
                      <div className="fw-bolder">
                        Form submission successful!
                      </div>
                      To activate this form, sign up at
                      <br />
                      <a href="https://startbootstrap.com/solution/contact-forms">
                        https://startbootstrap.com/solution/contact-forms
                      </a>
                    </div>
                  </div>
                  {/* Submit error message*/}
                  {/**/}
                  {/* This is what your users will see when there is*/}
                  {/* an error submitting the form*/}
                  <div className="d-none" id="submitErrorMessage">
                    <div className="text-center text-danger mb-3">
                      Error sending message!
                    </div>
                  </div>
                  {/* Submit Button*/}
                  <div className="d-grid">
                    <button
                      className="btn btn-primary btn-lg disabled"
                      id="submitButton"
                      type="submit"
                    >
                      제출
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
