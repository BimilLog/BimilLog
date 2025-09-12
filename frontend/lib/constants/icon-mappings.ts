import {
  // 과일 관련 아이콘
  Apple, Cherry, Grape,
  // 음료 관련 아이콘
  Coffee, Wine, Beer, CupSoda,
  // 음식 관련 아이콘
  Cake, Pizza, Fish, Soup, Egg, Cookie, Beef,
  // 동물 관련 아이콘
  Cat, Dog, Bird, PawPrint, Fish as FishIcon,
  // 자연 관련 아이콘
  Star, Sun, Moon, Mountain, Flower, Flower2, Leaf, Trees,
  Sparkles, Droplet, Waves, TreePalm,
  // 몬스터/판타지 관련 아이콘
  Skull, Flame, Ghost, Eye, Zap,
  // 기본 모양 아이콘
  Circle, Package, Utensils, Shield, Snowflake, GlassWater,
  // 기타 아이콘
  Baby, HelpCircle, Rabbit, Bug, Footprints, Milk, Sandwich,
  Drumstick,
  type LucideIcon
} from 'lucide-react';
import { DecoType } from '@/types/domains/paper';

export interface IconMapping {
  icon: LucideIcon;
  color: string;
  bgColor?: string;
}

export const decoIconMappings: Record<DecoType, IconMapping> = {
  // 과일류
  POTATO: { 
    icon: Circle, 
    color: 'text-yellow-700',
    bgColor: 'bg-yellow-100'
  },
  CARROT: { 
    icon: Package, // Carrot 아이콘이 없으므로 대체
    color: 'text-orange-600',
    bgColor: 'bg-orange-100'
  },
  CABBAGE: { 
    icon: Flower2, 
    color: 'text-green-600',
    bgColor: 'bg-green-100'
  },
  TOMATO: { 
    icon: Apple, 
    color: 'text-red-600',
    bgColor: 'bg-red-100'
  },
  STRAWBERRY: { 
    icon: Cherry, 
    color: 'text-pink-600',
    bgColor: 'bg-pink-100'
  },
  WATERMELON: { 
    icon: Circle, 
    color: 'text-green-600',
    bgColor: 'bg-green-100'
  },
  PUMPKIN: { 
    icon: Circle, 
    color: 'text-orange-600',
    bgColor: 'bg-orange-100'
  },
  APPLE: { 
    icon: Apple, 
    color: 'text-red-600',
    bgColor: 'bg-red-100'
  },
  GRAPE: { 
    icon: Cherry, // Grape 아이콘 대체
    color: 'text-purple-600',
    bgColor: 'bg-purple-100'
  },
  BANANA: { 
    icon: Package, 
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100'
  },
  BLUEBERRY: { 
    icon: Cherry, 
    color: 'text-blue-600',
    bgColor: 'bg-blue-100'
  },

  // 이상한 몬스터류
  GOBLIN: { 
    icon: Skull, 
    color: 'text-green-600',
    bgColor: 'bg-green-100'
  },
  SLIME: { 
    icon: Circle, 
    color: 'text-blue-600',
    bgColor: 'bg-blue-100'
  },
  ORC: { 
    icon: Shield, 
    color: 'text-gray-600',
    bgColor: 'bg-gray-100'
  },
  DRAGON: { 
    icon: Flame, 
    color: 'text-red-600',
    bgColor: 'bg-red-100'
  },
  PHOENIX: { 
    icon: Flame, 
    color: 'text-orange-600',
    bgColor: 'bg-orange-100'
  },
  WEREWOLF: { 
    icon: Dog, 
    color: 'text-gray-600',
    bgColor: 'bg-gray-100'
  },
  ZOMBIE: { 
    icon: Ghost, 
    color: 'text-gray-600',
    bgColor: 'bg-gray-100'
  },
  KRAKEN: { 
    icon: Waves, 
    color: 'text-blue-600',
    bgColor: 'bg-blue-100'
  },
  CYCLOPS: { 
    icon: Eye, 
    color: 'text-purple-600',
    bgColor: 'bg-purple-100'
  },
  DEVIL: { 
    icon: Zap, 
    color: 'text-red-600',
    bgColor: 'bg-red-100'
  },
  ANGEL: { 
    icon: Sparkles, 
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100'
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
    icon: GlassWater, 
    color: 'text-blue-600',
    bgColor: 'bg-blue-100'
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
    icon: Package, 
    color: 'text-amber-700',
    bgColor: 'bg-amber-100'
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
    icon: Package, 
    color: 'text-green-600',
    bgColor: 'bg-green-100'
  },
  SUNDAE: { 
    icon: Sandwich, 
    color: 'text-gray-600',
    bgColor: 'bg-gray-100'
  },
  MANDU: { 
    icon: Cookie, 
    color: 'text-gray-600',
    bgColor: 'bg-gray-100'
  },
  SAMGYEOPSAL: { 
    icon: Beef, 
    color: 'text-pink-600',
    bgColor: 'bg-pink-100'
  },
  FROZENFISH: { 
    icon: Fish, 
    color: 'text-blue-600',
    bgColor: 'bg-blue-100'
  },
  HOTTEOK: { 
    icon: Cookie, 
    color: 'text-brown-600',
    bgColor: 'bg-yellow-100'
  },
  COOKIE: { 
    icon: Cookie, 
    color: 'text-brown-600',
    bgColor: 'bg-yellow-100'
  },
  PICKLE: { 
    icon: Leaf, 
    color: 'text-green-600',
    bgColor: 'bg-green-100'
  },

  // 동물류
  CAT: { 
    icon: Cat, 
    color: 'text-gray-600',
    bgColor: 'bg-gray-100'
  },
  DOG: { 
    icon: Dog, 
    color: 'text-yellow-700',
    bgColor: 'bg-yellow-100'
  },
  RABBIT: { 
    icon: Rabbit, 
    color: 'text-pink-600',
    bgColor: 'bg-pink-100'
  },
  FOX: { 
    icon: Dog, 
    color: 'text-orange-600',
    bgColor: 'bg-orange-100'
  },
  TIGER: { 
    icon: Cat, 
    color: 'text-orange-600',
    bgColor: 'bg-orange-100'
  },
  PANDA: { 
    icon: Circle, 
    color: 'text-gray-600',
    bgColor: 'bg-gray-100'
  },
  LION: { 
    icon: Cat, 
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100'
  },
  ELEPHANT: { 
    icon: Footprints, 
    color: 'text-gray-600',
    bgColor: 'bg-gray-100'
  },
  SQUIRREL: { 
    icon: Bug, // Squirrel 대체
    color: 'text-brown-600',
    bgColor: 'bg-yellow-100'
  },
  HEDGEHOG: { 
    icon: Bug, 
    color: 'text-brown-600',
    bgColor: 'bg-yellow-100'
  },
  CRANE: { 
    icon: Bird, 
    color: 'text-gray-600',
    bgColor: 'bg-gray-100'
  },
  SPARROW: { 
    icon: Bird, 
    color: 'text-brown-600',
    bgColor: 'bg-yellow-100'
  },
  CHIPMUNK: { 
    icon: Bug, 
    color: 'text-gray-600',
    bgColor: 'bg-gray-100'
  },
  GIRAFFE: { 
    icon: TreePalm, 
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100'
  },
  HIPPO: { 
    icon: Circle, // Hippo 대체
    color: 'text-gray-600',
    bgColor: 'bg-gray-100'
  },
  POLARBEAR: { 
    icon: PawPrint, 
    color: 'text-blue-600',
    bgColor: 'bg-blue-100'
  },
  BEAR: { 
    icon: PawPrint, 
    color: 'text-brown-600',
    bgColor: 'bg-yellow-100'
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
    icon: Circle, 
    color: 'text-red-600',
    bgColor: 'bg-red-100'
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
    icon: Circle, 
    color: 'text-blue-400',
    bgColor: 'bg-blue-100'
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