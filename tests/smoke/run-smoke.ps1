[CmdletBinding()]
param(
    [string]$HostName = 'localhost',
    [switch]$SkipComposeStatus
)

$ErrorActionPreference = 'Stop'
$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$ComposeRoot = Join-Path $RepositoryRoot 'backend-java'
$failures = [System.Collections.Generic.List[string]]::new()
$telemetryFailures = [System.Collections.Generic.List[string]]::new()

function Add-Failure {
    param([string]$Message, [switch]$Telemetry)
    if ($Telemetry) { $telemetryFailures.Add($Message) } else { $failures.Add($Message) }
}

function Test-HttpEndpoint {
    param([string]$Name, [string]$Uri, [int[]]$ExpectedStatus = @(200), [switch]$Telemetry)
    try {
        $response = Invoke-WebRequest -Uri $Uri -Method Get -TimeoutSec 10 -UseBasicParsing
        if ($response.StatusCode -notin $ExpectedStatus) {
            Add-Failure "$Name retornou HTTP $($response.StatusCode); esperado: $($ExpectedStatus -join ', ')." -Telemetry:$Telemetry
            return
        }
        Write-Host "[OK] $Name -> HTTP $($response.StatusCode)" -ForegroundColor Green
    } catch {
        Add-Failure "$Name indisponível: $($_.Exception.Message)" -Telemetry:$Telemetry
    }
}

function Test-TcpPort {
    param([string]$Name, [int]$Port, [switch]$Telemetry)
    try {
        $result = Test-NetConnection -ComputerName $HostName -Port $Port -WarningAction SilentlyContinue
        if (-not $result.TcpTestSucceeded) {
            Add-Failure "$Name não aceitou conexão TCP em ${HostName}:$Port." -Telemetry:$Telemetry
            return
        }
        Write-Host "[OK] $Name -> TCP $Port" -ForegroundColor Green
    } catch {
        Add-Failure "$Name indisponível: $($_.Exception.Message)" -Telemetry:$Telemetry
    }
}

function Test-ComposeServices {
    Push-Location $ComposeRoot
    try {
        $running = @(docker compose ps --services --filter status=running 2>$null)
        if ($LASTEXITCODE -ne 0) {
            Add-Failure 'Não foi possível consultar o estado do backend-java com docker compose.'
            return
        }
        $required = @('gateway', 'iam-service', 'patient-clinical-service', 'ai-service', 'reporting-service', 'db-iam', 'db-patient-clinical', 'db-reporting', 'db-ai', 'redis', 'rabbitmq', 'minio')
        foreach ($service in $required) {
            if ($running -notcontains $service) { Add-Failure "Serviço Compose não está em execução: $service" }
            else { Write-Host "[OK] Compose -> $service" -ForegroundColor Green }
        }
    } finally { Pop-Location }
}

if (-not $SkipComposeStatus) { Test-ComposeServices }

Test-HttpEndpoint 'Gateway healthz' "http://${HostName}:8000/healthz"
Test-HttpEndpoint 'Gateway healthz/services' "http://${HostName}:8000/healthz/services"
Test-HttpEndpoint 'Gateway actuator health' "http://${HostName}:8000/actuator/health"
Test-HttpEndpoint 'IAM healthz' "http://${HostName}:8001/healthz"
Test-HttpEndpoint 'Patient-clinical healthz' "http://${HostName}:8002/healthz"
Test-HttpEndpoint 'IA healthz' "http://${HostName}:8004/healthz"
Test-HttpEndpoint 'Reporting healthz' "http://${HostName}:8005/healthz"

# PostgreSQL is intentionally internal to the Compose network; its availability
# is validated through the running/healthy database services above and the
# gateway aggregate health check, without exposing database ports on the host.
Test-TcpPort 'RabbitMQ AMQP' 5672
Test-TcpPort 'Redis' 6379
Test-TcpPort 'MinIO API' 9000
Test-HttpEndpoint 'MinIO liveness' "http://${HostName}:9000/minio/health/live"
Test-HttpEndpoint 'RabbitMQ management' "http://${HostName}:15672" -ExpectedStatus @(200, 301, 302)
Test-HttpEndpoint 'Prometheus readiness' "http://${HostName}:9090/-/ready" -Telemetry
Test-HttpEndpoint 'Grafana health' "http://${HostName}:3001/api/health" -Telemetry
Test-HttpEndpoint 'Jaeger UI' "http://${HostName}:16686" -Telemetry

if ($telemetryFailures.Count -gt 0) {
    Write-Warning 'Falhas de OTEL/telemetria detectadas; elas não mascaram os checks funcionais:'
    $telemetryFailures | ForEach-Object { Write-Warning "- $_" }
}
if ($failures.Count -gt 0) {
    Write-Error ("Smoke test funcional falhou:`n" + ($failures -join "`n"))
    exit 1
}
Write-Host 'Smoke test funcional concluído com sucesso.' -ForegroundColor Green
if ($telemetryFailures.Count -gt 0) { exit 2 }
