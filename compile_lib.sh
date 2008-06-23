#!/bin/sh
#===========================================================================

BASIC_CLASSES="vsdk/toolkit/common/*.java vsdk/toolkit/environment/*.java vsdk/toolkit/environment/geometry/*.java vsdk/toolkit/environment/geometry/polyhedralBoundedSolidNodes/*.java vsdk/toolkit/environment/scene/*.java vsdk/toolkit/media/*.java vsdk/toolkit/render/*.java vsdk/toolkit/gui/Gizmo.java vsdk/toolkit/gui/ViewportWindow.java vsdk/toolkit/gui/ViewportWindowSetManager.java vsdk/toolkit/gui/ProgressMonitor.java vsdk/toolkit/gui/ProgressMonitorConsole.java vsdk/toolkit/gui/Controller.java vsdk/toolkit/gui/PresentationElement.java vsdk/toolkit/processing/*.java vsdk/toolkit/gui/KeyEvent.java"

IO_CLASSES="vsdk/toolkit/io/*.java vsdk/toolkit/io/metadata/*.java vsdk/toolkit/io/image/ImageNotRecognizedException.java vsdk/toolkit/io/image/ImagePersistenceSGI.java vsdk/toolkit/io/image/RGBColorPalettePersistence.java vsdk/toolkit/io/geometry/EnvironmentPersistence.java vsdk/toolkit/io/geometry/ViewpointBinaryPersistence.java vsdk/toolkit/io/geometry/FontReader.java vsdk/toolkit/io/geometry/ParametricBiCubicPatchPersistence.java vsdk/toolkit/io/geometry/ParametricCurvePersistence.java vsdk/toolkit/io/geometry/Reader3ds.java vsdk/toolkit/io/geometry/ReaderAse.java"

AWT_CLASSES="vsdk/toolkit/render/awt/*.java vsdk/toolkit/gui/CameraController.java vsdk/toolkit/gui/AwtSystem.java vsdk/toolkit/gui/CameraControllerAquynza.java vsdk/toolkit/gui/CameraControllerBlender.java vsdk/toolkit/gui/TranslateGizmo.java vsdk/toolkit/gui/RotateGizmo.java vsdk/toolkit/gui/ScaleGizmo.java vsdk/toolkit/gui/RendererConfigurationController.java vsdk/toolkit/io/image/ImagePersistence.java vsdk/toolkit/io/image/TargaImage.java vsdk/toolkit/io/geometry/ReaderObj.java"

JOGL_CLASSES="vsdk/toolkit/render/jogl/*.java"

VITRALARCHITECTURE_CLASSES="vsdk/framework/*.java vsdk/framework/shapeMatching/*.java"

#---------------------------------------------------------------------------

# Create empty working directories if they does not exist
if [ ! -d ./classes ]; then
    mkdir ./classes
fi

if [ ! -d ./lib ]; then
    mkdir ./lib
fi

clear
cd src
javac -Xlint:deprecation -Xlint:unchecked -Xlint -classpath . -d ../classes $BASIC_CLASSES $IO_CLASSES $AWT_CLASSES $JOGL_CLASSES $VITRALARCHITECTURE_CLASSES
cd ..
cd classes
jar cf ../lib/vsdk.jar vsdk
cd ..
cd pkgs/SpharmonicKit27;make;cd ../..
cd pkgs/LempelZivWelch;make;cd ../..

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
