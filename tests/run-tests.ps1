$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$jdk=(Get-ChildItem "$root/.tools/jdk-*" -Directory | Select-Object -First 1).FullName
$out=Join-Path $root ('build/policy-tests-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force $out | Out-Null
& "$jdk/bin/javac.exe" --release 17 -encoding UTF-8 -d $out "$root/src/uk/co/kiera/simringtones/RingtonePolicy.java" "$PSScriptRoot/RingtonePolicyTest.java"
if($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& "$jdk/bin/java.exe" -cp $out uk.co.kiera.simringtones.RingtonePolicyTest
if($LASTEXITCODE -ne 0) { throw 'URI policy tests failed' }
[xml]$manifest=Get-Content -LiteralPath "$root/AndroidManifest.xml"
$ns=New-Object System.Xml.XmlNamespaceManager($manifest.NameTable)
$ns.AddNamespace('a','http://schemas.android.com/apk/res/android')
$permissions=@($manifest.SelectNodes('/manifest/uses-permission/@a:name',$ns) | ForEach-Object Value)
$expected=@('android.permission.READ_PHONE_STATE','android.permission.READ_PHONE_NUMBERS','android.permission.WRITE_SETTINGS')
if(@(Compare-Object $permissions $expected).Count) { throw 'Unexpected permission change' }
if($manifest.SelectSingleNode('/manifest/application/@a:allowBackup',$ns).Value -ne 'false') { throw 'Backup must be disabled' }
if($manifest.SelectNodes('/manifest/application/service|/manifest/application/receiver|/manifest/application/provider').Count) { throw 'Unexpected background or data-sharing component' }
if($manifest.SelectSingleNode('/manifest/application/@a:debuggable',$ns)) { throw 'Do not set debuggable in release manifest' }
Write-Output 'PASS: manifest permission and component contracts'
