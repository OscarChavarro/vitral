@echo off
rem Assemble the DOS-specific objects into the Turbo C++ library.
if not exist BUILD md BUILD
del BUILD\DOS.LIB > NUL
if "%1"=="8086" goto real_mode
goto protected_mode
:real_mode

echo DOS.LIB Adding BUILDTC\XMSMEMOR.OBJ
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\XMSMEMOR.OBJ > TLIB.LOG
find "Error:" TLIB.LOG > NUL
if errorlevel 1 goto L00001
type TLIB.LOG
goto error
:L00001

echo DOS.LIB Adding BUILDTC\XMSRGBIM.OBJ
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\XMSRGBIM.OBJ > TLIB.LOG
find "Error:" TLIB.LOG > NUL
if errorlevel 1 goto L00002
type TLIB.LOG
goto error
:L00002
echo Real-mode 8086 XMS library created successfully.
goto end
:protected_mode
if not exist BUILDTC\PMSW286.OBJ goto missing_asm

rem TLINK 5 scans this library in member order.  Put consumers before the
rem providers they pull, so the one scan resolves the entire protected chain.
echo DOS.LIB Adding BUILDTC\PMSW286.OBJ
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\PMSW286.OBJ > TLIB.LOG
find "Error:" TLIB.LOG > NUL
if errorlevel 1 goto L00002A
type TLIB.LOG
goto error
:L00002A

echo DOS.LIB Adding BUILDTC\PLOAD286.OBJ
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\PLOAD286.OBJ > TLIB.LOG
find "Error:" TLIB.LOG > NUL
if errorlevel 1 goto L00002L
type TLIB.LOG
goto error
:L00002L

echo DOS.LIB Adding BUILDTC\PKERN286.OBJ
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\PKERN286.OBJ > TLIB.LOG
find "Error:" TLIB.LOG > NUL
if errorlevel 1 goto L00003
type TLIB.LOG
goto error
:L00003

echo DOS.LIB Adding BUILDTC\PPROG286.OBJ
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\PPROG286.OBJ > TLIB.LOG
find "Error:" TLIB.LOG > NUL
if errorlevel 1 goto L00003P
type TLIB.LOG
goto error
:L00003P

echo DOS.LIB Adding BUILDTC\PRUNT286.OBJ
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\PRUNT286.OBJ > TLIB.LOG
find "Error:" TLIB.LOG > NUL
if errorlevel 1 goto L00003A
type TLIB.LOG
goto error
:L00003A

echo DOS.LIB Adding BUILDTC\PFAT286.OBJ
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\PFAT286.OBJ > TLIB.LOG
find "Error:" TLIB.LOG > NUL
if errorlevel 1 goto L00003F
type TLIB.LOG
goto error
:L00003F

echo DOS.LIB Adding BUILDTC\PATA286.OBJ
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\PATA286.OBJ > TLIB.LOG
find "Error:" TLIB.LOG > NUL
if errorlevel 1 goto L00003AT
type TLIB.LOG
goto error
:L00003AT

echo DOS.LIB Adding BUILDTC\PCRT286.OBJ
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\PCRT286.OBJ > TLIB.LOG
find "Error:" TLIB.LOG > NUL
if errorlevel 1 goto L00003CRT
type TLIB.LOG
goto error
:L00003CRT

echo DOS.LIB Adding BUILDTC\PVGA286.OBJ
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\PVGA286.OBJ > TLIB.LOG
find "Error:" TLIB.LOG > NUL
if errorlevel 1 goto L00003B
type TLIB.LOG
goto error
:L00003B

echo DOS.LIB Adding BUILDTC\PKEY286.OBJ
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\PKEY286.OBJ > TLIB.LOG
find "Error:" TLIB.LOG > NUL
if errorlevel 1 goto L00004
type TLIB.LOG
goto error
:L00004

echo DOS.LIB Adding BUILDTC\PMEM286.OBJ
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\PMEM286.OBJ > TLIB.LOG
find "Error:" TLIB.LOG > NUL
if errorlevel 1 goto L00004A
type TLIB.LOG
goto error
:L00004A

echo DOS.LIB Adding BUILDTC\PPORT286.OBJ
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\PPORT286.OBJ > TLIB.LOG
find "Error:" TLIB.LOG > NUL
if errorlevel 1 goto L00005
type TLIB.LOG
goto error
:L00005

echo DOS.LIB Adding protected segmented RGB storage
tlib BUILD\DOS.LIB /C /E /P64 +BUILDTC\PRGB286.OBJ
if errorlevel 1 goto error
echo DOS library created successfully.
goto end
:error
type TLIB.LOG
echo DOS library build failed. See the output above.
goto end
:missing_asm
echo BUILDTC\PMSW286.OBJ is missing. Run DOS.BAT with TASM.EXE installed.
:end
