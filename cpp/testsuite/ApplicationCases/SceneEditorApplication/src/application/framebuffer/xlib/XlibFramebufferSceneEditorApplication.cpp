#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <exception>

#include "application/framebuffer/FramebufferSceneEditor.h"
#include "java/util/ArrayList.txx"
#include "model/GuiState.h"
#include "vsdk/toolkit/gui/framebuffer/xlib/XlibFramebuffer.h"
#include "vsdk/toolkit/gui/framebuffer/xlib/XlibInput.h"

// The scene editor on the Xlib backend of the framebuffer GUI: one X11 window
// drawn with Xlib primitives and a core font, without toolkits or OpenGL (see
// FramebufferSceneEditor). The VSDKFatalException of the model are
// std::exceptions.
namespace {

void usage()
{
    std::printf(
        "usage: SceneEditorApplication [-display DISPLAY] [-geometry WxH+X+Y]"
        " [-fn FONT] [LANGUAGE]\n"
        "  -geometry  size and place of the window; by default it fills the"
        " work area\n"
        "             of the desktop and asks the window manager to maximize"
        " it\n"
        "  -fn        a fixed width core font (default: the first of 10x20,"
        " 9x15,\n"
        "             8x13 and fixed found)\n"
        "  LANGUAGE   a GUI definition of etc/gui (english, spanish...);"
        " also the\n"
        "             SCENE_EDITOR_GUI_LANGUAGE environment variable\n");
}

bool isKnownLanguage(const char* language)
{
    java::ArrayList<java::String> languages = GuiState::listLanguages();
    for ( long i = 0; i < languages.size(); i++ ) {
        if ( languages.get(i).equals(language) ) {
            return true;
        }
    }
    return false;
}

}

int main(int argc, char** argv)
{
    const char* display = nullptr;
    const char* geometry = nullptr;
    const char* font = nullptr;
    const char* language = std::getenv("SCENE_EDITOR_GUI_LANGUAGE");
    for ( int i = 1; i < argc; i++ ) {
        bool hasValue = i + 1 < argc;
        if ( std::strcmp(argv[i], "-display") == 0 && hasValue ) {
            display = argv[++i];
        }
        else if ( (std::strcmp(argv[i], "-geometry") == 0 ||
                   std::strcmp(argv[i], "-g") == 0) && hasValue ) {
            geometry = argv[++i];
        }
        else if ( (std::strcmp(argv[i], "-fn") == 0 ||
                   std::strcmp(argv[i], "-font") == 0) && hasValue ) {
            font = argv[++i];
        }
        else if ( argv[i][0] != '-' ) {
            language = argv[i];
        }
        else {
            usage();
            return argv[i][1] == 'h' ? 0 : 1;
        }
    }
    if ( language == nullptr || language[0] == '\0' ) {
        language = "english";
    }
    if ( !isKnownLanguage(language) ) {
        std::fprintf(stderr, "Unknown GUI language %s: see %s\n", language,
                     GuiState::GUI_LANGUAGE_FOLDER);
        return 1;
    }

    XlibFramebuffer surface;
    if ( !surface.open(display, geometry, font,
                       "Vitral Scene Editor - X11 framebuffer") ) {
        return 1;
    }
    surface.waitMapped();
    XlibInput input(surface);
    int status = 1;
    try {
        FramebufferSceneEditor editor(surface, input, language);
        status = editor.run();
    }
    catch ( const std::exception& exception ) {
        std::fprintf(stderr, "SceneEditorApplication: %s\n", exception.what());
    }
    surface.close();
    return status;
}
