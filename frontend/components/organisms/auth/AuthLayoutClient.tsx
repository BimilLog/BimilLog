"use client";

import { ToastContainer } from "@/components";
import { useToast } from "@/hooks";

export function AuthLayoutClient() {
  const { toasts, removeToast } = useToast();

  return <ToastContainer toasts={toasts} onRemove={removeToast} />;
}