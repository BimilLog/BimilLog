"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Badge } from "@/components/ui/badge"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { MessageSquare, Plus, Heart, Share2, ArrowLeft, Send } from "lucide-react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { rollingPaperApi, getDecoInfo, type VisitMessage } from "@/lib/api"

export default function VisitRollingPaperPage() {
  const params = useParams()
  const nickname = params.nickname as string
  const [messages, setMessages] = useState<{ [key: number]: VisitMessage }>({})
  const [isLoading, setIsLoading] = useState(true)
  const [selectedPosition, setSelectedPosition] = useState<{ x: number; y: number } | null>(null)

  // 롤링페이퍼 메시지 조회
  useEffect(() => {
    const fetchMessages = async () => {
      try {
        const response = await rollingPaperApi.getRollingPaper(nickname)
        if (response.success && response.data) {
          const messageMap: { [key: number]: VisitMessage } = {}
          response.data.forEach((message) => {
            const position = message.height * 6 + message.width // 6칸으로 변경
            messageMap[position] = message
          })
          setMessages(messageMap)
        }
      } catch (error) {
        console.error("Failed to fetch messages:", error)
      } finally {
        setIsLoading(false)
      }
    }

    if (nickname) {
      fetchMessages()
    }
  }, [nickname])

  const handleShare = async () => {
    const url = window.location.href
    if (navigator.share) {
      try {
        await navigator.share({
          title: `${nickname}님의 롤링페이퍼`,
          text: "익명으로 따뜻한 메시지를 남겨보세요!",
          url: url,
        })
      } catch (error) {
        console.log("Share cancelled")
      }
    } else {
      try {
        await navigator.clipboard.writeText(url)
        alert("링크가 클립보드에 복사되었습니다!")
      } catch (error) {
        console.error("Failed to copy to clipboard:", error)
      }
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-4">
            <MessageSquare className="w-7 h-7 text-white animate-pulse" />
          </div>
          <p className="text-gray-600 font-medium">롤링페이퍼를 불러오는 중...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/90 backdrop-blur-md border-b shadow-sm">
        <div className="container mx-auto px-4 py-3">
          <div className="flex items-center justify-between">
            <Link href="/visit">
              <Button variant="ghost" size="sm" className="text-gray-600 hover:text-gray-800">
                <ArrowLeft className="w-4 h-4 mr-2" />
                뒤로가기
              </Button>
            </Link>
            <Button variant="ghost" size="sm" onClick={handleShare} className="text-gray-600 hover:text-gray-800">
              <Share2 className="w-4 h-4 mr-2" />
              공유
            </Button>
          </div>
        </div>
      </header>

      <div className="container mx-auto px-4 py-6 max-w-md">
        {/* Profile Section */}
        <Card className="bg-white/90 backdrop-blur-sm border-0 shadow-xl mb-6 rounded-3xl border-4 border-pink-200">
          <CardContent className="p-6 text-center">
            <div className="w-16 h-16 bg-gradient-to-r from-pink-500 to-purple-600 rounded-full flex items-center justify-center mx-auto mb-4 shadow-lg">
              <span className="text-2xl text-white font-bold">{decodeURIComponent(nickname).charAt(0)}</span>
            </div>
            <h1 className="text-xl font-bold text-gray-800 mb-2">{decodeURIComponent(nickname)}님의 롤링페이퍼 💕</h1>
            <div className="flex items-center justify-center space-x-4 text-sm text-gray-600">
              <div className="flex items-center space-x-1">
                <MessageSquare className="w-4 h-4" />
                <span className="font-medium">{Object.keys(messages).length}개의 메시지</span>
              </div>
              <div className="flex items-center space-x-1">
                <Heart className="w-4 h-4 text-red-500" />
                <span className="font-medium">따뜻한 마음</span>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Rolling Paper Grid - 귀여운 종이 디자인 */}
        <div className="relative mb-6">
          <div
            className="relative bg-gradient-to-br from-amber-50 via-yellow-50 to-orange-50 rounded-3xl shadow-2xl border-4 border-pink-200 p-6"
            style={{
              backgroundImage: `
                radial-gradient(circle at 15px 15px, rgba(255,182,193,0.3) 1px, transparent 1px),
                radial-gradient(circle at 60px 60px, rgba(255,192,203,0.2) 1px, transparent 1px)
              `,
              backgroundSize: "30px 30px, 120px 120px",
            }}
          >
            {/* 바인더 구멍들 */}
            <div className="absolute left-3 top-0 bottom-0 flex flex-col justify-evenly">
              {Array.from({ length: 8 }, (_, i) => (
                <div key={i} className="w-4 h-4 bg-white rounded-full shadow-inner border-2 border-pink-300" />
              ))}
            </div>

            {/* 제목 */}
            <div className="text-center mb-4 ml-6">
              <h2 className="text-lg font-bold text-pink-800 transform -rotate-1">💌 롤링페이퍼 💌</h2>
              <div className="absolute top-2 right-4 text-xl animate-bounce">🌸</div>
            </div>

            {/* 메시지 그리드 - 6칸으로 변경 */}
            <div className="ml-6">
              <div className="grid grid-cols-6 gap-1 bg-white/30 p-3 rounded-2xl border-2 border-dashed border-pink-300">
                {Array.from({ length: 84 }, (_, i) => {
                  // 6x14 = 84칸
                  const hasMessage = messages[i]
                  const decoInfo = hasMessage ? getDecoInfo(hasMessage.decoType) : null
                  const x = i % 6 // 6칸으로 변경
                  const y = Math.floor(i / 6)

                  return (
                    <Dialog key={i}>
                      <DialogTrigger asChild>
                        <div
                          className={`
                            aspect-square rounded-lg border-2 flex items-center justify-center cursor-pointer transition-all duration-300
                            ${
                              hasMessage
                                ? `bg-gradient-to-br ${decoInfo?.color} border-white shadow-md hover:scale-110 hover:shadow-lg`
                                : "border-dashed border-pink-300 hover:border-pink-400 hover:bg-pink-50 hover:scale-105"
                            }
                          `}
                          onClick={() => setSelectedPosition({ x, y })}
                        >
                          {hasMessage ? (
                            <span className="text-lg animate-bounce">{decoInfo?.emoji}</span>
                          ) : (
                            <Plus className="w-3 h-3 text-gray-400 hover:text-pink-500 transition-colors" />
                          )}
                        </div>
                      </DialogTrigger>
                      <DialogContent className="max-w-sm mx-4 bg-gradient-to-br from-pink-50 to-purple-50 border-4 border-pink-200 rounded-3xl">
                        <DialogHeader>
                          <DialogTitle className="text-center text-pink-800 font-bold">
                            {hasMessage ? "💌 메시지" : "✨ 새 메시지 작성"}
                          </DialogTitle>
                        </DialogHeader>
                        {hasMessage ? (
                          <MessagePreview message={hasMessage} />
                        ) : (
                          <MessageForm
                            nickname={nickname}
                            position={{ x, y }}
                            onSubmit={(newMessage) => {
                              setMessages((prev) => ({
                                ...prev,
                                [i]: newMessage,
                              }))
                            }}
                          />
                        )}
                      </DialogContent>
                    </Dialog>
                  )
                })}
              </div>
            </div>

            {/* 귀여운 스티커들 */}
            <div className="absolute top-4 right-6 text-2xl animate-spin-slow">🌟</div>
            <div className="absolute bottom-4 right-8 text-xl animate-pulse">🌺</div>
          </div>
        </div>

        {/* Recent Messages Preview */}
        <Card className="bg-white/90 backdrop-blur-sm border-0 shadow-lg rounded-3xl border-4 border-pink-200">
          <CardHeader className="bg-gradient-to-r from-pink-100 to-purple-100 rounded-t-3xl">
            <CardTitle className="flex items-center space-x-2 text-lg text-pink-800">
              <MessageSquare className="w-5 h-5" />
              <span className="font-bold">최근 메시지들 💕</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {Object.values(messages)
                .slice(-3)
                .map((message) => {
                  const decoInfo = getDecoInfo(message.decoType)
                  return (
                    <div
                      key={message.id}
                      className="flex items-center space-x-3 p-3 rounded-lg bg-gradient-to-r from-gray-50 to-white border border-gray-100"
                    >
                      <div
                        className={`w-10 h-10 rounded-full bg-gradient-to-r ${decoInfo.color} flex items-center justify-center shadow-sm`}
                      >
                        <span className="text-lg">{decoInfo.emoji}</span>
                      </div>
                      <div className="flex-1">
                        <p className="text-gray-600 text-sm font-medium">누군가 메시지를 남겼어요</p>
                        <Badge variant="outline" className="text-xs mt-1 font-medium">
                          {decoInfo.name}
                        </Badge>
                      </div>
                    </div>
                  )
                })}
              {Object.keys(messages).length === 0 && (
                <div className="text-center py-8">
                  <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <MessageSquare className="w-8 h-8 text-gray-400" />
                  </div>
                  <p className="text-gray-500 text-sm font-semibold">아직 메시지가 없어요</p>
                  <p className="text-gray-400 text-xs mt-1 font-medium">첫 번째 메시지를 남겨보세요! 💌</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

function MessagePreview({ message }: { message: VisitMessage }) {
  const decoInfo = getDecoInfo(message.decoType)

  return (
    <div className="space-y-4 text-center">
      <div className={`p-6 rounded-xl bg-gradient-to-br ${decoInfo.color} border-2 border-white shadow-lg`}>
        <div className="flex items-center justify-center mb-4">
          <span className="text-4xl animate-bounce">{decoInfo.emoji}</span>
        </div>
        <Badge variant="secondary" className="mb-3 font-semibold">
          {decoInfo.name}
        </Badge>
        <p className="text-gray-600 text-sm italic font-medium">"누군가 따뜻한 메시지를 남겼어요 💕"</p>
      </div>
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
        <p className="text-yellow-800 text-xs font-medium">
          🔒 다른 사람의 롤링페이퍼에서는 메시지 내용을 볼 수 없어요
        </p>
      </div>
    </div>
  )
}

function MessageForm({
  nickname,
  position,
  onSubmit,
}: {
  nickname: string
  position: { x: number; y: number }
  onSubmit: (message: VisitMessage) => void
}) {
  const [content, setContent] = useState("")
  const [anonymity, setAnonymity] = useState("")
  const [decoType, setDecoType] = useState("POTATO")
  const [isSubmitting, setIsSubmitting] = useState(false)

  const decoOptions = [
    // 농작물 & 과일
    { value: "POTATO", emoji: "🥔", name: "감자", category: "농작물" },
    { value: "CARROT", emoji: "🥕", name: "당근", category: "농작물" },
    { value: "CABBAGE", emoji: "🥬", name: "양배추", category: "농작물" },
    { value: "TOMATO", emoji: "🍅", name: "토마토", category: "농작물" },
    { value: "STRAWBERRY", emoji: "🍓", name: "딸기", category: "과일" },
    { value: "WATERMELON", emoji: "🍉", name: "수박", category: "과일" },
    { value: "PUMPKIN", emoji: "🎃", name: "호박", category: "과일" },
    { value: "APPLE", emoji: "🍎", name: "사과", category: "과일" },
    { value: "GRAPE", emoji: "🍇", name: "포도", category: "과일" },
    { value: "BANANA", emoji: "🍌", name: "바나나", category: "과일" },
    // 몬스터 (재미요소)
    { value: "GOBLIN", emoji: "👹", name: "고블린", category: "몬스터" },
    { value: "SLIME", emoji: "🟢", name: "슬라임", category: "몬스터" },
    { value: "ORC", emoji: "👺", name: "오크", category: "몬스터" },
    { value: "DRAGON", emoji: "🐉", name: "드래곤", category: "몬스터" },
    { value: "PHOENIX", emoji: "🔥", name: "피닉스", category: "몬스터" },
    { value: "WEREWOLF", emoji: "🐺", name: "늑대인간", category: "몬스터" },
    { value: "ZOMBIE", emoji: "🧟", name: "좀비", category: "몬스터" },
    { value: "KRAKEN", emoji: "🐙", name: "크라켄", category: "몬스터" },
    { value: "CYCLOPS", emoji: "👁️", name: "사이클롭스", category: "몬스터" },
  ]

  const handleSubmit = async () => {
    if (!content.trim() || !anonymity.trim()) {
      alert("모든 필드를 입력해주세요.")
      return
    }

    setIsSubmitting(true)
    try {
      const response = await rollingPaperApi.createMessage(nickname, {
        decoType,
        anonymity: anonymity.trim(),
        content: content.trim(),
        width: position.x,
        height: position.y,
      })

      if (response.success) {
        onSubmit({
          id: Date.now(),
          userId: 0,
          decoType,
          width: position.x,
          height: position.y,
        })
        setContent("")
        setAnonymity("")
        alert("메시지가 성공적으로 전달되었습니다! 💌")
      } else {
        alert("메시지 전송에 실패했습니다. 다시 시도해주세요.")
      }
    } catch (error) {
      console.error("Failed to create message:", error)
      alert("메시지 전송에 실패했습니다. 다시 시도해주세요.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const selectedDeco = decoOptions.find((d) => d.value === decoType)

  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-bold mb-2 text-pink-800">💭 익명 닉네임</label>
        <Input
          placeholder="익명의 친구"
          value={anonymity}
          onChange={(e) => setAnonymity(e.target.value)}
          className="border-2 border-pink-300 rounded-2xl focus:border-pink-400 bg-pink-50 font-medium"
        />
      </div>

      <div>
        <label className="block text-sm font-bold mb-2 text-pink-800">💌 따뜻한 메시지</label>
        <Textarea
          placeholder="따뜻한 메시지를 남겨주세요..."
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={4}
          className="border-2 border-pink-300 rounded-2xl focus:border-pink-400 bg-pink-50 resize-none font-medium"
        />
      </div>

      <div>
        <label className="block text-sm font-bold mb-2 text-pink-800">🎨 메시지 디자인</label>
        <Select value={decoType} onValueChange={setDecoType}>
          <SelectTrigger className="border-2 border-pink-300 rounded-2xl focus:border-pink-400 bg-pink-50">
            <SelectValue>
              <div className="flex items-center space-x-2">
                <span className="text-lg">{selectedDeco?.emoji}</span>
                <span className="font-semibold">{selectedDeco?.name}</span>
                <Badge variant="outline" className="text-xs font-medium">
                  {selectedDeco?.category}
                </Badge>
              </div>
            </SelectValue>
          </SelectTrigger>
          <SelectContent className="max-h-60 rounded-2xl border-2 border-pink-300">
            {["농작물", "과일", "몬스터"].map((category) => (
              <div key={category}>
                <div className="px-2 py-1 text-xs font-semibold text-gray-500 bg-gray-50">
                  {category} {category === "몬스터" && "🎭"}
                </div>
                {decoOptions
                  .filter((option) => option.category === category)
                  .map((option) => (
                    <SelectItem key={option.value} value={option.value} className="rounded-xl">
                      <div className="flex items-center space-x-2">
                        <span className="text-lg">{option.emoji}</span>
                        <span className="font-semibold">{option.name}</span>
                      </div>
                    </SelectItem>
                  ))}
              </div>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="bg-pink-50 border border-pink-200 rounded-lg p-3">
        <p className="text-pink-800 text-xs text-center font-medium">🎉 재미있는 몬스터 디자인도 선택해보세요!</p>
      </div>

      <Button
        onClick={handleSubmit}
        className="w-full h-12 bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700 text-white font-semibold rounded-2xl shadow-lg transform hover:scale-105 transition-all"
        disabled={isSubmitting || !content.trim() || !anonymity.trim()}
      >
        {isSubmitting ? (
          <div className="flex items-center space-x-2">
            <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            <span>전송 중...</span>
          </div>
        ) : (
          <div className="flex items-center space-x-2">
            <Send className="w-4 h-4" />
            <span>메시지 전송하기 💕</span>
          </div>
        )}
      </Button>
    </div>
  )
}
