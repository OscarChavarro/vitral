@echo off
rem Hosted real-mode ABI test only. The raytracer never uses this link.
rem Borland startup/CRT supply assert and FPU initialization for the probe;
rem SCAN.OBJ and STRINGS.OBJ supply the implementation under test explicitly.
echo -ml > TEST.RSP
echo -f87 >> TEST.RSP
echo -IC:\TURBOC\INCLUDE >> TEST.RSP
echo -LC:\TURBOC\LIB >> TEST.RSP
echo -eBUILD\SCANTEST.EXE >> TEST.RSP
echo SCANTEST.CPP >> TEST.RSP
echo BUILD\SCAN.OBJ >> TEST.RSP
echo BUILD\STRINGS.OBJ >> TEST.RSP
tcc @TEST.RSP
if errorlevel 1 goto error
BUILD\SCANTEST.EXE
if errorlevel 1 goto error
echo Scanner ABI checks passed.
goto end
:error
echo Scanner ABI checks FAILED.
:end
