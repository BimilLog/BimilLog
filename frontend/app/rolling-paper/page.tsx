"use client"

import { useState, useEffect } from "react"
import { useAuth } from "@/hooks/useAuth"
import { rollingPaperApi, getDecoInfo, decoTypeMap, type RollingPaperMessage } from "@/lib/api"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Badge } from "@/components/ui/badge"
import { MessageSquare, Plus, Heart, Share2, Trash2, ArrowLeft } from "lucide-react"
import Link from "next/link"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

export default function RollingPaperPage() {
  const { user, isAuthenticated, isLoading } = useAuth()
  const router = useRouter()
  const [selectedCell, setSelectedCell] = useState<number | null>(null)
  const [messages, setMessages] = useState<{ [key: number]: RollingPaperMessage }>({})
  const [isLoadingMessages, setIsLoadingMessages] = useState(true)

  // 로그인하지 않은 사용자는 로그인 페이지로 리다이렉트
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login")
    }
  }, [isAuthenticated, isLoading, router])

  // 내 롤링페이퍼 메시지 조회
  useEffect(() => {
    const fetchMessages = async () => {
      if (!isAuthenticated || !user) return

      try {
        const response = await rollingPaperApi.getMyRollingPaper()
        if (response.success && response.data) {
          const messageMap: { [key: number]: RollingPaperMessage } = {}
          response.data.messages.forEach((message) => {
            const position = message.positionY * 7 + message.positionX
            messageMap[position] = message
          })
          setMessages(messageMap)
        }
      } catch (error) {
        console.error("Failed to fetch messages:", error)
      } finally {
        setIsLoadingMessages(false)
      }
    }

    fetchMessages()
  }, [isAuthenticated, user])

  if (isLoading || isLoadingMessages) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-4">
            <MessageSquare className="w-7 h-7 text-white animate-pulse" />
          </div>
          <p className="text-gray-600">롤링페이퍼를 불러오는 중...</p>
        </div>
      </div>
    )
  }

  if (!isAuthenticated || !user) {
    return null
  }

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
                <MessageSquare className="w-5 h-5 text-white" />
              </div>
              <div>
                <h1 className="font-bold text-gray-800">{user.nickname}님의 롤링페이퍼</h1>
                <p className="text-xs text-gray-500">총 {Object.keys(messages).length}개의 메시지</p>
              </div>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <Button variant="outline" size="sm" className="bg-white">
              <Share2 className="w-4 h-4 mr-2" />
              공유하기
            </Button>
            <Button variant="outline" size="sm" className="bg-white">
              <Heart className="w-4 h-4" />
            </Button>
          </div>
        </div>
      </header>

      <div className="container mx-auto px-4 py-8">
        {/* Rolling Paper Grid */}
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-xl mb-8">
          <CardHeader className="text-center">
            <CardTitle className="text-2xl bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent">
              롤링페이퍼
            </CardTitle>
            <p className="text-gray-600">메시지를 클릭하면 내용을 볼 수 있어요</p>
          </CardHeader>
          <CardContent className="p-6">
            <div className="grid grid-cols-7 gap-2 max-w-4xl mx-auto">
              {Array.from({ length: 98 }, (_, i) => {
                const hasMessage = messages[i]
                const decoInfo = hasMessage ? getDecoInfo(hasMessage.decoType) : null // decoInfo 가져오기
                return (
                  <Dialog key={i}>
                    <DialogTrigger asChild>
                      <div
                        className={`
                          aspect-square rounded-lg border-2 flex items-center justify-center cursor-pointer transition-all duration-200
                          ${
                            hasMessage
                              ? `bg-gradient-to-br ${decoInfo?.color} hover:scale-105 shadow-md` // decoInfo.color 사용
                              : "border-dashed border-gray-200 hover:border-gray-300 hover:bg-gray-50"
                          }
                        `}
                      >
                        {hasMessage ? (
                          <span className="text-lg">{decoInfo?.emoji}</span> // decoInfo.emoji 사용
                        ) : (
                          <Plus className="w-4 h-4 text-gray-400" />
                        )}
                      </div>
                    </DialogTrigger>
                    <DialogContent className="max-w-md">
                      <DialogHeader>
                        <DialogTitle>{hasMessage ? "메시지 보기" : "새 메시지 작성"}</DialogTitle>
                      </DialogHeader>
                      {hasMessage ? (
                        <MessageView message={hasMessage} isOwner={true} /> // isOwner는 항상 true로 가정
                      ) : (
                        <MessageForm
                          onSubmit={(data) => {
                            setMessages((prev) => ({
                              ...prev,
                              [i]: {
                                ...data,
                                id: Date.now(),
                                userId: user?.userId || 0,
                                width: i % 7,
                                height: Math.floor(i / 7),
                              }, // 임시 ID 및 위치 추가
                            }))
                          }}
                        />
                      )}
                    </DialogContent>
                  </Dialog>
                )
              })}
            </div>
          </CardContent>
        </Card>

        {/* Recent Messages */}
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <MessageSquare className="w-5 h-5 text-purple-600" />
              <span>최근 메시지</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {Object.entries(messages)
                .slice(-3)
                .map(([index, message]) => {
                  const decoInfo = getDecoInfo(message.decoType) // decoInfo 가져오기
                  return (
                    <div key={index} className="flex items-start space-x-3 p-3 rounded-lg bg-gray-50">
                      <div
                        className={`w-8 h-8 rounded-full bg-gradient-to-r ${decoInfo.color} flex items-center justify-center`} // decoInfo.color 사용
                      >
                        <span className="text-sm">{decoInfo.emoji}</span> {/* decoInfo.emoji 사용 */}
                      </div>
                      <div className="flex-1">
                        <p className="text-gray-800 text-sm">{message.content}</p>
                        <div className="flex items-center space-x-2 mt-1">
                          <Badge variant="outline" className="text-xs">
                            {message.anonymousNickname}
                          </Badge>
                          <span className="text-xs text-gray-500">{decoInfo.name}</span> {/* decoInfo.name 사용 */}
                        </div>
                      </div>
                    </div>
                  )
                })}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

