#ifndef __XT_SCENE_EDITOR_OPENGL_VARIANT__
#define __XT_SCENE_EDITOR_OPENGL_VARIANT__

/**
Names of the classes of the OpenGL version the editor is built with, for
the classes shared by every variant (the Xt GUI and the composition root).
The turboc port only has the OpenGL 1.2 fixed function variant (the cpp
port also offers OpenGL 4.1): the names are kept so the shared classes stay
as in the cpp port.
*/
class OpenGL1LabelImageProvider;
class XtOpenGL1ApplicationController;
class XtOpenGL1GuiEventExecutor;
class XtOpenGL1SceneBridge;
class XtOpenGL1SceneEditorApplication;
class XtOpenGL1VitralEditorMCP;
typedef OpenGL1LabelImageProvider XtOpenGLLabelImageProvider;
typedef XtOpenGL1ApplicationController XtOpenGLApplicationController;
typedef XtOpenGL1GuiEventExecutor XtOpenGLGuiEventExecutor;
typedef XtOpenGL1SceneBridge XtOpenGLSceneBridge;
typedef XtOpenGL1SceneEditorApplication XtOpenGLSceneEditorApplication;
typedef XtOpenGL1VitralEditorMCP XtOpenGLVitralEditorMCP;

#endif
