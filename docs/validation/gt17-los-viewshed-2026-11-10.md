# gt-17 LOS 옵션·Viewshed 구현 및 검증

검증일: 2026-11-10 (Asia/Seoul). 기준 브랜치: `gt-17`.

이 문서는 최초 구현과 최적화 전 측정 기록이다. 이후 반경별 DEM 부분 읽기와 JAI 행 쓰기를 적용한 결과는 [Viewshed 성능 개선 보고서](gt17-viewshed-optimization-2026-11-10.md)에 정리했다.

## 구현 범위

- GeoTools 모듈과 Eclipse 클래스패스를 17.5로 통일했다. Java 8에서 실행 가능한 Maven 플러그인 버전을 고정했다.
- `scripts/build-gt17.ps1`은 지정한 JDK 8을 현재 빌드 프로세스에만 적용하고 원래 `JAVA_HOME`을 복구한다. Maven 캐시는 저장소의 `bin/m2-gt17`을 사용한다.
- GeoTools 17.5에 없는 `URLs` 참조 3곳을 `DataUtilities`의 파일/URL 변환 API로 변경했다.
- GeoServer 연계 모듈은 2.11.5로 맞췄다. [공식 GeoServer 2.11.5 POM](https://repo.osgeo.org/repository/release/org/geoserver/geoserver/2.11.5/geoserver-2.11.5.pom)의 `gt.version`은 17.5이다.
- 계산과 래스터 출력은 JAI 1.1.3과 JAITools를 사용한다. ImageN 의존성은 추가하지 않았다.

### Linear / Radial Line of Sight

`statistics:LinearLineOfSight`, `statistics:RadialLineOfSight`에 선택적 Boolean `stopAtFirstInvisible`을 추가했다. 기본값은 `false`다.

- 생략/`false`: 기존 계산 결과를 반환한다. 기존 Java 메서드 시그니처도 유지했다.
- `true`: 관측점부터 처음 나타난 비가시 샘플을 포함해 뒤의 모든 샘플을 비가시로 유지한다.
- Radial은 방사선마다 상태를 새로 시작한다. 전체 샘플의 XY 위치와 기존 라인 길이를 보존한다.
- 정규화는 가시성별 라인 병합 전에 수행한다. 피처의 반환 순서에 의존하지 않는다.
- 기존 NoData 건너뛰기, 표고 기울기 동률 판정, 곡률 보정 등은 유지했다. 따라서 이 옵션 추가가 기존 LOS 계산 모델을 바꾸지는 않는다.

```java
RasterLinearLOSProcess.process(dem, observer, 1.6d, target,
        false, false, 0.13d, true, null);
RasterRadialLOSProcess.process(dem, observer, 1.6d, 5000d, 180,
        false, false, 0.13d, true, null);
```

### Viewshed

등록 이름은 `statistics:Viewshed`이다. `RasterViewshedProcess`, Factory, Operation, SPI 등록과 한·영 리소스를 추가했다.

| 입력 | 기본값 / 계약 |
|---|---|
| `inputCoverage` | 필수. CRS가 정의된 DEM. 첫 번째 밴드 사용, 표고 단위 m |
| `observerPoint` | 필수. 비어 있지 않은 단일 Point. CRS는 geometry userData로 지정하거나 DEM CRS 사용 |
| `observerOffset` | 1.6m, 유한한 0 이상 값 |
| `maxDistance` | 0m이면 전체 영역, 양수이면 분석 반경. 유한한 0 이상 값 |
| `useCurvature` | false |
| `useRefraction` | false. true이면 useCurvature=true 필요 |
| `refractionFactor` | 0.13, 0 이상 1 이하 |

출력은 BYTE `GridCoverage2D`이다. `1`은 가시, `0`은 비가시, `255`는 NoData·반경 밖·분석 불가다. 양수 반경에서는 포함되는 셀 중심의 경계 사각형으로 출력 격자를 자르고, 원본 CRS·해상도·회전/전단 변환을 보존한다. 관측 셀은 가시다. 관측점이 DEM 밖이나 NoData에 있으면 명시적으로 오류를 반환한다.

```java
GridCoverage2D result = RasterViewshedProcess.process(dem, observer,
        1.6d, 5000d, false, false, 0.13d, null);
```

## 계산 모델과 GeoAI 구현과의 차이

참조 구현의 실제 계산부는 `opengxt-for-geoai/backend-java/engines/opengxt-raster-engine/.../GeoToolsRasterSurface.java`의 `viewshed` 메서드다. 제공된 `Viewshed.java`는 실행 래퍼다.

1. 시선당 약 100개 지점 제한을 제거했다. DDA로 시선이 내부를 통과하는 모든 DEM 셀을 방문한다. 모서리에만 닿는 셀은 제외한다. 교차 파라미터 허용오차는 `1e-12`다.
2. DEM을 셀별 일정 표고로 해석한다. 관측점에서 대상 셀 중심까지의 시선과 셀 진입·이탈 구간을 검사한다. 관측 셀과 대상 셀 자체는 차폐 검사에서 제외한다. 선 위의 동률은 가시로 판단하고 표고 허용오차는 `1e-9m`로 둔다.
3. 중간 NoData가 있고 알려진 지형으로 차폐를 확정할 수 없으면 `255`다. 다른 유효 셀에서 차폐가 확정되면 `0`이다. 유효 최솟값을 임의로 NoData로 간주하지 않는다.
4. 곡률 보정은 지형과 대상에 같은 기준으로 적용한다. 지구 반지름은 6,378,137m이다. `D`가 대상까지의 거리, `t`가 시선상 비율, `h0`가 관측 높이, `ht`가 대상 표고일 때 차폐 차이는 아래와 같다. 구간 양 끝과 내부 극댓값을 검사한다.

   `q(t) = ground - h0 - (ht - h0) * t + C * t * (1 - t)`

   `C = D² / (2R) * (1 - k)`; 굴절 미사용 시 `k=0`, 곡률 미사용 시 `C=0`.
5. 투영 좌표계는 축 단위를 m로 환산한다. 지리좌표계는 반경이 0인 경우에도 GeodeticCalculator로 대상까지의 거리(m)를 계산한다. 지리좌표계의 시선은 원래 격자에서 직선이며, 중간 곡률 거리는 `D*t`로 근사한다. 큰 지리좌표계 영역·극지·날짜변경선 분석은 적절한 미터 단위 투영 CRS에서 수행하는 편이 적합하다.
6. 기본 관측 높이는 1.6m, 기본 최대 거리는 0m로 통일했다. GeoAI 명세와 래퍼가 서로 달리 정했던 기본값을 명확히 했다. MultiPoint에서 첫 점만 사용하는 동작은 단일 Point 검증으로 바꿨다.
7. GeoTools 17.5 writer는 숫자형 `GC_NODATA`를 내보내기 배경값으로 사용하지 않는다. `NoDataContainer(255)`와 표준 NoData Category를 지정해 GeoTIFF에서 유효한 `0`과 NoData `255`가 구분되도록 했다.

이 검증은 위 계산 모델의 구현 일관성을 확인한다. 실제 지형의 관측 실험이나 다른 도구의 보간 기반 viewshed와의 동일성을 의미하지 않는다. 원본 30m DEM에 없는 지형·건물은 복원하지 않는다.

## 데이터와 실행 조건

입력: `C:\dataset\dataset\seoul_raster\dem30.tif`

- SHA-256: `24dd80186ef55680756d2d90ae33565cef1cbdfe2b53d4e62f011c4b58d1e749`
- 1,235 × 1,010셀, 30 × 30m, 1밴드 UInt16.
- 유효 셀 674,411개. NoData 65535인 셀 572,939개(45.93%). 유효 표고 1–754m. 표고 단위는 사용자 확인으로 m를 적용했다.
- Bessel 기반 TM 원본 WKT를 유지했다. EPSG 코드를 임의로 부여하지 않았다.
- 관측점: `(197626.8424993126, 451398.77749999944)` (DEM CRS), 관측 높이 1.6m.
- Viewshed: 반경 1·5·10km와 전체 영역. 곡률·굴절 false, 계수 0.13.
- Linear: 관측점에서 동쪽으로 1·5·10km. Radial: 같은 반경과 180개 방사선. 옵션 false/true 각각 실행.
- Oracle JDK `1.8.0_202`, Java HotSpot 64-Bit Server VM, 논리 프로세서 8개.
- 반경마다 새 JVM, `-Xms256m -Xmx1024m`. Runtime에서 보고한 최대 힙은 954,728,448바이트(910.5MiB)다.
- 첫 실행과 후속 3회를 기록했다. 아래 시간은 후속 3회 중앙값이다. 각 반복 전 GC 요청과 100ms 대기는 측정에서 제외했다.
- 계산 시간은 입력 파라미터 처리·DEM 픽셀 읽기·계산·출력 Coverage 생성까지 포함한다. JVM 시작·GeoTIFF reader 생성/read 호출·파일 저장·결과 검사·GC 대기는 제외한다. reader.read는 지연 로딩이므로 실제 픽셀 읽기는 계산 시간에 포함된다. 별도 read 시간은 원시 CSV에 있다.
- 힙은 계산 중 2ms 간격과 계산 직후의 관측 최댓값이다. OS RSS·네이티브 메모리·정확한 할당량 측정은 아니다. GC·스케줄링 영향을 받으며 짧은 LOS 실행의 순간 최고값은 놓칠 수 있다.

## 정확도와 회귀 검사

- Java 8의 clean 빌드에서 기존 7개와 새 14개를 합쳐 **21개 테스트 통과**.
- 합성 지형: LOS 재가시/차폐 유지·기본값 호환성·방사선 상태 초기화, 평지/좁은 능선, NoData/NaN, 곡률/굴절, 회전 격자, 피트 단위, 지리좌표계 전체 영역, 잘못된 입력·취소, SPI와 기본값, GeoTIFF 저장·재읽기.
- 무작위 합성 DEM의 792개 대상 셀을 독립적인 셀-사각형 교차 구현과 비교했다.
- 실제 DEM은 Python/Numpy의 셀-사각형 교차 방식으로 검증했다. Java DDA 코드를 재사용하지 않는다. 1km 결과는 전체 셀을, 큰 범위는 고정 시드 1024개 표본과 관측 행·열 전체, 대각선 교차를 검사했다.
- 모든 출력 셀에 대해 값 영역, 원본 NoData 및 반경 밖 처리, 관측 셀 가시성, 격자 위치·해상도·CRS를 확인했다. 각 조건의 4회 출력 SHA-256도 일치했다.

| Viewshed 범위 | 출력 크기 | 독립 대조 셀 수 | 불일치 | 가시 / 비가시 / 255 |
|---|---:|---:|---:|---:|
| 1km | 67 × 67 | 4,489 (전체) | 0 | 1,639 / 1,866 / 984 |
| 5km | 333 × 333 | 1,712 | 0 | 8,397 / 78,856 / 23,636 |
| 10km | 667 × 667 | 2,389 | 0 | 11,362 / 319,207 / 114,320 |
| 전체 | 1,235 × 1,010 | 3,306 | 0 | 19,285 / 655,044 / 573,021 |

합계 **11,896셀 대조, 불일치 0**이다. 큰 범위의 모든 셀을 독립 참조로 전수 대조한 것은 아니다.

전체 영역의 `255` 573,021셀은 원본 NoData 572,939셀과 유효 표고가 있지만 시선 중간 NoData 때문에 판단할 수 없는 82셀로 구성된다.

실제 LOS는 3개 반경 × 180개 방사선의 **85,131개 순서 있는 샘플**에서 최초 비가시 이후 값·XY 좌표를 검사해 불일치가 없었다. 옵션 전후 기하 길이도 유지됐다.

## 처리시간과 사용 힙

최댓값 열은 첫 실행을 포함한 4회 중 관측 최댓값이다. 힙 증가량은 각 실행의 GC 후 기준 힙 대비 증가다.

| Viewshed 범위 | 첫 계산(ms) | 후속 중앙값(ms) | 후속 최소–최대(ms) | 사용 힙 최대(MiB) | 힙 증가 최대(MiB) |
|---|---:|---:|---:|---:|---:|
| 1km | 1,710.628 | 1,801.743 | 1,600.800–2,177.849 | 38.58 | 27.89 |
| 5km | 2,646.560 | 2,118.024 | 1,966.589–2,842.450 | 77.67 | 65.32 |
| 10km | 3,633.048 | 2,988.050 | 2,737.852–3,733.820 | 200.43 | 188.00 |
| 전체 | 4,268.378 | 2,551.937 | 2,536.902–2,837.881 | 265.92 | 252.84 |

| LOS 반경 | Linear false / true 중앙값(ms) | Radial false / true 중앙값(ms) | Radial false / true 사용 힙 최대(MiB) |
|---|---:|---:|---:|
| 1km | 1.932 / 1.703 | 20.896 / 12.662 | 19.92 / 18.58 |
| 5km | 3.063 / 1.317 | 69.862 / 56.372 | 46.25 / 38.01 |
| 10km | 8.807 / 3.025 | 123.027 / 104.273 | 74.42 / 63.15 |

한 작업 PC에서 반복 3회로 얻은 기준값이다. 장기 안정 상태의 처리량이나 다른 하드웨어의 보장값으로 해석하지 않는다. LOS true에서는 병합 후 생성되는 피처 수도 줄어든다. 예를 들어 10km Radial은 1,684개에서 360개로 줄었다.

양수 반경은 정확한 출력 경계를 찾기 위해 원본 격자를 한 번 추가 순회한다. 전체 영역은 이 순회를 생략하므로 10km보다 빠른 결과가 나올 수 있다. 현재는 반경이 작아도 전체 DEM을 double 배열로 읽는다(이 입력은 약 9.52MiB). 임시 할당과 GC 때문에 사용 힙 최대는 이 배열 크기보다 크다.

## 후속 성능 개선 판단

이번 결과를 기준으로 개선 여부를 결정한다. 우선 후보는 투영 CRS에서 반경 창을 먼저 구해 해당 범위만 읽는 방식, JAI 타일/행 writer를 통한 픽셀별 접근 비용 감소다. 시선당 샘플 수를 줄이는 근사화 없이 동일한 출력 체크섬과 독립 대조를 유지하는 것을 통과 조건으로 삼는다. 대형 DEM에서는 배열 크기·입출력·캐시, 지리좌표계에서는 거리 변환 비용을 추가 측정할 필요가 있다.

## 재현 방법과 산출물

저장소 루트의 PowerShell에서 실행한다. Python 검증은 numpy와 rasterio가 필요하다.

```powershell
.\scripts\build-gt17.ps1 -MavenArguments @('clean', 'install')
.\scripts\benchmark-viewshed.ps1 -OutputDirectory .\bin\viewshed-dem-final
python .\scripts\verify-viewshed.py C:\dataset\dataset\seoul_raster\dem30.tif .\bin\viewshed-dem-final
```

- [Viewshed 원시 측정 CSV](gt17-terrain-2026-11-10/viewshed-metrics.csv)
- [LOS 원시 측정 CSV](gt17-terrain-2026-11-10/los-metrics.csv)
- [Viewshed 독립 대조 결과·체크섬](gt17-terrain-2026-11-10/accuracy.json)
- [LOS 샘플 회귀 결과](gt17-terrain-2026-11-10/los-accuracy.json)
- [JVM·관측점·원본 CRS](gt17-terrain-2026-11-10/environment.txt)

결과 GeoTIFF는 `bin/viewshed-dem-final/{1000,5000,10000,0}/viewshed.tif`에 생성한다. 대용량 데이터·Maven 캐시·실행 로그·생성 TIFF는 Git에 포함하지 않는다.

`clean install`과 GeoServer 연계 모듈 `package`가 모두 성공했다. 두 JAR의 모든 클래스가 Java 8 바이트코드(major version 52)인 것도 확인했다.

- `GeoTools/process-spatialstatistics/target/gt-process-spatialstatistics-17.5.jar`
- `GeoServer/gs-wps-spatialstatistics/target/gs-wps-spatialstatistics-2.11.5.jar`

GeoServer는 패키지 빌드까지 확인했다. 실제 서버 배포와 WPS HTTP 호출은 이 검증에 포함하지 않는다.
