clear
echo "Remember that you need doxygen and graphviz/dot for this to function!"
echo "This could take a time of around 11 minutes, please wait..."
rm -rf doc/html_doxygen
rm -rf doc/html_javadoc
time doxygen doc/_doxygen/doxyfile.config
javadoc -d ./doc/html_javadoc ./src/vsdk/framework/* ./src/vsdk/framework/shapeMatching/* ./src/vsdk/toolkit/common/* ./src/vsdk/toolkit/environment/* ./src/vsdk/toolkit/environment/geometry/* ./src/vsdk/toolkit/environment/geometry/polyhedralBoundedSolidNodes/* ./src/vsdk/toolkit/environment/scene/* ./src/vsdk/toolkit/gui/* ./src/vsdk/toolkit/io/* ./src/vsdk/toolkit/io/geometry/* ./src/vsdk/toolkit/io/image/* ./src/vsdk/toolkit/io/metadata/* ./src/vsdk/toolkit/media/* ./src/vsdk/toolkit/processing/* ./src/vsdk/toolkit/render/* ./src/vsdk/toolkit/render/awt/* ./src/vsdk/toolkit/render/jogl/*
