@echo off
rem Build the DOS 8.3 RaytracingOfflineExample using the sphere-only MIT reader.
rem RAYTRACE.EXE is intentionally limited to the non-GUI renderer and PPM output.

if not exist BUILD md BUILD
if not exist BUILDTC md BUILDTC

echo Compiling RaytracingOfflineExample
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\..\..\BASE\SRC;..\..\..\BASE\SRC\MAIN;SRC >> TC.RSP
echo -I..\..\..\DOS\SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYTSIMP.OBJ >> TC.RSP
echo SRC\RAYTSIMP.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto compiler_error

echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\..\..\BASE\SRC;..\..\..\BASE\SRC\MAIN;SRC >> TC.RSP
echo -I..\..\..\DOS\SRC\MAIN >> TC.RSP
echo -oBUILDTC\COMOPTPR.OBJ >> TC.RSP
echo SRC\COMOPTPR.CPP >> TC.RSP
tcc @TC.RSP >> TC.LOG
if errorlevel 1 goto compiler_error

echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\..\..\BASE\SRC;..\..\..\BASE\SRC\MAIN;SRC >> TC.RSP
echo -I..\..\..\DOS\SRC\MAIN >> TC.RSP
echo -oBUILDTC\IMAGEXPO.OBJ >> TC.RSP
echo SRC\IMAGEXPO.CPP >> TC.RSP
tcc @TC.RSP >> TC.LOG
if errorlevel 1 goto compiler_error

rem Do not scan BASEENG1.LIB: its unused mesh modules overflow TLINK's
rem internal name table.  The sphere-only renderer needs just these objects.
echo C:\TURBOC\LIB\C0L.OBJ+BUILDTC\RAYTSIMP.OBJ+BUILDTC\COMOPTPR.OBJ+BUILDTC\IMAGEXPO.OBJ+ > OBJ.RSP
echo ..\..\..\BASE\BUILDTC\RAY.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\RAYHIT.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\GEOMETRY.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\SURFACE.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\HALFSPAC.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\INFIPLAN.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\SURRAYIN.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\VOLUME.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\SOLID.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\SPHERE.OBJ >> OBJ.RSP
echo ..\..\..\BASE\BUILD\BASEREND.LIB+ > LIB.RSP
echo ..\..\..\BASE\BUILD\BASEIO.LIB+ >> LIB.RSP
rem ReaderMitScene pulls the sphere, scene, camera, lights and material groups.
rem They must follow BASEIO: TLINK searches libraries only from left to right.
echo ..\..\..\BASE\BUILD\BASEENVS.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD\BASEENVM.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD\BASEMEDI.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD\BASECOMM.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD\BASELANG.LIB+ >> LIB.RSP
echo ..\..\..\DOS\BUILD\DOS.LIB+ >> LIB.RSP
echo C:\TURBOC\LIB\EMU.LIB+C:\TURBOC\LIB\MATHL.LIB+C:\TURBOC\LIB\CL.LIB >> LIB.RSP

echo Linking RAYTRACE.EXE
tlink /c /yx @OBJ.RSP,RAYTRACE.EXE,RAYTRACE.MAP,@LIB.RSP > TLINK.LOG
if errorlevel 1 goto linker_error
echo RAYTRACE.EXE created successfully.
goto end

:compiler_error
type TC.LOG
echo Compilation failed.
goto end

:linker_error
type TLINK.LOG
echo Link failed.

:end
