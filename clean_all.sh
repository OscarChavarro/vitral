cd pkgs/SpharmonicKit27;make clean;cd ../..
rm -rf `find . -name "*~"` `find . -name "*.jar"` `find . -name "*.war"` `find . -name "*.class"` `find . -name "output.jpg"` ./doc/html_doxygen ./doc/html_javadoc ./doc/_doxygen/warnings.log
rm -rf `find . -type d -name "lib"` `find . -type d -name "classes"` testsuite/ApplicationCases/SearchEngineFor3DModels/tmp pkgs/SpharmonicKit27/_ide/VisualStudioDotNet2005/SpharmonicKit/Release pkgs/SpharmonicKit27/_ide/VisualStudioDotNet2005/SpharmonicKit/Debug
if [ -f ./testsuite/ApplicationCases/SearchEngineFor3DModels/etc/metadata.bin ]; then
    rm -i ./testsuite/ApplicationCases/SearchEngineFor3DModels/etc/metadata.bin
fi
