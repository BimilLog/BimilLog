"use client";

import Image from "next/image";
import { useParams } from "next/navigation";
import { useState } from "react";

export default function FarmPage() {
  const params = useParams();
  const farmName = params.farmname;

  return (
    <main className="flex-shrink-0">
      <div className="container px-5 py-5">
        <div className="row">
          <div className="col-lg-12">
              <div className="py-3 bg-light">
                  <div className="text-center">
              <h2 className="fw-bolder">{farmName} 농장</h2>
                  </div>
              </div>
              <div
                className="card-body"
                style={{
                  backgroundColor: "black",
                  padding: "3px",
                  position: "relative",
                }}
              >
                <img
                  src="/farmImage.jpeg"
                  alt="농장 이미지"
                  className="img-fluid rounded"
                  style={{ width: "100%", height: "auto", objectFit: "cover" }}
                />
                <div
                  style={{
                    position: "absolute",
                    top: "7px",
                    left: "7px",
                    right: "7px",
                    bottom: "7px",
                    display: "grid",
                    gridTemplateColumns: "repeat(10, 1fr)",
                    gridTemplateRows: "repeat(5, 1fr)",
                    pointerEvents: "none", // 이미지 클릭 이벤트 통과
                  }}
                >
                  {Array.from({ length: 50 }).map((_, index) => (
                    <div
                      key={index}
                      className="grid-cell"
                      style={{
                        border: "0.5px solid rgba(255, 255, 255, 0)",
                        borderRadius: "4px",
                        backgroundColor: "rgba(0, 0, 0, 0.1)",
                        pointerEvents: "auto", // 개별 셀은 클릭 가능
                        transition: "all 0.2s ease-in-out",
                      }}
                    />
                  ))}
                </div>
              </div>
          </div>
        </div>
      </div>

      <style jsx>{`
        .grid-cell:hover {
          background-color: rgba(255, 255, 255, 0.3) !important;
          transform: scale(1.05);
          border: 0.5px solid rgba(255, 255, 255, 0.5) !important;
          box-shadow: 0 0 8px rgba(255, 255, 255, 0.5);
          z-index: 10;
          cursor: pointer;
        }
      `}</style>
    </main>
  );
}
