@echo off
rem Compile one source (its path without .CPP) to BUILDTC (named by the second argument), in real mode.
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_8086_REAL_MODE_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\..\..\BASE\SRC;..\..\..\BASE\SRC\MAIN;SRC >> TC.RSP
echo -I..\..\..\BGI\SRC\MAIN;..\..\..\DOS\SRC\MAIN >> TC.RSP
echo -oBUILDTC\%2.OBJ >> TC.RSP
echo %1.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
