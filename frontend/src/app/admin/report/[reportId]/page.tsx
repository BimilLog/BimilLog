"use client";

import { useState, useEffect, use } from "react";
import { useRouter } from "next/navigation";
import useAuthStore from "@/util/authStore";
import { ReportDTO, UserRole } from "@/components/types/schema";
import Link from "next/link";

// 내부 클라이언트 컴포넌트 - reportId를 props로 직접 받음
function ReportDetailContent({ reportId }: { reportId: string }) {
  const { user } = useAuthStore();
  const router = useRouter();
  const [report, setReport] = useState<ReportDTO | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isBanning, setIsBanning] = useState(false);

  // Admin 권한 체크
  useEffect(() => {
    if (user && user.role !== UserRole.ADMIN) {
      alert("관리자만 접근할 수 있는 페이지입니다.");
      router.push("/");
    }
  }, [user, router]);

  // 신고 상세 정보 불러오기
  useEffect(() => {
    const fetchReportDetail = async () => {
      if (!user || user.role !== UserRole.ADMIN || !reportId) return;

      setIsLoading(true);
      setError(null);

      try {
        const response = await fetch(
          `https://grow-farm.com/api/admin/report/${reportId}`,
          {
            method: "GET",
            credentials: "include",
          }
        );

        if (response.ok) {
          const data = await response.json();
          setReport(data);
        } else {
          setError(
            `신고 정보를 불러오는데 실패했습니다: ${response.statusText}`
          );
        }
      } catch (error) {
        console.error("신고 상세 정보 불러오기 중 오류 발생:", error);
        setError("신고 상세 정보를 불러오는 중 오류가 발생했습니다.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchReportDetail();
  }, [reportId, user]);

  // 신고된 유저 차단 처리
  const handleBanUser = async () => {
    if (!report) return;

    if (!confirm(`정말 ID가 ${report.userId}인 유저를 차단하시겠습니까?`)) {
      return;
    }

    setIsBanning(true);
    try {
      const response = await fetch(
        `https://grow-farm.com/api/admin/user/ban?userId=${report.userId}`,
        {
          method: "POST",
          credentials: "include",
        }
      );

      if (response.ok) {
        alert("유저가 성공적으로 차단되었습니다.");
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

  if (user.role !== UserRole.ADMIN) {
    return null; // useEffect에서 리다이렉트 처리
  }

  return (
    <main className="flex-shrink-0">
      <div className="container px-5 py-5">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <h1 className="fw-bold">신고 상세 정보</h1>
          <Link href="/admin" className="btn btn-outline-secondary">
            관리자 페이지로 돌아가기
          </Link>
        </div>

        {isLoading ? (
          <div className="text-center py-5">
            <div className="spinner-border" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
          </div>
        ) : error ? (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        ) : report ? (
          <div className="card">
            <div className="card-header">
              <h5 className="mb-0">신고 #{report.reportId}</h5>
            </div>
            <div className="card-body">
              <div className="row mb-4">
                <div className="col-md-6">
                  <h6 className="fw-bold">신고 유형</h6>
                  <p>{report.reportType}</p>
                </div>
                <div className="col-md-6">
                  <h6 className="fw-bold">신고자 ID</h6>
                  <p>{report.userId}</p>
                </div>
              </div>

              <div className="row mb-4">
                <div className="col-md-6">
                  <h6 className="fw-bold">대상 ID</h6>
                  <p>{report.targetId}</p>
                </div>
                <div className="col-md-6">
                  <h6 className="fw-bold">조치</h6>
                  <button
                    className="btn btn-danger"
                    onClick={handleBanUser}
                    disabled={isBanning}
                  >
                    {isBanning ? "처리 중..." : "신고자 차단하기"}
                  </button>
                </div>
              </div>

              <div className="mb-4">
                <h6 className="fw-bold">신고 내용</h6>
                <div className="p-3 bg-light rounded">
                  <p className="mb-0 white-space-pre-wrap">{report.content}</p>
                </div>
              </div>

              <div className="mb-4">
                <h6 className="fw-bold">조치 안내</h6>
                <ul>
                  <li>
                    <strong>게시글/댓글 신고</strong>: 해당 콘텐츠를 확인하고
                    필요시 삭제 및 작성자 제재
                  </li>
                  <li>
                    <strong>버그 신고</strong>: 개발팀에 전달하여 수정 조치
                  </li>
                  <li>
                    <strong>개선사항</strong>: 검토 후 반영 여부 결정
                  </li>
                </ul>
              </div>
            </div>
            <div className="card-footer">
              <div className="d-flex justify-content-between">
                <Link href="/admin" className="btn btn-secondary">
                  목록으로 돌아가기
                </Link>
                {report.reportType === "POST" ||
                report.reportType === "COMMENT" ? (
                  <button
                    className="btn btn-primary"
                    onClick={() => {
                      // 신고 유형에 따라 해당 콘텐츠로 이동
                      if (report.reportType === "POST") {
                        router.push(`/board/${report.targetId}`);
                      } else if (report.reportType === "COMMENT") {
                        // 댓글의 경우 해당 게시글로 이동 (댓글 ID로 게시글 찾는 API가 필요)
                        alert("댓글 기능은 아직 구현되지 않았습니다.");
                      }
                    }}
                  >
                    신고된 콘텐츠 보기
                  </button>
                ) : null}
              </div>
            </div>
          </div>
        ) : (
          <div className="alert alert-warning" role="alert">
            신고 정보를 찾을 수 없습니다.
          </div>
        )}
      </div>
    </main>
  );
}

// 외부 페이지 컴포넌트 - URL에서 reportId 직접 추출
// Next.js 15.3.1에서는 PageProps 타입이 변경되어 필요
type Props = {
  params: Promise<{
    reportId: string;
  }>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

export default function ReportDetailPage({ params }: Props) {
  // params에서 reportId 사용 (Promise 해결)
  const resolvedParams = use(params);
  return <ReportDetailContent reportId={resolvedParams.reportId} />;
}
