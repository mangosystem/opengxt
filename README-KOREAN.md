<p align="center">
  <a href="http://opengxt.mangosystem.com"><img align="center" width="50%" src="https://github.com/mangosystem/opengxt/raw/master/docs/images/opengxt.png"></img></a>
</p>

[![English](https://img.shields.io/badge/language-English-orange.svg)](README.md)
[![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](README-KOREAN.md)

## OpenGXT - Spatial Analysis

### 빌드 (GeoServer 3.0.2 / GeoTools 35.2)

JDK 17과 Maven을 사용합니다. 저장소 루트에서 다음 PowerShell 명령으로 두 모듈을 함께 빌드하고 테스트할 수 있습니다.

```powershell
$env:JAVA_HOME = 'C:\Programs-20200206\java\jdk-17.0.10+7'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn clean verify
```

GeoServer 버전은 [3.0.2 릴리스 POM](https://github.com/geoserver/geoserver/blob/3.0.2/src/pom.xml)을 기준으로 고정했습니다.
`3.0.2.x` 브랜치는 SNAPSHOT 버전을 선언하므로 릴리스 배포본과 구분해야 합니다.
GeoServer의 의존성 관리 설정으로 Wicket 10.11.0 및 Spring 버전을 맞추며,
GeoTools 35.2의 플랫폼 BOM으로 ImageN 0.9.3과 ImageIO-Ext 2.1.4 버전을 관리합니다.
GeoJSON PPIO는 GeoServer 3.0.2와 동일한 Jackson 3 API를 사용합니다.
기존 JUnit 3/4 테스트는 JUnit 4.13.2로 실행합니다.

결과 파일은 `GeoTools/process-spatialstatistics/target/gt-process-spatialstatistics-35.2.jar`와
`GeoServer/gs-wps-spatialstatistics/target/gs-wps-spatialstatistics-3.0.2.jar`입니다.
GeoServer 3.0.2에 같은 버전의 WPS 확장을 설치한 후 두 JAR를 `WEB-INF/lib`에 복사합니다.

2026-10-20에 GeoServer 3.0.2의 WPS 확장 설치 후 OpenGXT가 정상 실행되는 것을 확인했습니다.
WPS 요청 빌더에서 `statistics:CollectEvents` 프로세스가 등록되고 입력 파라미터와 한국어 설명이 정상 표시되었습니다.
JDK 17.0.10+7에서 `mvn clean verify`도 성공했으며, GeoTools 테스트 7개와 GeoServer 테스트 5개가 모두 통과했습니다.

### 개요
OGC 국제표준과 Open Source GIS에 기반하여 개발한 공간(통계)분석 엔진으로 3개의 프로젝트로 구성되어 있습니다.
  * GeoTools 기반의 공간(통계)분석 Library
  * GeoServer OGC WPS 지원 공간분석 Service Extension
  * uDig 기반의 공간분석 Processing Toolbox Plugin

![screenshot](docs/images/architecture.png?width=600)
 
### 관련 프로젝트
* [GeoTools](http://geotools.org)는 표준 준수 솔루션을 개발하기 위한 오픈 소스 소프트웨어 Java GIS 툴킷이며, OGC(Open Geospatial Consortium) 스펙 구현을 제공합니다.
* [GeoServer](http://geoserver.org) 는 지리공간 데이터를 공유하고 편집할 수 있는 Java로 개발된 오픈 소스 GIS 소프트웨어 서버이다. 상호운용성을 전제로 개발되었기 때문에, 개방형 표준을 사용하여 다양한 공간 데이터 소스를 서비스할 수 있게 한다.
* [uDig](http://locationtech.org/projects/technology.udig)은 자바로 만들어진 Eclipse Rich Client (RCP) 기반의 오픈 소스(EPL, BSD) 데스크톱 GIS 프로그램
* [uDig Processing Toolbox](https://github.com/mangosystem/opengxt-udig-plugin) is a geospatial analysis plugin.

### 라이선스
이 프로젝트는 process-spatialstatistics와 gs-wps-spatialstatistics 2개의 하위 프로젝트로 구성됩니다.
* process-spatialstatistics는 GeoTools [LGPL](http://www.gnu.org/licenses/lgpl.html) 라이선스를 따릅니다. [라이선스 가이드](http://docs.geotools.org/latest/userguide/welcome/license.html)
* gs-wps-spatialstatistics는 GeoServer [GPL](http://www.gnu.org/licenses/old-licenses/gpl-2.0.html) 라이선스를 따릅니다.

### GeoTools & GeoServer용 OpenGXT 다운로드
* [SourceForge](https://sourceforge.net/projects/opengxt) 연결

* Geotools
  * gt-process-spatialstatistics-xx.x.jar 파일 다운로드  
* GeoServer
  * WPS Extension 설치 확인
  * GeoServer-Extension-OpenGxT-x.xx.x.zip 파일 다운로드
  * 압축 해제 후 2개의 jar 파일을 GeoServer의 WEB-INF/lib 폴더에 복사
  * GeoServer 재시작

### uDig Processing Toolbox 플러그인 프로젝트
* [OpenGXT - uDig Processing Toolbox](https://github.com/mangosystem/opengxt-udig-plugin/) 프로젝트

### 사용자 지침서
* OpenGXT 온라인 사용자 지침서mangosystem
  * [OpenGXT Online User Manual](http://opengxt.mangosystem.com/)
  
* GeoTools 개발자 가이드
  * [Korean v1.0 - 2MB](docs/manual/GeoTools_Process_1.0_Developer_Guide_ko_v.1.0.pdf)
  * [Korean v1.0-latest - 2MB](docs/manual/GeoTools_Process_1.0_Developer_Guide_ko_v.1.latest.pdf)
  
* GeoServer WPS Processes 사용자 지침서
  * [Korean v1.0 - 25MB](docs/manual/GeoServer_WPS_1.0_User_Manual_ko_v.1.0.pdf)
  * [Korean v2.x-latest - 33MB](docs/manual/GeoServer_WPS_1.0_User_Manual_ko_v.2.latest.pdf)
  * [English v2.x-latest - 21MB](docs/manual/GeoServer_WPS_1.0_User_Manual_en_v.2.latest.pdf)

* 소개 및 발표자료
  * [SlideShare](https://www.slideshare.net/mapplus)
 
### 지역화
* [Transifex - English(defalut), Korean ...](https://www.transifex.com/mangosystem/opengxt/)

### 기여자
* mapplus (mapplus@gmail.com)
* mangowoong (https://github.com/mangowoong)
* favorson (https://github.com/favorson)
* jyajya (https://github.com/jyajya)
* saranghyuni (https://github.com/saranghyuni)
* jinifor (https://github.com/jinifor)
* tkdnsk0070 (https://github.com/tkdnsk0070)

### 상업적 지원
OpenGXT에 관심있는 고객들께서는 연락(이메일, 전화) 또는 직접 방문 부탁드립니다.
  * 주소: [14057] 경기도 안양시 벌말로 126, 2307호 (관양동, 평촌오비즈타워)
  * 전화번호: 031-450-3411~3
  * 이메일: mango@mangosystem.com | minpa.lee@mangosystem.com

### 활용 예

![screenshot](docs/images/geoserver_wps_request.png?width=800)


![screenshot](docs/images/geoserver_wps_client.png?width=800)


![screenshot](docs/images/udig_processing_toolbox.png?width=800)
