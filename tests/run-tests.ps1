$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$jdk=(Get-ChildItem "$root/.tools/jdk-*" -Directory | Select-Object -First 1).FullName
$out=Join-Path $root ('build/policy-tests-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force $out | Out-Null
& "$jdk/bin/javac.exe" --release 17 -encoding UTF-8 -d $out "$root/src/uk/co/kiera/simringtones/RingtonePolicy.java" "$PSScriptRoot/RingtonePolicyTest.java" "$root/src/uk/co/kiera/simringtones/UpdatePolicy.java" "$PSScriptRoot/UpdatePolicyTest.java"
if($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& "$jdk/bin/java.exe" -cp $out uk.co.kiera.simringtones.RingtonePolicyTest
if($LASTEXITCODE -ne 0) { throw 'URI policy tests failed' }
& "$jdk/bin/java.exe" -cp $out uk.co.kiera.simringtones.UpdatePolicyTest
if($LASTEXITCODE -ne 0) { throw 'Updater policy tests failed' }
[xml]$manifest=Get-Content -LiteralPath "$root/AndroidManifest.xml"
$ns=New-Object System.Xml.XmlNamespaceManager($manifest.NameTable)
$ns.AddNamespace('a','http://schemas.android.com/apk/res/android')
$permissions=@($manifest.SelectNodes('/manifest/uses-permission/@a:name',$ns) | ForEach-Object Value)
$expected=@('android.permission.READ_PHONE_STATE','android.permission.READ_PHONE_NUMBERS','android.permission.WRITE_SETTINGS','android.permission.INTERNET','android.permission.REQUEST_INSTALL_PACKAGES')
if(@(Compare-Object $permissions $expected).Count) { throw 'Unexpected permission change' }
if($manifest.SelectSingleNode('/manifest/application/@a:allowBackup',$ns).Value -ne 'false') { throw 'Backup must be disabled' }
if($manifest.SelectNodes('/manifest/application/service|/manifest/application/receiver').Count) { throw 'Unexpected background component' }
$providers=$manifest.SelectNodes('/manifest/application/provider')
if($providers.Count -ne 1 -or $providers[0].GetAttribute('name','http://schemas.android.com/apk/res/android') -ne '.UpdateFileProvider' -or $providers[0].GetAttribute('exported','http://schemas.android.com/apk/res/android') -ne 'false') { throw 'Unexpected exported data provider' }
if($providers[0].GetAttribute('grantUriPermissions','http://schemas.android.com/apk/res/android') -ne 'false' -or $providers[0].SelectNodes('grant-uri-permission').Count -ne 1 -or $providers[0].SelectSingleNode('grant-uri-permission').GetAttribute('path','http://schemas.android.com/apk/res/android') -ne '/update.apk') { throw 'APK grant must be limited to the update URI' }
if($manifest.SelectSingleNode('/manifest/application/@a:usesCleartextTraffic',$ns).Value -ne 'false') { throw 'Cleartext traffic must be disabled' }
if($manifest.SelectSingleNode('/manifest/application/@a:debuggable',$ns)) { throw 'Do not set debuggable in release manifest' }
Write-Output 'PASS: manifest permission and component contracts'

& "$PSScriptRoot/setup-test-tools.ps1"
$sdk=Join-Path $root '.tools/platforms-android-37.0/android-37.0/android.jar'
$json=Join-Path $root '.tools/json-20250517.jar'
& "$jdk/bin/javac.exe" --release 17 -encoding UTF-8 -classpath "$json;$sdk;$out" -d $out "$root/src/uk/co/kiera/simringtones/UpdateClient.java" "$PSScriptRoot/ReleaseMetadataTest.java"
if($LASTEXITCODE -ne 0) { throw 'Release metadata test compilation failed' }
& "$jdk/bin/java.exe" -cp "$json;$out;$sdk" uk.co.kiera.simringtones.ReleaseMetadataTest
if($LASTEXITCODE -ne 0) { throw 'Release metadata tests failed' }
