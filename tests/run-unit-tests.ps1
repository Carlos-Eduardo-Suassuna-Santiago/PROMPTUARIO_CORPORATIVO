[CmdletBinding()]
param(
    [ValidateSet('all', 'python', 'java')]
    [string]$Suite = 'all',

    [ValidateSet('all', 'iam-service', 'patient-clinical-service', 'gateway', 'reporting-service', 'backup-service')]
    [string]$Service = 'all',

    [switch]$IncludeIntegration,
    [switch]$Coverage,
    [switch]$FailFast
)

$ErrorActionPreference = 'Stop'
$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$BackendRoot = Join-Path $RepositoryRoot 'backend'
$JavaRoot = Join-Path $RepositoryRoot 'backend-java'

function Invoke-External {
    param([string]$File, [string[]]$Arguments, [string]$WorkingDirectory)

    Push-Location $WorkingDirectory
    try {
        Write-Host ("`n> {0} {1}" -f $File, ($Arguments -join ' ')) -ForegroundColor Cyan
        & $File @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Comando falhou com código ${LASTEXITCODE}: $File"
        }
    }
    finally {
        Pop-Location
    }
}

function Invoke-PythonSuite {
    if (-not (Get-Command pytest -ErrorAction SilentlyContinue)) {
        throw 'pytest não foi encontrado no PATH. Instale backend/requirements-test.txt.'
    }

    if ($Coverage -and -not (pytest --help 2>$null | Select-String '--cov' -Quiet)) {
        throw 'pytest-cov não está instalado. Execute: python -m pip install -r backend/requirements-test.txt'
    }

    # Cada serviço possui um pacote top-level chamado "app". Executá-los em
    # processos/cwd separados evita que o primeiro serviço coletado sombreie os
    # imports dos demais.
    $pythonTargets = @(
        @{ Name = 'backend'; TestPath = (Join-Path $RepositoryRoot 'tests/python/shared'); WorkingDirectory = $BackendRoot },
        @{ Name = 'iam-service'; TestPath = (Join-Path $RepositoryRoot 'tests/python/iam-service'); WorkingDirectory = (Join-Path $BackendRoot 'iam-service') },
        @{ Name = 'patient-clinical-service'; TestPath = (Join-Path $RepositoryRoot 'tests/python/patient-clinical-service'); WorkingDirectory = (Join-Path $BackendRoot 'patient-clinical-service') },
        @{ Name = 'ai-service'; TestPath = (Join-Path $RepositoryRoot 'tests/python/ai-service'); WorkingDirectory = (Join-Path $BackendRoot 'ai-service') },
        @{ Name = 'gateway'; TestPath = (Join-Path $RepositoryRoot 'tests/python/gateway'); WorkingDirectory = (Join-Path $BackendRoot 'gateway') },
        @{ Name = 'reporting-service'; TestPath = (Join-Path $RepositoryRoot 'tests/python/reporting-service'); WorkingDirectory = (Join-Path $BackendRoot 'reporting-service') },
        @{ Name = 'backup'; TestPath = (Join-Path $RepositoryRoot 'tests/python/backup'); WorkingDirectory = (Join-Path $BackendRoot 'backup') }
    )

    $previousDebug = $env:DEBUG
    $previousOtelDisabled = $env:OTEL_SDK_DISABLED
    $env:DEBUG = 'false'
    $env:OTEL_SDK_DISABLED = 'true'
    try {
        foreach ($target in $pythonTargets) {
            if (-not (Test-Path $target.TestPath)) { continue }
            $arguments = @('-q', $target.TestPath)
            if (-not $IncludeIntegration) { $arguments += @('-m', 'not integration') }
            if ($Coverage) { $arguments += @('--cov=.', '--cov-report=term-missing') }
            if ($FailFast) { $arguments += '-x' }
            Write-Host "Executando testes Python de $($target.Name)" -ForegroundColor DarkCyan
            Invoke-External -File 'pytest' -Arguments $arguments -WorkingDirectory $target.WorkingDirectory
        }
    }
    finally {
        $env:DEBUG = $previousDebug
        $env:OTEL_SDK_DISABLED = $previousOtelDisabled
    }
}

function Get-JavaModules {
    $all = @(
        'iam-service',
        'patient-clinical-service',
        'gateway',
        'reporting-service',
        'backup-service'
    )
    if ($Service -eq 'all') { return $all }
    return @($Service)
}

function Invoke-JavaSuite {
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        throw 'mvn não foi encontrado no PATH.'
    }

    foreach ($module in (Get-JavaModules)) {
        $moduleRoot = Join-Path $JavaRoot $module
        if (-not (Test-Path (Join-Path $moduleRoot 'pom.xml'))) {
            Write-Warning "Módulo Java não encontrado: $module"
            continue
        }

        $arguments = @('test', '-DfailIfNoTests=false')
        if ($FailFast) { $arguments += '-Dsurefire.failIfNoSpecifiedTests=false' }

        if (-not $IncludeIntegration) {
            $testFiles = @(Get-ChildItem (Join-Path $moduleRoot 'src/test/java') -Recurse -Filter '*Test.java' -ErrorAction SilentlyContinue)
            $unitTests = @($testFiles | Where-Object {
                $_.FullName -notmatch '[\\/]integration[\\/]' -and $_.BaseName -notmatch 'IntegrationTest$'
            } | ForEach-Object {
                $relative = $_.FullName.Substring((Join-Path $moduleRoot 'src/test/java').Length).TrimStart('\', '/')
                [IO.Path]::ChangeExtension($relative, $null).Replace('\', '.').Replace('/', '.')
            })

            if ($unitTests.Count -eq 0) {
                Write-Host "Sem testes unitários Java em $module; ignorando." -ForegroundColor Yellow
                continue
            }
            $arguments += "-Dtest=$($unitTests -join ',')"
        }

        Invoke-External -File 'mvn' -Arguments $arguments -WorkingDirectory $moduleRoot
    }
}

if ($Suite -in @('all', 'python')) { Invoke-PythonSuite }
if ($Suite -in @('all', 'java')) { Invoke-JavaSuite }

Write-Host "`nSuíte '$Suite' concluída com sucesso." -ForegroundColor Green
