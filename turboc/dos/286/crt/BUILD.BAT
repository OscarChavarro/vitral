@echo off
rem Run from C:\VITRAL\TURBOC\DOS\286\CRT with Turbo C++ 3.0 in PATH.
rem No C0L or CL library is involved in building CRT286.LIB.
if not exist BUILD md BUILD
if exist BUILD\CRT.OK del BUILD\CRT.OK
tcc -c -ml -2 -f87 -O- -IC:\TURBOC\INCLUDE;.. -oBUILD\HEAP.OBJ HEAP.CPP
if errorlevel 1 goto error
tcc -c -ml -2 -f87 -O- -IC:\TURBOC\INCLUDE;.. -oBUILD\START.OBJ START.CPP
if errorlevel 1 goto error
tcc -c -ml -2 -f87 -O- -IC:\TURBOC\INCLUDE;.. -oBUILD\MEMORY.OBJ MEMORY.CPP
if errorlevel 1 goto error
tcc -c -ml -2 -f87 -O- -IC:\TURBOC\INCLUDE;.. -oBUILD\CXX.OBJ CXX.CPP
if errorlevel 1 goto error
tcc -c -ml -2 -f87 -O- -IC:\TURBOC\INCLUDE;.. -oBUILD\STRINGS.OBJ STRINGS.CPP
if errorlevel 1 goto error
tcc -c -ml -2 -f87 -O- -IC:\TURBOC\INCLUDE;.. -oBUILD\STDIO.OBJ STDIO.CPP
if errorlevel 1 goto error
tcc -c -ml -2 -f87 -O- -IC:\TURBOC\INCLUDE;.. -oBUILD\SCAN.OBJ SCAN.CPP
if errorlevel 1 goto error
tcc -c -ml -2 -f87 -O- -IC:\TURBOC\INCLUDE;.. -oBUILD\FORMAT.OBJ FORMAT.CPP
if errorlevel 1 goto error
tcc -c -ml -2 -f87 -O- -IC:\TURBOC\INCLUDE;.. -oBUILD\DIRS.OBJ DIRS.CPP
if errorlevel 1 goto error
tcc -c -ml -2 -f87 -O- -IC:\TURBOC\INCLUDE;.. -oBUILD\VECTOR.OBJ VECTOR.CPP
if errorlevel 1 goto error
tcc -c -ml -2 -f87 -O- -IC:\TURBOC\INCLUDE;.. -oBUILD\TIME.OBJ TIME.CPP
if errorlevel 1 goto error
tasm /m2 /ml HELPERS.ASM BUILD\HELPERS.OBJ
if errorlevel 1 goto error
tasm /m2 /ml FPU.ASM BUILD\FPU.OBJ
if errorlevel 1 goto error
tlib BUILD\CRT286.LIB /C /P64 -+BUILD\CXX.OBJ -+BUILD\HEAP.OBJ -+BUILD\START.OBJ -+BUILD\MEMORY.OBJ
if errorlevel 1 goto error
tlib BUILD\CRT286.LIB /C /P64 -+BUILD\STRINGS.OBJ -+BUILD\STDIO.OBJ
if errorlevel 1 goto error
tlib BUILD\CRT286.LIB /C /P64 -+BUILD\HELPERS.OBJ
if errorlevel 1 goto error
tlib BUILD\CRT286.LIB /C /P64 -+BUILD\SCAN.OBJ
if errorlevel 1 goto error
tlib BUILD\CRT286.LIB /C /P64 -+BUILD\FORMAT.OBJ -+BUILD\DIRS.OBJ
if errorlevel 1 goto error
tlib BUILD\CRT286.LIB /C /P64 -+BUILD\VECTOR.OBJ -+BUILD\TIME.OBJ
if errorlevel 1 goto error
tlib BUILD\CRT286.LIB /C /P64 -+BUILD\FPU.OBJ
if errorlevel 1 goto error
tcc -c -ml -2 -f87 -O- -IC:\TURBOC\INCLUDE;.. -oBUILD\TRIG.OBJ TRIG.CPP
if errorlevel 1 goto error
tlib BUILD\CRT286.LIB /C /P64 -+BUILD\TRIG.OBJ
if errorlevel 1 goto error
echo CRT286.LIB built.
echo ready>BUILD\CRT.OK
goto end
:error
echo CRT286 build failed.
:end
