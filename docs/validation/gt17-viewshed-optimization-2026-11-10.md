# Viewshed 부분 읽기·JAI 행 쓰기 최적화 검증

검증일: 2026-11-10 (Asia/Seoul). 브랜치 `gt-17`, GeoTools 17.5, JAI 1.1.3, Oracle JDK 1.8.0_202.

반경에 맞춘 DEM 부분 읽기와 JAI 행 반복자를 적용했다. 동일 DEM·관측 조건에서 개선 전후 출력 **1,807,617셀을 전수 비교해 불일치가 없었다**. 독립 계산 대조 11,896셀에서도 불일치가 없었다. Java 8 clean 빌드의 **26개 테스트가 통과**했다.

## 구현

변경 계산부: `GeoTools/process-spatialstatistics/src/main/java/org/geotools/process/spatialstatistics/gridcoverage/RasterViewshedOperation.java`.

1. **투영 좌표계의 반경 후보 영역 계산.** 미터 단위 원을 역 affine 변환해 격자에서의 보수적인 경계 영역을 구한다. 축 단위 환산과 회전·전단을 포함한다. 셀 중심 기준과 반올림 오차를 고려한 여유 셀을 더하고 원본 격자 범위로 제한한다. 이후 기존 `distance.metres` 판정을 그대로 적용해 정확한 출력 경계를 확정한다. 큰 반경의 정수 오버플로도 방지한다.
2. **DEM 부분 읽기.** 출력 경계 사각형만 double 배열로 읽는다. 관측점과 대상 사이의 직선은 이 사각형 안에 있으므로 시선 검사에 필요한 셀이 포함된다. 입력은 범위가 지정된 JAI `RectIter`로 행 순회한다. 원본 TIFF의 실제 읽기는 타일 단위일 수 있다.
3. **JAI 타일·행 쓰기.** 픽셀마다 `DiskMemImage.setSample`을 호출하던 경로를 `WritableRectIter`로 교체했다. 반복자가 행과 타일의 버퍼를 재사용한다. JAI의 `nextPixelDone`·`nextLineDone`을 사용해 타일 경계에서 실제 버퍼가 전환되도록 했다.
4. **판정 좌표 유지.** 배열 인덱스만 읽기 창에 상대적으로 바꿨다. DDA의 관측점·대상 좌표, 시선 교차 파라미터, 곡률 식, 거리 식, 허용오차와 NoData 정책은 기존 격자 좌표 그대로 유지했다.
5. **지리좌표계와 전체 영역.** 지리좌표계는 기존 전체 격자 거리 탐색으로 경계를 결정한 뒤 해당 DEM 창만 읽는다. `maxDistance=0`은 전체 DEM을 읽으며, 입력·출력 행 순회 개선을 적용한다.

새 파라미터나 의존성은 추가하지 않았다. LOS 프로세스의 계산과 옵션 계약도 유지했다.

## 데이터·측정 방법

- DEM: `C:\dataset\dataset\seoul_raster\dem30.tif`, 1,235 × 1,010셀, 30m, 1밴드 UInt16, NoData 65535.
- DEM SHA-256: `24dd80186ef55680756d2d90ae33565cef1cbdfe2b53d4e62f011c4b58d1e749`.
- 원본 Bessel 기반 TM WKT, 수평·표고 단위 m.
- 관측점: `(197626.8424993126, 451398.77749999944)`, 관측 높이 1.6m.
- 반경 1·5·10km 및 전체 영역. 곡률·굴절 false, 계수 0.13.
- Java HotSpot 64-Bit, 논리 프로세서 8개, `-Xms256m -Xmx1024m`, headless.

이전 보고서의 측정값을 그대로 대입하지 않고 **같은 시점에 개선 전후를 재측정**했다. 보존한 개선 전 Operation 소스를 Java 8로 별도 컴파일하고, 동일한 프로세스 클래스·의존성과 벤치마크로 실행했다. 두 버전의 실제 클래스 로딩 위치는 environment 파일에 기록했다.

반경·버전마다 새 JVM에서 첫 계산 1회와 후속 3회를 실행했다(총 32회). 반경마다 버전 실행 순서를 바꿨다: 1km는 개선 전→후, 5km는 후→전, 10km는 전→후, 전체는 후→전. 처리시간 표는 후속 3회 중앙값이다. 이전 측정과 현재 기준값 사이에는 PC 부하와 실행 시점에 따른 차이가 있다.

