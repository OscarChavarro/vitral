rem = Config ==================================================================
set TOMCAT_DIR=C:\usr\internet\apache-tomcat-6.0.13

rem = Create empty working directories if they does not exist =================
md tmp
md tmp\WEB-INF
md tmp\WEB-INF\classes

rem = Compile sources and pack web archive ====================================
javac -Xlint:deprecation -Xlint:unchecked -sourcepath .\src -d .\tmp\WEB-INF\classes -classpath .;..\..\..\lib\vsdk.jar;%TOMCAT_DIR%\lib\servlet-api.jar .\src\ServletConsole.java
xcopy ..\..\..\classes\vsdk tmp\WEB-INF\classes
copy web\*.html tmp\
copy web\web.xml tmp\WEB-INF
cd tmp
del ..\vitralSearchEngine.war
jar -cfM ..\vitralSearchEngine.war *
cd ..

rem = Deploy web application system to application container ==================
deltree %TOMCAT_DIR%\webapps\vitralSearchEngine
del %TOMCAT_DIR%\webapps\vitralSearchEngine.war
copy vitralSearchEngine.war %TOMCAT_DIR%\webapps

rem ===========================================================================
rem = EOF                                                                     =
rem ===========================================================================
pause
