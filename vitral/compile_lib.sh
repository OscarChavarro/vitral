#!/bin/sh
#===========================================================================

BASIC_CLASSES="vsdk/toolkit/common/*.java vsdk/toolkit/environment/*.java vsdk/toolkit/environment/geometry/*.java vsdk/toolkit/environment/geometry/polyhedralBoundedSolidNodes/*.java vsdk/toolkit/environment/scene/*.java vsdk/toolkit/media/*.java vsdk/toolkit/render/*.java vsdk/toolkit/gui/Gizmo.java vsdk/toolkit/gui/ProgressMonitor.java vsdk/toolkit/gui/ProgressMonitorConsole.java vsdk/toolkit/gui/Controller.java vsdk/toolkit/gui/PresentationElement.java vsdk/toolkit/processing/*.java"

IO_CLASSES="vsdk/toolkit/io/*.java vsdk/toolkit/io/image/ImageNotRecognizedException.java vsdk/toolkit/io/image/ImagePersistenceSGI.java vsdk/toolkit/io/image/RGBColorPalettePersistence.java vsdk/toolkit/io/geometry/EnvironmentPersistence.java vsdk/toolkit/io/geometry/FontReader.java vsdk/toolkit/io/geometry/ParametricBiCubicPatchPersistence.java vsdk/toolkit/io/geometry/ParametricCurvePersistence.java vsdk/toolkit/io/geometry/Reader3ds.java"

AWT_CLASSES="vsdk/toolkit/render/awt/*.java vsdk/toolkit/gui/CameraController.java vsdk/toolkit/gui/CameraControllerAquynza.java vsdk/toolkit/gui/CameraControllerBlender.java vsdk/toolkit/gui/TranslateGizmo.java vsdk/toolkit/gui/RotateGizmo.java vsdk/toolkit/gui/ScaleGizmo.java vsdk/toolkit/gui/RendererConfigurationController.java vsdk/toolkit/io/image/ImagePersistence.java vsdk/toolkit/io/image/TargaImage.java vsdk/toolkit/io/geometry/ReaderObj.java"

JOGL_CLASSES="vsdk/toolkit/render/jogl/*.java"

#---------------------------------------------------------------------------

if [ ! -d ./classes ]; then
    mkdir ./classes
fi

if [ ! -d ./lib ]; then
    mkdir ./lib
fi

clear
cd src
javac -Xlint -classpath . -d ../classes $BASIC_CLASSES $IO_CLASSES $AWT_CLASSES $JOGL_CLASSES
cd ..
cd classes
jar cf ../lib/vsdk.jar vsdk
cd ..
cd pkgs/SpharmonicKit27;make;cd ../..

#===========================================================================
#= EOF                                                                     =
#===========================================================================
