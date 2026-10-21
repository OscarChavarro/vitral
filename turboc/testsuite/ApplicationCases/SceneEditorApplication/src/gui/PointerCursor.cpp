#include <stddef.h>

#include "gui/PointerCursor.h"

const char* const PointerCursor::IMAGE_FOLDER = "../../../../etc/cursors/";

static const char* const IMAGE_FILE_NAMES[PointerCursor::COUNT] = {
    NULL,
    NULL,
    "cursor_camrotate.gif",
    "cursor_camtranslate.gif",
    "cursor_camadvance.gif",
    "cursor_translate.png",
    "cursor_rotate.png",
    "cursor_scale.png"
};

static const char* const DISPLAY_NAMES[PointerCursor::COUNT] = {
    "Select",
    "ViewportTitle",
    "CameraRotation",
    "CameraTranslation",
    "CameraAdvance",
    "Translation",
    "Rotation",
    "Scale"
};

String PointerCursor::getImagePath(Value cursor)
{
    const char* imageFileName = IMAGE_FILE_NAMES[cursor];
    return imageFileName == NULL ? String("") :
        String(IMAGE_FOLDER) + imageFileName;
}

String PointerCursor::getDisplayName(Value cursor)
{
    return DISPLAY_NAMES[cursor];
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const int PointerCursor::COUNT;
