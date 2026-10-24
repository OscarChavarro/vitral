@echo off
rem Install the current Turbo C++ source fixes from A: into the DOS tree.
rem Run this before BASE.BAT and BASELINK.BAT from C:\VITRAL\TURBOC\BASE.

copy A:\_PBSSNIC.H C:\VITRAL\TURBOC\BASE\SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSNIC.H
if errorlevel 1 goto error
copy A:\_PBSSNIC.CPP C:\VITRAL\TURBOC\BASE\SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSNIC.CPP
if errorlevel 1 goto error
copy A:\_POBOSSO.H C:\VITRAL\TURBOC\BASE\SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\_POBOSSO.H
if errorlevel 1 goto error
copy A:\BASE.BAT C:\VITRAL\TURBOC\BASE\BASE.BAT
if errorlevel 1 goto error
copy A:\BASELINK.BAT C:\VITRAL\TURBOC\BASE\BASELINK.BAT
if errorlevel 1 goto error

echo DOS source copy updated successfully.
goto end

:error
echo Update failed.

:end
