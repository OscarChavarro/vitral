@echo off
rem Run from the DOS directory after building the 286 runtime.
rem Hosted ABI probe with a fake directory backend; no guest disk writes.
echo -ml > TEST.RSP
echo -IC:\TURBOC\INCLUDE;SRC\MAIN\V\KERNEL\286 >> TEST.RSP
echo -LC:\TURBOC\LIB >> TEST.RSP
echo -eBUILDTC\DIRTEST.EXE >> TEST.RSP
echo SRC\TEST\V\KERNEL\286\VLIBC\DIRTEST.CPP >> TEST.RSP
echo SRC\MAIN\V\KERNEL\286\VLIBC\BUILD\DIRS.OBJ >> TEST.RSP
tcc @TEST.RSP
if errorlevel 1 goto error
BUILDTC\DIRTEST.EXE
if errorlevel 1 goto error
echo Directory ABI checks passed.
goto end
:error
echo Directory ABI checks FAILED.
:end
