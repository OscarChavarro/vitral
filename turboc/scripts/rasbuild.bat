@echo off
rem Install and build the DOS Rasterizer2DExample from the floppy.
rem Leaves the DOS prompt in the example folder, ready to run RASTERIZ.EXE.

copy A:\RAS2DEXA.CPP C:\VITRAL\TURBOC\TESTSUIT\OFFLEXAM\RAS2DEXA\SRC\RAS2DEXA.CPP
if errorlevel 1 goto error
copy A:\RASCOM.BAT C:\VITRAL\TURBOC\TESTSUIT\OFFLEXAM\RAS2DEXA\COMPILE.BAT
if errorlevel 1 goto error
C:
cd \VITRAL\TURBOC\TESTSUIT\OFFLEXAM\RAS2DEXA
call COMPILE.BAT
goto end

:error
echo Rasterizer batch update failed.

:end
