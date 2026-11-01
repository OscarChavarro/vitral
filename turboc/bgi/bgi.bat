@echo off
rem Compile the BGI framebuffer backend (real mode, 8086 with XMS) with Turbo C++ 3.0.
rem Run it from BGI after mapping the source tree; BGILINK.BAT archives BUILD\BGI.LIB.
if not exist BUILD\NUL md BUILD
if not exist BUILDTC\NUL md BUILDTC

echo [1/4] Compiling SRC\MAIN\V\T\GUI\FRAMEBUF\BGI\BGIMODES.CPP
call BGICC.BAT BGIMODES
if errorlevel 1 goto error
echo [2/4] Compiling SRC\MAIN\V\T\GUI\FRAMEBUF\BGI\BGIPOINT.CPP
call BGICC.BAT BGIPOINT
if errorlevel 1 goto error
echo [3/4] Compiling SRC\MAIN\V\T\GUI\FRAMEBUF\BGI\BGIFB.CPP
call BGICC.BAT BGIFB
if errorlevel 1 goto error
echo [4/4] Compiling SRC\MAIN\V\T\GUI\FRAMEBUF\BGI\BGIINPUT.CPP
call BGICC.BAT BGIINPUT
if errorlevel 1 goto error
echo BGI objects compiled successfully.
goto end

:error
type TC.LOG
echo Build failed. See the compiler output above.

:end
