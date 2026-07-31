# 冒烟测试（Windows PowerShell）
param(
  [string]$BaseUrl = "http://localhost:8081/api/v1"
)

$ErrorActionPreference = "Stop"
Write-Host "==> Smoke against $BaseUrl"

$probe = Invoke-RestMethod "$BaseUrl/canary/probe"
if ($probe.data.status -ne "UP") { throw "canary probe failed" }
Write-Host "[ok] canary/probe"

$home = Invoke-RestMethod "$BaseUrl/home/dashboard"
if ($home.code -ne 200) { throw "home dashboard failed" }
Write-Host "[ok] home/dashboard"

$login = Invoke-RestMethod -Method POST "$BaseUrl/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"admin123"}'
if (-not $login.data.token) { throw "login failed" }
Write-Host "[ok] auth/login"

Write-Host "Smoke test passed"
