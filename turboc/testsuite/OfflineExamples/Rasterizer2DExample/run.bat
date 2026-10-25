@echo off
rem Run Rasterizer2DExample in DOS. Optional arguments are mode, output, width, and height.
if not exist RASTERIZ.EXE goto missing_program

if "%1"=="" goto default
RASTERIZ.EXE %1 %2 %3 %4
goto end

:default
RASTERIZ.EXE line OUTPUT1.PPM
goto end

:missing_program
echo RASTERIZ.EXE not found. Run COMPILE.BAT first.

:end
