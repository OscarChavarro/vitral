@echo off
rem Run from the DOS directory after building the 286 runtime.
rem Hosted real-mode ABI test only. The raytracer never uses this link.
rem Borland startup/CRT supply assert and FPU initialization for the probe;
rem SCAN.OBJ and STRINGS.OBJ supply the implementation under test explicitly.
echo -ml > TEST.RSP
echo -f87 >> TEST.RSP
echo -IC:\TURBOC\INCLUDE;SRC\MAIN\V\KERNEL\286 >> TEST.RSP
echo -LC:\TURBOC\LIB >> TEST.RSP
echo -eBUILDTC\SCANTEST.EXE >> TEST.RSP
echo SRC\TEST\V\KERNEL\286\VLIBC\SCANTEST.CPP >> TEST.RSP
echo SRC\MAIN\V\KERNEL\286\VLIBC\BUILD\SCAN.OBJ >> TEST.RSP
echo SRC\MAIN\V\KERNEL\286\VLIBC\BUILD\STRINGS.OBJ >> TEST.RSP
tcc @TEST.RSP
if errorlevel 1 goto error
BUILDTC\SCANTEST.EXE
if errorlevel 1 goto error
echo Scanner ABI checks passed.
goto end
:error
echo Scanner ABI checks FAILED.
:end
