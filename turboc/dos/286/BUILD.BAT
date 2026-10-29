@echo off
rem Turbo C++ 3.0 80286 protected-mode object build.  The loader and the
rem protected image are linked separately; no DOS/XMS library is included.
if not exist BUILD md BUILD
if not exist BUILDTC md BUILDTC

set PMFLAGS=-c -f87 -2 -ml -O1 -d -v- -w-inl -DTURBO_C_286_ENABLED
echo %PMFLAGS% > TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -oBUILDTC\PMEM286.OBJ >> TC.RSP
echo PMEM286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo %PMFLAGS% > TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -oBUILDTC\PPORT286.OBJ >> TC.RSP
echo PPORT286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo %PMFLAGS% > TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -oBUILDTC\PVGA286.OBJ >> TC.RSP
echo PVGA286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo %PMFLAGS% > TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -oBUILDTC\PKEY286.OBJ >> TC.RSP
echo PKEY286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo %PMFLAGS% > TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -oBUILDTC\PLOAD286.OBJ >> TC.RSP
echo PLOAD286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo %PMFLAGS% > TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -oBUILDTC\PRUNT286.OBJ >> TC.RSP
echo PRUNT286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo %PMFLAGS% > TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -oBUILDTC\PATA286.OBJ >> TC.RSP
echo PATA286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo %PMFLAGS% > TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -oBUILDTC\PFAT286.OBJ >> TC.RSP
echo PFAT286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo %PMFLAGS% > TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -oBUILDTC\PCRT286.OBJ >> TC.RSP
echo PCRT286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo %PMFLAGS% > TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -oBUILDTC\PKERN286.OBJ >> TC.RSP
echo PKERN286.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

C:\TURBOC\BIN\TASM.EXE /m2 PMSW286.ASM BUILDTC\PMSW286.OBJ > TASM.LOG
if errorlevel 1 goto asmerror
echo 286 protected-mode objects compiled successfully.
goto end
:error
type TC.LOG
goto end
:asmerror
type TASM.LOG
:end
