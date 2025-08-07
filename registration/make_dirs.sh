#!/bin/bash

BASE_DIR="src/main/java/com/course/registration"
DOMAINS=("user" "course" "enrollment" "redis" "common" "config")

for DOMAIN in "${DOMAINS[@]}"; do
  mkdir -p $BASE_DIR/$DOMAIN/domain
  mkdir -p $BASE_DIR/$DOMAIN/repository
  mkdir -p $BASE_DIR/$DOMAIN/service
  mkdir -p $BASE_DIR/$DOMAIN/controller
  mkdir -p $BASE_DIR/$DOMAIN/dto
done

# 추가로 common 하위 구조
mkdir -p $BASE_DIR/common/exception
mkdir -p $BASE_DIR/common/response
mkdir -p $BASE_DIR/common/util

echo "📁 디렉토리 구조 생성 완료!"

