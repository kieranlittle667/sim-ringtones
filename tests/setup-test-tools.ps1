$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$jar=Join-Path $root '.tools/json-20250517.jar'
$expected='3ea61b2a06e31edf1c91134fe9106b0ebb16628be169f3db75bc7a2b06b45796'
if(!(Test-Path -LiteralPath $jar)) {
    Invoke-WebRequest -UseBasicParsing 'https://repo.maven.apache.org/maven2/org/json/json/20250517/json-20250517.jar' -OutFile $jar
}
if((Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) { throw 'JSON test library checksum mismatch' }
