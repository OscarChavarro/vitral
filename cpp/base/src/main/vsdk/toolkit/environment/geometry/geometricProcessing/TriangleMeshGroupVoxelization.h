#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_TRIANGLEMESHGROUPVOXELIZATION_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_TRIANGLEMESHGROUPVOXELIZATION_H__

class TriangleMeshGroup;
class VoxelVolume;
class Matrix4x4d;
class ProgressMonitor;

class TriangleMeshGroupVoxelization {
public:
    static void doVoxelization(
        TriangleMeshGroup& meshGroup,
        VoxelVolume& vv,
        const Matrix4x4d& M,
        ProgressMonitor* reporter);
};

#endif
