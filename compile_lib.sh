#!/bin/sh
#===========================================================================

BASE_CLASSES="./base/src/vsdk/toolkit/common/*.java ./base/src/vsdk/toolkit/common/linealAlgebra/*.java ./base/src/vsdk/toolkit/environment/*.java ./base/src/vsdk/toolkit/environment/geometry/*.java ./base/src/vsdk/toolkit/environment/geometry/polyhedralBoundedSolidNodes/*.java ./base/src/vsdk/toolkit/environment/scene/*.java ./base/src/vsdk/toolkit/media/*.java ./base/src/vsdk/toolkit/render/*.java ./base/src/vsdk/toolkit/gui/*.java ./base/src/vsdk/toolkit/processing/*.java ./base/src/vsdk/toolkit/animation/*.java ./base/src/vsdk/toolkit/gui/*.java"

IO_CLASSES="./base/src/vsdk/toolkit/io/*.java ./base/src/vsdk/toolkit/io/xml/*.java ./base/src/vsdk/toolkit/io/metadata/*.java ./base/src/vsdk/toolkit/io/image/ImageNotRecognizedException.java ./base/src/vsdk/toolkit/io/image/ImagePersistenceSGI.java ./base/src/vsdk/toolkit/io/image/RGBColorPalettePersistence.java ./base/src/vsdk/toolkit/io/image/NativeImageReaderWrapper.java ./base/src/vsdk/toolkit/io/image/_NativeImageReaderWrapperHeaderInfo.java ./base/src/vsdk/toolkit/io/geometry/EnvironmentPersistence.java ./base/src/vsdk/toolkit/io/geometry/ViewpointBinaryPersistence.java ./base/src/vsdk/toolkit/io/geometry/FontReader.java ./base/src/vsdk/toolkit/io/geometry/ParametricBiCubicPatchPersistence.java ./base/src/vsdk/toolkit/io/geometry/ParametricCurvePersistence.java ./base/src/vsdk/toolkit/io/geometry/Reader3ds.java ./base/src/vsdk/toolkit/io/geometry/ReaderAse.java ./base/src/vsdk/toolkit/io/geometry/ReaderMitScene.java ./base/src/vsdk/toolkit/io/geometry/Md2Persistence.java ./base/src/vsdk/toolkit/io/gui/*.java ./base/src/vsdk/toolkit/io/image/ImagePersistence.java ./base/src/vsdk/toolkit/io/geometry/ReaderObj.java"

AWT_CLASSES="./awt/src/vsdk/toolkit/render/awt/*.java ./awt/src/vsdk/toolkit/render/swing/*.java ./awt/src/vsdk/toolkit/gui/AwtSystem.java ./awt/src/vsdk/toolkit/io/image/*.java"

JOGL_CLASSES="./jogl/src/vsdk/toolkit/render/jogl/*.java ./jogl/src/vsdk/toolkit/render/jogl/animation/*.java ./jogl/src/vsdk/framework/shapeMatching/*.java ./jogl/src/vsdk/toolkit/io/image/*.java"

#JOGL_CLASSES="./jogl/src/vsdk/toolkit/render/jogl/*.java ./jogl/src/vsdk/toolkit/render/jogl/animation/*.java ./jogl/srccg/vsdk/toolkit/render/joglcg/*.java ./jogl/src/vsdk/framework/shapeMatching/*.java ./jogl/src/vsdk/toolkit/io/image/*.java"

VITRALARCHITECTURE_CLASSES="./base/src/vsdk/framework/*.java ./base/src/vsdk/framework/shapeMatching/*.java ./base/src/vsdk/framework/shapeMatching/plugins/*.java ./jogl/src/vsdk/framework/shapeMatching/plugins/*.java"

#---------------------------------------------------------------------------

# Create empty working directories if they does not exist
if [ ! -d ./classes ]; then
    mkdir ./classes
fi

if [ ! -d ./lib ]; then
    mkdir ./lib
fi

clear

# Sometimes those are needed to specify its location...
JOGL_CP=$JAVA_HOME/jre/lib/ext/gluegen-rt-2.3.2-natives-linux-amd64.jar:$JAVA_HOME/jre/lib/ext/gluegen-rt-main-2.3.2.jar:$JAVA_HOME/jre/lib/ext/jogl-all-2.3.2-natives-linux-amd64.jar:$JAVA_HOME/jre/lib/ext/jogl-all-main-2.3.2.jar:$JAVA_HOME/jre/lib/ext/jogl-all-2.3.2.jar

# -proc:none option disables annotation processing which generates warnings
# when using some gluegen/JOGL features.
javac -proc:none -Xmaxerrs 10000 -Xlint:deprecation -Xlint:unchecked -Xlint -classpath ./base/src:./awt/src:./jogl/src:$JOGL_CP -d ./classes $BASE_CLASSES $IO_CLASSES $AWT_CLASSES $JOGL_CLASSES $VITRALARCHITECTURE_CLASSES

cd classes
jar cf ../lib/vsdk.jar vsdk
cd ..
cd pkgs/SpharmonicKit27;make;cd ../..
cd pkgs/LempelZivWelch;make;cd ../..
cd pkgs/NativeImageReader;make;cd ../..

# Set proper permisions
chmod 755 `find ./classes -type d`
chmod 755 `find ./doc -type d`
chmod 755 `find ./etc -type d`
chmod 755 `find ./lib -type d`
chmod 755 `find ./pkgs -type d`
chmod 755 `find ./base/src -type d`
chmod 755 `find ./awt/src -type d`
chmod 755 `find ./jogl/src -type d`
chmod 755 `find ./joglcg/src -type d`
chmod 755 `find ./android/src -type d`
chmod 755 `find ./jme/src -type d`
chmod 755 `find ./testsuite -type d`
chmod 644 `find . -name "*.java"`
chmod 644 `find . -name "*.bat"`
chmod 755 `find . -name "*.sh"`

#===========================================================================
#= EOF                                                                     =
#===========================================================================
