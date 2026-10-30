@echo off
rem Appel 3 BREP scene; run from this example directory after a bare boot.
set PMDIR=..\..\..\DOS\SRC\MAIN\V\KERNEL\286\LOADER
if not exist %PMDIR%\LOADER.EXE goto missing_program
if not exist RAY286.BIN goto missing_program
if not exist RAY286.MAP goto missing_program
if not exist ..\..\..\..\ETC\APPEL3.RAY goto missing_scene
set PATH=%PMDIR%
LOADER RAY286 -r 320x240 -s ..\..\..\..\ETC\APPEL3.RAY -o APPEL3.PPM
goto end
:missing_program
echo Loader or payload missing. Build DOS and run COMPILE.BAT.
goto end
:missing_scene
echo Scene missing from the staged ETC directory.
:end
