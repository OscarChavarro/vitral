@echo off
rem FAT backend reads/writes 8.3 files in the C: root.
rem Boot without CONFIG/AUTOEXEC (F5) before using the protected 286 build.
if not exist RAYTRACE.EXE goto missing_program
if not exist RAY286.BIN goto missing_program
RAYTRACE.EXE -r 320x240 -s C:\BALLS.RAY -o C:\RAYTRACE.PPM %1 %2 %3 %4 %5 %6 %7 %8 %9
goto end

:missing_program
echo RAYTRACE.EXE not found. Run COMPILE.BAT first.

:end
