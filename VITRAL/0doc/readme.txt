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
