"use client";

import { Input, Button, Card } from "@/components";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/molecules/select";
import { Search, ListFilter } from "lucide-react";

interface BoardSearchProps {
  searchTerm: string;
  setSearchTerm: (term: string) => void;
  searchType: "TITLE" | "TITLE_CONTENT" | "AUTHOR";
  setSearchType: (type: "TITLE" | "TITLE_CONTENT" | "AUTHOR") => void;
  postsPerPage: string;
  setPostsPerPage: (value: string) => void;
  handleSearch: () => void;
}

export const BoardSearch = ({
  searchTerm,
  setSearchTerm,
  searchType,
  setSearchType,
  postsPerPage,
  setPostsPerPage,
  handleSearch,
}: BoardSearchProps) => {
  return (
    <Card variant="default" className="mb-6 p-4">
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="flex-1 flex items-center bg-white rounded-md border">
          <Select
            value={searchType}
            onValueChange={(value: "TITLE" | "TITLE_CONTENT" | "AUTHOR") =>
              setSearchType(value)
            }
          >
            <SelectTrigger className="w-[120px] border-0 rounded-r-none focus:ring-0">
              <SelectValue placeholder="검색 유형" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="TITLE">제목</SelectItem>
              <SelectItem value="TITLE_CONTENT">제목+내용</SelectItem>
              <SelectItem value="AUTHOR">작성자</SelectItem>
            </SelectContent>
          </Select>
          <Input
            type="text"
            placeholder="검색어를 입력하세요..."
            className="flex-1 border-0 focus-visible:ring-0 focus-visible:ring-offset-0"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyPress={(e) => e.key === "Enter" && handleSearch()}
          />
          <Button variant="ghost" size="icon" onClick={handleSearch}>
            <Search className="w-5 h-5 text-brand-secondary" />
          </Button>
        </div>
        <div className="flex items-center gap-2">
          <ListFilter className="w-5 h-5 text-brand-secondary" />
          <Select value={postsPerPage} onValueChange={setPostsPerPage}>
            <SelectTrigger className="w-[120px] bg-white border">
              <SelectValue placeholder="개수" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="15">15개씩</SelectItem>
              <SelectItem value="30">30개씩</SelectItem>
              <SelectItem value="50">50개씩</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
    </Card>
  );
};
