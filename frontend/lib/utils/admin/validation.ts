import { logger } from '@/lib/utils/logger';

export type ReportType = "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT";

export interface ValidatedReport {
  id: number;
  reporterId?: number;
  userId?: number;
  reporterName: string;
  reportType: ReportType;
  targetId?: number | null;
  content: string;
  createdAt: string;
  updatedAt?: string;
  status?: string;
}

export interface ValidatedPageResponse {
  content: ValidatedReport[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

const VALID_REPORT_TYPES: readonly ReportType[] = ["POST", "COMMENT", "ERROR", "IMPROVEMENT"];

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function validateReport(data: unknown): ValidatedReport | null {
  try {
    if (!isRecord(data)) {
      throw new Error("Invalid data type");
    }

    if (typeof data.id !== "number") {
      throw new Error("Invalid id");
    }

    if (typeof data.reporterName !== "string") {
      throw new Error("Invalid reporterName");
    }

    if (typeof data.reportType !== "string" || !isValidReportType(data.reportType)) {
      throw new Error("Invalid reportType");
    }

    if (typeof data.content !== "string") {
      throw new Error("Invalid content");
    }

    if (typeof data.createdAt !== "string") {
      throw new Error("Invalid createdAt");
    }

    return data as unknown as ValidatedReport;
  } catch (error) {
    logger.error("Report validation failed:", error);
    return null;
  }
}

export function validatePageResponse(data: unknown): ValidatedPageResponse | null {
  try {
    if (!isRecord(data)) {
      throw new Error("Invalid data type");
    }

    if (!Array.isArray(data.content)) {
      throw new Error("Invalid content array");
    }

    if (typeof data.totalElements !== "number") {
      throw new Error("Invalid totalElements");
    }

    if (typeof data.totalPages !== "number") {
      throw new Error("Invalid totalPages");
    }

    return data as unknown as ValidatedPageResponse;
  } catch (error) {
    logger.error("PageResponse validation failed:", error);
    return null;
  }
}

export function isValidReportType(type: string): type is ReportType {
  return VALID_REPORT_TYPES.includes(type as ReportType);
}

export function sanitizeSearchInput(input: string): string {
  return input.trim().replace(/[<>]/g, "");
}