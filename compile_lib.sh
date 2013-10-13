#!/bin/sh
#===========================================================================

BASIC_CLASSES="./src/vsdk/toolkit/common/*.java ./src/vsdk/toolkit/common/linealAlgebra/*.java ./src/vsdk/toolkit/environment/*.java ./src/vsdk/toolkit/environment/geometry/*.java ./src/vsdk/toolkit/environment/geometry/polyhedralBoundedSolidNodes/*.java ./src/vsdk/toolkit/environment/scene/*.java ./src/vsdk/toolkit/media/*.java ./src/vsdk/toolkit/render/*.java ./src/vsdk/toolkit/gui/Gizmo.java ./src/vsdk/toolkit/gui/ViewportWindow.java ./src/vsdk/toolkit/gui/ViewportWindowSetManager.java ./src/vsdk/toolkit/gui/ProgressMonitor.java ./src/vsdk/toolkit/gui/ProgressMonitorConsole.java ./src/vsdk/toolkit/gui/Controller.java ./src/vsdk/toolkit/gui/PresentationElement.java ./src/vsdk/toolkit/processing/*.java ./src/vsdk/toolkit/gui/KeyEvent.java"

IO_CLASSES="./src/vsdk/toolkit/io/*.java ./src/vsdk/toolkit/io/metadata/*.java ./src/vsdk/toolkit/io/image/ImageNotRecognizedException.java ./src/vsdk/toolkit/io/image/ImagePersistenceSGI.java ./src/vsdk/toolkit/io/image/RGBColorPalettePersistence.java ./src/vsdk/toolkit/io/image/NativeImageReaderWrapper.java ./src/vsdk/toolkit/io/image/_NativeImageReaderWrapperHeaderInfo.java ./src/vsdk/toolkit/io/geometry/EnvironmentPersistence.java ./src/vsdk/toolkit/io/geometry/ViewpointBinaryPersistence.java ./src/vsdk/toolkit/io/geometry/FontReader.java ./src/vsdk/toolkit/io/geometry/ParametricBiCubicPatchPersistence.java ./src/vsdk/toolkit/io/geometry/ParametricCurvePersistence.java ./src/vsdk/toolkit/io/geometry/Reader3ds.java ./src/vsdk/toolkit/io/geometry/ReaderAse.java ./src/vsdk/toolkit/io/geometry/ReaderMitScene.java"

AWT_CLASSES="./src/vsdk/toolkit/render/awt/*.java ./src/vsdk/toolkit/gui/CameraController.java ./src/vsdk/toolkit/gui/AwtSystem.java ./src/vsdk/toolkit/gui/CameraControllerAquynza.java ./src/vsdk/toolkit/gui/CameraControllerBlender.java ./src/vsdk/toolkit/gui/TranslateGizmo.java ./src/vsdk/toolkit/gui/RotateGizmo.java ./src/vsdk/toolkit/gui/ScaleGizmo.java ./src/vsdk/toolkit/gui/RendererConfigurationController.java ./src/vsdk/toolkit/io/image/ImagePersistence.java ./src/vsdk/toolkit/io/image/TargaImage.java ./src/vsdk/toolkit/io/geometry/ReaderObj.java"

JOGL_CLASSES="./src_jogl/vsdk/toolkit/render/jogl/*.java ./src_joglcg/vsdk/toolkit/render/joglcg/*.java ./src_jogl/vsdk/framework/shapeMatching/*.java"

VITRALARCHITECTURE_CLASSES="./src/vsdk/framework/*.java ./src/vsdk/framework/shapeMatching/*.java ./src/vsdk/framework/shapeMatching/plugins/*.java ./src_jogl/vsdk/framework/shapeMatching/plugins/*.java"

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
#$JAVA_HOME/jre/lib/ext/jogl.all.jar:$JAVA_HOME/jre/lib/ext/gluegen.jar:$JAVA_HOME/jre/lib/ext/jogl.cg.jar

# -proc:none option disables annotation processing which generates warnings
# when using some gluegen/JOGL features.
javac -proc:none -Xmaxerrs 10000 -Xlint:deprecation -Xlint:unchecked -Xlint -classpath ./src:./src_jogl:./src_joglcg -d ./classes $BASIC_CLASSES $IO_CLASSES $AWT_CLASSES $JOGL_CLASSES $VITRALARCHITECTURE_CLASSES

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
chmod 755 `find ./_ide -type d`
chmod 755 `find ./lib -type d`
chmod 755 `find ./pkgs -type d`
chmod 755 `find ./src -type d`
chmod 755 `find ./testsuite -type d`
chmod 644 `find . -name "*.java"`
chmod 644 `find . -name "*.bat"`
chmod 755 `find . -name "*.sh"`

#===========================================================================
#= EOF                                                                     =
#===========================================================================
