[CmdletBinding()]
param(
    [ValidateSet('java', 'python', 'frontend', 'smoke', 'security', 'load', 'mutation', 'newman', 'all')]
    [string]$Suite = 'java',
    [ValidateSet('unit', 'verify', 'integration')]
    [string]$Mode = 'unit',
    [ValidateSet('all', 'iam-service', 'patient-clinical-service', 'gateway', 'reporting-service', 'backup-service')]
    [string]$Service = 'all',
    [switch]$Coverage,
    [switch]$FailFast,
    [switch]$IncludeIntegration,
    [string]$SecurityPassword,
    [ValidateSet('all', 'java', 'python', 'frontend')]
    [string]$MutationScope = 'all'
)

$ErrorActionPreference = 'Stop'
$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$JavaRoot = Join-Path $RepositoryRoot 'backend-java'
$PythonRoot = Join-Path $RepositoryRoot 'backend'
$FrontendRoot = Join-Path $RepositoryRoot 'frontend'

if ($IncludeIntegration) { $Mode = 'integration' }

function Invoke-External {
    param([string]$File, [string[]]$Arguments, [string]$WorkingDirectory)
    Push-Location $WorkingDirectory
    try {
        Write-Host ("`n> {0} {1}" -f $File, ($Arguments -join ' ')) -ForegroundColor Cyan
        & $File @Arguments
        if ($LASTEXITCODE -ne 0) { throw "Comando falhou com código ${LASTEXITCODE}: $File" }
    } finally { Pop-Location }
}

function Get-JavaModules {
    $modules = @('iam-service', 'patient-clinical-service', 'gateway', 'reporting-service', 'backup-service')
    if ($Service -eq 'all') { return $modules }
    return @($Service)
}

