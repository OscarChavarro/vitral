#ifndef __XT_OPENGL1_SCENE_EDITOR_APPLICATION__
#define __XT_OPENGL1_SCENE_EDITOR_APPLICATION__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"

class ApplicationModel;
class XtOpenGL1ApplicationController;

/**
The scene editor application with its Xt GUI and its OpenGL 1.2 drawing
area, as offered to the classes that work over it without presenting it
(i.e. the automation service, `XtOpenGL1VitralEditorMCP`), like
`AwtJogl4SceneEditorApplication` in the Java application.

It is an interface without Xt types, so it can be used from the classes
that work with the vitral `Widget` (whose name collides with the Xt one).
All the methods must be called from the thread of the Xt event loop (see
`XtEventQueue`).
*/
class XtOpenGL1SceneEditorApplication {
public:
    virtual ~XtOpenGL1SceneEditorApplication() {}

    /**
    @return technology independent model of the application
    */
    virtual ApplicationModel* getApplicationModel() = 0;

    /**
    @return the controller of the drawing area
    */
    virtual XtOpenGL1ApplicationController* getOpenGL1Controller() = 0;

    /**
    @return the identifiers of the languages available for the GUI, sorted:
    the names (without extension) of the JSON files in the GUI language
    folder
    */
    virtual java::ArrayList<java::String> getGuiLanguages() = 0;

    /**
    @return the identifier of the language currently used by the GUI
    */
    virtual java::String getCurrentGuiLanguage() = 0;

    /**
    Changes the language of the GUI, rebuilding it (and so, propagating the
    new messages to the application model).
    @param language one of the identifiers given by `getGuiLanguages`
    @return true if the language exists and was selected
    */
    virtual bool setGuiLanguageById(const java::String& language) = 0;

    /**
    Ray traces the scene into the raytraced image of the application model,
    reporting it in the console and exporting it to a file.
    */
    virtual void doRaytracingImage() = 0;

    /**
    Ends the application.
    */
    virtual void closeApplication() = 0;
};

#endif