계산 시간에는 파라미터 처리, 지연 로딩되는 DEM 픽셀 읽기, 분석과 출력 Coverage 생성이 포함된다. JVM 시작, reader 생성/read 호출, GC 요청·대기, 결과 검사와 GeoTIFF 파일 저장은 제외한다. 첫 계산과 reader 시간, 모든 반복값은 원시 CSV에 있다.

사용 힙은 계산 중 2ms 간격과 계산 직후에 관측한 값의 최댓값이다. 아래 힙 표는 첫 계산을 포함한 4회 중 최댓값이며, OS RSS·네이티브 메모리나 정확한 할당량을 뜻하지 않는다. 반복 3회의 단일 PC 기준값이므로 다른 환경의 보장 성능으로 해석하지 않는다.

## 개선 전후 처리시간

| 범위 | 개선 전 중앙값(ms) | 개선 후 중앙값(ms) | 시간 감소 | 속도 배수 |
|---|---:|---:|---:|---:|
| 1km | 1,255.096 | 9.980 | 99.20% | 125.76× |
| 5km | 1,197.536 | 214.459 | 82.09% | 5.58× |
| 10km | 1,927.286 | 888.872 | 53.88% | 2.17× |
| 전체 | 3,869.163 | 1,690.865 | 56.30% | 2.29× |

| 범위 | 개선 전 후속 최소–최대(ms) | 개선 후 후속 최소–최대(ms) | 개선 전 첫 계산(ms) | 개선 후 첫 계산(ms) |
|---|---:|---:|---:|---:|
| 1km | 794.065–1,345.316 | 8.323–15.138 | 974.429 | 127.678 |
| 5km | 1,135.024–1,305.485 | 195.878–258.993 | 1,215.266 | 311.166 |
| 10km | 1,914.013–1,991.894 | 878.619–1,026.933 | 2,169.913 | 1,165.413 |
| 전체 | 3,162.073–5,102.720 | 1,502.903–2,205.557 | 3,745.419 | 1,630.545 |

작은 반경은 전체 격자 탐색과 전체 DEM 읽기를 제거한 효과가 크다. 전체 영역은 읽는 셀 수가 같으며 입력·출력 행 반복자 변경으로 처리시간과 임시 할당 부담이 줄었다. 개별 개선 항목의 기여도를 분리한 프로파일링 수치는 아니다.

## 개선 전후 메모리

| 범위 | 개선 전 사용 힙 최대(MiB) | 개선 후 사용 힙 최대(MiB) | 감소 |
|---|---:|---:|---:|
| 1km | 43.10 | 18.43 | 57.23% |
| 5km | 76.69 | 21.72 | 71.68% |
| 10km | 198.55 | 26.43 | 86.69% |
| 전체 | 273.08 | 39.99 | 85.36% |

DEM 배열 크기는 소스의 전체 1,247,350셀에서 다음과 같이 줄었다. 아래 크기는 double 값 자체이며 객체 헤더·입력 타일 캐시·출력·JVM 고정 비용은 제외한다.

| 범위 | 개선 전 배열 셀 수 | 개선 후 배열 셀 수 | 개선 전 / 후 값 배열(MiB) |
|---|---:|---:|---:|
| 1km | 1,247,350 | 4,489 | 9.517 / 0.034 |
| 5km | 1,247,350 | 110,889 | 9.517 / 0.846 |
| 10km | 1,247,350 | 444,889 | 9.517 / 3.394 |
| 전체 | 1,247,350 | 1,247,350 | 9.517 / 9.517 |

## 결과 일치·정확도

개선 전후 GeoTIFF의 픽셀을 전수 비교했다. CRS, affine 변환, 크기, 자료형, NoData와 색상표도 일치한다. 모든 반복의 픽셀 SHA-256이 같고, 최초 구현 검증 때 보존한 체크섬과도 일치한다.

| 범위 | 전수 비교 셀 수 | 불일치 | 독립 계산 대조 셀 수 | 독립 계산 불일치 |
|---|---:|---:|---:|---:|
| 1km | 4,489 | 0 | 4,489 (전체) | 0 |
| 5km | 110,889 | 0 | 1,712 | 0 |
| 10km | 444,889 | 0 | 2,389 | 0 |
| 전체 | 1,247,350 | 0 | 3,306 | 0 |
| 합계 | **1,807,617** | **0** | **11,896** | **0** |

