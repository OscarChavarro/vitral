clear
echo "Remember that you need doxygen and graphviz/dot for this to function!"
echo "This could take a time of around 3 minutes, please wait..."
rm -rf doc/html_doxygen
rm -rf doc/html_javadoc
time doxygen doc/_doxygen/doxyfile.config

BASIC_CLASSES="./src/vsdk/toolkit/common/*.java ./src/vsdk/toolkit/common/linealAlgebra/*.java ./src/vsdk/toolkit/environment/*.java ./src/vsdk/toolkit/environment/geometry/*.java ./src/vsdk/toolkit/environment/geometry/polyhedralBoundedSolidNodes/*.java ./src/vsdk/toolkit/environment/scene/*.java ./src/vsdk/toolkit/media/*.java ./src/vsdk/toolkit/render/*.java ./src/vsdk/toolkit/gui/*.java src/vsdk/toolkit/processing/*.java"

IO_CLASSES="./src/vsdk/toolkit/io/*.java ./src/vsdk/toolkit/io/metadata/*.java ./src/vsdk/toolkit/io/image/ImageNotRecognizedException.java ./src/vsdk/toolkit/io/image/ImagePersistenceSGI.java ./src/vsdk/toolkit/io/image/RGBColorPalettePersistence.java ./src/vsdk/toolkit/io/image/NativeImageReaderWrapper.java ./src/vsdk/toolkit/io/image/_NativeImageReaderWrapperHeaderInfo.java ./src/vsdk/toolkit/io/geometry/EnvironmentPersistence.java ./src/vsdk/toolkit/io/geometry/ViewpointBinaryPersistence.java ./src/vsdk/toolkit/io/geometry/FontReader.java ./src/vsdk/toolkit/io/geometry/ParametricBiCubicPatchPersistence.java ./src/vsdk/toolkit/io/geometry/ParametricCurvePersistence.java ./src/vsdk/toolkit/io/geometry/Reader3ds.java ./src/vsdk/toolkit/io/geometry/ReaderAse.java ./src/vsdk/toolkit/io/geometry/ReaderMitScene.java"

AWT_CLASSES="./src_awt/vsdk/toolkit/render/awt/*.java ./src/vsdk/toolkit/gui/CameraController.java ./src_awt/vsdk/toolkit/gui/AwtSystem.java ./src/vsdk/toolkit/gui/CameraControllerAquynza.java ./src/vsdk/toolkit/gui/CameraControllerBlender.java ./src/vsdk/toolkit/gui/TranslateGizmo.java ./src/vsdk/toolkit/gui/RotateGizmo.java ./src/vsdk/toolkit/gui/ScaleGizmo.java ./src/vsdk/toolkit/gui/RendererConfigurationController.java ./src/vsdk/toolkit/io/image/ImagePersistence.java ./src_awt/vsdk/toolkit/io/image/*.java ./src/vsdk/toolkit/io/geometry/ReaderObj.java"

JOGL_CLASSES="./src_jogl/vsdk/toolkit/render/jogl/*.java ./src_joglcg/vsdk/toolkit/render/joglcg/*.java ./src_jogl/vsdk/framework/shapeMatching/*.java ./src_jogl/vsdk/toolkit/io/image/*.java"

VITRALARCHITECTURE_CLASSES="./src/vsdk/framework/*.java ./src/vsdk/framework/shapeMatching/*.java ./src/vsdk/framework/shapeMatching/plugins/*.java ./src_jogl/vsdk/framework/shapeMatching/plugins/*.java"

J2ME_CLASSES="src_jme/vsdk/toolkit/render/j2me/*.java src_jme/vsdk/toolkit/gui/*.java"

ANDROID_CLASSES="src_android/vsdk/toolkit/gui/*.java src_android/vsdk/toolkit/render/androidgles20/*.java"

ANDROID_LIBRARY="/usr/local/androidDeveloperTools/adt-bundle-linux-x86_64-20131030/sdk/platforms/android-19/android.jar"
J2ME_LIBRARY="/usr/local/jme_platform_sdk_3.4/lib/midp_2.1.jar"

javadoc -classpath ./src:./src_android:$J2ME_LIBRARY:$ANDROID_LIBRARY -d ./doc/html_javadoc $BASIC_CLASSES $IO_CLASSES $AWT_CLASSES $JOGL_CLASSES $VITRALARCHITECTURE_CLASSES $J2ME_CLASSES $ANDROID_CLASSES

