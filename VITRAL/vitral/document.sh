clear
echo "Remember that you need doxygen and graphviz/dot for this to function!"
echo "This could take a time of around 3 minutes, please wait..."
rm -rf doc/html
time doxygen doc/_doxygen/doxyfile.config
