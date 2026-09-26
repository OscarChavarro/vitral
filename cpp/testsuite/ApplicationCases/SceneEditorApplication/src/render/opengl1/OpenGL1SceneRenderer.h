#ifndef __SCENE_EDITOR_OPEN_GL_1_SCENE_RENDERER__
#define __SCENE_EDITOR_OPEN_GL_1_SCENE_RENDERER__
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
class BodyEditFeedbackProvider;class Camera;class Light;class RendererConfiguration;class Scene;class SimpleBody;class SimpleBodyGroup;
class OpenGL1SceneRenderer{public:static void drawBody(SimpleBody*,Camera*,const java::ArrayList<Light*>*,RendererConfiguration*);static void drawBodyGroup(SimpleBodyGroup*,Camera*,const java::ArrayList<Light*>*,RendererConfiguration*);static void draw(Scene*,BodyEditFeedbackProvider*);static void drawEditorOverlays(Scene*,BodyEditFeedbackProvider*);private:static void drawBody(SimpleBody*,const Matrix4x4d&,Camera*,const java::ArrayList<Light*>*,RendererConfiguration*);static void drawLightsAndDebugEntities(Scene*);};
#endif
