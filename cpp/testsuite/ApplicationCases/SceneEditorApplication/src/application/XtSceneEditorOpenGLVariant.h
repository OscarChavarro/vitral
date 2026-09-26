#ifndef __XT_SCENE_EDITOR_OPENGL_VARIANT__
#define __XT_SCENE_EDITOR_OPENGL_VARIANT__

/**
Names of the classes of the OpenGL version the editor is built with, for
the classes shared by every variant (the Xt GUI and the composition root).
The version is chosen when building (see `SCENE_EDITOR_VARIANT` in
CMakeLists.txt): `VITRAL_SCENE_EDITOR_OPENGL1` selects the OpenGL 1.2 fixed
function pipeline, otherwise the OpenGL 4.1 core profile is used.
*/
#ifdef VITRAL_SCENE_EDITOR_OPENGL1
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
#else
class OpenGL4LabelImageProvider;
class XtOpenGL4ApplicationController;
class XtOpenGL4GuiEventExecutor;
class XtOpenGL4SceneBridge;
class XtOpenGL4SceneEditorApplication;
class XtOpenGL4VitralEditorMCP;
typedef OpenGL4LabelImageProvider XtOpenGLLabelImageProvider;
typedef XtOpenGL4ApplicationController XtOpenGLApplicationController;
typedef XtOpenGL4GuiEventExecutor XtOpenGLGuiEventExecutor;
typedef XtOpenGL4SceneBridge XtOpenGLSceneBridge;
typedef XtOpenGL4SceneEditorApplication XtOpenGLSceneEditorApplication;
typedef XtOpenGL4VitralEditorMCP XtOpenGLVitralEditorMCP;
#endif

#endif
