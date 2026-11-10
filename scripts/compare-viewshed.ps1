param(
    [string]$Dem = 'C:\dataset\dataset\seoul_raster\dem30.tif',
    [string]$OutputDirectory,
    [string]$JavaHome = 'C:\Program Files\Java\jdk1.8.0_202'
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not (Test-Path -LiteralPath $Dem -PathType Leaf)) { throw "DEM not found: $Dem" }
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $projectRoot 'bin\viewshed-optimization\comparison' }
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

& (Join-Path $PSScriptRoot 'build-gt17.ps1') -JavaHome $JavaHome -MavenArguments @('package', '-DskipTests', 'org.apache.maven.plugins:maven-dependency-plugin:3.7.0:build-classpath', '-Dmdep.outputFile=target/benchmark-classpath.txt', '-DincludeScope=test')
$moduleRoot = Join-Path $projectRoot 'GeoTools\process-spatialstatistics'
$dependencies = (Get-Content (Join-Path $moduleRoot 'target\benchmark-classpath.txt') -Raw).Trim()
$jar = Join-Path $moduleRoot 'target\gt-process-spatialstatistics-17.5.jar'
$artifactDirectory = Join-Path $OutputDirectory 'optimized-artifact'
New-Item -ItemType Directory -Path $artifactDirectory -Force | Out-Null
$measuredJar = Join-Path $artifactDirectory 'gt-process-spatialstatistics-17.5.jar'
Copy-Item -LiteralPath $jar -Destination $measuredJar
$jar = $measuredJar
$baselineFixture = Join-Path $projectRoot 'docs\validation\gt17-viewshed-optimization-2026-11-10\baseline-RasterViewshedOperation.java.txt'
$baselineBuild = Join-Path $OutputDirectory 'baseline-build'
$baselineClasses = Join-Path $baselineBuild 'classes'
New-Item -ItemType Directory -Path $baselineClasses -Force | Out-Null
$source = Join-Path $baselineBuild 'RasterViewshedOperation.java'
Copy-Item -LiteralPath $baselineFixture -Destination $source
& (Join-Path $JavaHome 'bin\javac.exe') -encoding UTF-8 -source 1.8 -target 1.8 -cp "$jar;$dependencies" -d $baselineClasses $source
if ($LASTEXITCODE -ne 0) { throw 'Baseline Java 8 compilation failed' }
$testClasses = Join-Path $moduleRoot 'target\test-classes'
$classPaths = @{
    baseline = "$testClasses;$baselineClasses;$jar;$dependencies"
    optimized = "$testClasses;$jar;$dependencies"
}
$manifest = [ordered]@{
    dem = (Resolve-Path -LiteralPath $Dem).Path
    dem_sha256 = (Get-FileHash -LiteralPath $Dem -Algorithm SHA256).Hash.ToLowerInvariant()
    baseline_source_sha256 = (Get-FileHash -LiteralPath $baselineFixture -Algorithm SHA256).Hash.ToLowerInvariant()
    optimized_jar_sha256 = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant()
    optimized_jar_snapshot = $jar
    java_home = $JavaHome
    jvm_arguments = @('-Xms256m', '-Xmx1024m', '-Djava.awt.headless=true')
    started_at = (Get-Date).ToString('o')
    order = @()
}
$caseIndex = 0
foreach ($radius in @(1000, 5000, 10000, 0)) {
    # Alternate execution order to reduce a systematic first/second run bias.
    $variants = if ($caseIndex % 2 -eq 0) { @('baseline', 'optimized') } else { @('optimized', 'baseline') }
    foreach ($variant in $variants) {
        $caseDirectory = Join-Path $OutputDirectory "$variant\$radius"
        New-Item -ItemType Directory -Path $caseDirectory -Force | Out-Null
        $manifest.order += "$variant/$radius"
        & (Join-Path $JavaHome 'bin\java.exe') -Xms256m -Xmx1024m '-Djava.awt.headless=true' -cp $classPaths[$variant] org.geotools.process.spatialstatistics.ViewshedDemBenchmark $Dem $caseDirectory $radius *> (Join-Path $caseDirectory 'run.log')
        if ($LASTEXITCODE -ne 0) {
            Get-Content (Join-Path $caseDirectory 'run.log') -Tail 25
            throw "Viewshed comparison failed: $variant/$radius"
        }
        Write-Output "Completed $variant/$radius"
    }
    $caseIndex++
}
$manifest['finished_at'] = (Get-Date).ToString('o')
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $OutputDirectory 'manifest.json') -Encoding utf8
