#include <GL/glew.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/curve/ParametricCurve.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/GeometryTriangulator.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ColoredPrimitiveRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MinMaxRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SelectionCornersRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SphereRenderer.h"

namespace {
void add(java::ArrayList<float>& p, java::ArrayList<float>& c,
         const java::ArrayList<double>& v, int i, const ColorRgb& color, float alpha) {
    p.add((float)v[3*i]); p.add((float)v[3*i+1]); p.add((float)v[3*i+2]);
    c.add((float)color.r()); c.add((float)color.g()); c.add((float)color.b()); c.add(alpha);
}
void lineVertex(java::ArrayList<float>&p,java::ArrayList<float>&c,const java::ArrayList<double>&v,int i,const ColorRgb&x){p.add(v[3*i]);p.add(v[3*i+1]);p.add(v[3*i+2]);c.add(x.r());c.add(x.g());c.add(x.b());}
}

void OpenGL4GeometryRenderer::draw(Geometry* geometry, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap,
    const Matrix4x4d& local)
{
    if(!geometry||!camera||!quality)return;
    ParametricCurve* curve = dynamic_cast<ParametricCurve*>(geometry);
    if ( curve != 0 ) {
        java::ArrayList<float> positions, colors;
        ColorRgb wire = quality->getWireColor();
        for ( int segment = 1; segment < curve->getPointSize(); ++segment ) {
            if ( curve->getPointType(segment) == ParametricCurve::BREAK ) {
                ++segment; continue;
            }
            java::ArrayList<Vector3Dd> points = curve->calculatePoints(segment, false);
            for ( long i = 0; i + 1 < points.size(); ++i ) {
                const Vector3Dd ends[2] = {points[i], points[i + 1]};
                for ( int j = 0; j < 2; ++j ) {
                    positions.add((float)ends[j].x()); positions.add((float)ends[j].y());
                    positions.add((float)ends[j].z()); colors.add((float)wire.r());
                    colors.add((float)wire.g()); colors.add((float)wire.b());
                }
            }
        }
        OpenGL4LineRenderer::drawLines(camera->calculateProjectionMatrix().multiply(local),
            positions, colors, 1.0f);
        if(quality->isBoundingVolumeSet())OpenGL4MinMaxRenderer::draw(geometry,camera,local);
        if(quality->isSelectionCornersSet())OpenGL4SelectionCornersRenderer::draw(geometry,camera,local);
        return;
    }
    Sphere* sphere = dynamic_cast<Sphere*>(geometry);
    if ( sphere != 0 ) {
        const Light* light = lights && lights->size() ? (*lights)[0] : 0;
        OpenGL4SphereRenderer::draw(sphere, camera, light, material, quality,
            textureMap, normalMap, local, 32, 16);
        if(quality->isBoundingVolumeSet())OpenGL4MinMaxRenderer::draw(geometry,camera,local);
        if(quality->isSelectionCornersSet())OpenGL4SelectionCornersRenderer::draw(geometry,camera,local);
        return;
    }
    TriangleMeshGroup group;
    if(GeometryTriangulator::exportToTriangleMeshGroup(geometry,group)) {
        Matrix4x4d mvp=camera->calculateProjectionMatrix().multiply(local);
        ColorRgb fill=material?material->getDiffuse():ColorRgb(.7,.7,.7);
        float alpha=material?(float)material->getOpacity():1;
        if(alpha<1){glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);}
        for(long mi=0;mi<group.getMeshes().size();++mi){TriangleMesh& mesh=group.getMeshes()[mi];java::ArrayList<double>&v=mesh.getVertexPositions();java::ArrayList<int>&ix=mesh.getTriangleIndexes();
            if(quality->isSurfacesSet()){java::ArrayList<float>p,c;for(long k=0;k<ix.size();++k)add(p,c,v,ix[k],fill,alpha);OpenGL4ColoredPrimitiveRenderer::draw(mvp,GL_TRIANGLES,p,c);}
            if(quality->isWiresSet()){java::ArrayList<float>p,c;ColorRgb wc=quality->getWireColor();for(long k=0;k+2<ix.size();k+=3){int a=ix[k],b=ix[k+1],d=ix[k+2];lineVertex(p,c,v,a,wc);lineVertex(p,c,v,b,wc);lineVertex(p,c,v,b,wc);lineVertex(p,c,v,d,wc);lineVertex(p,c,v,d,wc);lineVertex(p,c,v,a,wc);}OpenGL4LineRenderer::drawLines(mvp,p,c,1);}
            if(quality->isPointsSet()){java::ArrayList<float>p,c;for(long k=0;k<ix.size();++k)add(p,c,v,ix[k],fill,alpha);OpenGL4ColoredPrimitiveRenderer::draw(mvp,GL_POINTS,p,c);}
        }
        if(alpha<1)glDisable(GL_BLEND);
    }
    if(quality->isBoundingVolumeSet())OpenGL4MinMaxRenderer::draw(geometry,camera,local);
    if(quality->isSelectionCornersSet())OpenGL4SelectionCornersRenderer::draw(geometry,camera,local);
}
void OpenGL4GeometryRenderer::dispose(){OpenGL4ColoredPrimitiveRenderer::release();OpenGL4LineRenderer::release();}
