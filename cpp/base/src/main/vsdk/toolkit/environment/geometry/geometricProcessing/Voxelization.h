#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_VOXELIZATION_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_VOXELIZATION_H__

class Geometry;
class VoxelVolume;
class Matrix4x4d;
class ProgressMonitor;

class Voxelization {
public:
    static void doVoxelization(
        Geometry& geometry,
        VoxelVolume& vv,
        const Matrix4x4d& M,
        ProgressMonitor* reporter);
};

#endif
