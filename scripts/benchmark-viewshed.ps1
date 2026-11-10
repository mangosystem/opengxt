param(
    [string]$Dem = 'C:\dataset\dataset\seoul_raster\dem30.tif',
    [string]$OutputDirectory,
    [string]$JavaHome = 'C:\Program Files\Java\jdk1.8.0_202',
    [switch]$LosOnly
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not (Test-Path -LiteralPath $Dem -PathType Leaf)) { throw "DEM not found: $Dem" }
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $projectRoot 'bin\viewshed-dem' }
& (Join-Path $PSScriptRoot 'build-gt17.ps1') -JavaHome $JavaHome -MavenArguments @('test-compile', 'org.apache.maven.plugins:maven-dependency-plugin:3.7.0:build-classpath', '-Dmdep.outputFile=target/benchmark-classpath.txt', '-DincludeScope=test')
$moduleRoot = Join-Path $projectRoot 'GeoTools\process-spatialstatistics'
$dependencies = (Get-Content (Join-Path $moduleRoot 'target\benchmark-classpath.txt') -Raw).Trim()
$classPath = "$moduleRoot\target\test-classes;$moduleRoot\target\classes;$dependencies"
if (-not $LosOnly) {
foreach ($radius in @(1000, 5000, 10000, 0)) {
    $caseDirectory = Join-Path $OutputDirectory "$radius"
    & (Join-Path $JavaHome 'bin\java.exe') -Xms256m -Xmx1024m '-Djava.awt.headless=true' -cp $classPath org.geotools.process.spatialstatistics.ViewshedDemBenchmark $Dem $caseDirectory $radius
    if ($LASTEXITCODE -ne 0) { throw "Benchmark failed at radius $radius" }
}
}
foreach ($radius in @(1000, 5000, 10000)) {
    $caseDirectory = Join-Path $OutputDirectory "los\$radius"
    & (Join-Path $JavaHome 'bin\java.exe') -Xms256m -Xmx1024m '-Djava.awt.headless=true' -cp $classPath org.geotools.process.spatialstatistics.LOSDemBenchmark $Dem $caseDirectory $radius
    if ($LASTEXITCODE -ne 0) { throw "LOS benchmark failed at radius $radius" }
}
