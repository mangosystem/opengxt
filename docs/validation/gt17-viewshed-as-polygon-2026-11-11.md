# ViewshedAsPolygon 프로세스

검증일: 2026-11-11 (Asia/Seoul). 브랜치 `gt-17`, GeoTools 17.5·JAI·Java 8.

`statistics:ViewshedAsPolygon`은 기존 Viewshed의 DEM·관측점 입력으로 가시권을 계산한 뒤 `SimpleFeatureCollection`을 반환한다. 기존 최적화된 Viewshed 계산을 재사용하며, 같은 값으로 변이 연결된 셀을 영역별 피처로 묶는다.

## 입력·출력

입력은 `statistics:Viewshed`와 동일하다. Factory에서 기존 Parameter 정의를 공유한다.

| 입력 | 기본값·계약 |
|---|---|
| `inputCoverage` | 필수 DEM. 첫 밴드, 표고 m, CRS 필요 |
| `observerPoint` | 필수 단일 Point. DEM CRS 또는 geometry userData로 입력 CRS 지정 |
| `observerOffset` | 1.6m, 유한한 0 이상 값 |
| `maxDistance` | 0이면 전체 영역, 양수이면 반경(m) |
| `useCurvature` | false |
| `useRefraction` | false. true이면 useCurvature=true 필요 |
| `refractionFactor` | 0.13, 0 이상 1 이하 |

결과 키는 `result`, 타입은 `SimpleFeatureCollection`이다.

| 필드 | 타입 | 의미 |
|---|---|---|
| `geom` | MultiPolygon | 연결 영역 하나를 담은 폴리곤. 기존 WPS/GML 관례에 맞춰 MultiPolygon으로 감싼다 |
| `Visible` | Integer | 1 가시, 0 비가시 |
| `area` | Double | 피처 면적. 출력 CRS 단위의 제곱 |

NoData·분석 불가·반경 밖인 `255`는 제외한다. 값 `0`은 유효한 비가시 영역으로 보존한다. 같은 값이라도 모서리에만 닿으면 별도 피처다. 영역 전체를 가시성별 단일 피처로 dissolve하지 않는다.

원본 셀 경계를 유지하고 작은 한 셀 영역도 보존한다. 일직선 위의 중간 꼭짓점만 제거하며 Douglas–Peucker 등의 경계 단순화는 적용하지 않는다. NoData 구멍도 보존한다. 미터 단위 투영 CRS의 `area`는 m²이고, 지리좌표계에서는 각도 단위의 제곱이므로 지표 면적(m²)으로 해석하면 안 된다.

## 사용 예

DEM에서 바로 폴리곤 결과를 얻는 경우:

```java
SimpleFeatureCollection polygons = RasterViewshedAsPolygonProcess.process(
        dem, observerPoint, 1.6d, 5000d, false, false, 0.13d, null);
```

이미 계산한 Viewshed `GridCoverage2D`를 변환하는 경우:

```java
SimpleFeatureCollection polygons = new RasterViewshedAsPolygonOperation()
        .execute(viewshedCoverage, null);
```

클래스 위치:

- `org.geotools.process.spatialstatistics.RasterViewshedAsPolygonProcess`
- `org.geotools.process.spatialstatistics.RasterViewshedAsPolygonProcessFactory`
- `org.geotools.process.spatialstatistics.gridcoverage.RasterViewshedAsPolygonOperation`

프로세스 SPI와 한·영 설명 리소스를 추가했다. 별도 GeoServer PPIO는 추가하지 않고 기존 SimpleFeatureCollection 출력 경로를 사용한다.

## 구현 시 확인 사항

일반 `RasterToPolygonOperation`은 입력이 BYTE일 때 제안 NoData 값 `0`도 제외한다. 가시권의 비가시 영역을 유지하기 위해 새 변환기는 JAITools Vectorize에 제외 값으로 `255`만 명시한다. 가시성별 내부 경계를 유지하고 작은 영역 제거 임계값을 0으로 지정한다.

