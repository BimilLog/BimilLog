"use client";

import { useState } from "react";
import { ReportType } from "@/components/types/schema";
import { useRouter } from "next/navigation";
import useAuthStore from "@/util/authStore";
import LoadingSpinner from "@/components/LoadingSpinner";
import fetchClient from "@/util/fetchClient";
import { validateNoXSS, escapeHTML } from "@/util/inputValidation";

const API_BASE = "http://localhost:8080";

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
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
      alert("신고/건의를 작성하려면 로그인이 필요합니다.");
      router.push("/");
      return;
    }

    if (!formData.content.trim()) {
      alert("내용을 입력해주세요.");
      return;
    }

    if (formData.content.length > 500) {
      alert("내용은 500자 이내로 작성해주세요.");
      return;
    }

    if (!validateNoXSS(formData.content)) {
      alert("특수문자(<, >, &, \", ', \\)는 사용이 불가능합니다.");
      return;
    }

    setIsSubmitting(true);
    setSubmitError(false);

    try {
      const response = await fetchClient(`${API_BASE}/user/suggestion`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          reportType: formData.reportType,
          userId: user.userId,
          content: escapeHTML(formData.content),
        }),
      });

      if (response.ok) {
        setSubmitSuccess(true);
        setFormData({
          reportType: ReportType.BUG,
          content: "",
        });
        setError("문의가 성공적으로 제출되었습니다.");
      } else {
        const errorText = await response.text();
        throw new Error(errorText || "문의 제출 중 오류가 발생했습니다.");
      }
    } catch (error) {
      console.error("문의 제출 오류:", error);
      setSubmitError(true);
      setError(
        error instanceof Error
          ? error.message
          : "문의 제출 중 오류가 발생했습니다."
      );
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
                      maxLength={500}
                    ></textarea>
                    <label htmlFor="content">내용</label>
                    <div className="form-text text-end">
                      {formData.content.length}/500자
                    </div>
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

                  {error && (
                    <div className="alert alert-danger mb-3" role="alert">
                      {error}
                    </div>
                  )}

                  {/* Submit Button*/}
                  <div className="d-grid">
                    <button
                      className="btn btn-primary btn-lg"
                      type="submit"
                      disabled={isLoading}
                    >
                      {isLoading ? <LoadingSpinner /> : "제출"}
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
