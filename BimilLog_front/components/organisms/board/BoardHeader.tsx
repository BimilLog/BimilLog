import React from "react";
import { Button } from "@/components";
import { Edit } from "lucide-react";
import Link from "next/link";

export const BoardHeader = React.memo(() => {
  return (
    <header className="py-8">
      <div className="container mx-auto px-4 text-center">
        <Link href="/board/write">
          <Button
            size="lg"
            className="inline-flex items-center"
          >
            <Edit className="w-5 h-5 mr-2 stroke-slate-600 fill-slate-100" />
            <span>글쓰기</span>
          </Button>
        </Link>
      </div>
    </header>
  );
});

BoardHeader.displayName = "BoardHeader";
