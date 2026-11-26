[CmdletBinding()]
param(
    [ValidateSet('all', 'java', 'python', 'frontend')]
    [string]$Scope = 'all',
    [ValidateSet('all', 'iam-service', 'patient-clinical-service', 'gateway', 'reporting-service', 'backup-service')]
    [string]$Service = 'all'
)

$ErrorActionPreference = 'Stop'
$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$modules = if ($Service -eq 'all') { @('iam-service', 'patient-clinical-service', 'gateway', 'reporting-service', 'backup-service') } else { @($Service) }

function Invoke-MutationCommand {
    param([string]$Name, [string]$File, [string[]]$Arguments, [string]$WorkingDirectory)
    if (-not (Get-Command $File -ErrorAction SilentlyContinue)) { throw "${Name}: comando não encontrado: $File" }
    Push-Location $WorkingDirectory
    try {
        Write-Host "`n> $File $($Arguments -join ' ')" -ForegroundColor Cyan
        & $File @Arguments
        if ($LASTEXITCODE -ne 0) { throw "$Name falhou com código $LASTEXITCODE." }
    } finally { Pop-Location }
}

if ($Scope -in @('all', 'java')) {
    foreach ($module in $modules) {
        $root = Join-Path $RepositoryRoot "backend-java\$module"
        if (-not (Test-Path (Join-Path $root 'pom.xml'))) { throw "pom.xml não encontrado para $module." }
        Invoke-MutationCommand "PIT $module" 'mvn' @('test', 'org.pitest:pitest-maven:mutationCoverage', '-DmutationThreshold=70') $root
    }
}

if ($Scope -in @('all', 'python')) {
    $pythonRoot = Join-Path $RepositoryRoot 'backend'
    Invoke-MutationCommand 'Mutmut run' 'mutmut' @('run') $pythonRoot
    Invoke-MutationCommand 'Mutmut results' 'mutmut' @('results') $pythonRoot
    Invoke-MutationCommand 'Mutmut html' 'mutmut' @('html') $pythonRoot
}

if ($Scope -in @('all', 'frontend')) {
    $frontendRoot = Join-Path $RepositoryRoot 'frontend'
    Invoke-MutationCommand 'Stryker' 'npx' @('stryker', 'run') $frontendRoot
}

Write-Host "`nMutation suite ($Scope) concluída com sucesso." -ForegroundColor Green
