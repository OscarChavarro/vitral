#ifndef __POINTER_CURSOR__
#define __POINTER_CURSOR__

#include "java/lang/String.h"

/**
Kinds of pointer shapes the drawing area asks the GUI technology to present.
Each one names the image that presents it (in `IMAGE_FOLDER`), shared by all
the GUI technologies, or none for the default pointer of the system.

C++ port note: the Java enum with fields is a class with an enumeration and
static methods taking the kind.
*/
class PointerCursor {
public:
    enum Value {
        /** Normal pointer for selection */
        SELECT,
        /** Normal pointer over the title of a viewport (feedback that it can
        be clicked) */
        VIEWPORT_TITLE,
        /** Camera rotation gesture */
        CAMERA_ROTATE,
        /** Camera translation gesture */
        CAMERA_TRANSLATE,
        /** Camera advance gesture */
        CAMERA_ADVANCE,
        /** Selected things can be translated (translation mode with a
        selection) */
        TRANSLATE,
        /** Selected things can be rotated (rotation mode with a selection) */
        ROTATE,
        /** Selected things can be scaled (scale mode with a selection) */
        SCALE
    };
    static const int COUNT = 8;

    /** Folder with the images of the pointers */
    static const char* const IMAGE_FOLDER;

    /**
    @return path of the image of the pointer (hot spot at its center), or an
    empty string if the default pointer of the system is used
    */
    static java::String getImagePath(Value cursor);

    /**
    @return name of the pointer, for GUI technologies that name their
    cursors
    */
    static java::String getDisplayName(Value cursor);

private:
    PointerCursor() {}
};

#endif
