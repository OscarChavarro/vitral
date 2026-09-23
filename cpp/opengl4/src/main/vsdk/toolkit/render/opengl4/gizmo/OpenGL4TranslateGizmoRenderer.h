#ifndef __OPEN_GL_4_TRANSLATE_GIZMO_RENDERER__
#define __OPEN_GL_4_TRANSLATE_GIZMO_RENDERER__
class TranslateGizmo; class Camera;
class OpenGL4TranslateGizmoRenderer { public: static void draw(TranslateGizmo* gizmo, Camera* camera); };
#endif
