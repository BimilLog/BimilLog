"use client";

import React from "react";

interface LegalDocumentHeaderProps {
  title: string;
  gradientClassName?: string;
}

export function LegalDocumentHeader({
  title,
  gradientClassName = "bg-gradient-to-r from-green-600 to-blue-600",
}: LegalDocumentHeaderProps) {
  return (
    <div className={`${gradientClassName} px-8 py-6 text-white`}>
      <h1 className="text-3xl font-bold text-center drop-shadow-sm">{title}</h1>
    </div>
  );
}
