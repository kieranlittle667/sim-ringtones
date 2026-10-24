[CmdletBinding()]
param([switch]$Release, [string]$KeyStore=$env:SIM_RINGTONES_KEYSTORE, [string]$KeyAlias='simringtones')
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$jdk=(Get-ChildItem "$root/.tools/jdk-*" -Directory | Select-Object -First 1).FullName
if(!$jdk) { throw 'Run setup-tools.ps1 first.' }
$env:JAVA_HOME=$jdk
$env:PATH="$jdk/bin;"+$env:PATH
$sdk="$root/.tools/platforms-android-37.0/android-37.0/android.jar"
$bt=(Get-ChildItem "$root/.tools/build-tools-37.0.0" -Directory | Select-Object -First 1).FullName
$output=Join-Path $root 'build'
$build=Join-Path $output ('work-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force "$build/classes","$build/dex",$output | Out-Null
function Check { if($LASTEXITCODE -ne 0) { throw "Build command failed: $LASTEXITCODE" } }
if($Release) {
    if(!$KeyStore -or !(Test-Path -LiteralPath $KeyStore) -or !$env:SIM_RINGTONES_STORE_PASSWORD) {
        throw 'Release requires an existing private keystore and SIM_RINGTONES_STORE_PASSWORD. No development-key fallback.'
    }
    $password='env:SIM_RINGTONES_STORE_PASSWORD'
    [xml]$manifest=Get-Content -LiteralPath (Join-Path $root 'AndroidManifest.xml')
    $version=$manifest.manifest.GetAttribute('versionName','http://schemas.android.com/apk/res/android')
    if($version -notmatch '^\d+\.\d+\.\d+$') { throw 'Invalid release version' }
    $apk=Join-Path $output ('sim-ringtones-'+$version+'.apk')
} else {
    $KeyStore=Join-Path $root '.tools/test-key.p12'
    $password='pass:android'
    $apk=Join-Path $output 'sim-ringtones.apk'
    if(!(Test-Path -LiteralPath $KeyStore)) {
        & "$jdk/bin/keytool.exe" -genkeypair -keystore $KeyStore -storepass android -keypass android -alias simringtones -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=SIM Ringtones Development'
        Check
    }
}
& "$bt/aapt2.exe" compile --dir "$root/res" -o "$build/res.zip"
Check
& "$bt/aapt2.exe" link -o "$build/unsigned.apk" -I $sdk --manifest "$root/AndroidManifest.xml" --auto-add-overlay "$build/res.zip"
Check
$sources=Get-ChildItem "$root/src" -Recurse -Filter *.java | ForEach-Object FullName
& "$jdk/bin/javac.exe" -encoding UTF-8 --release 17 -classpath $sdk -d "$build/classes" $sources
Check
& "$jdk/bin/jar.exe" --create --file "$build/classes.jar" -C "$build/classes" .
Check
& "$bt/d8.bat" --lib $sdk --min-api 37 --output "$build/dex" "$build/classes.jar"
Check
Push-Location "$build/dex"
try { & "$jdk/bin/jar.exe" --update --file "$build/unsigned.apk" classes.dex; Check } finally { Pop-Location }
& "$bt/zipalign.exe" -f -p 4 "$build/unsigned.apk" "$build/aligned.apk"
Check
& "$bt/apksigner.bat" sign --ks $KeyStore --ks-key-alias $KeyAlias --ks-pass $password --key-pass $password --out $apk "$build/aligned.apk"
Check
& "$bt/apksigner.bat" verify --verbose --print-certs $apk
Check
& "$bt/zipalign.exe" -c -p 4 $apk
Check
Get-FileHash $apk -Algorithm SHA256
