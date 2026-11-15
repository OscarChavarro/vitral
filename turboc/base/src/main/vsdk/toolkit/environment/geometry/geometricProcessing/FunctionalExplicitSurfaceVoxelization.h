#ifndef __FNCTNL_EXPLCT_SRFC_VXLZTN__
#define __FNCTNL_EXPLCT_SRFC_VXLZTN__

class FunctionalExplicitSurface;
class VoxelVolume;
class Matrix4x4d;
class ProgressMonitor;

class FunctionalExplicitSurfaceVoxelization {
public:
    static void doVoxelization(
        FunctionalExplicitSurface& surface,
        VoxelVolume& vv,
        const Matrix4x4d& M,
        ProgressMonitor* reporter);
};

#endif
