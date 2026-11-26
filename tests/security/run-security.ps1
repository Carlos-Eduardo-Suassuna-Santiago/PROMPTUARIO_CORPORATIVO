[CmdletBinding()]
param(
    [ValidateSet('all', 'dependencies', 'api')]
    [string]$Scope = 'all',
    [string]$NewmanPassword,
    [switch]$ContinueOnError
)

$ErrorActionPreference = 'Stop'
$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$failures = [System.Collections.Generic.List[string]]::new()

function Invoke-SecurityCommand {
    param([string]$Name, [string]$File, [string[]]$Arguments, [string]$WorkingDirectory)
    if (-not (Get-Command $File -ErrorAction SilentlyContinue)) {
        $failures.Add("${Name}: comando não encontrado: $File")
        return
    }
    Push-Location $WorkingDirectory
    try {
        Write-Host "`n> $File $($Arguments -join ' ')" -ForegroundColor Cyan
        & $File @Arguments
        if ($LASTEXITCODE -ne 0) {
            $failures.Add("$Name falhou com código $LASTEXITCODE")
            if (-not $ContinueOnError) { throw $failures[-1] }
        }
    } finally { Pop-Location }
}

if ($Scope -in @('all', 'dependencies')) {
    Invoke-SecurityCommand 'Frontend npm audit' 'npm' @('audit', '--audit-level=high') (Join-Path $RepositoryRoot 'frontend')
    Invoke-SecurityCommand 'Python pip-audit' 'pip-audit' @() (Join-Path $RepositoryRoot 'backend')
    Invoke-SecurityCommand 'Python bandit' 'bandit' @('-r', '.') (Join-Path $RepositoryRoot 'backend')

    foreach ($service in @('iam-service', 'patient-clinical-service', 'gateway', 'reporting-service', 'backup-service')) {
        $serviceRoot = Join-Path $RepositoryRoot "backend-java\$service"
        if (Test-Path (Join-Path $serviceRoot 'pom.xml')) {
            Invoke-SecurityCommand "Java dependency-check ($service)" 'mvn' @('org.owasp:dependency-check-maven:check') $serviceRoot
        }
    }
}

if ($Scope -in @('all', 'api')) {
    if ([string]::IsNullOrWhiteSpace($NewmanPassword)) {
        Write-Warning 'API security Newman não executado: informe -NewmanPassword com uma credencial descartável de teste.'
    } else {
        $newman = Join-Path $RepositoryRoot 'tests\newman\run-newman.ps1'
        & $newman -Password $NewmanPassword
        if ($LASTEXITCODE -ne 0) {
            $failures.Add("Newman security falhou com código $LASTEXITCODE")
            if (-not $ContinueOnError) { throw $failures[-1] }
        }
    }
}

if ($failures.Count -gt 0) {
    Write-Error ("Security suite falhou:`n" + ($failures -join "`n"))
    exit 1
}
Write-Host 'Security suite concluída sem falhas detectadas.' -ForegroundColor Green