독립 검증은 Java DDA를 재사용하지 않는 Python/Numpy의 시선-셀 사각형 교차 계산이다. 개선 전·후 모두 실행했다. 큰 범위의 독립 검증은 표본 및 관측 행·열·대각선 검사이며, 개선 전후 직접 비교는 모든 출력 셀을 대상으로 했다.

추가 회귀 검사:

- 회전·전단, 큰 좌표 원점, 영상의 양수·음수 최소 인덱스, 경계 근처의 관측점, 작은 반경에서 부분 읽기 결과와 전체 읽기 결과 비교(24개 합성 격자).
- 출력이 가로·세로 128·256픽셀 타일 경계를 넘는 경우의 전체 값 비교.
- 지리좌표계의 기존 전체 거리 탐색과 부분 배열 적용 결과 비교.
- 피트 단위 투영 좌표계에서의 반경 후보 계산 및 매우 큰 반경의 안전한 범위 제한.
- 513 × 385셀, 64 × 64 타일의 기록용 영상에서 선택 창에 걸치는 **고유 입력 타일 4개**만 읽음을 확인. 정확히 타일 경계에서 끝나는 창도 포함한다.

기존 LOS·Viewshed 검사와 합쳐 26개 테스트가 모두 통과했다. Java 8 clean install이 성공했고 모든 배포 클래스의 바이트코드 major version은 52다. 최종 패키지의 794개 클래스가 실제 측정한 최적화 JAR의 클래스와 바이트 단위로 동일함도 확인했다.

## 재현과 원시 자료

저장소 루트의 PowerShell에서 실행한다. Python 검증에 numpy와 rasterio가 필요하다.

```powershell
.\scripts\build-gt17.ps1 -MavenArguments @('clean', 'install')
.\scripts\compare-viewshed.ps1
python .\scripts\compare-viewshed.py .\bin\viewshed-optimization\comparison
python .\scripts\verify-viewshed.py C:\dataset\dataset\seoul_raster\dem30.tif .\bin\viewshed-optimization\comparison\optimized
python .\scripts\verify-viewshed.py C:\dataset\dataset\seoul_raster\dem30.tif .\bin\viewshed-optimization\comparison\baseline
```

- [개선 전 계산부 소스 스냅샷](gt17-viewshed-optimization-2026-11-10/baseline-RasterViewshedOperation.java.txt): 비교 스크립트가 별도 클래스 경로에 컴파일해 사용한다.
- [실행 순서·소스 및 JAR 해시](gt17-viewshed-optimization-2026-11-10/manifest.json)
- [개선 전후 요약 CSV](gt17-viewshed-optimization-2026-11-10/comparison.csv)
- [전체 32회 원시 측정 CSV](gt17-viewshed-optimization-2026-11-10/all-metrics.csv)
- [픽셀 전수 비교·메타데이터·측정 통계 JSON](gt17-viewshed-optimization-2026-11-10/comparison.json)
- [개선 전 독립 계산 검증](gt17-viewshed-optimization-2026-11-10/baseline-accuracy.json), [개선 후 독립 계산 검증](gt17-viewshed-optimization-2026-11-10/optimized-accuracy.json)
- [개선 전 JVM·클래스 경로](gt17-viewshed-optimization-2026-11-10/baseline-environment.txt), [개선 후 JVM·클래스 경로](gt17-viewshed-optimization-2026-11-10/optimized-environment.txt)

생성 TIFF는 `bin/viewshed-optimization/comparison/{baseline,optimized}/{1000,5000,10000,0}/viewshed.tif`에 있다. 측정한 최적화 JAR의 사본도 같은 비교 폴더의 `optimized-artifact`에 보존했다. 배포용 JAR은 `GeoTools/process-spatialstatistics/target/gt-process-spatialstatistics-17.5.jar`이며 workspace의 Maven 캐시에도 설치했다.

계산 모델의 정확도 범위와 지리좌표계의 시선·곡률 거리 근사 등 기존 제약은 [초기 구현 보고서](gt17-los-viewshed-2026-11-10.md)의 설명을 따른다. 전체 영역은 여전히 전체 double 배열을 사용한다. 지리좌표계의 반경 경계 탐색도 전체 격자 순회가 필요하다.
