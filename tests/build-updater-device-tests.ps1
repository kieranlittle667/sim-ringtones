# Creates a separate application ID. Never installs or modifies the normal app.
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$sandbox=Join-Path $root ('build/updater-device-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force $sandbox | Out-Null
foreach($item in @('src','res','tests','AndroidManifest.xml','build.ps1')) { Copy-Item -LiteralPath (Join-Path $root $item) -Destination $sandbox -Recurse }
New-Item -ItemType Junction -Path (Join-Path $sandbox '.tools') -Target (Join-Path $root '.tools') | Out-Null
# Isolate source and manifest names, including the provider authority and test target.
Get-ChildItem $sandbox -Recurse -File | Where-Object { $_.Extension -in @('.java','.xml') -and $_.FullName -notlike '*\.tools\*' } | ForEach-Object {
    $text=[IO.File]::ReadAllText($_.FullName).Replace('uk.co.kiera.simringtones','uk.co.kiera.simringtones.updatertest')
    [IO.File]::WriteAllText($_.FullName,$text)
}
$manifest=Join-Path $sandbox 'AndroidManifest.xml'
$base=[IO.File]::ReadAllText($manifest).Replace('android:label="SIM Ringtones"','android:label="SIM Ringtones Updater Test"')
[IO.File]::WriteAllText($manifest,$base)
& (Join-Path $sandbox 'build.ps1')
$fixtures=Join-Path $sandbox 'build/fixtures'
New-Item -ItemType Directory -Force $fixtures | Out-Null
Copy-Item (Join-Path $sandbox 'build/sim-ringtones.apk') (Join-Path $fixtures 'older.apk')
# The update fixture has a newer version and the same signing key.
[IO.File]::WriteAllText($manifest,$base.Replace('versionCode="3"','versionCode="4"').Replace('versionName="0.3.0"','versionName="0.3.1"'))
& (Join-Path $sandbox 'build.ps1')
Copy-Item (Join-Path $sandbox 'build/sim-ringtones.apk') (Join-Path $fixtures 'valid.apk')
$jdk=(Get-ChildItem "$root/.tools/jdk-*" -Directory | Select-Object -First 1).FullName
$env:JAVA_HOME=$jdk
$wrongKey=Join-Path $root '.tools/updater-negative-test-key.p12'
if(!(Test-Path $wrongKey)) {
    & "$jdk/bin/keytool.exe" -genkeypair -storetype PKCS12 -keystore $wrongKey -storepass android -keypass android -alias simringtones -keyalg RSA -keysize 3072 -validity 3650 -dname 'CN=Updater Negative Test Only'
    if($LASTEXITCODE -ne 0) { throw 'Negative test key generation failed' }
}
$oldPassword=$env:SIM_RINGTONES_STORE_PASSWORD
try {
    $env:SIM_RINGTONES_STORE_PASSWORD='android'
    & (Join-Path $sandbox 'build.ps1') -Release -KeyStore $wrongKey
} finally { $env:SIM_RINGTONES_STORE_PASSWORD=$oldPassword }
Copy-Item (Join-Path $sandbox 'build/sim-ringtones-0.3.1.apk') (Join-Path $fixtures 'wrong-key.apk')
# Restore the base app; a public APK provides the wrong-package fixture.
[IO.File]::WriteAllText($manifest,$base)
& (Join-Path $sandbox 'build.ps1')
& (Join-Path $root 'build.ps1')
Copy-Item (Join-Path $root 'build/sim-ringtones.apk') (Join-Path $fixtures 'wrong-package.apk')
$bt=(Get-ChildItem "$root/.tools/build-tools-37.0.0" -Directory | Select-Object -First 1).FullName
$sdk=Join-Path $root '.tools/platforms-android-37.0/android-37.0/android.jar'
$test=Join-Path $sandbox 'build/updater-tests'
New-Item -ItemType Directory -Force "$test/classes","$test/dex" | Out-Null
function Check { if($LASTEXITCODE -ne 0) { throw 'Updater device test build failed' } }
$testManifest=Join-Path $sandbox 'tests/AndroidManifest.xml'
# Build the isolated test APK with the updater runner and embedded fixtures.
[IO.File]::WriteAllText($testManifest,[IO.File]::ReadAllText($testManifest).Replace('android:name=".DeviceTests"','android:name=".UpdaterDeviceTests"'))
& "$bt/aapt2.exe" link -o "$test/unsigned.apk" -I $sdk --manifest $testManifest -A $fixtures
Check
& "$jdk/bin/javac.exe" --release 17 -encoding UTF-8 -classpath $sdk -d "$test/classes" "$sandbox/tests/UpdaterDeviceTests.java"
Check
& "$jdk/bin/jar.exe" --create --file "$test/classes.jar" -C "$test/classes" .
Check
& "$bt/d8.bat" --lib $sdk --min-api 37 --output "$test/dex" "$test/classes.jar"
Check
Push-Location "$test/dex"
try { & "$jdk/bin/jar.exe" --update --file "$test/unsigned.apk" classes.dex; Check } finally { Pop-Location }
& "$bt/zipalign.exe" -f -p 4 "$test/unsigned.apk" "$test/aligned.apk"
Check
& "$bt/apksigner.bat" sign --ks "$root/.tools/test-key.p12" --ks-pass pass:android --out "$test/updater-tests.apk" "$test/aligned.apk"
Check
@{app=(Join-Path $sandbox 'build/sim-ringtones.apk');tests=(Join-Path $test 'updater-tests.apk');sandbox=$sandbox} | ConvertTo-Json | Set-Content (Join-Path $root 'build/updater-device-paths.json')
Write-Output 'Built isolated updater test app and fixtures; see build/updater-device-paths.json.'
