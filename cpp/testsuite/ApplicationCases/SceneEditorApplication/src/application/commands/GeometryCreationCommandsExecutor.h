#ifndef __GEOMETRY_CREATION_COMMANDS_EXECUTOR__
#define __GEOMETRY_CREATION_COMMANDS_EXECUTOR__

#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "application/commands/CommandResult.h"

class ApplicationModel;
class Presenter;
class Scene;
class SimpleBody;
class VoxelVolume;

/**
Executes the commands of the GUI that create geometries (`IDC_CREATE_*`),
for the `GuiEventExecutor`.
*/
class GeometryCreationCommandsExecutor {
private:
    ApplicationModel* model;
    Presenter* presenter;

    Scene* scene() const;
    java::String message(const char* id) const;
    void createPlane();
    void createSphericalHarmonicDebugSpheres();
    bool createVolume();
    static bool hasFilledVoxels(const VoxelVolume& vv);
    void createBrep();
    void createParametricCubicCurve();
    void createFunctionalExplicitSurface();
    void createParametricBiCubicPatch();
    SimpleBody* addDebugSphere(SimpleBody* voxelBody, int groupIndex,
                               const Vector3Dd& cm, double averageDistance);

public:
    /**
    @param model application model the commands work over (referenced)
    @param presenter presents the status messages of the commands
    (referenced)
    */
    GeometryCreationCommandsExecutor(ApplicationModel* model,
                                     Presenter* presenter);

    /**
    @param label identifier of the command (`IDC_*`)
    @return whether the command was executed, failed, or is not one of the
    commands of this class
    */
    CommandResult execute(const java::String& label);
};

#endif
