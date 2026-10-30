@echo off
rem Default: bare protected 286. Pass 8086 to build the real-mode XMS backend.
if not exist BUILD md BUILD
if not exist BUILDTC md BUILDTC
if "%1"=="8086" goto real_mode
goto protected_mode
:real_mode

echo [1/14] Compiling SRC\MAIN\V\T\MEDIA\XMSMEMOR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_8086_REAL_MODE_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC;SRC\MAIN;..\BASE\SRC;..\BASE\SRC\MAIN >> TC.RSP
echo -oBUILDTC\XMSMEMOR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\XMSMEMOR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [2/14] Compiling SRC\MAIN\V\T\MEDIA\XMSRGBIM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_8086_REAL_MODE_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC;SRC\MAIN;..\BASE\SRC;..\BASE\SRC\MAIN >> TC.RSP
echo -oBUILDTC\XMSRGBIM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\XMSRGBIM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo Real-mode 8086 XMS objects compiled successfully.
goto end
:protected_mode
rem Keep the ring-0 runtime in DOS.LIB so protected applications can link it
rem through the same DOS library dependency as the rest of the port.
echo [3/14] Compiling SRC\MAIN\V\KERNEL\286\KERNEL\PMEM286.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -2 >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -oBUILDTC\PMEM286.OBJ >> TC.RSP
echo SRC\MAIN\V\KERNEL\286\KERNEL\PMEM286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [4/14] Compiling SRC\MAIN\V\KERNEL\286\DRIVERS\PPORT286.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -2 >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -oBUILDTC\PPORT286.OBJ >> TC.RSP
echo SRC\MAIN\V\KERNEL\286\DRIVERS\PPORT286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [5/14] Compiling SRC\MAIN\V\KERNEL\286\SVGALIB\PVGA286.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -2 >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -oBUILDTC\PVGA286.OBJ >> TC.RSP
echo SRC\MAIN\V\KERNEL\286\SVGALIB\PVGA286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [6/14] Compiling SRC\MAIN\V\KERNEL\286\DRIVERS\PKEY286.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -2 >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -oBUILDTC\PKEY286.OBJ >> TC.RSP
echo SRC\MAIN\V\KERNEL\286\DRIVERS\PKEY286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [7/14] Compiling SRC\MAIN\V\KERNEL\286\BOOT\PLOAD286.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -2 >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -oBUILDTC\PLOAD286.OBJ >> TC.RSP
echo SRC\MAIN\V\KERNEL\286\BOOT\PLOAD286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [8/14] Compiling SRC\MAIN\V\KERNEL\286\KERNEL\PRUNT286.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -2 >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -oBUILDTC\PRUNT286.OBJ >> TC.RSP
echo SRC\MAIN\V\KERNEL\286\KERNEL\PRUNT286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [9/14] Compiling SRC\MAIN\V\KERNEL\286\DRIVERS\PATA286.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -2 >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -oBUILDTC\PATA286.OBJ >> TC.RSP
echo SRC\MAIN\V\KERNEL\286\DRIVERS\PATA286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [10/14] Compiling SRC\MAIN\V\KERNEL\286\FS\PFAT286.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -2 >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -oBUILDTC\PFAT286.OBJ >> TC.RSP
echo SRC\MAIN\V\KERNEL\286\FS\PFAT286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [11/14] Compiling SRC\MAIN\V\KERNEL\286\VLIBC\PCRT286.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -2 >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -oBUILDTC\PCRT286.OBJ >> TC.RSP
echo SRC\MAIN\V\KERNEL\286\VLIBC\PCRT286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [12/14] Compiling SRC\MAIN\V\KERNEL\286\SVGALIB\PPROG286.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -2 >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC\MAIN\V\KERNEL\286;..\BASE\SRC;..\BASE\SRC\MAIN >> TC.RSP
echo -oBUILDTC\PPROG286.OBJ >> TC.RSP
echo SRC\MAIN\V\KERNEL\286\SVGALIB\PPROG286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [13/14] Compiling SRC\MAIN\V\KERNEL\286\KERNEL\PKERN286.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -2 >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -oBUILDTC\PKERN286.OBJ >> TC.RSP
echo SRC\MAIN\V\KERNEL\286\KERNEL\PKERN286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [14/14] Assembling SRC\MAIN\V\KERNEL\286\BOOT\PMSW286.ASM
if not exist C:\TURBOC\BIN\TASM.EXE goto asmtoolmissing
rem /ml preserves the C++ spelling of PUBLIC and EXTRN symbols.  Without it
rem TASM uppercases _enterProtected286 and TLINK cannot retrieve it from
rem the case-sensitive DOS.LIB dictionary.
C:\TURBOC\BIN\TASM.EXE /m2 /ml SRC\MAIN\V\KERNEL\286\BOOT\PMSW286.ASM BUILDTC\PMSW286.OBJ > TASM.LOG
if errorlevel 1 goto asmerror
if not exist BUILDTC\PMSW286.OBJ goto asmerror

echo Compiling protected segmented RGB storage
echo -c -ml -2 -f87 -O1 > TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC\MAIN\V\KERNEL\286;..\BASE\SRC;..\BASE\SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRGB286.OBJ >> TC.RSP
echo SRC\MAIN\V\KERNEL\286\SVGALIB\PRGB286.CPP >> TC.RSP
tcc @TC.RSP
if errorlevel 1 goto error
cd SRC\MAIN\V\KERNEL\286\VLIBC
call BUILD.BAT
cd ..\..\..\..\..\..
if not exist SRC\MAIN\V\KERNEL\286\VLIBC\BUILD\CRT286.LIB goto error
if not exist SRC\MAIN\V\KERNEL\286\VLIBC\BUILD\CRT.OK goto error
call SRC\MAIN\V\KERNEL\286\LOADER\BUILD.BAT
if not exist SRC\MAIN\V\KERNEL\286\LOADER\LOADER.EXE goto error
echo DOS objects compiled successfully.
goto end
:error
type TC.LOG
echo DOS compilation failed. See the output above.
goto end
:asmerror
type TASM.LOG
echo DOS protected-mode assembly failed. See the output above.
goto end
:asmtoolmissing
echo TASM.EXE is required to build BUILDTC\PMSW286.OBJ.
:end
