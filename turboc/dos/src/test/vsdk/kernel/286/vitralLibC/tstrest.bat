@echo off
rem Run from the DOS directory after building the 286 runtime.
rem Hosted real-mode ABI probes only; not the application link.
echo -ml > TEST.RSP
echo -IC:\TURBOC\INCLUDE;SRC\MAIN\V\KERNEL\286 >> TEST.RSP
echo -LC:\TURBOC\LIB >> TEST.RSP
echo -eBUILDTC\VECTEST.EXE >> TEST.RSP
echo SRC\TEST\V\KERNEL\286\VLIBC\VECTEST.CPP >> TEST.RSP
echo SRC\MAIN\V\KERNEL\286\VLIBC\BUILD\VECTOR.OBJ >> TEST.RSP
tcc @TEST.RSP
if errorlevel 1 goto error
BUILDTC\VECTEST.EXE
if errorlevel 1 goto error
echo Vector ABI checks passed.
echo -ml > TEST.RSP
echo -IC:\TURBOC\INCLUDE;SRC\MAIN\V\KERNEL\286 >> TEST.RSP
echo -LC:\TURBOC\LIB >> TEST.RSP
echo -eBUILDTC\TIMETEST.EXE >> TEST.RSP
echo SRC\TEST\V\KERNEL\286\VLIBC\TIMETEST.CPP >> TEST.RSP
echo SRC\MAIN\V\KERNEL\286\VLIBC\BUILD\TIME.OBJ >> TEST.RSP
tcc @TEST.RSP
if errorlevel 1 goto error
BUILDTC\TIMETEST.EXE
if errorlevel 1 goto error
echo RTC conversion checks passed.
goto end
:error
echo Runtime ABI checks FAILED.
:end