function MessageForm({
  onSubmit,
}: {
  onSubmit: (data: { content: string; anonymousNickname: string; decoType: string }) => void
}) {
  const [content, setContent] = useState("")
  const [anonymousNickname, setAnonymousNickname] = useState("") // author -> anonymousNickname
  const [decoType, setDecoType] = useState("POTATO") // design -> decoType
  const [isSubmitting, setIsSubmitting] = useState(false)

  const decoOptions = Object.entries(decoTypeMap).map(([key, info]) => ({
    value: key,
    label: `${info.emoji} ${info.name}`,
    info,
  }))

  const handleSubmit = async () => {
    if (!content.trim() || !anonymousNickname.trim()) {
      alert("모든 필드를 입력해주세요.")
      return
    }

    setIsSubmitting(true)
    try {
      // 이 페이지는 내 롤링페이퍼이므로, user.userName을 사용해야 합니다.
      // 하지만 현재 MessageForm은 nickname prop을 받지 않으므로, 임시로 user.userName을 사용합니다.
      // 실제로는 상위 컴포넌트에서 nickname을 prop으로 전달해야 합니다.
      const response = await rollingPaperApi.createMessage(
        "my_nickname_placeholder", // TODO: 실제 사용자 닉네임으로 대체 필요
        {
          content: content.trim(),
          anonymousNickname: anonymousNickname.trim(),
          decoType,
          positionX: 0, // 임시 값, 실제로는 클릭된 셀의 x, y를 사용해야 함
          positionY: 0,
        },
      )

      if (response.success && response.data) {
        onSubmit({
          id: response.data.id,
          content: response.data.content,
          anonymousNickname: response.data.anonymity,
          decoType: response.data.decoType,
        })
        setContent("")
        setAnonymousNickname("")
        alert("메시지가 성공적으로 등록되었습니다!")
      } else {
        alert("메시지 등록에 실패했습니다. 다시 시도해주세요.")
      }
    } catch (error) {
      console.error("Failed to create message:", error)
      alert("메시지 등록에 실패했습니다. 다시 시도해주세요.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const selectedDecoInfo = getDecoInfo(decoType)

  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-2">익명 닉네임</label>
        <Input
          placeholder="익명의 친구"
          value={anonymousNickname}
          onChange={(e) => setAnonymousNickname(e.target.value)}
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-2">메시지</label>
        <Textarea
          placeholder="따뜻한 메시지를 남겨주세요..."
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={4}
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-2">데코레이션 선택</label>
        <Select value={decoType} onValueChange={setDecoType}>
          <SelectTrigger>
            <SelectValue>
              <div className="flex items-center space-x-2">
                <span>{selectedDecoInfo.emoji}</span>
                <span>{selectedDecoInfo.name}</span>
              </div>
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            {decoOptions.map((d) => (
              <SelectItem key={d.value} value={d.value}>
                <div className="flex items-center space-x-2">
                  <span>{d.info.emoji}</span>
                  <span>{d.info.name}</span>
                </div>
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Button
        onClick={handleSubmit}
        className="w-full bg-gradient-to-r from-pink-500 to-purple-600"
        disabled={isSubmitting}
      >
        {isSubmitting ? "등록 중..." : "메시지 남기기"}
      </Button>
    </div>
  )
}

function MessageView({ message, isOwner }: { message: RollingPaperMessage; isOwner: boolean }) {
  const decoInfo = getDecoInfo(message.decoType)

  return (
    <div className="space-y-4">
      <div className={`p-4 rounded-lg bg-gradient-to-br ${decoInfo.color}`}>
        <p className="text-gray-800 leading-relaxed">{message.content}</p>
      </div>
      <div className="flex items-center justify-between">
        <div>
          <Badge variant="secondary">{message.anonymousNickname}</Badge>
          {/* <p className="text-xs text-gray-500 mt-1">2024.01.15 14:30</p> */}
        </div>
        {isOwner && (
          <Button variant="outline" size="sm" className="text-red-600 border-red-200 hover:bg-red-50">
            <Trash2 className="w-4 h-4" />
          </Button>
        )}
      </div>
    </div>
  )
}
