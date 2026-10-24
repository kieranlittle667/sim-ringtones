$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$jdk=(Get-ChildItem "$root/.tools/jdk-*" -Directory | Select-Object -First 1).FullName
$env:JAVA_HOME=$jdk
$sdk="$root/.tools/platforms-android-37.0/android-37.0/android.jar"
$bt=(Get-ChildItem "$root/.tools/build-tools-37.0.0" -Directory | Select-Object -First 1).FullName
$output="$root/build/tests"
$build=Join-Path $output ("work-"+[guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force $output | Out-Null
New-Item -ItemType Directory -Force "$build/classes","$build/dex" | Out-Null
function Check { if($LASTEXITCODE -ne 0) { throw "Test build failed: $LASTEXITCODE" } }
& "$bt/aapt2.exe" link -o "$build/unsigned.apk" -I $sdk --manifest "$PSScriptRoot/AndroidManifest.xml"
Check
& "$jdk/bin/javac.exe" -encoding UTF-8 --release 17 -classpath $sdk -d "$build/classes" "$PSScriptRoot/DeviceTests.java"
Check
& "$jdk/bin/jar.exe" --create --file "$build/classes.jar" -C "$build/classes" .
Check
& "$bt/d8.bat" --lib $sdk --min-api 37 --output "$build/dex" "$build/classes.jar"
Check
Push-Location "$build/dex"
try { & "$jdk/bin/jar.exe" --update --file "$build/unsigned.apk" classes.dex; Check } finally { Pop-Location }
& "$bt/zipalign.exe" -f -p 4 "$build/unsigned.apk" "$build/aligned.apk"
Check
& "$bt/apksigner.bat" sign --ks "$root/.tools/test-key.p12" --ks-pass pass:android --out "$output/tests.apk" "$build/aligned.apk"
Check
