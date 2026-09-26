#ifndef __XT_APPLICATION_HOST__
#define __XT_APPLICATION_HOST__

#include <string>

#include <X11/Intrinsic.h>

#include "gui/xt/XtModifyPanelHost.h"

class RGBImageUncompressed;
class XtOpenGL4SceneBridge;
class XtUiFactory;

/**
Services that the Xt GUI classes need from the application hosting them, as
`AwtApplicationHost` provides them to the AWT/Swing ones. It keeps those
classes independent of the rendering technology of the drawing area.
*/
class XtApplicationHost : public XtModifyPanelHost {
public:
    virtual ~XtApplicationHost() {}

    /**
    @return the scene model, its commands and its drawing area
    */
    virtual XtOpenGL4SceneBridge* getSceneBridge() = 0;

    /**
    @return the widget set of the GUI and its file dialog
    */
    virtual XtUiFactory* getUiFactory() = 0;

    /**
    @param id identifier of a message of the I18N file (`IDM_*`)
    @return its text in the language of the GUI, or the identifier
    */
    virtual std::string getMessage(const char* id) = 0;

    /**
    @param message text for the status bar
    */
    virtual void showStatusMessage(const std::string& message) = 0;

    /**
    Shows an image (i.e. the raytraced one) in the image control window.
    @param image the image (not owned)
    */
    virtual void showImage(RGBImageUncompressed* image) = 0;

    /**
    Creates a popup shell with the visual of the application (its GLX
    visual is not the default one of the screen), for dialogs and windows.
    @param name name of the shell
    @param shellClass class of the shell (i.e. `transientShellWidgetClass`)
    @param title title of its window
    */
    virtual Widget createDialogShell(const char* name, WidgetClass shellClass,
                                     const char* title) = 0;

    /**
    Changes the language of the GUI, rebuilding it once the current event
    is processed (it may come from a widget that is destroyed).
    @param language identifier of a language (i.e. "spanish")
    */
    virtual void setGuiLanguage(const std::string& language) = 0;

    /**
    Ray traces the scene into the raytraced image of the application model.
    */
    virtual void doRaytracingImage() = 0;

    /**
    Ends the application.
    */
    virtual void closeApplication() = 0;
};

#endif
