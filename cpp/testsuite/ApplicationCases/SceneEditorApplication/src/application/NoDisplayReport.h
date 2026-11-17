#ifndef __NO_DISPLAY_REPORT__
#define __NO_DISPLAY_REPORT__

#include <cstdio>
#include <cstdlib>

/**
The editor needs a graphical display: without one (i.e. with no DISPLAY
variable defined, or with one naming a display that can not be opened), the
reason is reported in a short message, instead of a fatal error, and the
application ends with status 1.

C++ counterpart of `exitIfNoDisplayIsAvailable` of Java's
`AwtJogl4SceneEditorApplication`: AWT checks for a display before creating
the first window, while the X11 variants know there is none when opening it
fails, so this is called then.
*/
class NoDisplayReport {
public:
    static void exitBecauseNoDisplayIsAvailable()
    {
        const char* display = getenv("DISPLAY");

        if ( display == nullptr || display[0] == '\0' ) {
            fprintf(stderr, "ERROR: No graphical display is available: the "
                "DISPLAY environment variable is not defined.\n");
            fprintf(stderr, "The scene editor needs a display. Define DISPLAY "
                "(i.e. export DISPLAY=:0), connect with X11 forwarding "
                "(ssh -X), or use a virtual display (i.e. xvfb-run "
                "./run.sh).\n");
        }
        else {
            fprintf(stderr, "ERROR: No graphical display is available: the "
                "display \"%s\" (DISPLAY environment variable) can not be "
                "opened.\n", display);
            fprintf(stderr, "The scene editor needs a display. Check that "
                "the X server is running and accepts connections, or use a "
                "virtual display (i.e. xvfb-run ./run.sh).\n");
        }
        fflush(stderr);
        exit(1);
    }
};

#endif
