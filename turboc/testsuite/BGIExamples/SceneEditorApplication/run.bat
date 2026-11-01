@echo off
rem Run the BGI SceneEditorApplication. Optional arguments: english, spanish or a GUI file, and a graphics mode
rem (SCENEDIT -h lists them). The BGI drivers are read from C:\TURBOC\BGI, or from the BGIPATH variable.
if not exist SCENEDIT.EXE goto missing_program
SCENEDIT.EXE %1 %2
goto end

:missing_program
echo SCENEDIT.EXE not found. Run COMPILE.BAT first.

:end
