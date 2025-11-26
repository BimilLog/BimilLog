// Rolling paper-related type definitions

// DecoType enum - 백엔드 DecoType과 완전 일치
// 롤링페이퍼 메시지 작성 시 선택할 수 있는 장식 타입들 (총 6개 카테고리)
export type DecoType =
  // 과일 카테고리
  | "POTATO" | "CARROT" | "CABBAGE" | "TOMATO" | "STRAWBERRY" | "BLUEBERRY"
  | "WATERMELON" | "PUMPKIN" | "APPLE" | "GRAPE" | "BANANA"

  // 몬스터/판타지 카테고리 (이상한 장식)
  | "GOBLIN" | "SLIME" | "ORC" | "DRAGON" | "PHOENIX"
  | "WEREWOLF" | "ZOMBIE" | "KRAKEN" | "CYCLOPS" | "DEVIL" | "ANGEL"

  // 음료 카테고리
  | "COFFEE" | "MILK" | "WINE" | "SOJU" | "BEER" | "BUBBLETEA" | "SMOOTHIE"
  | "BORICHA" | "STRAWBERRYMILK" | "BANANAMILK"

  // 음식 카테고리
  | "BREAD" | "BURGER" | "CAKE" | "SUSHI" | "PIZZA" | "CHICKEN" | "NOODLE" | "EGG"
  | "SKEWER" | "KIMBAP" | "SUNDAE" | "MANDU" | "SAMGYEOPSAL" | "FROZENFISH" | "HOTTEOK"
  | "COOKIE" | "PICKLE"

  // 동물 카테고리
  | "CAT" | "DOG" | "RABBIT" | "FOX" | "TIGER" | "PANDA" | "LION" | "ELEPHANT"
  | "SQUIRREL" | "HEDGEHOG" | "CRANE" | "SPARROW" | "CHIPMUNK" | "GIRAFFE" | "HIPPO" | "POLARBEAR" | "BEAR"

  // 자연 카테고리
  | "STAR" | "SUN" | "MOON" | "VOLCANO" | "CHERRY" | "MAPLE" | "BAMBOO" | "SUNFLOWER"
  | "STARLIGHT" | "CORAL" | "ROCK" | "WATERDROP" | "WAVE" | "RAINBOW"

  // 기타 카테고리
  | "DOLL" | "BALLOON" | "SNOWMAN" | "FAIRY" | "BUBBLE"

// 롤링페이퍼 메시지 타입 - v2 백엔드 MessageDTO 완전 호환
export interface RollingPaperMessage {
  id: number
  memberId: number
  decoType: DecoType
  anonymity: string
  content: string
  x: number  // 그리드 X 좌표 (0-based, 0부터 시작)
  y: number  // 그리드 Y 좌표 (0-based, 0부터 시작)
  createdAt: string // ISO 8601 string format - 백엔드 Instant는 ISO string으로 변환됨
}

// 방문용 메시지 타입 - v2 백엔드 VisitMessageDTO 완전 호환
// RollingPaperMessage와 차이: content, anonymity, createdAt 필드가 없음 (방문자에게 표시되지 않는 정보)
export interface VisitMessage {
  id: number
  memberId: number
  ownerId: number  // 롤링페이퍼 소유자 ID
  decoType: DecoType
  x: number  // 그리드 X 좌표 (0-based, 0부터 시작)
  y: number  // 그리드 Y 좌표 (0-based, 0부터 시작)
}

// 실시간 인기 롤링페이퍼 정보 타입 - v2 백엔드 PopularPaperInfo 완전 호환
export interface PopularPaperInfo {
  memberId: number
  memberName: string
  rank: number  // 실시간 등수
  popularityScore: number  // 실시간 점수
  recentMessageCount: number  // 24시간 이내 메시지 수
}

