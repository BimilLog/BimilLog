import React from "react";
import { Button } from "@/components";
import { Edit } from "lucide-react";
import Link from "next/link";

export const BoardHeader = React.memo(() => {
  return (
    <div className="absolute right-0 top-1 z-10">
      <Link href="/board/write">
        <Button
          size="sm"
          className="inline-flex items-center"
        >
          <Edit className="w-4 h-4 mr-1 stroke-slate-600 fill-slate-100" />
          <span className="text-sm">글쓰기</span>
        </Button>
      </Link>
    </div>
  );
});

BoardHeader.displayName = "BoardHeader";
