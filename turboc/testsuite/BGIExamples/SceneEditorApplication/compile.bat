@echo off
rem Build the DOS 8.3 copy of the BGI SceneEditorApplication with Turbo C++ 3.0, in real mode (8086 with XMS).
rem Run it from TESTSUIT\BGIEXAM\SCENEDIT after mapping the source tree and building, in this order:
rem   BASE\BASE86.BAT and BASE\BASELK86.BAT (the real mode base libraries in BASE\BUILD86)
rem   BGI\BGI.BAT and BGI\BGILINK.BAT (BGI\BUILD\BGI.LIB)
rem The XMS objects of DOS are compiled here, in real mode, so that DOS\BUILD keeps its protected 286 library.
rem The output name is limited to 8.3 by DOS: SCENEDIT.EXE.

if not exist BUILDTC\NUL md BUILDTC

echo Compiling SRC\SCENEDIT.CPP
call CC.BAT SRC\SCENEDIT SCENEDIT
if errorlevel 1 goto compiler_error
echo Compiling DOS\SRC\MAIN\V\T\MEDIA\XMSMEMOR.CPP
call CC.BAT ..\..\..\DOS\SRC\MAIN\V\T\MEDIA\XMSMEMOR XMSMEMOR
if errorlevel 1 goto compiler_error
echo Compiling DOS\SRC\MAIN\V\T\MEDIA\XMSRGBIM.CPP
call CC.BAT ..\..\..\DOS\SRC\MAIN\V\T\MEDIA\XMSRGBIM XMSRGBIM
if errorlevel 1 goto compiler_error

if not exist ..\..\..\BGI\BUILD\BGI.LIB goto missing_libraries
if not exist ..\..\..\BASE\BUILD86\BASEWIDG.LIB goto missing_libraries
if not exist ..\..\..\BASE\BUILD86\BASEGUI.LIB goto missing_libraries
if not exist ..\..\..\BASE\BUILD86\BASELANG.LIB goto missing_libraries
if not exist ..\..\..\BASE\BUILD86\BASECOMM.LIB goto missing_libraries

rem TLINK does not read TURBOC.CFG, so every Borland runtime component has an explicit path. Keep the object and
rem library lists in separate response files: TLINK reliably accepts @OBJ.RSP,...,@LIB.RSP in Turbo C++ 3.0.
rem /yx lets it use XMS; C0L/CL/MATHL match TCC's -ml model, EMU the -f floating point emulation.
echo C:\TURBOC\LIB\C0L.OBJ+BUILDTC\SCENEDIT.OBJ+BUILDTC\XMSMEMOR.OBJ+BUILDTC\XMSRGBIM.OBJ > OBJ.RSP
echo ..\..\..\BGI\BUILD\BGI.LIB+ > LIB.RSP
echo ..\..\..\BASE\BUILD86\BASEWIDG.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD86\BASEGUI.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD86\BASEREND.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD86\BASEENVS.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD86\BASEENVM.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD86\BASEENG1.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD86\BASEENG2.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD86\BASEIO.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD86\BASEMEDI.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD86\BASECOMM.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD86\BASELANG.LIB+ >> LIB.RSP
echo C:\TURBOC\LIB\GRAPHICS.LIB+C:\TURBOC\LIB\EMU.LIB+C:\TURBOC\LIB\MATHL.LIB+C:\TURBOC\LIB\CL.LIB >> LIB.RSP

echo Linking SCENEDIT.EXE
tlink /c /yx @OBJ.RSP,SCENEDIT.EXE,SCENEDIT.MAP,@LIB.RSP > TLINK.LOG
if errorlevel 1 goto linker_error
echo SCENEDIT.EXE created successfully.
goto end

:missing_libraries
echo Missing BGI\BUILD\BGI.LIB or the real mode base libraries of BASE\BUILD86.
goto end

:compiler_error
type TC.LOG
echo Compilation failed.
goto end

:linker_error
type TLINK.LOG
echo Link failed.

:end
