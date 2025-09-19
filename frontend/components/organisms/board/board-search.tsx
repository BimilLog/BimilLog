"use client";

import { Input, Button, Card } from "@/components";
import { Dropdown, DropdownItem } from "flowbite-react";
import { Search, ListFilter, ChevronDown } from "lucide-react";

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
  // 검색 실행 핸들러
  const executeSearch = () => {
    if (searchTerm.trim()) {
      handleSearch(); // 실제 검색 실행
    }
  };

  return (
    <Card variant="default" className="mb-6 p-4 bg-white backdrop-blur-none">
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="flex-1">
          <div className="flex items-center border border-gray-300 rounded-lg bg-white overflow-hidden hover:border-brand-secondary/50 focus-within:border-brand-secondary focus-within:ring-2 focus-within:ring-brand-secondary/20 transition-all">
            <Dropdown
              label=""
              dismissOnClick={true}
              renderTrigger={() => (
                <button className="w-[120px] px-3 py-2 border-0 rounded-none focus:ring-0 focus-visible:ring-0 focus-visible:ring-offset-0 bg-gray-50 hover:bg-gray-100 border-r border-gray-200 flex items-center justify-between text-sm">
                  <span>
                    {searchType === "TITLE" ? "제목" :
                     searchType === "TITLE_CONTENT" ? "제목+내용" :
                     searchType === "AUTHOR" ? "작성자" : "제목"}
                  </span>
                  <ChevronDown className="w-4 h-4 stroke-slate-600 fill-slate-100" />
                </button>
              )}
            >
              <DropdownItem onClick={() => setSearchType("TITLE")}>
                제목
              </DropdownItem>
              <DropdownItem onClick={() => setSearchType("TITLE_CONTENT")}>
                제목+내용
              </DropdownItem>
              <DropdownItem onClick={() => setSearchType("AUTHOR")}>
                작성자
              </DropdownItem>
            </Dropdown>
            <Input
              type="text"
              placeholder="검색어를 입력하세요..."
              className="flex-1 border-0 rounded-none focus-visible:ring-0 focus-visible:ring-offset-0 bg-transparent"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              onKeyPress={(e) => e.key === "Enter" && executeSearch()}
            />
            <Button
              variant="ghost"
              size="icon"
              onClick={executeSearch}
              className="border-0 rounded-none hover:bg-brand-secondary/10 border-l border-gray-200"
            >
              <Search className="w-5 h-5 stroke-blue-600 fill-blue-100" />
            </Button>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <ListFilter className="w-5 h-5 stroke-blue-600 fill-blue-100" />
          <Dropdown
            label=""
            dismissOnClick={true}
            renderTrigger={() => (
              <button className="w-[120px] px-3 py-2 bg-white border border-gray-300 rounded-lg hover:border-brand-secondary/50 focus:ring-2 focus:ring-brand-secondary/20 flex items-center justify-between text-sm">
                <span>{postsPerPage}개씩</span>
                <ChevronDown className="w-4 h-4 stroke-slate-600 fill-slate-100" />
              </button>
            )}
          >
            <DropdownItem onClick={() => setPostsPerPage("15")}>
              15개씩
            </DropdownItem>
            <DropdownItem onClick={() => setPostsPerPage("30")}>
              30개씩
            </DropdownItem>
            <DropdownItem onClick={() => setPostsPerPage("50")}>
              50개씩
            </DropdownItem>
          </Dropdown>
        </div>
      </div>
    </Card>
  );
};
