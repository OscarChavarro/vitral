Current directory contains an example of a native dynamic link library
(also known  as shared object) developed on C language, and two sample
programs for the library, one developed also in C (the native program)
and the other developed on Java (which uses the Java Native Interface
JNI).

You will need a C compiler (tested with GNU GCC on Linux and Microsoft
Visual Studio C++ 2005) and Java J2SDK (tested with J2SDK 6).

First compile the java project (this will export C declarations needed
to compile the C library). Later compile the native program and finally
the java program.

You will need to locate the compiled native library in the native
program working directory (or modify your LD_LIBRARY_PATH environment
library) for running the native program.

Adittional documentation made on Microsoft Visio can be found on the
doc folder.
