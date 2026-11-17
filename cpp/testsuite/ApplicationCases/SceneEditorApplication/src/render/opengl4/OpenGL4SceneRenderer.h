#ifndef __SCENE_EDITOR_OPEN_GL_4_SCENE_RENDERER__
#define __SCENE_EDITOR_OPEN_GL_4_SCENE_RENDERER__
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
class BodyEditFeedbackProvider;class Camera;class Light;class RendererConfiguration;class Scene;class SimpleBody;class SimpleBodyGroup;
/**
Draws the scene of the editor into the current viewport with the GL4 core
pipeline: every geometry goes through `OpenGL4GeometryRenderer`, so all of
them honor the same bits of the `RendererConfiguration` of the viewport (the
polyhedral bounded solids, or breps, without texture nor normal map).
*/
class OpenGL4SceneRenderer{public:static void drawBody(SimpleBody*,Camera*,const java::ArrayList<Light*>*,RendererConfiguration*);static void drawBodyGroup(SimpleBodyGroup*,Camera*,const java::ArrayList<Light*>*,RendererConfiguration*);static void draw(Scene*,BodyEditFeedbackProvider*);static void drawEditorOverlays(Scene*,BodyEditFeedbackProvider*);private:static void drawBody(SimpleBody*,const Matrix4x4d&,Camera*,const java::ArrayList<Light*>*,RendererConfiguration*);static void drawLightsAndDebugEntities(Scene*);};
#endif
