"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import useAuthStore from "@/util/authStore";
import { ReportDTO, ReportType, UserRole } from "@/components/types/schema";

export default function AdminPage() {
  const { user } = useAuthStore();
  const router = useRouter();
  const [reports, setReports] = useState<ReportDTO[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [reportType, setReportType] = useState<ReportType | "ALL">("ALL");
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [banUserId, setBanUserId] = useState<string>("");
  const [isBanning, setIsBanning] = useState(false);

  // Admin 권한 체크
  useEffect(() => {
    if (user && user.role !== UserRole.ADMIN) {
      // 관리자가 아니면 404 페이지로 리다이렉트
      router.push("/not-found");
    }
  }, [user, router]);

  // 신고 목록 불러오기
  const fetchReports = async () => {
    if (!user || user.role !== UserRole.ADMIN) return;

    setIsLoading(true);
    try {
      // reportType이 ALL인 경우 파라미터에서 제외
      const url =
        reportType === "ALL"
          ? `http://localhost:8080/admin/report?page=${page}&size=${size}`
          : `http://localhost:8080/admin/report?page=${page}&size=${size}&reportType=${reportType}`;

      const response = await fetch(url, {
        method: "GET",
        credentials: "include",
      });

      if (response.ok) {
        const data = await response.json();
        setReports(data.content);
        setTotalPages(data.totalPages);
      } else {
        console.error("신고 목록 불러오기 실패:", response.statusText);
      }
    } catch (error) {
      console.error("신고 목록 불러오기 중 오류 발생:", error);
    } finally {
      setIsLoading(false);
    }
  };

  // 유저 차단 처리
  const handleBanUser = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!banUserId.trim()) {
      alert("차단할 유저 ID를 입력해주세요.");
      return;
    }

    if (!confirm(`정말 ID가 ${banUserId}인 유저를 차단하시겠습니까?`)) {
      return;
    }

    setIsBanning(true);
    try {
      const response = await fetch(
        `http://localhost:8080/admin/user/ban?userId=${banUserId}`,
        {
          method: "POST",
          credentials: "include",
        }
      );

      if (response.ok) {
        alert("유저가 성공적으로 차단되었습니다.");
        setBanUserId("");
      } else {
        alert(`유저 차단에 실패했습니다: ${response.statusText}`);
      }
    } catch (error) {
      console.error("유저 차단 중 오류 발생:", error);
      alert("유저 차단 중 오류가 발생했습니다.");
    } finally {
      setIsBanning(false);
    }
  };

  // 페이지 변경 시 신고 목록 다시 불러오기
  useEffect(() => {
    fetchReports();
  }, [page, size, reportType, user]);

  // 신고 유형 변경 핸들러
  const handleReportTypeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setReportType(e.target.value as ReportType | "ALL");
    setPage(0); // 유형 변경 시 첫 페이지로 이동
  };

  // 페이지네이션 컴포넌트
  const Pagination = () => {
    const pages = [];
    const maxVisiblePages = 5;
    const startPage = Math.max(
      0,
      Math.min(
        page - Math.floor(maxVisiblePages / 2),
        totalPages - maxVisiblePages
      )
    );
    const endPage = Math.min(startPage + maxVisiblePages, totalPages);

    for (let i = startPage; i < endPage; i++) {
      pages.push(
        <li key={i} className={`page-item ${page === i ? "active" : ""}`}>
          <button
            className="page-link"
            onClick={() => setPage(i)}
            disabled={isLoading}
          >
            {i + 1}
          </button>
        </li>
      );
    }

    return (
      <nav aria-label="Page navigation">
        <ul className="pagination justify-content-center">
          <li className={`page-item ${page === 0 ? "disabled" : ""}`}>
            <button
              className="page-link"
              onClick={() => setPage(0)}
              disabled={page === 0 || isLoading}
            >
              처음
            </button>
          </li>
          <li className={`page-item ${page === 0 ? "disabled" : ""}`}>
            <button
              className="page-link"
              onClick={() => setPage(page - 1)}
              disabled={page === 0 || isLoading}
            >
              이전
            </button>
          </li>
          {pages}
          <li
            className={`page-item ${page === totalPages - 1 ? "disabled" : ""}`}
          >
            <button
              className="page-link"
              onClick={() => setPage(page + 1)}
              disabled={page === totalPages - 1 || isLoading}
            >
              다음
            </button>
          </li>
          <li
            className={`page-item ${page === totalPages - 1 ? "disabled" : ""}`}
          >
            <button
              className="page-link"
              onClick={() => setPage(totalPages - 1)}
              disabled={page === totalPages - 1 || isLoading}
            >
              마지막
            </button>
          </li>
        </ul>
      </nav>
    );
  };

  // 사용자 정보를 기다리는 동안 로딩 상태 표시
  if (!user) {
    return (
      <main className="flex-shrink-0">
        <div className="container px-5 py-5 text-center">
          <div className="spinner-border" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      </main>
    );
  }

  // 관리자가 아니면 아무것도 렌더링하지 않음
  if (user.role !== UserRole.ADMIN) {
    return null; // useEffect에서 리다이렉트 처리
  }

  return (
    <main className="flex-shrink-0">
      <div className="container px-5 py-5">
        <h1 className="fw-bold mb-4">관리자 페이지</h1>

        <div className="row mb-4">
          {/* 유저 차단 카드 */}
          <div className="col-md-6 mb-4">
            <div className="card h-100">
              <div className="card-header">
                <h5 className="mb-0">유저 차단</h5>
              </div>
              <div className="card-body">
                <form onSubmit={handleBanUser}>
                  <div className="mb-3">
                    <label htmlFor="banUserId" className="form-label">
                      차단할 유저 ID
                    </label>
                    <input
                      type="number"
                      className="form-control"
                      id="banUserId"
                      value={banUserId}
                      onChange={(e) => setBanUserId(e.target.value)}
                      placeholder="차단할 유저의 ID를 입력하세요"
                      required
                      disabled={isBanning}
                    />
                  </div>
                  <button
                    type="submit"
                    className="btn btn-danger"
                    disabled={isBanning || !banUserId.trim()}
                  >
                    {isBanning ? "처리 중..." : "유저 차단"}
                  </button>
                </form>
              </div>
            </div>
          </div>

          {/* 신고 통계 카드 */}
          <div className="col-md-6 mb-4">
            <div className="card h-100">
              <div className="card-header">
                <h5 className="mb-0">신고 통계</h5>
              </div>
              <div className="card-body">
                <p>총 신고 수: {reports.length}개</p>
                <p>
                  현재 페이지: {page + 1} / {totalPages}
                </p>
                <p>현재 필터: {reportType === "ALL" ? "전체" : reportType}</p>
              </div>
            </div>
          </div>
        </div>

        {/* 신고 목록 카드 */}
        <div className="card mb-4">
          <div className="card-header d-flex justify-content-between align-items-center">
            <h5 className="mb-0">신고 목록</h5>
            <div className="d-flex align-items-center">
              <label htmlFor="reportType" className="me-2">
                신고 유형:
              </label>
              <select
                id="reportType"
                className="form-select"
                value={reportType}
                onChange={handleReportTypeChange}
                disabled={isLoading}
              >
                <option value="ALL">전체</option>
                <option value={ReportType.POST}>게시글</option>
                <option value={ReportType.COMMENT}>댓글</option>
                <option value={ReportType.BUG}>버그</option>
                <option value={ReportType.IMPROVEMENT}>개선사항</option>
              </select>
            </div>
          </div>
          <div className="card-body">
            {isLoading ? (
              <div className="text-center py-5">
                <div className="spinner-border" role="status">
                  <span className="visually-hidden">Loading...</span>
                </div>
              </div>
            ) : reports.length > 0 ? (
              <div className="table-responsive">
                <table className="table table-hover">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>유형</th>
                      <th>신고자 ID</th>
                      <th>대상 ID</th>
                      <th>내용</th>
                      <th>상세</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reports.map((report) => (
                      <tr key={report.reportId}>
                        <td>{report.reportId}</td>
                        <td>{report.reportType}</td>
                        <td>{report.userId}</td>
                        <td>{report.targetId}</td>
                        <td>
                          {report.content.length > 10
                            ? `${report.content.substring(0, 10)}...`
                            : report.content}
                        </td>
                        <td>
                          <button
                            className="btn btn-sm btn-primary"
                            onClick={() =>
                              router.push(`/admin/report/${report.reportId}`)
                            }
                          >
                            상세보기
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="text-center py-5">
                <p className="mb-0">신고 내역이 없습니다.</p>
              </div>
            )}
          </div>
          <div className="card-footer">
            <Pagination />
          </div>
        </div>
      </div>
    </main>
  );
}
