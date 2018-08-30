## Spatial Tools for GeoTools, GeoServer WPS, uDig

### Overview
The goal of this project is to provide a spatial statistics tools for Geotools, Geoserver WPS, uDig Statistics Toolbox

![screenshot](https://github.com/mapplus/spatial_statistics_for_geotools_udig/blob/master/docs/images/architecture.png?width=600)
 
### Related Projects
* [GeoTools](http://geotools.org) is an open source Java library that provides tools for geospatial data. 
* [GeoServer](http://geoserver.org) GeoServer is an open source software server written in Java that 
allows users to share and edit geospatial data. Designed for interoperability, it publishes data from 
any major spatial data source using open standards.
* [uDig](http://locationtech.org/projects/technology.udig) is an open source desktop application framework, built with the Eclipse Rich Client (RCP) technology.

### License
* GeoTools licensed under the [LGPL](http://www.gnu.org/licenses/lgpl.html). [License guide](http://docs.geotools.org/latest/userguide/welcome/license.html)
* GeoServer licensed under the [GPL](http://www.gnu.org/licenses/old-licenses/gpl-2.0.html).
* uDig licensed under the [EPL](http://www.eclipse.org/legal/epl-v10.html) + [BSD](http://udig.refractions.net/files/bsd3-v10.html).

### Download GeoTools & GeoServer WPS libraries
* Visit [SourceForge](https://sourceforge.net/projects/mango-spatialstatistics/)
* Geotools 14.x
  * Download gt-process-spatialstatistics-14.5.jar file
* GeoServer 2.8.x
  * Download gs-wps-spatialstatistics-2.8.5.jar, gt-process-spatialstatistics-14.5.jar files
  * Copy these files to the WEB-INF/lib directory of the GeoServer installation.
  * Restart GeoServer

### uDig Processing Toolbox Plugin Installation
* Official uDig 2.0.0
  * Recommended!
    * Visit [uDig Download Site](http://udig.refractions.net/download/)
    * Download  Release 2.0.0
    * Install uDig

    * Visit [Processing Toolbox Plugin Site](https://sourceforge.net/projects/mango-spatialstatistics/files/uDig/)
    * Download latest version : ex> org.locationtech.udig.processingtoolbox_2.0.0.xxxxxxxxxx.jar
    * Copy plugin file to [uDig Installed Folder\udig\plugins] folder
    * Restart uDig

* Or

* Customized uDig 2.0.0-SNAPSHOT
  * Will be deprecated in 2018!
    * Windows [x86_Installer](https://sourceforge.net/projects/mango-spatialstatistics/files/uDig/2.0.0/udig-2.0.0-SNAPSHOT.win32.win32.x86.exe)
    * Windows [x86_zip](https://sourceforge.net/projects/mango-spatialstatistics/files/uDig/2.0.0/udig-2.0.0-SNAPSHOT.win32.win32.x86.zip)
    * Windows [x86_64 Installer](https://sourceforge.net/projects/mango-spatialstatistics/files/uDig/2.0.0/udig-2.0.0-SNAPSHOT.win32.win32.x86_64.exe)
    * Windows [x86_64 zip](https://sourceforge.net/projects/mango-spatialstatistics/files/uDig/2.0.0/udig-2.0.0-SNAPSHOT.win32.win32.x86_64.zip)
    * Linux [x86_64 zip](https://sourceforge.net/projects/mango-spatialstatistics/files/uDig/2.0.0/udig-2.0.0-SNAPSHOT.linux.gtk.x86_64.zip)
    * Mac OS/X [x86_64 zip](https://sourceforge.net/projects/mango-spatialstatistics/files/uDig/2.0.0/udig-2.0.0-SNAPSHOT.macosx.cocoa.x86_64.zip)
  
    * Install uDig and start uDig application
    * Choose [Help] -> [Find and Install...] from the menu bar to open up the Install/Update window
    * Select [Search for new features] to install option and press [Next].
    * Click on the [New Remote Site] button and enter the location and name for the site
    ```
        Name: Spatial Statistics Toolbox
        URL : http://www.mangosystem.com:8080/s2toolbox_updates
    ```
    * Press [OK] and Finish
    * Install plugin and restart uDig

### Manual & Documents
* uDig Processing Toolbox Manual
  * [Korean v2.0 - 28MB](https://github.com/mapplus/spatial_statistics_for_geotools_udig/blob/master/docs/manual/uDig_ProcessingToolbox_1.0_User_Manual_ko_v.2.0.pdf)
  * [Korean v3.x-latest - 34MB](https://github.com/mapplus/spatial_statistics_for_geotools_udig/blob/master/docs/manual/uDig_ProcessingToolbox_1.0_User_Manual_ko_v.3.latest.pdf)

* GeoServer WPS Processes Manual
  * [Korean v1.0 - 25MB](https://github.com/mapplus/spatial_statistics_for_geotools_udig/blob/master/docs/manual/GeoServer_WPS_1.0_User_Manual_ko_v.1.0.pdf)
  * [Korean v2.x-latest - 31MB](https://github.com/mapplus/spatial_statistics_for_geotools_udig/blob/master/docs/manual/GeoServer_WPS_1.0_User_Manual_ko_v.2.latest.pdf)
  * [English v2.x-latest - 21MB](https://github.com/mapplus/spatial_statistics_for_geotools_udig/blob/master/docs/manual/GeoServer_WPS_1.0_User_Manual_en_v.2.latest.pdf)

* GeoTools Developer Guide
  * [Korean v1.0 - 2MB](https://github.com/mapplus/spatial_statistics_for_geotools_udig/blob/master/docs/manual/GeoTools_Process_1.0_Developer_Guide_ko_v.1.0.pdf)
  * [Korean v1.0-latest - 2MB](https://github.com/mapplus/spatial_statistics_for_geotools_udig/blob/master/docs/manual/GeoTools_Process_1.0_Developer_Guide_ko_v.1.latest.pdf)
  
* Presentation
  * [SlideShare](https://www.slideshare.net/mapplus)
 
### Localization
* [Transifex - English(defalut), Korean ...](https://www.transifex.com/projects/p/ss-rd/)

### Contributor
* mapplus (mapplus@gmail.com)
* mangosystem (https://github.com/mangosystem)
* mangowoong (https://github.com/mangowoong)
* favorson (https://github.com/favorson)
* rlawlgus18 (https://github.com/rlawlgus18)

### Gallery

![screenshot](https://github.com/mapplus/spatial_statistics_for_geotools_udig/blob/master/docs/images/udig_processing_toolbox.png?width=800)


![screenshot](https://github.com/mapplus/spatial_statistics_for_geotools_udig/blob/master/docs/images/geoserver_wps_request.png?width=800)


![screenshot](https://github.com/mapplus/spatial_statistics_for_geotools_udig/blob/master/docs/images/geoserver_wps_client.png?width=800)
