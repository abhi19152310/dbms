param(
    [ValidateSet('ml', 'backend', 'frontend')]
    [string]$Service
)

$ErrorActionPreference = 'Stop'

$mlDirectory = Join-Path $PSScriptRoot 'ml-service'
$backendDirectory = Join-Path $PSScriptRoot 'backend'
$frontendDirectory = Join-Path $PSScriptRoot 'frontend'
$modelPath = Join-Path $mlDirectory 'models\demand_model.joblib'

if (-not (Test-NetConnection 127.0.0.1 -Port 5432 -InformationLevel Quiet -WarningAction SilentlyContinue)) {
    throw 'PostgreSQL is not running on port 5432. Start PostgreSQL, then run start.bat again.'
}

if (-not (Test-Path $modelPath)) {
    throw "The trained model is missing: $modelPath"
}

$python = (Get-Command python.exe -ErrorAction Stop).Source
$npm = (Get-Command npm.cmd -ErrorAction Stop).Source
$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
$maven = if ($mavenCommand) { $mavenCommand.Source } else { 'C:\apache-maven\apache-maven-3.9.16\bin\mvn.cmd' }

if (-not (Test-Path $maven)) {
    throw 'Maven was not found. Install Maven or add its bin folder to PATH.'
}

if ($Service) {
    switch ($Service) {
        'ml' {
            $Host.UI.RawUI.WindowTitle = 'Ride Demand ML'
            Set-Location $mlDirectory
            & $python -m uvicorn predict_service:app --port 8000
        }
        'backend' {
            $Host.UI.RawUI.WindowTitle = 'Ride Demand Backend'
            Set-Location $backendDirectory
            & $maven spring-boot:run
        }
        'frontend' {
            $Host.UI.RawUI.WindowTitle = 'Ride Demand Frontend'
            Set-Location $frontendDirectory
            & $npm run dev
        }
    }
    return
}

& $python -c 'import fastapi, joblib, numpy, pandas, sklearn, uvicorn, xgboost' 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Installing ML service requirements...'
    & $python -m pip install -r (Join-Path $mlDirectory 'requirements.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Installing ML service requirements failed.' }
}

if (-not (Test-Path (Join-Path $frontendDirectory 'node_modules'))) {
    Write-Host 'Installing frontend requirements...'
    Push-Location $frontendDirectory
    try {
        & $npm install
        if ($LASTEXITCODE -ne 0) { throw 'Installing frontend requirements failed.' }
    }
    finally {
        Pop-Location
    }
}

function Start-ServiceWindow {
    param([string]$Name)

    $arguments = "-NoExit -NoProfile -File `"$PSCommandPath`" -Service $Name"
    Start-Process powershell.exe -ArgumentList $arguments
}

Start-ServiceWindow 'ml'
Start-ServiceWindow 'backend'
Start-ServiceWindow 'frontend'

Write-Host 'Ride Demand is starting. Open http://localhost:5173 when the frontend window is ready.'