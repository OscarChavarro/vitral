#include <cstdio>

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/VoxelVolume.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshBuilder.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4VoxelVolumeRenderer.h"

void OpenGL4VoxelVolumeRenderer::draw(VoxelVolume* volume, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL4GeometryRenderer::draw(volume, camera, lights, material, quality,
        textureMap, normalMap, localTransform);
}

std::string OpenGL4VoxelVolumeRenderer::meshKey(const VoxelVolume& volume)
{
    char key[96];
    snprintf(key, sizeof(key), "voxelvolume/%p/%d", (const void*)&volume,
             volume.getThreshold());
    return key;
}

OpenGL4MeshRenderer::Mesh* OpenGL4VoxelVolumeRenderer::buildMesh(
    const VoxelVolume& volume)
{
    int size[3] = { volume.getXSize(), volume.getYSize(), volume.getZSize() };
    double step[3] = { 2.0 / size[0], 2.0 / size[1], 2.0 / size[2] };
    OpenGL4MeshBuilder builder(2.0);
    int faceCount = 0;
    int x;
    int y;
    int z;

    for ( z = 0; z < size[2]; z++ ) {
        for ( y = 0; y < size[1]; y++ ) {
            for ( x = 0; x < size[0]; x++ ) {
                if ( !volume.isFilled(x, y, z) ) {
                    continue;
                }
                int cell[3] = { x, y, z };
                for ( int axis = 0; axis < 3; axis++ ) {
                    for ( int side = -1; side <= 1; side += 2 ) {
                        int neighbor[3] = { x, y, z };
                        neighbor[axis] += side;
                        if ( !volume.isFilled(neighbor[0], neighbor[1], neighbor[2]) ) {
                            addVoxelFace(builder, cell, step, axis, side > 0);
                            faceCount++;
                        }
                    }
                }
            }
        }
    }
    if ( faceCount == 0 ) {
        return nullptr;
    }
    return builder.build();
}

/**
Adds the face of a voxel perpendicular to an axis, counterclockwise seen
from outside the voxel.

@param builder builder receiving the face
@param cell indexes of the voxel
@param step size of a voxel along each axis
@param axis axis perpendicular to the face (0: x, 1: y, 2: z)
@param positive true for the face on the positive side of the axis
*/
void OpenGL4VoxelVolumeRenderer::addVoxelFace(OpenGL4MeshBuilder& builder,
    const int cell[3], const double step[3], int axis, bool positive)
{
    // The other two axes, in cyclic order, so u x v points along +axis
    int uAxis = (axis + 1) % 3;
    int vAxis = (axis + 2) % 3;
    double base[3];
    double u[3] = { 0, 0, 0 };
    double v[3] = { 0, 0, 0 };
    double n[3] = { 0, 0, 0 };

    for ( int i = 0; i < 3; i++ ) {
        base[i] = cell[i] * step[i] - 1;
    }
    if ( positive ) {
        base[axis] += step[axis];
    }
    u[uAxis] = step[uAxis];
    v[vAxis] = step[vAxis];
    n[axis] = positive ? 1 : -1;

    Vector3Dd p0(base[0], base[1], base[2]);
    Vector3Dd p1(base[0] + u[0], base[1] + u[1], base[2] + u[2]);
    Vector3Dd p2(base[0] + u[0] + v[0], base[1] + u[1] + v[1],
        base[2] + u[2] + v[2]);
    Vector3Dd p3(base[0] + v[0], base[1] + v[1], base[2] + v[2]);
    Vector3Dd normal(n[0], n[1], n[2]);

    if ( positive ) {
        builder.addQuad(p0, normal, 0, 0, p1, normal, 1, 0,
            p2, normal, 1, 1, p3, normal, 0, 1);
    }
    else {
        builder.addQuad(p0, normal, 0, 0, p3, normal, 0, 1,
            p2, normal, 1, 1, p1, normal, 1, 0);
    }
}
