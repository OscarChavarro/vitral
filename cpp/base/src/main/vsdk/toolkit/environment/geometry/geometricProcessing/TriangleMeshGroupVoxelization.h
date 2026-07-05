#ifndef __TRIANGLEMESHGROUPVOXELIZATION__
#define __TRIANGLEMESHGROUPVOXELIZATION__

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
