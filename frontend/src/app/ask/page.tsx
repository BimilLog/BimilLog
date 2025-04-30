"use client";

import { useState } from "react";
import { ReportType } from "@/components/types/schema";
import { useRouter } from "next/navigation";
import useAuthStore from "@/util/authStore";

export default function AskPage() {
  const { user } = useAuthStore();
  const [formData, setFormData] = useState({
    reportType: ReportType.BUG,
    content: "",
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [submitError, setSubmitError] = useState(false);
  const router = useRouter();

  const handleInputChange = (
    e: React.ChangeEvent<HTMLSelectElement | HTMLTextAreaElement>
  ) => {
    const { id, value } = e.target;
    setFormData({
      ...formData,
      [id === "reportType" ? "reportType" : "content"]: value,
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!user) {
      alert("로그인이 필요합니다.");
      router.push("/");
      return;
    }

    if (!formData.content.trim()) {
      alert("내용을 입력해주세요.");
      return;
    }

    setIsSubmitting(true);
    setSubmitError(false);
    setSubmitSuccess(false);

    try {
      const reportDTO = {
        reportId: null, // 빈 칸
        reportType: formData.reportType,
        userId: user.userId,
        targetId: null, // 빈 칸
        content: formData.content,
      };

      const response = await fetch("https://grow-farm.com/api/user/suggestion", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include", // 쿠키 포함
        body: JSON.stringify(reportDTO),
      });

      if (response.ok) {
        setSubmitSuccess(true);
        setFormData({
          reportType: ReportType.BUG,
          content: "",
        });
      } else {
        setSubmitError(true);
      }
    } catch (error) {
      console.error("제출 중 오류 발생:", error);
      setSubmitError(true);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="flex-shrink-0">
      {/* Page content*/}
      <section className="py-5">
        <div className="container px-5">
          {/* Contact form*/}
          <div className="bg-light rounded-3 py-5 px-4 px-md-5 mb-5">
            <div className="row gx-5 justify-content-center">
              <div className="col-lg-8 col-xl-6">
                <form id="contactForm" onSubmit={handleSubmit}>
                  {/* 신고 유형 선택 */}
                  <div className="form-floating mb-3">
                    <select
                      className="form-select"
                      id="reportType"
                      value={formData.reportType}
                      onChange={handleInputChange}
                      aria-label="신고 유형 선택"
                    >
                      <option value={ReportType.BUG}>버그 신고</option>
                      <option value={ReportType.IMPROVEMENT}>개선 사항</option>
                    </select>
                    <label htmlFor="reportType">유형</label>
                  </div>

                  {/* 내용 */}
                  <div className="form-floating mb-3">
                    <textarea
                      className="form-control"
                      id="content"
                      value={formData.content}
                      onChange={handleInputChange}
                      placeholder="내용을 입력하세요"
                      style={{ height: "10rem" }}
                      required
                    ></textarea>
                    <label htmlFor="content">내용</label>
                  </div>

                  {submitSuccess && (
                    <div className="alert alert-success mb-3" role="alert">
                      건의가 완료되었습니다.
                    </div>
                  )}

                  {submitError && (
                    <div className="alert alert-danger mb-3" role="alert">
                      제출 중 오류가 발생했습니다. 다시 시도해주세요.
                    </div>
                  )}

                  {/* Submit Button*/}
                  <div className="d-grid">
                    <button
                      className="btn btn-primary btn-lg"
                      type="submit"
                      disabled={isSubmitting}
                    >
                      {isSubmitting ? "제출 중..." : "제출"}
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
