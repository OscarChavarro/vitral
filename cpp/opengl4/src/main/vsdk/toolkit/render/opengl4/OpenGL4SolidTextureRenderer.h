#ifndef __OPEN_GL_4_SOLID_TEXTURE_RENDERER__
#define __OPEN_GL_4_SOLID_TEXTURE_RENDERER__

#include "java/util/ArrayList.h"

#include "java/lang/String.h"

class Camera;
class InfinitePlane;
class Light;
class RendererConfiguration;
class SimpleBody;
class SimpleScene;
class TriangleMesh;

/**
C++ counterpart of Java's `Jogl4SolidTextureRenderer`: draws triangle meshes
using an RGB 3D texture sampled in object-space bounding-box coordinates.
*/
class OpenGL4SolidTextureRenderer {
private:
    static const float SURFACE_POLYGON_OFFSET_FACTOR;
    static const float SURFACE_POLYGON_OFFSET_UNITS;
    static const float LINE_POLYGON_OFFSET_FACTOR;
    static const float LINE_POLYGON_OFFSET_UNITS;
    static const float POINT_SIZE;
    static const class ColorRgb POINT_COLOR;

    unsigned int vaoId;
    unsigned int positionVboId;
    unsigned int normalVboId;
    int vertexCount;
    unsigned int programId;
    unsigned int solidTextureId;
    long uploadedTextureRevision;
    int uploadedTextureSize;
    java::String shaderDirectory;

    void ensureBuffers();
    void ensureProgram();
    void ensureSolidTexture(const java::ArrayList<unsigned char>& volume,
                            int size, long revision);
    void drawBody(SimpleBody* body, Camera* camera,
                  RendererConfiguration* quality);
    void drawMesh(RendererConfiguration* quality);
    void uploadFrame(const java::ArrayList<float>& positions,
                     const java::ArrayList<float>& normals);
    bool buildFrame(TriangleMesh* mesh, java::ArrayList<float>& positions,
                    java::ArrayList<float>& normals);
    void configureClippingPlane(InfinitePlane* clippingPlane);

    static unsigned int buildProgram(const java::String& shaderDirectory,
                                     const char* vsFile,
                                     const char* fsFile);
    static unsigned int compileShader(unsigned int type, const char* source);
    static java::String readTextFile(const java::String& path);
    static void setMatrix(unsigned int programId, const char* name,
                          const class Matrix4x4d& matrix);
    static void setVector3(unsigned int programId, const char* name,
                           const class Vector3Dd& value);
    static void setColor(unsigned int programId, const char* name,
                         const class ColorRgb& value);
    static void setVector4(unsigned int programId, const char* name,
                           const class Vector4Dd& value);
    static void setInt(unsigned int programId, const char* name, int value);
    static void setFloat(unsigned int programId, const char* name, float value);

public:
    explicit OpenGL4SolidTextureRenderer(const java::String& shaderDirectory);
    ~OpenGL4SolidTextureRenderer();

    void draw(SimpleScene* scene, Camera* camera,
              const java::ArrayList<Light*>& lights,
              const java::ArrayList<unsigned char>& solidTextureVolumeRgb8,
              int solidTextureSize, long solidTextureRevision,
              InfinitePlane* clippingPlane);
    void draw(SimpleScene* scene, Camera* camera,
              const java::ArrayList<Light*>& lights,
              const java::ArrayList<unsigned char>& solidTextureVolumeRgb8,
              int solidTextureSize, long solidTextureRevision,
              InfinitePlane* clippingPlane, RendererConfiguration* quality);
    void dispose();
};

#endif
