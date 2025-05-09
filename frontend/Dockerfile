# 1. 빌드 단계
FROM --platform=linux/arm64 node:alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# 2. 실행 단계 (Node.js 기반)
FROM --platform=linux/arm64 node:20-alpine
WORKDIR /app

COPY --from=builder /app ./

CMD ["npm", "start"]
