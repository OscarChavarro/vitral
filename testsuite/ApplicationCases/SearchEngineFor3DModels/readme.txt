===========================================================================
This directory contains three (3) applications:
  - BatchConsole: it is the simplest of all interfaces to the search engine.
    It is supposed to be used for simple tests with small number of models
    (less than 10k).  It must allways read the full database before any
    operation, so it is slow. Use it for testing purposes.
  - ServerDaemon: a modified and improved version of BatchConsole which only
    load the database once (at startup), and after that enters in a cicle of
    recieving network connections over which commands can be sent. This program
    generates an end user formatted output in an html file.
  - NetworkClientConsole: simple text console for ServerDaemon.

===========================================================================
About the 2D sketch applet
===========================================================================

The included 2D sketch applet was borrowed from http://shape.cs.princeton.edu,
and later modified and bettered. It followed the following process:

  - After html code examination, the original .jar file containing the
    applet classes was downloaded from http://shape.cs.princeton.edu.
  - The .jar file was unpacked, and the resulting .class files was decompiled
    using jcavaj decompiler, which resulted in .java source files (without
    comments).
  - Resulting java files was studied an enhanched. Original old java code
    (1.1.?) was updated to java 1.6, to remove most of compiling warnings.
  - Minor modifications was made to applet code in order to organize logging
    messages.
  - Finally, current applet was integrated to servlet code of current
    implementation.

===========================================================================
= EOF                                                                     =
===========================================================================
