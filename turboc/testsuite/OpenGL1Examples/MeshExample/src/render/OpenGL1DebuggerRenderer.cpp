#include <stdio.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/gizmo/LightGizmoStyle.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmo.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1CameraRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ImageRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1LightRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1RendererConfigurationStateSelector.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ViewportWindow.h"
#include "vsdk/toolkit/render/opengl1/gizmo/OpenGL1RayGizmoRenderer.h"
#include "model/MeshModel.h"
#include "render/OpenGL1DebuggerRenderer.h"

const float OpenGL1DebuggerRenderer::SURFACE_POLYGON_OFFSET_FACTOR = 1.0f;
const float OpenGL1DebuggerRenderer::SURFACE_POLYGON_OFFSET_UNITS = 1.0f;
const float OpenGL1DebuggerRenderer::LINE_POLYGON_OFFSET_FACTOR = -1.0f;
const float OpenGL1DebuggerRenderer::LINE_POLYGON_OFFSET_UNITS = -1.0f;

OpenGL1DebuggerRenderer::OpenGL1DebuggerRenderer(
    MeshModel* model, OpenGL1LabelImageProvider* labelImageProvider)
    : model(model)
{
    viewportSet = new ViewportSet();
    viewport = new Viewport();
    viewport->setBorder(0);
    viewportSet->addViewport(viewport);
    viewportWindow = new OpenGL1ViewportWindow(viewportSet, viewport, labelImageProvider);
}

OpenGL1DebuggerRenderer::~OpenGL1DebuggerRenderer()
{
    delete viewportWindow;
    delete viewportSet;
}

ViewportSet* OpenGL1DebuggerRenderer::getViewportSet()
{
    return viewportSet;
}

bool OpenGL1DebuggerRenderer::init()
{
    // The fixed function pipeline needs no programs nor buffers
    return true;
}

void OpenGL1DebuggerRenderer::display()
{
    glEnable(GL_DEPTH_TEST);
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
    glDisable(GL_CULL_FACE);

    glClearColor(0.5f, 0.5f, 0.9f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    ArrayList<Light*>& activeLights = model->getLights();
    if ( activeLights.size() == 0 ) {
        return;
    }

    ArrayList<SimpleBody*>& bodies = model->getScene()->getSimpleBodies();
    for ( long i = 0; i < bodies.size(); i++ ) {
        drawSimpleBody(bodies.get(i), model->getCamera(), activeLights, model->getQualitySelection());
    }

    for ( long i = 0; i < activeLights.size(); i++ ) {
        if ( activeLights.get(i) != 0 ) {
            OpenGL1LightRenderer::draw(activeLights.get(i), model->getCamera(), LightGizmoStyle::OMNI_BILLBOARD);
        }
    }

    model->getRayGizmo()->acquireSnapshot();
    RayGizmo* rayGizmo = model->getRayGizmo();
    OpenGL1RayGizmoRenderer::draw(rayGizmo, model->getCamera(), activeLights);
    if ( rayGizmo->isVisible() ) {
        viewportWindow->drawInputGizmo(rayGizmo->getInputGizmo());
    }
}

void OpenGL1DebuggerRenderer::reshape(int width, int height)
{
    glViewport(0, 0, width, height);
    model->getCamera()->updateViewportResize(width, height);
    viewportSet->resize(width, height);
}

void OpenGL1DebuggerRenderer::dispose()
{
    OpenGL1CameraRenderer::dispose();
    OpenGL1RayGizmoRenderer::dispose();
    viewportWindow->disposeGlResources();
    OpenGL1LightRenderer::dispose();
    OpenGL1ImageRenderer::dispose();
    OpenGL1RendererConfigurationStateSelector::dispose();
}

void OpenGL1DebuggerRenderer::drawArrays(int mode, ArrayList<float>& positions,
    ArrayList<float>* normals, ArrayList<float>* uvs)
{
    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, positions.data());
    if ( normals != NULL ) {
        glEnableClientState(GL_NORMAL_ARRAY);
        glNormalPointer(GL_FLOAT, 0, normals->data());
    }
    if ( uvs != NULL ) {
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glTexCoordPointer(2, GL_FLOAT, 0, uvs->data());
    }
    glDrawArrays((GLenum)mode, 0, (GLsizei)(positions.size() / 3));
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    glDisableClientState(GL_NORMAL_ARRAY);
    glDisableClientState(GL_VERTEX_ARRAY);
}

