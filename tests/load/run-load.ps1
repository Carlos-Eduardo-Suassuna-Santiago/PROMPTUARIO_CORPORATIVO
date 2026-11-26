[CmdletBinding()]
param(
    [int]$VUs = 10,
    [string]$Duration = '1m'
)

$ErrorActionPreference = 'Stop'
if (-not (Get-Command k6 -ErrorAction SilentlyContinue)) { throw 'k6 não foi encontrado no PATH.' }
if ([string]::IsNullOrWhiteSpace($env:K6_EMAIL) -or [string]::IsNullOrWhiteSpace($env:K6_PASSWORD)) {
    throw 'Defina K6_EMAIL e K6_PASSWORD com credenciais descartáveis de teste.'
}

$script = Join-Path $PSScriptRoot 'promptuario.js'
$arguments = @('run', '--vus', $VUs, '--duration', $Duration, $script)
& k6 @arguments
if ($LASTEXITCODE -ne 0) { throw "k6 falhou com código $LASTEXITCODE." }