JAITools 1.4의 Vectorize는 영상의 최소 인덱스가 0이 아닐 때 오류가 발생할 수 있다. 이 경우에만 픽셀 배열을 복사하거나 재표본화하지 않는 정수 좌표 Raster 뷰로 원점을 옮기고, 대응하는 affine 변환을 적용한다. 일반 Viewshed 결과의 영상 원점은 0이므로 이 보정은 별도 래스터를 직접 변환할 때 주로 사용된다.

프로세스는 자신이 만든 중간 Viewshed 래스터를 `finally`에서 해제한다. 변환 Operation은 호출자가 전달한 래스터를 해제하지 않는다. 폴리곤은 메모리에 완전히 생성되어 반환되므로 중간 래스터의 수명에 의존하지 않는다. 취소는 계산 단계와 폴리곤 생성 전후 및 피처 반복에서 확인한다. JAI 내부 경계 추적 한 번을 진행하는 동안에는 별도 취소 콜백이 없다.

## 검증

Java 8 clean install과 테스트 **34개 통과**, 실패·오류·스킵 0개.

추가 테스트는 `ViewshedAsPolygonProcessTest`의 8개다. 단독 빌드에서는 외부 DEM 테스트 한 개를 선택적으로 건너뛰며 아래 명령으로 DEM 경로를 주면 모두 실행한다.

```powershell
.\scripts\build-gt17.ps1 -MavenArguments @('clean', 'install', '-Dviewshed.dem=C:\dataset\dataset\seoul_raster\dem30.tif')
```

합성 래스터에서 0 보존, 255 제외와 구멍, 모서리 접촉, 한 셀 영역, 빈 결과의 스키마, 회전·전단 및 이동된 영상 원점, 기본 파라미터·SPI·진행률·취소와 잘못된 입력을 검사했다.

실제 DEM은 기존과 같은 `dem30.tif`, 관측점 `(197626.8424993126, 451398.77749999944)`, 높이 1.6m, 곡률·굴절 false를 사용했다. DEM SHA-256은 `24dd80186ef55680756d2d90ae33565cef1cbdfe2b53d4e62f011c4b58d1e749`이다.

| 범위 | 피처 수 | 셀 중심 전수 대조 | 불일치 |
|---|---:|---:|---:|
| 1km | 83 | 4,489 | 0 |
| 5km | 162 | 110,889 | 0 |
| 10km | 211 | 444,889 | 0 |
| 전체 | 271 | 1,247,350 | 0 |
| 합계 | | **1,807,617** | **0** |

검사 내용:

- 래스터의 유효 셀 중심은 동일한 Visible 값을 가진 피처 하나에만 포함된다.
- 값 255의 셀 중심은 어떤 피처에도 포함되지 않는다.
- 모든 피처의 geometry는 유효한 MultiPolygon이고, 스키마 CRS가 래스터와 일치한다.
- 모든 꼭짓점을 역변환하면 셀 경계의 정수 격자 좌표와 허용오차 `1e-7` 내에서 일치한다.
- Visible별 피처 면적 합은 해당 셀 수 × affine 행렬식 절댓값과 일치한다.
- `area` 속성은 geometry 면적과 일치하며 `Visible`은 Integer, `area`는 Double이다.

셀 중심 비교와 면적·꼭짓점 검사는 기존 래스터 결과를 폴리곤으로 보존하는지 확인한다. 실제 지형 관측값에 대한 별도의 현장 정확도 검증은 아니다.

[실제 DEM 대조 결과 CSV](gt17-viewshed-as-polygon-2026-11-11.csv)

배포 JAR: `GeoTools/process-spatialstatistics/target/gt-process-spatialstatistics-17.5.jar`. 모든 클래스는 Java 8 바이트코드(major version 52)이며 새 Factory가 SPI에 포함되어 있다. GeoServer 실서버 배포·HTTP 호출은 이 검증에 포함하지 않는다.
