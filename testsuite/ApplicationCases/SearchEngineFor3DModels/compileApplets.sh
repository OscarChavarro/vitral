#= Create empty working directories if they does not exist =================
if [ ! -d ./classes ]; then
    mkdir ./classes
fi
if [ ! -d ./tmp ]; then
    mkdir ./tmp
fi

#= Compile sources and pack web archive ====================================
cd srcApplets
javac -proc:none -Xlint:deprecation -Xlint:unchecked -classpath . -d ../classes *.java
cp ../web/index.html ../classes
cd ..
cd classes
jar cf ../tmp/sketch2Dapplet.jar *.class
cd ..
cp -f web/applet.html tmp
cp -f web/index.html tmp
cp -f web/startPage.html tmp
cd ..

#===========================================================================
#= EOF                                                                     =
#===========================================================================
