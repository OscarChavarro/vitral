#ifndef __OPEN_GL_1_VIEWPORT_SET_RENDERER__
#define __OPEN_GL_1_VIEWPORT_SET_RENDERER__
#include <map>
class OpenGL1LabelImageProvider;class OpenGL1ViewportWindow;class Viewport;class ViewportSet;
class OpenGL1ViewportSetRenderer { public:class ViewRenderer{public:virtual~ViewRenderer(){};virtual void configureView(OpenGL1ViewportWindow*)=0;virtual void drawView(OpenGL1ViewportWindow*)=0;};OpenGL1ViewportSetRenderer(ViewportSet*,OpenGL1LabelImageProvider*,ViewRenderer*);~OpenGL1ViewportSetRenderer();ViewportSet*getViewportSet()const;OpenGL1ViewportWindow*getWindow(Viewport*);OpenGL1ViewportWindow*getSelectedWindow();void draw(bool);void disposeGlResources();void invalidateGlResources();private:ViewportSet*set;OpenGL1LabelImageProvider*labels;ViewRenderer*renderer;std::map<Viewport*,OpenGL1ViewportWindow*>windows;void activate(Viewport*);void drawBorder(Viewport*);};
#endif
