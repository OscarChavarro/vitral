@echo off
rem Build the protected 286 example with spheres and the Appel BREP fixture.
rem RAYTRACE.EXE is intentionally limited to the non-GUI renderer and PPM output.

if not exist BUILD md BUILD
if not exist BUILDTC md BUILDTC

echo Compiling RaytracingOfflineExample
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -2 >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\..\..\BASE\SRC;..\..\..\BASE\SRC\MAIN;SRC >> TC.RSP
echo -I..\..\..\DOS\SRC\MAIN >> TC.RSP
echo -I..\..\..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -oBUILDTC\RAYTSIMP.OBJ >> TC.RSP
echo SRC\RAYTSIMP.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto compiler_error

echo -c > TC.RSP
echo -ml >> TC.RSP
echo -2 >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\..\..\BASE\SRC;..\..\..\BASE\SRC\MAIN;SRC >> TC.RSP
echo -I..\..\..\DOS\SRC\MAIN >> TC.RSP
echo -I..\..\..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -oBUILDTC\COMOPTPR.OBJ >> TC.RSP
echo SRC\COMOPTPR.CPP >> TC.RSP
tcc @TC.RSP >> TC.LOG
if errorlevel 1 goto compiler_error

echo -c > TC.RSP
echo -ml >> TC.RSP
echo -2 >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\..\..\BASE\SRC;..\..\..\BASE\SRC\MAIN;SRC >> TC.RSP
echo -I..\..\..\DOS\SRC\MAIN >> TC.RSP
echo -I..\..\..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -oBUILDTC\IMAGEXPO.OBJ >> TC.RSP
echo SRC\IMAGEXPO.CPP >> TC.RSP
tcc @TC.RSP >> TC.LOG
if errorlevel 1 goto compiler_error

rem Select the geometry closure explicitly: scanning whole geometry archives
rem includes unused mesh dictionaries that exhaust TLINK's conventional RAM.
rem The Appel fixture includes its real boolean/Euler/topology dependencies.
rem PMSW286 is the executable entry point.  Do NOT add C0L.OBJ here: C0L is
rem the DOS real-mode startup code and cannot run after the 80286 mode switch.
echo ..\..\..\DOS\BUILDTC\PMSW286.OBJ+BUILDTC\RAYTSIMP.OBJ+BUILDTC\COMOPTPR.OBJ+BUILDTC\IMAGEXPO.OBJ+ > OBJ.RSP
echo ..\..\..\BASE\BUILDTC\RAY.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\RAYHIT.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\GEOMETRY.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\SURFACE.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\HALFSPAC.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\INFIPLAN.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\SURRAYIN.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\VOLUME.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\SOLID.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\SPHERE.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\BOX.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\CONE.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\CURVE.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\PARACURV.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\POBOSOEO.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\POBOSOGV.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\POBOSOMO.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\POBOSONP.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\POBOSOPR.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\POBOSOTE.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\POBOSOTS.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\POBOSOVE.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\POLBOUSO.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\POLYGO2D.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\POLYPROC.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\SITEGELI.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\VERTEX2D.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_PBSAACF.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_PBSSGPP.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_PBSSICB.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_PBSSNEC.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_PBSSNIC.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_PBSSONE.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_PBSSOSC.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_PBSSVFC.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_PBSSVVC.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBOSFG.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBOSHE.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBOSIN.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBOSOE.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBOSOF.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBOSOL.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBOSOO.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBOSOS.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBOSSC.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBOSSF.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBOSSI.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBOSSO.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBOSTV.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBOSVS.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBSBTP.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBSNTS.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBSOCF.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBSOPF.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POBSPDF.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_POL2DCO.OBJ+ >> OBJ.RSP
echo ..\..\..\BASE\BUILDTC\_SETOPTR.OBJ >> OBJ.RSP
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
rem Protected286ProgressMonitor derives from ProgressMonitor in BASEGUI.
rem Keep it after DOS.LIB so TLINK sees its unresolved base symbols first.
echo ..\..\..\BASE\BUILD\BASEGUI.LIB+ >> LIB.RSP
rem /n below disables default libraries.  In particular, never add CL.LIB:
rem it is the DOS real-mode CRT.  Every runtime service used in protected
rem mode must be supplied by DOS.LIB (Protected286Crt and its drivers).
echo ..\..\..\DOS\SRC\MAIN\V\KERNEL\286\VLIBC\BUILD\CRT286.LIB+ >> LIB.RSP
echo C:\TURBOC\LIB\MATHL.LIB >> LIB.RSP

echo Linking protected RAY286.BIN payload
tlink /n /c /s /yx @OBJ.RSP,RAY286.BIN,RAY286.MAP,@LIB.RSP > TLINK.LOG
if errorlevel 1 goto linker_error
rem DOS loads only this small launcher. The identical PMSW286 first object
rem fixes the protected-entry offset in both links; BREP stays above 1 MiB.
echo -c -ml -2 -f87 -O1 > BOOT.RSP
echo -IC:\TURBOC\INCLUDE >> BOOT.RSP
echo -I..\..\..\DOS\SRC\MAIN\V\KERNEL\286 >> BOOT.RSP
echo -oBUILDTC\PBOOT286.OBJ >> BOOT.RSP
echo ..\..\..\DOS\SRC\MAIN\V\KERNEL\286\BOOT\PBOOT286.CPP >> BOOT.RSP
tcc @BOOT.RSP >> TC.LOG
if errorlevel 1 goto compiler_error
echo ..\..\..\DOS\BUILDTC\PMSW286.OBJ+BUILDTC\PBOOT286.OBJ+ > BOOTOBJ.RSP
rem Keep compiler helpers as objects, without C0L or CL.
echo ..\..\..\DOS\SRC\MAIN\V\KERNEL\286\VLIBC\BUILD\HELPERS.OBJ+ >> BOOTOBJ.RSP
echo ..\..\..\DOS\SRC\MAIN\V\KERNEL\286\VLIBC\BUILD\FPU.OBJ >> BOOTOBJ.RSP
echo Linking small RAYTRACE.EXE DOS launcher
tlink /n /c /s @BOOTOBJ.RSP,RAYTRACE.EXE,RAYTRACE.MAP >> TLINK.LOG
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
