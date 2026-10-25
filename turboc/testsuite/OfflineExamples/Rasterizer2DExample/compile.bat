@echo off
rem Build the DOS 8.3 copy of Rasterizer2DExample with Turbo C++ 3.0.
rem Run this from TESTSUIT\OFFLEXAM\RAS2DEXA after mapping the source tree.
rem It links only the partial base libraries required by this non-GUI example.
rem The output name is limited to 8.3 by DOS: RASTERIZ.EXE.

if not exist BUILD md BUILD
if not exist BUILDTC md BUILDTC

echo Compiling SRC\RAS2DEXA.CPP
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
echo -oBUILDTC\RAS2DEXA.OBJ >> TC.RSP
echo SRC\RAS2DEXA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto compiler_error

if not exist ..\..\..\BASE\BUILD\BASECOMM.LIB goto missing_libraries
if not exist ..\..\..\BASE\BUILD\BASELANG.LIB goto missing_libraries
if not exist ..\..\..\BASE\BUILD\BASEIO.LIB goto missing_libraries
if not exist ..\..\..\BASE\BUILD\BASEMEDI.LIB goto missing_libraries
if not exist ..\..\..\BASE\BUILD\BASEREND.LIB goto missing_libraries
if not exist ..\..\..\BASE\BUILD\BASEENG1.LIB goto missing_libraries
if not exist ..\..\..\BASE\BUILD\BASEENG2.LIB goto missing_libraries
if not exist ..\..\..\DOS\BUILD\DOS.LIB goto missing_libraries

rem TLINK does not read TURBOC.CFG, so every Borland runtime component has
rem an explicit path. Keep object and library lists in separate response
rem files: TLINK reliably accepts @OBJ.RSP,...,@LIB.RSP in Turbo C++ 3.0.
rem /YX lets it use available XMS; C0L/CL/MATHL match TCC's -ml model.
echo C:\TURBOC\LIB\C0L.OBJ+BUILDTC\RAS2DEXA.OBJ > OBJ.RSP
echo ..\..\..\BASE\BUILD\BASEREND.LIB+ > LIB.RSP
echo ..\..\..\BASE\BUILD\BASEENG1.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD\BASEENG2.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD\BASEIO.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD\BASEMEDI.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD\BASECOMM.LIB+ >> LIB.RSP
echo ..\..\..\BASE\BUILD\BASELANG.LIB+ >> LIB.RSP
echo ..\..\..\DOS\BUILD\DOS.LIB+ >> LIB.RSP
echo C:\TURBOC\LIB\EMU.LIB+C:\TURBOC\LIB\MATHL.LIB+C:\TURBOC\LIB\CL.LIB >> LIB.RSP

echo Linking RASTERIZ.EXE
tlink /c /yx @OBJ.RSP,RASTERIZ.EXE,RASTERIZ.MAP,@LIB.RSP > TLINK.LOG
if errorlevel 1 goto linker_error
echo RASTERIZ.EXE created successfully.
goto end

:missing_libraries
echo Missing one or more required partial base libraries.
goto end

:compiler_error
type TC.LOG
echo Compilation failed.
goto end

:linker_error
type TLINK.LOG
echo Link failed.

:end
