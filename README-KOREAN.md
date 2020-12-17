[![English](https://img.shields.io/badge/language-English-orange.svg)](README.md)
[![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](README-KOREAN.md)

## OpenGXT - Spatial Analysis

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
