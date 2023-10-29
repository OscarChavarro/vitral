#!/bin/sh
#===========================================================================

BASE_CLASSES="./base/src/vsdk/toolkit/common/*.java ./base/src/vsdk/toolkit/common/linealAlgebra/*.java ./base/src/vsdk/toolkit/environment/*.java ./base/src/vsdk/toolkit/environment/geometry/*.java ./base/src/vsdk/toolkit/environment/geometry/polyhedralBoundedSolidNodes/*.java ./base/src/vsdk/toolkit/environment/scene/*.java ./base/src/vsdk/toolkit/media/*.java ./base/src/vsdk/toolkit/render/*.java ./base/src/vsdk/toolkit/gui/*.java ./base/src/vsdk/toolkit/processing/*.java ./base/src/vsdk/toolkit/animation/*.java ./base/src/vsdk/toolkit/gui/*.java"

IO_CLASSES="./base/src/vsdk/toolkit/io/*.java ./base/src/vsdk/toolkit/io/xml/*.java ./base/src/vsdk/toolkit/io/metadata/*.java ./base/src/vsdk/toolkit/io/image/*.java ./base/src/vsdk/toolkit/io/geometry/*.java ./base/src/vsdk/toolkit/io/gui/*.java"

AWT_CLASSES="./awt/src/vsdk/toolkit/render/awt/*.java ./awt/src/vsdk/toolkit/render/swing/*.java ./awt/src/vsdk/toolkit/gui/*.java ./awt/src/vsdk/toolkit/io/image/*.java"

#JOGL_CLASSES="./jogl/src/vsdk/toolkit/render/jogl/*.java ./jogl/src/vsdk/toolkit/render/jogl/animation/*.java ./jogl/src/vsdk/framework/shapeMatching/*.java ./jogl/src/vsdk/toolkit/io/image/*.java"

#JOGLCG_CLASSES="./joglcg/src/vsdk/toolkit/render/joglcg/*.java"

#VITRALARCHITECTURE_CLASSES="./base/src/vsdk/framework/*.java ./base/src/vsdk/framework/shapeMatching/*.java ./base/src/vsdk/framework/shapeMatching/plugins/*.java ./jogl/src/vsdk/framework/shapeMatching/plugins/*.java"

#---------------------------------------------------------------------------

# Create empty working directories if they does not exist
if [ ! -d ./classes ]; then
    mkdir ./classes
fi

if [ ! -d ./lib ]; then
    mkdir ./lib
fi

clear

# Check this all are well installed. Good idea is to install them to ~/.m2 using
# maven, and copy them to Java's extension folder. 
#JOGL_CP=$JAVA_HOME/jre/lib/ext/gluegen-rt-2.3.2-natives-linux-amd64.jar:$JAVA_HOME/jre/lib/ext/gluegen-rt-main-2.3.2.jar:$JAVA_HOME/jre/lib/ext/jogl-all-2.3.2-natives-linux-amd64.jar:$JAVA_HOME/jre/lib/ext/jogl-all-main-2.3.2.jar:$JAVA_HOME/jre/lib/ext/jogl-all-2.3.2.jar

# Compile main java library
# -proc:none option disables annotation processing which generates warnings
# when using some gluegen/JOGL features.
#javac -Xmaxerrs 10000 -Xlint:deprecation -Xlint:unchecked -Xlint -classpath ./base/src:./awt/src:./jogl/src:$JOGL_CP -d ./classes $BASE_CLASSES $IO_CLASSES $AWT_CLASSES $JOGL_CLASSES $VITRALARCHITECTURE_CLASSES
javac -Xmaxerrs 10000 -Xlint:deprecation -Xlint:unchecked -Xlint -classpath ./base/src:./awt/src:./jogl/src:$JOGL_CP -d ./classes $BASE_CLASSES $IO_CLASSES $AWT_CLASSES $VITRALARCHITECTURE_CLASSES

cd classes
jar cf ../lib/vsdk.jar vsdk
cd ..

# Compile native code packages
cd pkgs/SpharmonicKit27;make -j 72;cd ../..
cd pkgs/LempelZivWelch;make -j 72;cd ../..
cd pkgs/NativeImageReader;make -j 72;cd ../..

# Set proper permisions
chmod 755 `find . -type d`
chmod 644 `find . -name "*.java"`
chmod 644 `find . -name "*.bat"`
chmod 755 `find . -name "*.sh"`

#===========================================================================
#= EOF                                                                     =
#===========================================================================
