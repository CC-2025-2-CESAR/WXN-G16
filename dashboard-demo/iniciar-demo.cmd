@echo off
setlocal
cd /d "%~dp0"
where npm >nul 2>nul
if not errorlevel 1 (
  if not exist node_modules call npm install
  call npm run dev
  exit /b
)
set "WXN_RUNTIME=%USERPROFILE%\.cache\codex-runtimes\codex-primary-runtime\dependencies"
if exist "%WXN_RUNTIME%\bin\fallback\pnpm.cmd" (
  set "PATH=%WXN_RUNTIME%\node\bin;%WXN_RUNTIME%\bin\fallback;%PATH%"
  if not exist node_modules call "%WXN_RUNTIME%\bin\fallback\pnpm.cmd" install --no-frozen-lockfile
  call "%WXN_RUNTIME%\bin\fallback\pnpm.cmd" run dev
  exit /b
)
echo Instale o Node.js 24 para usar npm install e npm run dev.
pause
