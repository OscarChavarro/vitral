#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_FUNCTIONALEXPLICITSURFACEVOXELIZATION_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_FUNCTIONALEXPLICITSURFACEVOXELIZATION_H__

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
