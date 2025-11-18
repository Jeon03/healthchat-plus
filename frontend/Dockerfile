# ======================================
# 1단계: React(Vite) 빌드
# ======================================
FROM node:20 AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

# 환경 변수 + 전체 소스 복사
COPY . .
COPY .env.production .env.production

RUN npm run build

# ======================================
# 2단계: Nginx로 정적 파일 제공
# ======================================
FROM nginx:1.25
WORKDIR /usr/share/nginx/html

# Asia/Seoul 시간대 설정
RUN apt-get update && apt-get install -y tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    rm -rf /var/lib/apt/lists/*

# Nginx 설정 복사
COPY ../nginx/default.conf /etc/nginx/conf.d/default.conf

# Vite 빌드 결과 복사
COPY --from=build /app/dist .

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
