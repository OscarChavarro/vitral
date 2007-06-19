clear
echo "Remember that you need doxygen and graphviz/dot for this to function!"
echo "This could take a time of around 3 minutes, please wait..."
rm -rf doc/html_doxygen
rm -rf doc/html_javadoc
time doxygen doc/_doxygen/doxyfile.config
javadoc -d ./doc/html_javadoc src/vsdk/toolkit/common/*.java src/vsdk/toolkit/environment/*.java src/vsdk/toolkit/environment/geometry/*.java src/vsdk/toolkit/gui/*.java src/vsdk/toolkit/media/*.java src/vsdk/toolkit/io/*.java src/vsdk/toolkit/io/geometry/*.java src/vsdk/toolkit/io/image/*.java src/vsdk/toolkit/render/*.java src/vsdk/toolkit/render/jogl/*.java src/vsdk/toolkit/render/awt/*.java
