<p align="center">
  <a href="http://opengxt.mangosystem.com"><img align="center" width="50%" src="https://github.com/mangosystem/opengxt/raw/master/docs/images/opengxt.png"></img></a>
</p>

[![English](https://img.shields.io/badge/language-English-orange.svg)](README.md)
[![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](README-KOREAN.md)

## OpenGXT - Spatial Analysis

### Build (GeoServer 2.28.6 / GeoTools 34.6)

Use JDK 17 and Maven. From the repository root, build and test both modules with:

```powershell
$env:JAVA_HOME = 'C:\Programs-20200206\java\jdk-17.0.10+7'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn clean verify
```

Versions follow the [GeoServer 2.28.6 release POM](https://github.com/geoserver/geoserver/blob/2.28.6/src/pom.xml).
The `2.28.x` branch declares SNAPSHOT versions; release builds use GeoServer 2.28.6 and GeoTools 34.6.
GeoServer dependency management aligns Wicket 9.23.0 and Spring dependencies.
The GeoTools platform BOM manages ImageN 0.9.1 and ImageIO-Ext 2.0.6.
GeoJSON PPIOs use the Jackson 2 API, matching GeoServer 2.28.6.
Existing JUnit 3/4 tests run with JUnit 4.13.2.

The resulting JARs are `GeoTools/process-spatialstatistics/target/gt-process-spatialstatistics-34.6.jar`
and `GeoServer/gs-wps-spatialstatistics/target/gs-wps-spatialstatistics-2.28.6.jar`.
Install the matching WPS extension in GeoServer 2.28.6, then copy both JARs into `WEB-INF/lib`.

On 2026-10-20, `mvn verify` passed on JDK 17.0.10+7, including all 7 GeoTools tests
and 5 GeoServer tests covering raster analysis and GeoJSON input/output.
Deployment in GeoServer 2.28.6 has not yet been verified.

### Overview
The goal of this project is to provide a spatial statistics tools for Geotools, Geoserver WPS, uDig Statistics Toolbox

![screenshot](docs/images/architecture.png?width=600)
 
### Related Projects
* [GeoTools](http://geotools.org) is an open source Java library that provides tools for geospatial data. 
* [GeoServer](http://geoserver.org) is an open source software server written in Java that 
allows users to share and edit geospatial data. Designed for interoperability, it publishes data from any major spatial data source using open standards.
* [uDig](http://locationtech.org/projects/technology.udig) is an open source desktop application framework, built with the Eclipse Rich Client (RCP) technology.
* [uDig Processing Toolbox](https://github.com/mangosystem/opengxt-udig-plugin) is a geospatial analysis plugin.

### License
* GeoTools process-spatialstatistics licensed under the [LGPL](http://www.gnu.org/licenses/lgpl.html). [License guide](http://docs.geotools.org/latest/userguide/welcome/license.html)
* GeoServer gs-wps-spatialstatistics licensed under the [GPL](http://www.gnu.org/licenses/old-licenses/gpl-2.0.html).
* uDig org.locationtech.udig.processingtoolbox licensed under the [EPL](http://www.eclipse.org/legal/epl-v10.html) + [BSD](http://udig.refractions.net/files/bsd3-v10.html).

### Download GeoTools & GeoServer WPS libraries
* Visit [SourceForge](https://sourceforge.net/projects/opengxt)

* Geotools
  * Download gt-process-spatialstatistics-xx.x.jar file
  
* GeoServer
  * Download GeoServer-Extension-OpenGxT-x.xx.x.zip
  * Unzip and copy two files to the WEB-INF/lib directory of the GeoServer installation.
  * Restart GeoServer

### uDig Processing Toolbox Plugin project
* Visit [OpenGXT - uDig Processing Toolbox](https://github.com/mangosystem/opengxt-udig-plugin/)

### Manual & Documents
* OpenGXT Online User Manual
  * [OpenGXT Online User Manual](http://opengxt.mangosystem.com/)
  
* GeoTools Developer Guide
  * [Korean v1.0 - 2MB](docs/manual/GeoTools_Process_1.0_Developer_Guide_ko_v.1.0.pdf)
  * [Korean v1.0-latest - 2MB](docs/manual/GeoTools_Process_1.0_Developer_Guide_ko_v.1.latest.pdf)
  
* GeoServer WPS Processes Manual
  * [Korean v1.0 - 25MB](docs/manual/GeoServer_WPS_1.0_User_Manual_ko_v.1.0.pdf)
  * [Korean v2.x-latest - 33MB](hdocs/manual/GeoServer_WPS_1.0_User_Manual_ko_v.2.latest.pdf)
  * [English v2.x-latest - 21MB](docs/manual/GeoServer_WPS_1.0_User_Manual_en_v.2.latest.pdf)

* uDig Processing Toolbox Manual
  * [Korean v2.0 - 28MB](docs/manual/uDig_ProcessingToolbox_1.0_User_Manual_ko_v.2.0.pdf)
  * [Korean v3.x-latest - 35MB](docs/manual/uDig_ProcessingToolbox_1.0_User_Manual_ko_v.3.latest.pdf)
  * [2018 OSGeo UN Committee Educational Challenge - Training Material for UN Open GIS Spiral 3](https://wiki.osgeo.org/wiki/Training_Material_for_UN_Open_GIS_Spiral_3)

* Presentation
  * [SlideShare](https://www.slideshare.net/mapplus)
 
### Localization
* [Transifex - English(defalut), Korean ...](https://www.transifex.com/mangosystem/opengxt/)

### Contributor
* mapplus (mapplus@gmail.com)
* mangowoong (https://github.com/mangowoong)
* favorson (https://github.com/favorson)
* jyajya (https://github.com/jyajya)
* saranghyuni (https://github.com/saranghyuni)
* jinifor (https://github.com/jinifor)
* tkdnsk0070 (https://github.com/tkdnsk0070)

### Commercial Support
Customers interested in OpenGXT, please contact us (email, phone) or visit us in person.
  * Address: MangoSystem Inc. #2307, 126, Beolmal-ro, Dongan-gu, Anyang-si, Gyeonggi-do, 14057, Korea
  * Tel: +82-31-450-3411~3
  * Email: mango@mangosystem.com | minpa.lee@mangosystem.com

### Gallery

![screenshot](docs/images/geoserver_wps_request.png?width=800)


![screenshot](docs/images/geoserver_wps_client.png?width=800)


![screenshot](docs/images/udig_processing_toolbox.png?width=800)
