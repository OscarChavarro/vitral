@echo off
rem APPEL3.RAY is the 8.3 copy of balls_featured_object.ray.
rem Same protected executable; the reader constructs a real BREP Appel 3.
if not exist RAYTRACE.EXE goto missing_program
if not exist RAY286.BIN goto missing_program
if not exist C:\APPEL3.RAY goto missing_scene
RAYTRACE.EXE -r 320x240 -s C:\APPEL3.RAY -o C:\APPEL3.PPM %1 %2 %3 %4 %5 %6 %7 %8 %9
goto end
:missing_program
echo RAYTRACE.EXE not found. Run COMPILE.BAT first.
goto end
:missing_scene
echo Copy the staged APPEL3.RAY to C:\ before the bare boot.
:end
