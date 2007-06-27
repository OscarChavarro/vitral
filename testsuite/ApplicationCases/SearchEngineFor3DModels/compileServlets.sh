#= Config ==================================================================
TOMCAT_DIR="/usr/local/apache-tomcat-6.0.13"

#= Create empty working directories if they does not exist =================
if [ ! -d ./tmp ]; then
    mkdir ./tmp
fi
if [ ! -d ./tmp/WEB-INF ]; then
    mkdir ./tmp/WEB-INF
fi
if [ ! -d ./tmp/WEB-INF/classes ]; then
    mkdir ./tmp/WEB-INF/classes
fi

#= Compile sources and pack web archive ====================================
javac -Xlint:deprecation -Xlint:unchecked -sourcepath ./src -d ./tmp/WEB-INF/classes -classpath .:../../../lib/vsdk.jar:$TOMCAT_DIR/lib/servlet-api.jar ./src/ServletConsole.java
cp -r ../../../classes/vsdk tmp/WEB-INF/classes
cp web/*.html tmp/
cp web/web.xml tmp/WEB-INF
cd tmp
rm -f ../vitralSearchEngine.war
jar -cfM ../vitralSearchEngine.war *
cd ..

#= Deploy web application system to application container ==================
#rm -rf $TOMCAT_DIR/webapps/vitralSearchEngine
rm -rf $TOMCAT_DIR/webapps/vitralSearchEngine.war
cp vitralSearchEngine.war $TOMCAT_DIR/webapps

#===========================================================================
#= EOF                                                                     =
#===========================================================================
