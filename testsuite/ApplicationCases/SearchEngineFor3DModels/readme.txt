===========================================================================
Operation instructions
===========================================================================

This folder contains a VITRAL based 3D search engine, capable of processing
3D objects from VITRAL supported geometry files (as such .3DS and .OBJ) for
descriptor based characterization and shape matching indexing and retrieving.

The system can be used on stand-alone command line based operation (mainly
for quick test and development/debugging purposes) or as a web application
(for end users).

This directory contains three (3) applications:
  - BatchConsole: it is the simplest of all interfaces to the search engine.
    It is supposed to be used for simple tests with small number of models
    (less than 10k).  It must allways read the full database before any
    operation, so it is slow. Use it for testing purposes only and with
    small number of 3D objects.
    This application can be accessed using the "./run.sh" script, and
    needs the passing of a command. Running application without a command
    will list supported commands. It is recommended to use this application
    for initial 3D repository analysis, as described on installation step 9.

  - ServerDaemon & NetworkClientConsole: a modified and improved version of
    BatchConsole which only loads the database once (at startup), and after
    that enters in a cicle of recieving network connections over which
    commands can be sent. The ServerDaemon program generates an end user
    formatted output in an html file. Commands are the same from the
    BatchConsole application, but are given by using the NetworkClientConsole.
    In this way, for using this application, open two terminals, in one run
    the ServerDaemon by issuing the command
        sh ./runServer.sh
    and in the second terminal run the client console by running
        sh ./runClient.sh

  - ServletConsole: a modified version of ServerDaemon, which has been
    adapted for its used inside a tomcat / J2EE web application container.
    It is recommended to use it as described on web installation procedure.

===========================================================================
Web Installation Procedure
===========================================================================

The web based "ServletConsole" depends on VitralSDK toolkit, java (J2SE) and
tomcat web application container. To install follow this steps:

LINUX INSTALLATION INSTRUCTIONS
1. Build VitralSDK (which implies having installed J2SE and JOGL)
2. Install tomcat, which can be downloaded from http://tomcat.apache.org
3. Edit the "compileServlets.sh" and set the TOMCAT_DIR variable to
   point to the installation directory used by tomcat on step 2.
4. Configure tomcat to include an administrator user, so you can deploy
   applications (needed on step 8).  On the tomcat installation folder
   below, edit the file conf/tomcat-users.xml, and do something like:

<?xml version='1.0' encoding='utf-8'?>
<tomcat-users>
  <role rolename="manager"/>
  <role rolename="admin"/>
  <user username="mylogin" password="mypassword" roles="admin,manager"/>
</tomcat-users>

5. Start the tomcat webserver / application container by entering the
   bin directory under the main tomcat folder, and execute the
   "startup.sh" script. Note that all Vitral based servlets will print
   informational messages on tomcat's file log/catalina.out by
   default. Usually, you will have from here access to a page on
   http://localhost:8080.

6. Configure the vitral based servlet code to match your installation
   directories. By now, you will have to manually edit the file
   src/ServletConsole.java, and edit the "serverUrl" and
   "databaseFile" variables.

7. Run all the compilation scripts to make the system's applications:
   ./compile.sh
   ./compileServer.sh;./compileClient.sh
   ./compileApplets.sh;./compileServlets.sh
   the "compileServlets.sh" script try to copy the .war package on a
   first time non existent tomcat application folder. Ignore copying errors.

8. For the first time, manually deploy the generated vitralSearchEngine.war
   application to tomcat. By doing now and browsing to
   http://localhost:8080 you should see a still non-functional webpage
   search engine (we still do not have loaded files).

9. Use the BatchConsole application to populate the 3D repository of
   descriptors. The easier way of doing that is to provide a list of
   3D files in a simple text file. The UNIX "find" utility makes a good
   job in this:
       find / -name "*.3ds" > modelsList.txt
       ./run.sh addList modelsList.txt
   depending on the number of objects given, this step can last several
   hours. On a 2.5Mhz Intel Xeon processor with an Nvidia Quadro FX5600,
   10000 simple 3ds files are processed on about 4 hours. Note that
   this application make use of graphics card acceleration to render
   previews, so you need to run this process under an active X session.

10. Once the 3D objects are analized, you will find the descriptor
   database located on the binary file ./etc/metadata.bin, and a set
   of preview images / thumbnails on ./output folder. Recall from steps
   2 and 6 the final locations of those elements on your system's
   configuration. Usually, you will have to move the ./output/previews
   folder under tomcat's webapps/ROOT/images folder.

11. Test drive the system by opening a Java-enabled browser and loading
   the page on http://localhost:8080/vitralSearchEngine (or whatever
   network name you need to give). Try to draw a simple pattern and make
   a 2D sketch based query.  If "0 objects" are in database, check the
   databasefile location from step 6. A common error is a query given
   results with no images, on that case, check "serverUrl" from step 6
   and correct image file locations from step 10.

WINDOWS INSTALLATION INSTRUCTIONS

The rationale behind a Windows installation follows the same steps as on
the LINUX / UNIX installation, except that ".bat" scripts are provided and
and aditional procedure must be done for step 9 to work.

The current version of the system make use of an spherical harmonics
math kernel, which is coded on C++ and included on VitralSDK toolkit
distribution as an aditional package, which make use of JNI technology
(i.e. it is a "native" method, platform dependent library). The VitralSDK
build scripts compiles this libraries automatically on UNIX/Linux systems,
but for Windows, this has to be done manually.

For this, Microsoft Visual Studio is needed to open the C++ compilation
project on ../../../pkgs/SpharmonicKit27/_ide. When done, the compilation
will generate a .dll that should be place under WINDOWS/SYSTEM32 directory.
After installing the native library this way, step 9 on standard installarion
should work fine.

For the "find" usage shown in step 9, it is recommended to install CYGWIN
on Windows (freely available at http://www.cygwin.com).

===========================================================================
A note about the 2D sketch applet
===========================================================================

The included 2D sketch applet was borrowed from http://shape.cs.princeton.edu,
and later modified and optimized. This modification process was followed in
this procedure:

  - After html code examination of http://shape.cs.princeton.edu pages,
    the original applet's .jar file containing the applet classes was
    downloaded.
  - The .jar file was unpacked, and the resulting .class files was decompiled
    using jcavaj decompiler, which resulted in .java source files (without
    comments).
  - Resulting java files was studied an enhanched. Original old java code
    (JDK 1.1. based) was updated to J2SE 6, to remove most of compiling
    warnings.
  - Minor modifications was made to applet code in order to organize logging
    messages.
  - Finally, current applet was integrated to servlet code of current
    implementation.

===========================================================================
= EOF                                                                     =
===========================================================================
