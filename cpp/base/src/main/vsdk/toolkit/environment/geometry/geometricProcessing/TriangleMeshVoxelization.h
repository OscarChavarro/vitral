#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_TRIANGLEMESHVOXELIZATION_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_TRIANGLEMESHVOXELIZATION_H__

class TriangleMesh;
class VoxelVolume;
class Matrix4x4d;
class ProgressMonitor;

class TriangleMeshVoxelization {
public:
    static void doVoxelization(
        TriangleMesh& mesh,
        VoxelVolume& vv,
        const Matrix4x4d& M,
        ProgressMonitor* reporter);
};

#endif
