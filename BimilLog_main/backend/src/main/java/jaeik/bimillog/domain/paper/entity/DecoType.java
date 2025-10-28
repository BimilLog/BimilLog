package jaeik.bimillog.domain.paper.entity;

/**
 * <h2>장식 타입</h2>
 * <p>
 * 롤링페이퍼 메시지의 다양한 장식 종류를 정의하는 열거형
 * </p>
 * <p>Message 엔티티에서 메시지의 시각적 꾸밈을 결정하기 위해 사용되는 열거형</p>
 * <p>과일, 동물, 음식, 자연, 판타지 등 다양한 카테고리의 장식 타입을 제공</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public enum DecoType {
    // 과일
    POTATO, CARROT, CABBAGE, TOMATO, STRAWBERRY, BLUEBERRY,
    WATERMELON, PUMPKIN, APPLE, GRAPE, BANANA,

    // 이상한 장식
    GOBLIN, SLIME, ORC, DRAGON, PHOENIX,
    WEREWOLF, ZOMBIE, KRAKEN, CYCLOPS, DEVIL, ANGEL,

    // 음료
    COFFEE, MILK, WINE, SOJU, BEER, BUBBLETEA, SMOOTHIE,
    BORICHA, STRAWBERRYMILK, BANANAMILK,

    // 음식
    BREAD, BURGER, CAKE, SUSHI, PIZZA, CHICKEN, NOODLE, EGG,
    SKEWER, KIMBAP, SUNDAE, MANDU, SAMGYEOPSAL, FROZENFISH, HOTTEOK,
    COOKIE, PICKLE,

    // 동물
    CAT, DOG, RABBIT, FOX, TIGER, PANDA, LION, ELEPHANT,
    SQUIRREL, HEDGEHOG, CRANE, SPARROW, CHIPMUNK, GIRAFFE, HIPPO, POLARBEAR, BEAR,

    // 자연
    STAR, SUN, MOON, VOLCANO, CHERRY, MAPLE, BAMBOO, SUNFLOWER,
    STARLIGHT, CORAL, ROCK, WATERDROP, WAVE, RAINBOW,

    // 기타
    DOLL, BALLOON, SNOWMAN, FAIRY, BUBBLE
}

