@echo off

REM 프로젝트가 생성되어있는 디렉토리로 이동
d:

REM 각자환경에 맞게 설정
set "JAVA_HOME=D:\dev\java\openjdk\jdk-18.0.2"
set "MVN_HOME=D:\dev\util\apache-maven-3.9.2"
set "PRJ_HOME=D:\git_store\opengxt\GeoTools\process-spatialstatistics"
set "project_name=process-spatialstatistics"

cd %PRJ_HOME%

REM 메이븐리파지토리를 github에서 클론한 디렉토리
set "local_mg_maven_repo=D:\git_store\mg-repository"

REM 메이븐으로 메이븐리파지토리로 빌드 디플로이
%MVN_HOME%\bin\mvn -Dmaven.test.skip=true -DaltDeploymentRepository=snapshot-repo::default::file://%local_mg_maven_repo%\snapshots clean deploy

REM 메이븐 리파지토리 디렉토리로 이동
cd %local_mg_maven_repo%

REM git add, commit, and push
REM Assuming deploy key is configured, no username or password is required
git status
git add .
git status
git commit -m "release new version of %project_name%"
git push origin master
