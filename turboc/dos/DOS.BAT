@echo off
rem Compile the DOS-specific XMS services with Turbo C++ 3.0.
if not exist BUILD md BUILD
if not exist BUILDTC md BUILDTC

echo [1/2] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\XMSMEMOR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC;SRC\MAIN;..\BASE\SRC;..\BASE\SRC\MAIN >> TC.RSP
echo -oBUILDTC\XMSMEMOR.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\XMSMEMOR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [2/2] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\XMSRGBIM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -ISRC;SRC\MAIN;..\BASE\SRC;..\BASE\SRC\MAIN >> TC.RSP
echo -oBUILDTC\XMSRGBIM.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\XMSRGBIM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo DOS objects compiled successfully.
goto end
:error
type TC.LOG
echo DOS compilation failed. See the output above.
:end
