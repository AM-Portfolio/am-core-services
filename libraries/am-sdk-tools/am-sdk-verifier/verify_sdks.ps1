
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " STARTING SDK VERIFICATION (JAVA & FLUTTER)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Ensure that 'am-analysis' is running on port 8090." -ForegroundColor Yellow
Write-Host ""

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BaseDir = Resolve-Path (Join-Path $ScriptDir "..\..")

# 1. Verify Java SDK
Write-Host "[STEP 1] Verifying Java SDK..." -ForegroundColor Cyan
$JavaVerifierDir = Join-Path $ScriptDir "java-verifier"

# 1.1 Install the Generated Client to local Repo first (Required dependency)
$JavaSdkPath = Join-Path $BaseDir "services\am-analysis\am-analysis-sdk\java-sdk"
Write-Host "  Installing Generated SDK (mvn install)..." -ForegroundColor Gray
Push-Location $JavaSdkPath
cmd /c "mvn clean install -DskipTests -q"
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAILED] Could not install Generated Java SDK." -ForegroundColor Red
    exit 1
}
Pop-Location

# 1.2 Run Verifier Tests
Write-Host "  Running Java Integration Tests..." -ForegroundColor Gray
Push-Location $JavaVerifierDir
cmd /c "mvn test -q"
if ($LASTEXITCODE -eq 0) {
    Write-Host "  [SUCCESS] Java SDK Verified." -ForegroundColor Green
}
else {
    Write-Host "  [FAILED] Java SDK Verification Failed." -ForegroundColor Red
    exit 1
}
Pop-Location

Write-Host ""

# 2. Verify Flutter SDK
Write-Host "[STEP 2] Verifying Flutter SDK..." -ForegroundColor Cyan
$FlutterVerifierDir = Join-Path $ScriptDir "flutter-verifier"

# 2.1 Get Dependencies
Write-Host "  Getting Flutter Dependencies..." -ForegroundColor Gray
Push-Location $FlutterVerifierDir
cmd /c "flutter pub get"
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAILED] Flutter Pub Get failed." -ForegroundColor Red
    exit 1
}

# 2.2 Run Tests
Write-Host "  Running Flutter Tests..." -ForegroundColor Gray
cmd /c "flutter test"
if ($LASTEXITCODE -eq 0) {
    Write-Host "  [SUCCESS] Flutter SDK Verified." -ForegroundColor Green
}
else {
    Write-Host "  [FAILED] Flutter SDK Verification Failed." -ForegroundColor Red
    exit 1
}
Pop-Location

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host " ALL SDKs VERIFIED & READY TO PUBLISH!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
