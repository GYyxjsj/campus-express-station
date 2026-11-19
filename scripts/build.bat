@echo off
rem ============================================================
rem 校园快递驿站系统 一键构建脚本（Windows）
rem 产物：backend\target\campus-express.war + frontend\dist\
rem 依赖：JDK 8+、Maven 3.6+、Node.js 18+
rem ============================================================
setlocal
cd /d "%~dp0.."

echo [1/3] 构建后端 WAR ...
pushd backend
call mvn clean package -DskipTests
if errorlevel 1 (
    echo [错误] 后端构建失败，请检查 Maven / JDK 环境。
    popd & exit /b 1
)
popd

echo [2/3] 安装前端依赖 ...
pushd frontend
call npm install --no-audit --no-fund
if errorlevel 1 (
    echo [错误] npm install 失败，请检查 Node.js 环境或网络。
    popd & exit /b 1
)

echo [3/3] 构建前端静态资源 ...
call npm run build
if errorlevel 1 (
    echo [错误] 前端构建失败。
    popd & exit /b 1
)
popd

echo.
echo ============ 构建完成 ============
echo 后端 WAR : backend\target\campus-express.war
echo 前端产物 : frontend\dist\
echo 部署方法 : 参见 docs\02-Tomcat部署指南.md
endlocal