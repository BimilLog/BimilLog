export type ReportType = "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT";

export interface ValidatedReport {
  id: number;
  reporterId?: number;
  userId?: number;
  reporterName: string;
  reportType: ReportType;
  targetId?: number | null;
  targetTitle?: string | null;
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

export function validateReport(data: any): ValidatedReport | null {
  try {
    if (!data || typeof data !== "object") {
      throw new Error("Invalid data type");
    }

    if (typeof data.id !== "number") {
      throw new Error("Invalid id");
    }

    if (typeof data.reporterName !== "string") {
      throw new Error("Invalid reporterName");
    }

    if (!isValidReportType(data.reportType)) {
      throw new Error("Invalid reportType");
    }

    if (typeof data.content !== "string") {
      throw new Error("Invalid content");
    }

    if (typeof data.createdAt !== "string") {
      throw new Error("Invalid createdAt");
    }

    return data as ValidatedReport;
  } catch (error) {
    console.error("Report validation failed:", error);
    return null;
  }
}

export function validatePageResponse(data: any): ValidatedPageResponse | null {
  try {
    if (!data || typeof data !== "object") {
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

    return data as ValidatedPageResponse;
  } catch (error) {
    console.error("PageResponse validation failed:", error);
    return null;
  }
}

export function isValidReportType(type: string): type is ReportType {
  return VALID_REPORT_TYPES.includes(type as ReportType);
}

export function sanitizeSearchInput(input: string): string {
  return input.trim().replace(/[<>]/g, "");
}