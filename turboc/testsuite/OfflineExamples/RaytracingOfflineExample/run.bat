@echo off
rem Default DOS run: sphere-only BALLS.RAY at the conservative 320x240 size.
if not exist RAYTRACE.EXE goto missing_program
RAYTRACE.EXE %1 %2 %3 %4
goto end

:missing_program
echo RAYTRACE.EXE not found. Run COMPILE.BAT first.

:end
