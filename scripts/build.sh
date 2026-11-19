#!/usr/bin/env bash
# ============================================================
# 校园快递驿站系统 一键构建脚本（Linux / macOS）
# 产物：backend/target/campus-express.war + frontend/dist/
# 依赖：JDK 8+、Maven 3.6+、Node.js 18+
# ============================================================
set -euo pipefail
cd "$(dirname "$0")/.."

echo "[1/3] 构建后端 WAR ..."
(cd backend && mvn clean package -DskipTests)

echo "[2/3] 安装前端依赖 ..."
(cd frontend && npm install --no-audit --no-fund)

echo "[3/3] 构建前端静态资源 ..."
(cd frontend && npm run build)

echo
echo "============ 构建完成 ============"
echo "后端 WAR : backend/target/campus-express.war"
echo "前端产物 : frontend/dist/"
echo "部署方法 : 参见 docs/02-Tomcat部署指南.md"