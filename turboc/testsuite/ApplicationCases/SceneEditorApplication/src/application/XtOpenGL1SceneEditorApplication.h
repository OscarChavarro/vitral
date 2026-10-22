#ifndef __XT_OPENGL1_SCENE_EDITOR_APPLICATION__
#define __XT_OPENGL1_SCENE_EDITOR_APPLICATION__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/lang/IllegalArgumentException.h"

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
    virtual ArrayList<String> getGuiLanguages() = 0;

    /**
    @return the identifier of the language currently used by the GUI
    */
    virtual String getCurrentGuiLanguage() = 0;

    /**
    Changes the language of the GUI, rebuilding it (and so, propagating the
    new messages to the application model).
    @param language one of the identifiers given by `getGuiLanguages`
    @return true if the language exists and was selected
    */
    virtual bool setGuiLanguageById(const String& language) = 0;

    /**
    Ray traces the scene into the raytraced image of the application model,
    reporting it in the console and exporting it to a file.
    */
    virtual void doRaytracingImage() = 0;

    /** Opens the native File > Import dialog, owned by the editor window. */
    virtual bool showImportSceneDialog() = 0;

    /**
    Opens the tangible interface connection dialog, as its GUI command does
    (`IDC_TOOLS_TANGIBLE_INTERFACE_CLIENT`).
    */
    virtual void openTangibleInterfaceDialog() = 0;

    /**
    @return true while the tangible interface connection dialog is shown
    */
    virtual bool isTangibleInterfaceDialogVisible() = 0;

    /**
    @return text of the service URL field of the tangible interface dialog
    */
    virtual String getTangibleInterfaceDialogServiceUrl() = 0;

    /**
    @return text of the connect button of the tangible interface dialog
    */
    virtual String getTangibleInterfaceDialogConnectLabel() = 0;

    /**
    Clicks a button of the tangible interface dialog.
    @param action "connect" or "cancel"
    @param serviceUrl if not null, typed in the field before connecting
    @return false if the action is not a button of the dialog
    */
    virtual bool tangibleInterfaceDialogAction(const String& action,
        const String* serviceUrl) = 0;

    /**
    Opens (or refreshes) the scene selector dialog, as its key does.
    */
    virtual void openSelectorDialog() = 0;

    /**
    @return true while the scene selector dialog is shown
    */
    virtual bool isSelectorDialogVisible() = 0;

    /**
    Selects a geometry object through the tree of the scene selector.
    @param index index of the body, or negative to clear the selection
    */
    virtual void selectorSelectBody(int index) = 0;

    /**
    Runs an action of the scene selector (deleteSelected,
    toggleVisibility).
    @throws IllegalArgumentException for unknown actions
    */
    virtual void selectorAction(const String& action) = 0;

    /**
    Ends the application.
    */
    virtual void closeApplication() = 0;
};

#endif
