"use client";

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import useAuthStore from "@/util/authStore";
import { ReportDTO, UserRole } from "@/components/types/schema";
import Link from "next/link";
import { notFound } from "next/navigation";
import fetchClient from "@/util/fetchClient";

const API_BASE = "https://grow-farm.com/api";

export default function ReportDetailClient() {
  const params = useParams();
  const reportId = params.reportId as string;
  const { user } = useAuthStore();
  const router = useRouter();
  const [report, setReport] = useState<ReportDTO | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isBanning, setIsBanning] = useState(false);
  const [banReason, setBanReason] = useState("");

  // Admin 권한 체크
  useEffect(() => {
    if (user && user.role !== UserRole.ADMIN) {
      notFound();
    }
  }, [user, router]);

  // 신고 상세 정보 불러오기
  useEffect(() => {
    const fetchReportDetail = async () => {
      if (!user || user.role !== UserRole.ADMIN || !reportId) return;

      setIsLoading(true);
      setError(null);

      try {
        const response = await fetchClient(
          `${API_BASE}/admin/report/${reportId}`
        );

        if (response.ok) {
          const data = await response.json();
          setReport(data);
        } else {
          setError(
            `신고 정보를 불러오는데 실패했습니다: ${response.statusText}`
          );
          router.push("/admin");
        }
      } catch (error) {
        console.error("신고 상세 정보 불러오기 중 오류 발생:", error);
        setError("신고 상세 정보를 불러오는 중 오류가 발생했습니다.");
        router.push("/admin");
      } finally {
        setIsLoading(false);
      }
    };

    fetchReportDetail();
  }, [reportId, user, router]);

  // 신고된 유저 차단 처리
  const handleBanUser = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!report || !banReason.trim()) {
      alert("차단 사유를 입력해주세요.");
      return;
    }

    if (!confirm(`정말 이 사용자를 차단하시겠습니까?`)) return;

    setIsBanning(true);
    try {
      const response = await fetchClient(
        `${API_BASE}/admin/user/ban?userId=${report.userId}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ reason: banReason.trim() }),
        }
      );

      if (response.ok) {
        alert("사용자가 차단되었습니다.");
        router.push("/admin");
      } else {
        const errorText = await response.text();
        alert(`사용자 차단 실패: ${errorText}`);
      }
    } catch (error) {
      console.error("사용자 차단 중 오류:", error);
      alert("사용자 차단 중 오류가 발생했습니다.");
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
                    disabled={isBanning || !banReason.trim()}
                  >
                    {isBanning ? "처리 중..." : "사용자 차단하기"}
                  </button>
                </div>
              </div>

              <div className="mb-4">
                <h6 className="fw-bold">차단 사유</h6>
                <textarea
                  className="form-control mb-2"
                  rows={3}
                  value={banReason}
                  onChange={(e) => setBanReason(e.target.value)}
                  placeholder="차단 사유를 입력하세요"
                  disabled={isBanning}
                ></textarea>
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
