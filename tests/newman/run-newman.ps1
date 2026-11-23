param(
  [string]$Email = 'admin@promptuario.health',
  [Parameter(Mandatory = $true)]
  [string]$Password
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Password) -or $Password -eq 'SUA_SENHA') {
  throw 'Informe a senha real do usuário Newman. Exemplo: .\tests\newman\run-newman.ps1 -Password "Admin@12345"'
}
$collection = Join-Path $PSScriptRoot 'promptuario.postman_collection.json'
$environment = Join-Path $PSScriptRoot 'promptuario.postman_environment.json'
$results = Join-Path $PSScriptRoot 'results'
New-Item -ItemType Directory -Force -Path $results | Out-Null

npx --yes newman run $collection `
  -e $environment `
  --env-var "email=$Email" `
  --env-var "password=$Password" `
  --bail `
  --reporters cli,junit `
  --reporter-junit-export (Join-Path $results 'newman.xml')

if ($LASTEXITCODE -ne 0) {
  throw "Newman falhou com código $LASTEXITCODE"
}