void OpenGL1DebuggerRenderer::drawSimpleBody(SimpleBody* body, Camera* camera, const ArrayList<Light*>& lights, RendererConfiguration* quality)
{
    if ( body == 0 || camera == 0 || quality == 0 ) return;

    Geometry* geometry = body->getGeometry();
    if ( geometry == 0 ) return;

    ArrayList<TriangleMesh*> meshes;
    TriangleMesh* tm = (geometry != NULL && geometry->instanceOf(TriangleMesh::CLASS) ? (TriangleMesh*)geometry : NULL);
    if ( tm != 0 ) {
        meshes.add(tm);
    }
    else {
        TriangleMeshGroup* group = (geometry != NULL && geometry->instanceOf(TriangleMeshGroup::CLASS) ? (TriangleMeshGroup*)geometry : NULL);
        if ( group == 0 ) return;
        ArrayList<TriangleMesh>& groupMeshes = group->getMeshes();
        for ( long i = 0; i < groupMeshes.size(); i++ ) {
            meshes.add(&groupMeshes[i]);
        }
    }

    Matrix4x4d modelMatrix = body->getTransformationMatrix();
    Matrix4x4d projection = OpenGL1CameraRenderer::activate(camera);
    Matrix4x4d modelViewProjection = projection.multiply(modelMatrix);

    SimpleMaterial material = body->getMaterial() != 0 ? *(body->getMaterial()) : defaultMaterial();

    for ( long mi = 0; mi < meshes.size(); mi++ ) {
        TriangleMesh* mesh = meshes.get(mi);
        ArrayList<float> positions;
        ArrayList<float> normals;
        ArrayList<float> uvs;
        if ( !buildFrame(mesh, positions, normals, uvs) ) continue;

        Image* texture = body->getTexture();
        if ( texture == 0 ) {
            texture = mesh->getTextureAt(0);
        }

        int textureId = 0;
        if ( texture != 0 && quality->isTextureSet() ) {
            textureId = OpenGL1ImageRenderer::activate(texture);
            if ( textureId < 0 ) {
                textureId = 0;
            }
        }

        if ( quality->isSurfacesSet() ) {
            OpenGL1RendererConfigurationStateSelector::activateState(
                camera, projection, modelMatrix, lights, material, quality,
                textureId);
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(SURFACE_POLYGON_OFFSET_FACTOR, SURFACE_POLYGON_OFFSET_UNITS);
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glDepthMask(GL_TRUE);
            glDepthFunc(GL_LESS);
            drawArrays(GL_TRIANGLES, positions, &normals,
                textureId > 0 ? &uvs : NULL);
            glDisable(GL_POLYGON_OFFSET_FILL);
            OpenGL1RendererConfigurationStateSelector::deactivateState();
        }

        if ( quality->isWiresSet() ) {
            OpenGL1RendererConfigurationStateSelector::activateConstantState(
                modelViewProjection, 1.0f, 1.0f, 1.0f);
            glEnable(GL_POLYGON_OFFSET_LINE);
            glPolygonOffset(LINE_POLYGON_OFFSET_FACTOR, LINE_POLYGON_OFFSET_UNITS);
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glLineWidth(1.0f);
            glDepthMask(GL_FALSE);
            glDepthFunc(GL_LEQUAL);
            drawArrays(GL_TRIANGLES, positions, NULL, NULL);
            glDisable(GL_POLYGON_OFFSET_LINE);
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            OpenGL1RendererConfigurationStateSelector::deactivateState();
        }

        if ( quality->isPointsSet() ) {
            OpenGL1RendererConfigurationStateSelector::activateConstantState(
                modelViewProjection, 1.0f, 0.0f, 0.0f);
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glPointSize(4.0f);
            glDepthMask(GL_FALSE);
            glDepthFunc(GL_LEQUAL);
            drawArrays(GL_POINTS, positions, NULL, NULL);
            glPointSize(1.0f);
            OpenGL1RendererConfigurationStateSelector::deactivateState();
        }

        glBindTexture(GL_TEXTURE_2D, 0);
        glDepthMask(GL_TRUE);
        glDepthFunc(GL_LESS);
    }
}

bool OpenGL1DebuggerRenderer::buildFrame(TriangleMesh* mesh, ArrayList<float>& outPositions, ArrayList<float>& outNormals, ArrayList<float>& outUvs)
{
    if ( mesh == 0 ) return false;

    ArrayList<int>& indices = mesh->getTriangleIndexes();
    ArrayList<double>& vertices = mesh->getVertexPositions();
    if ( indices.size() == 0 || vertices.size() == 0 ) return false;

    ArrayList<double>& normals = mesh->getVertexNormals();
    ArrayList<double>& uvs = mesh->getVertexUvs();
    bool hasNormals = normals.size() >= vertices.size();
    bool hasUvs = (uvs.size() / 2) >= (vertices.size() / 3);

    outPositions.clear();
    outNormals.clear();
    outUvs.clear();
    outPositions.reserve(indices.size() * 3);
    outNormals.reserve(indices.size() * 3);
    outUvs.reserve(indices.size() * 2);

    for ( long i = 0; i < indices.size(); i++ ) {
        int idx = indices.get(i);
        int vp = idx * 3;
        outPositions.add((float)vertices.get(vp));
        outPositions.add((float)vertices.get(vp + 1));
        outPositions.add((float)vertices.get(vp + 2));

        if ( hasNormals ) {
            outNormals.add((float)normals.get(vp));
            outNormals.add((float)normals.get(vp + 1));
            outNormals.add((float)normals.get(vp + 2));
        }
        else {
            outNormals.add(0.0f);
            outNormals.add(0.0f);
            outNormals.add(1.0f);
        }

        if ( hasUvs ) {
            int uv = idx * 2;
            outUvs.add((float)uvs.get(uv));
            outUvs.add((float)uvs.get(uv + 1));
        }
        else {
            outUvs.add(0.0f);
            outUvs.add(0.0f);
        }
    }

    return true;
}

SimpleMaterial OpenGL1DebuggerRenderer::defaultMaterial()
{
    SimpleMaterial m;
    m = m.withAmbient(ColorRgb(0.2, 0.2, 0.2));
    m = m.withDiffuse(ColorRgb(0.8, 0.8, 0.8));
    m = m.withSpecular(ColorRgb(1.0, 1.0, 1.0));
    m = m.withPhongExponent(32.0);
    return m;
}
