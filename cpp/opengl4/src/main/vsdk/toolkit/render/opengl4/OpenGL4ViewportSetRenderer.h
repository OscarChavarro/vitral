#ifndef __OPEN_GL_4_VIEWPORT_SET_RENDERER__
#define __OPEN_GL_4_VIEWPORT_SET_RENDERER__
#include <map>
class OpenGL4LabelImageProvider;class OpenGL4ViewportWindow;class Viewport;class ViewportSet;
class OpenGL4ViewportSetRenderer { public:class ViewRenderer{public:virtual~ViewRenderer(){};virtual void configureView(OpenGL4ViewportWindow*)=0;virtual void drawView(OpenGL4ViewportWindow*)=0;};OpenGL4ViewportSetRenderer(ViewportSet*,OpenGL4LabelImageProvider*,ViewRenderer*);~OpenGL4ViewportSetRenderer();ViewportSet*getViewportSet()const;OpenGL4ViewportWindow*getWindow(Viewport*);OpenGL4ViewportWindow*getSelectedWindow();void draw(bool);void disposeGlResources();void invalidateGlResources();private:ViewportSet*set;OpenGL4LabelImageProvider*labels;ViewRenderer*renderer;std::map<Viewport*,OpenGL4ViewportWindow*>windows;void activate(Viewport*);void drawBorder(Viewport*);};
#endif