// 데코레이션 타입 매핑 - UI에 표시할 한글 이름과 Tailwind 그라데이션 색상
export const decoTypeMap = {
  // 과일 카테고리 - 따뜻하고 밝은 색상
  POTATO: { name: "감자", color: "from-yellow-100 to-amber-100" },
  CARROT: { name: "당근", color: "from-orange-100 to-red-100" },
  CABBAGE: { name: "양배추", color: "from-green-100 to-emerald-100" },
  TOMATO: { name: "토마토", color: "from-red-100 to-pink-100" },
  STRAWBERRY: { name: "딸기", color: "from-pink-100 to-red-100" },
  WATERMELON: { name: "수박", color: "from-green-100 to-red-100" },
  PUMPKIN: { name: "호박", color: "from-orange-100 to-yellow-100" },
  APPLE: { name: "사과", color: "from-red-100 to-pink-100" },
  GRAPE: { name: "포도", color: "from-purple-100 to-violet-100" },
  BANANA: { name: "바나나", color: "from-yellow-100 to-amber-100" },
  BLUEBERRY: { name: "블루베리", color: "from-blue-100 to-indigo-100" },

  // 몬스터/판타지 카테고리 - 다크하고 신비한 색상
  GOBLIN: { name: "고블린", color: "from-green-100 to-emerald-100" },
  SLIME: { name: "슬라임", color: "from-blue-100 to-indigo-100" },
  ORC: { name: "오크", color: "from-gray-100 to-slate-100" },
  DRAGON: { name: "드래곤", color: "from-red-100 to-orange-100" },
  PHOENIX: { name: "피닉스", color: "from-orange-100 to-red-100" },
  WEREWOLF: { name: "늑대인간", color: "from-gray-100 to-brown-100" },
  ZOMBIE: { name: "좀비", color: "from-gray-100 to-green-100" },
  KRAKEN: { name: "크라켄", color: "from-blue-100 to-purple-100" },
  CYCLOPS: { name: "사이클롭스", color: "from-purple-100 to-indigo-100" },
  DEVIL: { name: "악마", color: "from-red-100 to-orange-100" },
  ANGEL: { name: "천사", color: "from-white to-yellow-100" },

  // 음료 카테고리 - 시원하고 상쾌한 색상
  COFFEE: { name: "커피", color: "from-amber-100 to-brown-100" },
  MILK: { name: "우유", color: "from-white to-gray-100" },
  WINE: { name: "와인", color: "from-purple-100 to-red-100" },
  SOJU: { name: "소주", color: "from-blue-50 to-slate-100" },
  BEER: { name: "맥주", color: "from-yellow-100 to-amber-100" },
  BUBBLETEA: { name: "버블티", color: "from-pink-100 to-purple-100" },
  SMOOTHIE: { name: "스무디", color: "from-pink-100 to-red-100" },
  BORICHA: { name: "보리차", color: "from-amber-100 to-yellow-100" },
  STRAWBERRYMILK: { name: "딸기우유", color: "from-pink-100 to-red-100" },
  BANANAMILK: { name: "바나나우유", color: "from-yellow-100 to-amber-100" },

  // 음식 카테고리 - 따뜻하고 맛있는 색상
  BREAD: { name: "빵", color: "from-amber-100 to-yellow-100" },
  BURGER: { name: "햄버거", color: "from-yellow-100 to-red-100" },
  CAKE: { name: "케이크", color: "from-pink-100 to-yellow-100" },
  SUSHI: { name: "스시", color: "from-orange-100 to-red-100" },
  PIZZA: { name: "피자", color: "from-red-100 to-yellow-100" },
  CHICKEN: { name: "치킨", color: "from-yellow-100 to-orange-100" },
  NOODLE: { name: "라면", color: "from-yellow-100 to-red-100" },
  EGG: { name: "계란", color: "from-yellow-100 to-white" },
  SKEWER: { name: "꼬치", color: "from-red-100 to-orange-100" },
  KIMBAP: { name: "김밥", color: "from-green-100 to-yellow-100" },
  SUNDAE: { name: "순대", color: "from-gray-100 to-red-100" },
  MANDU: { name: "만두", color: "from-white to-yellow-100" },
  SAMGYEOPSAL: { name: "삼겹살", color: "from-pink-100 to-red-100" },
  FROZENFISH: { name: "동상걸린 붕어", color: "from-yellow-100 to-brown-100" },
  HOTTEOK: { name: "호떡", color: "from-brown-100 to-amber-100" },
  COOKIE: { name: "쿠키", color: "from-brown-100 to-yellow-100" },
  PICKLE: { name: "피클", color: "from-green-100 to-yellow-100" },

  // 동물 카테고리 - 귀엽고 자연스러운 색상
  CAT: { name: "고양이", color: "from-gray-100 to-orange-100" },
  DOG: { name: "강아지", color: "from-yellow-100 to-brown-100" },
  RABBIT: { name: "토끼", color: "from-pink-100 to-white" },
  FOX: { name: "여우", color: "from-orange-100 to-red-100" },
  TIGER: { name: "호랑이", color: "from-orange-100 to-yellow-100" },
  PANDA: { name: "판다", color: "from-gray-100 to-white" },
  LION: { name: "사자", color: "from-yellow-100 to-amber-100" },
  ELEPHANT: { name: "코끼리", color: "from-gray-100 to-slate-100" },
  SQUIRREL: { name: "다람쥐", color: "from-brown-100 to-orange-100" },
  HEDGEHOG: { name: "고슴도치", color: "from-brown-100 to-gray-100" },
  CRANE: { name: "두루미", color: "from-white to-gray-100" },
  SPARROW: { name: "참새", color: "from-brown-100 to-yellow-100" },
  CHIPMUNK: { name: "청설모", color: "from-gray-100 to-brown-100" },
  GIRAFFE: { name: "기린", color: "from-yellow-100 to-orange-100" },
  HIPPO: { name: "하마", color: "from-gray-100 to-purple-100" },
  POLARBEAR: { name: "북극곰", color: "from-white to-blue-100" },
  BEAR: { name: "곰", color: "from-red-100 to-rainbow-100" },

  // 자연 카테고리 - 신선하고 평화로운 색상
  STAR: { name: "별", color: "from-yellow-100 to-amber-100" },
  SUN: { name: "태양", color: "from-yellow-100 to-orange-100" },
  MOON: { name: "달", color: "from-blue-100 to-indigo-100" },
  VOLCANO: { name: "화산", color: "from-red-100 to-orange-100" },
  CHERRY: { name: "벚꽃", color: "from-pink-100 to-white" },
  MAPLE: { name: "단풍", color: "from-red-100 to-orange-100" },
  BAMBOO: { name: "대나무", color: "from-green-100 to-emerald-100" },
  SUNFLOWER: { name: "해바라기", color: "from-yellow-100 to-orange-100" },
  STARLIGHT: { name: "별빛", color: "from-yellow-100 to-blue-100" },
  CORAL: { name: "산호", color: "from-orange-100 to-pink-100" },
  ROCK: { name: "바위", color: "from-gray-100 to-slate-100" },
  WATERDROP: { name: "물방울", color: "from-blue-100 to-white" },
  WAVE: { name: "파도", color: "from-blue-100 to-cyan-100" },
  RAINBOW: { name: "무지개", color: "from-pink-100 to-purple-100" },

  // 기타 카테고리 - 다양하고 재미있는 색상
  DOLL: { name: "인형", color: "from-pink-100 to-purple-100" },
  BALLOON: { name: "풍선", color: "from-red-100 to-rainbow-100" },
  SNOWMAN: { name: "눈사람", color: "from-white to-blue-100" },
  FAIRY: { name: "요정", color: "from-pink-100 to-purple-100" },
  BUBBLE: { name: "비눗방울", color: "from-blue-100 to-white" }
}