function Get-JavaTestClasses {
    param([string]$ModuleRoot)
    $testRoot = Join-Path $ModuleRoot 'src/test/java'
    if (-not (Test-Path $testRoot)) { return @() }

    $selected = foreach ($file in (Get-ChildItem $testRoot -Recurse -Filter '*.java')) {
        if (-not (Select-String -Path $file.FullName -Pattern '@Test' -Quiet)) { continue }
        $relative = $file.FullName.Substring($testRoot.Length).TrimStart('\', '/')
        $className = $relative.Substring(0, $relative.Length - $file.Extension.Length).Replace('\', '.').Replace('/', '.')
        $isIntegration = $file.FullName -match '[\\/]integration[\\/]' -or $file.BaseName -match 'IntegrationTest$'
        if (($Mode -eq 'integration' -and $isIntegration) -or ($Mode -ne 'integration' -and -not $isIntegration)) {
            $className
        }
    }
    return @($selected | Sort-Object -Unique)
}

function Invoke-JavaSuite {
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'mvn não foi encontrado no PATH.' }
    foreach ($module in (Get-JavaModules)) {
        $moduleRoot = Join-Path $JavaRoot $module
        if (-not (Test-Path (Join-Path $moduleRoot 'pom.xml'))) { Write-Warning "Módulo Java não encontrado: $module"; continue }
        $classes = Get-JavaTestClasses -ModuleRoot $moduleRoot
        if ($classes.Count -eq 0) { Write-Host "Sem testes $Mode em $module; ignorando." -ForegroundColor Yellow; continue }
        $goal = if ($Mode -in @('verify', 'integration')) { 'verify' } else { 'test' }
        $arguments = @($goal, '-DfailIfNoTests=false', "-Dtest=$($classes -join ',')")
        if ($FailFast) { $arguments += '-Dsurefire.skipAfterFailureCount=1' }
        Invoke-External -File 'mvn' -Arguments $arguments -WorkingDirectory $moduleRoot
    }
}

function Invoke-PythonSuite {
    if (-not (Get-Command pytest -ErrorAction SilentlyContinue)) { throw 'pytest não foi encontrado no PATH.' }
    $target = Join-Path $RepositoryRoot 'tests/python'
    if (-not (Test-Path $target)) { Write-Host 'Nenhum teste Python centralizado encontrado; ignorando.' -ForegroundColor Yellow; return }
    $arguments = @('-q', $target)
    if ($Coverage) { $arguments += @('--cov=.', '--cov-report=term-missing') }
    if ($FailFast) { $arguments += '-x' }
    Invoke-External -File 'pytest' -Arguments $arguments -WorkingDirectory $PythonRoot
}

function Invoke-NewmanSuite {
    $script = Join-Path $PSScriptRoot 'newman/run-newman.ps1'
    if (-not (Test-Path $script)) { throw 'Runner Newman não encontrado.' }
    & $script
    if ($LASTEXITCODE -ne 0) { throw "Newman falhou com código $LASTEXITCODE." }
}

function Invoke-FrontendSuite {
    if (-not (Test-Path (Join-Path $FrontendRoot 'package.json'))) { throw 'package.json do frontend nÃ£o foi encontrado.' }
    $script = if ($Mode -eq 'unit') { 'test' } elseif ($Mode -eq 'integration') { 'test:e2e' } else { 'verify' }
    if ($script -eq 'verify') {
        foreach ($npmScript in @('type-check', 'lint', 'test', 'build')) {
            Invoke-External -File 'npm' -Arguments @('run', $npmScript) -WorkingDirectory $FrontendRoot
        }
        return
    }
    $arguments = @('run', $script)
    if ($Coverage -and $script -eq 'test') { $arguments = @('run', 'test:coverage') }
    if ($Mode -eq 'integration') {
        $previousFrontendUrl = $env:E2E_FRONTEND_URL
        try {
            $env:E2E_FRONTEND_URL = 'http://127.0.0.1'
            Invoke-External -File 'npm' -Arguments $arguments -WorkingDirectory $FrontendRoot
        }
        finally {
            $env:E2E_FRONTEND_URL = $previousFrontendUrl
        }
    }
    else {
        Invoke-External -File 'npm' -Arguments $arguments -WorkingDirectory $FrontendRoot
    }
}

function Invoke-SmokeSuite {
    $script = Join-Path $PSScriptRoot 'smoke/run-smoke.ps1'
    if (-not (Test-Path $script)) { throw 'Runner de smoke test não encontrado.' }
    & $script
    if ($LASTEXITCODE -notin @(0, 2)) { throw "Smoke test falhou com código $LASTEXITCODE." }
    if ($LASTEXITCODE -eq 2) { Write-Warning 'Smoke funcional passou, mas há falhas separadas de telemetria.' }
}

function Invoke-SecuritySuite {
    $script = Join-Path $PSScriptRoot 'security/run-security.ps1'
    if (-not (Test-Path $script)) { throw 'Runner de segurança não encontrado.' }
    if ([string]::IsNullOrWhiteSpace($SecurityPassword)) { & $script }
    else { & $script -NewmanPassword $SecurityPassword }
    if ($LASTEXITCODE -ne 0) { throw "Security suite falhou com código $LASTEXITCODE." }
}

function Invoke-LoadSuite {
    $script = Join-Path $PSScriptRoot 'load/run-load.ps1'
    if (-not (Test-Path $script)) { throw 'Runner de carga não encontrado.' }
    & $script
    if ($LASTEXITCODE -ne 0) { throw "Load test falhou com código $LASTEXITCODE." }
}

function Invoke-MutationSuite {
    $script = Join-Path $PSScriptRoot 'mutation/run-mutation.ps1'
    if (-not (Test-Path $script)) { throw 'Runner de mutação não encontrado.' }
    & $script -Scope $MutationScope -Service $Service
    if ($LASTEXITCODE -ne 0) { throw "Mutation suite falhou com código $LASTEXITCODE." }
}

if ($Suite -in @('java', 'all')) { Invoke-JavaSuite }
if ($Suite -in @('python', 'all')) { Invoke-PythonSuite }
if ($Suite -in @('frontend', 'all')) { Invoke-FrontendSuite }
if ($Suite -in @('smoke', 'all')) { Invoke-SmokeSuite }
if ($Suite -eq 'security') { Invoke-SecuritySuite }
if ($Suite -eq 'load') { Invoke-LoadSuite }
if ($Suite -eq 'mutation') { Invoke-MutationSuite }
if ($Suite -eq 'newman') { Invoke-NewmanSuite }
Write-Host "`nSuíte '$Suite' ($Mode) concluída com sucesso." -ForegroundColor Green
