@echo off
rem Assemble the DOS-specific objects into the Turbo C++ library.
if not exist BUILD md BUILD
del BUILD\DOS.LIB > NUL

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

echo DOS library created successfully.
goto end
:error
type TLIB.LOG
echo DOS library build failed. See the output above.
:end
