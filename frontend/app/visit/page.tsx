"use client"

import type React from "react"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Search, ArrowLeft, Heart, Users } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"

export default function VisitPage() {
  const [searchNickname, setSearchNickname] = useState("")
  const router = useRouter()

  const handleSearch = () => {
    if (searchNickname.trim()) {
      router.push(`/visit/${encodeURIComponent(searchNickname.trim())}`)
    }
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      handleSearch()
    }
  }

  // 인기 롤링페이퍼 예시 데이터
  const popularRollingPapers = [
    { nickname: "행복한토끼", messageCount: 47, emoji: "🐰" },
    { nickname: "별빛소녀", messageCount: 32, emoji: "⭐" },
    { nickname: "코딩마스터", messageCount: 28, emoji: "💻" },
    { nickname: "꽃길만걷자", messageCount: 25, emoji: "🌸" },
    { nickname: "햇살같은사람", messageCount: 23, emoji: "☀️" },
    { nickname: "달콤한하루", messageCount: 19, emoji: "🍯" },
  ]

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Link href="/">
              <Button variant="ghost" size="sm">
                <ArrowLeft className="w-4 h-4 mr-2" />
                홈으로
              </Button>
            </Link>
            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 bg-gradient-to-r from-pink-500 to-purple-600 rounded-lg flex items-center justify-center">
                <Heart className="w-5 h-5 text-white" />
              </div>
              <h1 className="text-xl font-bold text-gray-800">롤링페이퍼 방문</h1>
            </div>
          </div>
        </div>
      </header>

      <div className="container mx-auto px-4 py-8 max-w-md">
        {/* Search Section */}
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-xl mb-8">
          <CardHeader className="text-center pb-4">
            <CardTitle className="text-2xl bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent">
              누구의 롤링페이퍼를 방문할까요?
            </CardTitle>
            <p className="text-gray-600 text-sm">닉네임을 입력하여 롤링페이퍼를 찾아보세요</p>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
              <Input
                placeholder="닉네임을 입력하세요"
                value={searchNickname}
                onChange={(e) => setSearchNickname(e.target.value)}
                onKeyPress={handleKeyPress}
                className="pl-10 h-12 text-lg bg-white border-2 border-gray-200 focus:border-purple-400"
              />
            </div>
            <Button
              onClick={handleSearch}
              className="w-full h-12 bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700 text-lg font-semibold"
              disabled={!searchNickname.trim()}
            >
              롤링페이퍼 방문하기
            </Button>
          </CardContent>
        </Card>

        {/* Popular Rolling Papers */}
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
          <CardHeader>
            <CardTitle className="flex items-center space-x-2 text-lg">
              <Users className="w-5 h-5 text-orange-500" />
              <span>인기 롤링페이퍼</span>
            </CardTitle>
            <p className="text-gray-600 text-sm">많은 메시지를 받은 롤링페이퍼들이에요</p>
          </CardHeader>
          <CardContent className="space-y-3">
            {popularRollingPapers.map((paper, index) => (
              <div
                key={paper.nickname}
                className="flex items-center justify-between p-3 bg-gradient-to-r from-gray-50 to-white rounded-lg border border-gray-100 hover:shadow-md transition-all cursor-pointer"
                onClick={() => router.push(`/visit/${encodeURIComponent(paper.nickname)}`)}
              >
                <div className="flex items-center space-x-3">
                  <div className="flex items-center justify-center w-8 h-8 bg-gradient-to-r from-pink-100 to-purple-100 rounded-full">
                    <span className="text-lg">{paper.emoji}</span>
                  </div>
                  <div>
                    <h3 className="font-medium text-gray-800">{paper.nickname}</h3>
                    <p className="text-xs text-gray-500">{paper.messageCount}개의 메시지</p>
                  </div>
                </div>
                <div className="flex items-center space-x-1">
                  <div
                    className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white ${
                      index === 0
                        ? "bg-yellow-500"
                        : index === 1
                          ? "bg-gray-400"
                          : index === 2
                            ? "bg-orange-400"
                            : "bg-purple-400"
                    }`}
                  >
                    {index + 1}
                  </div>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Info Section */}
        <div className="mt-8 text-center">
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-start space-x-2">
              <Heart className="w-5 h-5 text-blue-600 mt-0.5 flex-shrink-0" />
              <div className="text-sm text-blue-800">
                <p className="font-medium mb-1">💌 익명으로 메시지를 남겨보세요!</p>
                <p>
                  로그인 없이도 누구나 따뜻한 메시지를 남길 수 있어요. 다양한 귀여운 디자인으로 메시지를 꾸며보세요!
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
