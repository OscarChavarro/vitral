#include "gui/PointerCursor.h"

const char* const PointerCursor::IMAGE_FOLDER = "./etc/cursors/";

namespace {
const char* const IMAGE_FILE_NAMES[PointerCursor::COUNT] = {
    nullptr,
    nullptr,
    "cursor_camrotate.gif",
    "cursor_camtranslate.gif",
    "cursor_camadvance.gif",
    "cursor_translate.png",
    "cursor_rotate.png",
    "cursor_scale.png"
};

const char* const DISPLAY_NAMES[PointerCursor::COUNT] = {
    "Select",
    "ViewportTitle",
    "CameraRotation",
    "CameraTranslation",
    "CameraAdvance",
    "Translation",
    "Rotation",
    "Scale"
};
}

java::String PointerCursor::getImagePath(Value cursor)
{
    const char* imageFileName = IMAGE_FILE_NAMES[cursor];
    return imageFileName == nullptr ? java::String("") :
        java::String(IMAGE_FOLDER) + imageFileName;
}

java::String PointerCursor::getDisplayName(Value cursor)
{
    return DISPLAY_NAMES[cursor];
}
