param(
    [string]$JavaHome = 'C:\Program Files\Java\jdk1.8.0_202',
    [string[]]$MavenArguments = @('verify')
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$javaVersion = & (Join-Path $JavaHome 'bin\javac.exe') -version 2>&1
if ("$javaVersion" -notmatch 'javac 1\.8\.') {
    throw "Java 8 JDK required: $javaVersion"
}
$previousJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = $JavaHome
    & mvn -B "-Dmaven.repo.local=$projectRoot\bin\m2-gt17" -f "$projectRoot\GeoTools\process-spatialstatistics\pom.xml" @MavenArguments
    if ($LASTEXITCODE -ne 0) { throw "Maven failed with exit code $LASTEXITCODE" }
} finally {
    $env:JAVA_HOME = $previousJavaHome
}
