@echo off
rem Compile one source of SRC\MAIN\V\T\GUI\FRAMEBUF\BGI (its name without .CPP) to BUILDTC.
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
echo -ISRC\MAIN;..\BASE\SRC;..\BASE\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -oBUILDTC\%1.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\FRAMEBUF\BGI\%1.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
