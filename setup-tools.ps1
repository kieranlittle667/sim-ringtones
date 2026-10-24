$ErrorActionPreference='Stop'
$toolRoot=Join-Path $PSScriptRoot '.tools'
New-Item -ItemType Directory -Force $toolRoot | Out-Null
if(!(Get-ChildItem "$toolRoot/jdk-*" -Directory -ErrorAction SilentlyContinue)) {
    $jdk=Invoke-RestMethod 'https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse'
    $pkg=$jdk[0].binary.package
    $zip=Join-Path $toolRoot 'jdk.zip'
    Invoke-WebRequest -UseBasicParsing $pkg.link -OutFile $zip
    if((Get-FileHash $zip -Algorithm SHA256).Hash.ToLowerInvariant() -ne $pkg.checksum) { throw 'JDK checksum mismatch' }
    tar -xf $zip -C $toolRoot
    if($LASTEXITCODE -ne 0) { throw 'JDK extraction failed' }
}
[xml]$repo=(Invoke-WebRequest -UseBasicParsing 'https://dl.google.com/android/repository/repository2-3.xml').Content
$repo.Save((Join-Path $toolRoot 'repository.xml'))
foreach($path in @('platforms;android-37.0','build-tools;37.0.0')) {
    $dest=Join-Path $toolRoot ($path -replace ';','-')
    if(Test-Path $dest) { continue }
    $package=$repo.SelectNodes('//*[local-name()="remotePackage"]') | Where-Object path -eq $path
    if(!$package) { throw "SDK package unavailable: $path" }
    $archive=$package.archives.archive | Where-Object { !$_.'host-os' -or $_.'host-os' -eq 'windows' } | Select-Object -First 1
    $zip=Join-Path $toolRoot $archive.complete.url
    Invoke-WebRequest -UseBasicParsing ('https://dl.google.com/android/repository/'+$archive.complete.url) -OutFile $zip
    if((Get-FileHash $zip -Algorithm SHA1).Hash.ToLowerInvariant() -ne $archive.complete.checksum.InnerText) { throw "SDK checksum mismatch: $path" }
    New-Item -ItemType Directory -Force $dest | Out-Null
    tar -xf $zip -C $dest
    if($LASTEXITCODE -ne 0) { throw "SDK extraction failed: $path" }
    "Ready: $path"
}
