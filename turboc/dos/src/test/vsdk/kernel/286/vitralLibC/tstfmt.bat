@echo off
rem Run from the DOS directory after building the 286 runtime.
rem Hosted real-mode ABI probe, not the protected application link.
echo -ml > TEST.RSP
echo -f87 >> TEST.RSP
echo -IC:\TURBOC\INCLUDE;SRC\MAIN\V\KERNEL\286 >> TEST.RSP
echo -LC:\TURBOC\LIB >> TEST.RSP
echo -eBUILDTC\FMTTEST.EXE >> TEST.RSP
echo SRC\TEST\V\KERNEL\286\VLIBC\FMTTEST.CPP >> TEST.RSP
echo SRC\MAIN\V\KERNEL\286\VLIBC\BUILD\FORMAT.OBJ >> TEST.RSP
tcc @TEST.RSP
if errorlevel 1 goto error
BUILDTC\FMTTEST.EXE
if errorlevel 1 goto error
echo Formatter ABI checks passed.
goto end
:error
echo Formatter ABI checks FAILED.
:end
