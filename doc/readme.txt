VITRAL CONFORMANCE CODING STANDARD:

ARCHITECTURE (ORGANIZATION)
  - Directories must follow a POSIX scheme: src, classes, doc, lib, tmp, etc
  - Scripts for compilation and execution must be provided for linux/unix and windows:
    compile.sh
    run.sh

DESIGN (GUI)
  - If using GUI, must use Swing, and try not to use AWT (i.e. use JFrame instead of Frame)
  - JFrames must include a nmemonic label identifying the program in its window title
  - JFrames must be configured to exit:
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

IMPLEMENTATION (SIZE RESTRICTION, DISTRIBUTION AND DEPLOYMENT)
  - Implementation should be distributed in two main files: one basic small
    autocontained package and one extra samples-only, posibly big package.
  - All files included in the basic package must be: java code, scripts,
    basic documentation and SMALL samples. This package should be as small
    as posible, and avoid the inclusion of big data files.

IMPLEMENTATION (PACKAGE USAGE AT THE TOOLKIT LEVEL)
  - In the sage of clarity, sentences like import package.*; shoul be avoided.
    As in the toolkit classes, as in the testsuite examples, all used classes
    should be imported using its full name, to help the application
    developer be aware of the complexity of the application, and the
    used classes.

IMPLEMENTATION ON OPENGL/JOGL
  - glLight operations are restricted to inside og Light renderer classes
  - glLoadIdentity should be avoided in application code, preferring a 
    glPushMatrix/glPopMatrix pair without glLoadIdentity use
  - glCullFace, GL_CULLING should be avoided in application code
  - glMaterial, glShadingModel should be only inside Material renderers

VERSION CONTROL

Latest version of the Vitral SDK project source code is available to the public
on the internet and is hosted at SourceForge.net SVN service. To download
vitral use the following command on linux or UNIX command line:

svn co https://vitral.svn.sourceforge.net/svnroot/vitral/vitral/trunk vitral

On windows, using tortoise:
  - Create an empty folder named "vitral" and double click on it
  - Right click on explorer context menu, and select "SVN Checkout"
  - On URL of repository use
