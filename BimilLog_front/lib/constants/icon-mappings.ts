import {
  // 음료 관련 아이콘
  Coffee, Wine, Beer, CupSoda,
  // 음식 관련 아이콘
  Cake, Pizza, Fish, Soup, Egg, Cookie, Beef, Sandwich, Drumstick, Utensils,
  // 자연 관련 아이콘
  Star, Sun, Moon, Mountain, Flower, Flower2, Leaf, Trees,
  Sparkles, Droplet, Waves,
  // 기타 아이콘
  Baby, HelpCircle, Milk, Snowflake,
  type LucideIcon
} from 'lucide-react';
import { DecoType } from '@/types/domains/paper';

export interface IconMapping {
  icon?: LucideIcon;
  emoji?: string;
  color: string;
  bgColor?: string;
  isEmoji?: boolean;
}

export const decoIconMappings: Record<DecoType, IconMapping> = {
  // 과일류
  POTATO: {
    emoji: '🥔',
    color: 'text-yellow-700',
    bgColor: 'bg-yellow-100',
    isEmoji: true
  },
  CARROT: {
    emoji: '🥕',
    color: 'text-orange-600',
    bgColor: 'bg-orange-100',
    isEmoji: true
  },
  CABBAGE: {
    emoji: '🥬',
    color: 'text-green-600',
    bgColor: 'bg-green-100',
    isEmoji: true
  },
  TOMATO: {
    emoji: '🍅',
    color: 'text-red-600',
    bgColor: 'bg-red-100',
    isEmoji: true
  },
  STRAWBERRY: {
    emoji: '🍓',
    color: 'text-pink-600',
    bgColor: 'bg-pink-100',
    isEmoji: true
  },
  WATERMELON: {
    emoji: '🍉',
    color: 'text-green-600',
    bgColor: 'bg-green-100',
    isEmoji: true
  },
  PUMPKIN: {
    emoji: '🎃',
    color: 'text-orange-600',
    bgColor: 'bg-orange-100',
    isEmoji: true
  },
  APPLE: {
    emoji: '🍎',
    color: 'text-red-600',
    bgColor: 'bg-red-100',
    isEmoji: true
  },
  GRAPE: {
    emoji: '🍇',
    color: 'text-purple-600',
    bgColor: 'bg-purple-100',
    isEmoji: true
  },
  BANANA: {
    emoji: '🍌',
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100',
    isEmoji: true
  },
  BLUEBERRY: {
    emoji: '🫐',
    color: 'text-blue-600',
    bgColor: 'bg-blue-100',
    isEmoji: true
  },

  // 이상한 몬스터류
  GOBLIN: {
    emoji: '👺',
    color: 'text-green-600',
    bgColor: 'bg-green-100',
    isEmoji: true
  },
  SLIME: {
    emoji: '💧',
    color: 'text-blue-600',
    bgColor: 'bg-blue-100',
    isEmoji: true
  },
  ORC: {
    emoji: '👹',
    color: 'text-gray-600',
    bgColor: 'bg-gray-100',
    isEmoji: true
  },
  DRAGON: {
    emoji: '🐉',
    color: 'text-red-600',
    bgColor: 'bg-red-100',
    isEmoji: true
  },
  PHOENIX: {
    emoji: '🔥',
    color: 'text-orange-600',
    bgColor: 'bg-orange-100',
    isEmoji: true
  },
  WEREWOLF: {
    emoji: '🐺',
    color: 'text-gray-600',
    bgColor: 'bg-gray-100',
    isEmoji: true
  },
  ZOMBIE: {
    emoji: '🧟',
    color: 'text-gray-600',
    bgColor: 'bg-gray-100',
    isEmoji: true
  },
  KRAKEN: {
    emoji: '🐙',
    color: 'text-blue-600',
    bgColor: 'bg-blue-100',
    isEmoji: true
  },
  CYCLOPS: {
    emoji: '👁️',
    color: 'text-purple-600',
    bgColor: 'bg-purple-100',
    isEmoji: true
  },
  DEVIL: {
    emoji: '😈',
    color: 'text-red-600',
    bgColor: 'bg-red-100',
    isEmoji: true
  },
  ANGEL: {
    emoji: '👼',
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100',
    isEmoji: true
  },

  // 음료류
  COFFEE: { 
    icon: Coffee, 
    color: 'text-amber-700',
    bgColor: 'bg-amber-100'
  },
  MILK: { 
    icon: Milk, 
    color: 'text-gray-700',
    bgColor: 'bg-gray-100'
  },
  WINE: { 
    icon: Wine, 
    color: 'text-purple-700',
    bgColor: 'bg-purple-100'
  },
  SOJU: {
    emoji: '🍶',
    color: 'text-blue-600',
    bgColor: 'bg-blue-100',
    isEmoji: true
  },
  BEER: { 
    icon: Beer, 
    color: 'text-amber-600',
    bgColor: 'bg-amber-100'
  },
  BUBBLETEA: { 
    icon: CupSoda, 
    color: 'text-pink-600',
    bgColor: 'bg-pink-100'
  },
  SMOOTHIE: { 
    icon: CupSoda, 
    color: 'text-pink-600',
    bgColor: 'bg-pink-100'
  },
  BORICHA: { 
    icon: Coffee, 
    color: 'text-amber-600',
    bgColor: 'bg-amber-100'
  },
  STRAWBERRYMILK: { 
    icon: Milk, 
    color: 'text-pink-600',
    bgColor: 'bg-pink-100'
  },
  BANANAMILK: { 
    icon: Milk, 
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100'
  },

  // 음식류
  BREAD: {
    emoji: '🍞',
    color: 'text-amber-700',
    bgColor: 'bg-amber-100',
    isEmoji: true
  },
  BURGER: { 
    icon: Sandwich, 
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100'
  },
  CAKE: { 
    icon: Cake, 
    color: 'text-pink-600',
    bgColor: 'bg-pink-100'
  },
  SUSHI: { 
    icon: Fish, 
    color: 'text-orange-600',
    bgColor: 'bg-orange-100'
  },
  PIZZA: { 
    icon: Pizza, 
    color: 'text-red-600',
    bgColor: 'bg-red-100'
  },
  CHICKEN: { 
    icon: Drumstick, 
    color: 'text-yellow-700',
    bgColor: 'bg-yellow-100'
  },
  NOODLE: { 
    icon: Soup, 
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100'
  },
  EGG: { 
    icon: Egg, 
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100'
  },
  SKEWER: { 
    icon: Utensils, 
    color: 'text-red-600',
    bgColor: 'bg-red-100'
  },
  KIMBAP: {
    emoji: '🍙',
    color: 'text-green-600',
    bgColor: 'bg-green-100',
    isEmoji: true
  },
  SUNDAE: {
    emoji: '🌭',
    color: 'text-gray-600',
    bgColor: 'bg-gray-100',
    isEmoji: true
  },
  MANDU: {
    emoji: '🥟',
    color: 'text-gray-600',
    bgColor: 'bg-gray-100',
    isEmoji: true
  },
  SAMGYEOPSAL: { 
    icon: Beef, 
    color: 'text-pink-600',
    bgColor: 'bg-pink-100'
  },
  FROZENFISH: {
    emoji: '🐟',
    color: 'text-blue-600',
    bgColor: 'bg-blue-100',
    isEmoji: true
  },
  HOTTEOK: {
    emoji: '🥞',
    color: 'text-amber-700',
    bgColor: 'bg-yellow-100',
    isEmoji: true
  },
  COOKIE: { 
    icon: Cookie, 
    color: 'text-amber-700',
    bgColor: 'bg-yellow-100'
  },
  PICKLE: { 
    icon: Leaf, 
    color: 'text-green-600',
    bgColor: 'bg-green-100'
  },

  // 동물류
  CAT: {
    emoji: '🐱',
    color: 'text-gray-600',
    bgColor: 'bg-gray-100',
    isEmoji: true
  },
  DOG: {
    emoji: '🐶',
    color: 'text-yellow-700',
    bgColor: 'bg-yellow-100',
    isEmoji: true
  },
  RABBIT: {
    emoji: '🐰',
    color: 'text-pink-600',
    bgColor: 'bg-pink-100',
    isEmoji: true
  },
  FOX: {
    emoji: '🦊',
    color: 'text-orange-600',
    bgColor: 'bg-orange-100',
    isEmoji: true
  },
  TIGER: {
    emoji: '🐯',
    color: 'text-orange-600',
    bgColor: 'bg-orange-100',
    isEmoji: true
  },
  PANDA: {
    emoji: '🐼',
    color: 'text-gray-600',
    bgColor: 'bg-gray-100',
    isEmoji: true
  },
  LION: {
    emoji: '🦁',
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100',
    isEmoji: true
  },
  ELEPHANT: {
    emoji: '🐘',
    color: 'text-gray-600',
    bgColor: 'bg-gray-100',
    isEmoji: true
  },
  SQUIRREL: {
    emoji: '🐿️',
    color: 'text-amber-700',
    bgColor: 'bg-yellow-100',
    isEmoji: true
  },
  HEDGEHOG: {
    emoji: '🦔',
    color: 'text-amber-700',
    bgColor: 'bg-yellow-100',
    isEmoji: true
  },
  CRANE: {
    emoji: '🦜',
    color: 'text-gray-600',
    bgColor: 'bg-gray-100',
    isEmoji: true
  },
  SPARROW: {
    emoji: '🐦',
    color: 'text-amber-700',
    bgColor: 'bg-yellow-100',
    isEmoji: true
  },
  CHIPMUNK: {
    emoji: '🐿️',
    color: 'text-gray-600',
    bgColor: 'bg-gray-100',
    isEmoji: true
  },
  GIRAFFE: {
    emoji: '🦒',
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100',
    isEmoji: true
  },
  HIPPO: {
    emoji: '🦛',
    color: 'text-gray-600',
    bgColor: 'bg-gray-100',
    isEmoji: true
  },
  POLARBEAR: {
    emoji: '🐻‍❄️',
    color: 'text-blue-600',
    bgColor: 'bg-blue-100',
    isEmoji: true
  },
  BEAR: {
    emoji: '🐻',
    color: 'text-amber-700',
    bgColor: 'bg-yellow-100',
    isEmoji: true
  },

  // 자연류
  STAR: { 
    icon: Star, 
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100'
  },
  SUN: { 
    icon: Sun, 
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100'
  },
  MOON: { 
    icon: Moon, 
    color: 'text-blue-600',
    bgColor: 'bg-blue-100'
  },
  VOLCANO: { 
    icon: Mountain, 
    color: 'text-red-600',
    bgColor: 'bg-red-100'
  },
  CHERRY: { 
    icon: Flower, 
    color: 'text-pink-600',
    bgColor: 'bg-pink-100'
  },
  MAPLE: { 
    icon: Leaf, 
    color: 'text-red-600',
    bgColor: 'bg-red-100'
  },
  BAMBOO: { 
    icon: Trees, 
    color: 'text-green-600',
    bgColor: 'bg-green-100'
  },
  SUNFLOWER: { 
    icon: Flower2, 
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100'
  },
  STARLIGHT: { 
    icon: Sparkles, 
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100'
  },
  CORAL: { 
    icon: Flower, 
    color: 'text-orange-600',
    bgColor: 'bg-orange-100'
  },
  ROCK: { 
    icon: Mountain, 
    color: 'text-gray-600',
    bgColor: 'bg-gray-100'
  },
  WATERDROP: { 
    icon: Droplet, 
    color: 'text-blue-600',
    bgColor: 'bg-blue-100'
  },
  WAVE: { 
    icon: Waves, 
    color: 'text-blue-600',
    bgColor: 'bg-blue-100'
  },
  RAINBOW: { 
    icon: Sparkles, 
    color: 'text-pink-600',
    bgColor: 'bg-pink-100'
  },

  // 기타류
  DOLL: { 
    icon: Baby, 
    color: 'text-pink-600',
    bgColor: 'bg-pink-100'
  },
  BALLOON: {
    emoji: '🎈',
    color: 'text-red-600',
    bgColor: 'bg-red-100',
    isEmoji: true
  },
  SNOWMAN: { 
    icon: Snowflake, 
    color: 'text-blue-600',
    bgColor: 'bg-blue-100'
  },
  FAIRY: { 
    icon: Sparkles, 
    color: 'text-pink-600',
    bgColor: 'bg-pink-100'
  },
  BUBBLE: {
    emoji: '🫧',
    color: 'text-blue-400',
    bgColor: 'bg-blue-100',
    isEmoji: true
  }
};

// 폴백 아이콘
export const fallbackIcon: IconMapping = {
  icon: HelpCircle,
  color: 'text-gray-500',
  bgColor: 'bg-gray-100'
};

// 아이콘 매핑 가져오기 함수
export function getIconMapping(decoType: DecoType): IconMapping {
  return decoIconMappings[decoType] || fallbackIcon;
}