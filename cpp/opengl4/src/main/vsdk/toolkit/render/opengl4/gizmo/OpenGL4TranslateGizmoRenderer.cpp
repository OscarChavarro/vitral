#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/gizmo/TranslateGizmo.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.h"
#include "vsdk/toolkit/render/opengl4/gizmo/OpenGL4TranslateGizmoRenderer.h"
void OpenGL4TranslateGizmoRenderer::draw(TranslateGizmo*g,Camera*c){if(!g||!c)return;g->setCamera(c);g->updateGeometryState();RendererConfiguration q;q.setWires(false);q.setSelectionCorners(false);java::ArrayList<SimpleBody*>&e=g->getElements();for(long i=0;i<e.size();i++)if(e[i])OpenGL4GeometryRenderer::draw(e[i]->getGeometry(),c,0,e[i]->getMaterial(),&q,0,0,e[i]->getTransformationMatrix());java::ArrayList<TranslateGizmoLineSegment>s=g->getLineSegments();java::ArrayList<float>p,col;for(long i=0;i<s.size();i++){const Vector3Dd&a=s[i].start();const Vector3Dd&b=s[i].end();const ColorRgb&x=s[i].color();p.add(a.x());p.add(a.y());p.add(a.z());p.add(b.x());p.add(b.y());p.add(b.z());for(int k=0;k<2;k++){col.add(x.r());col.add(x.g());col.add(x.b());}}OpenGL4LineRenderer::drawLines(c->calculateProjectionMatrix(),p,col,(float)g->getLineWidth());}
