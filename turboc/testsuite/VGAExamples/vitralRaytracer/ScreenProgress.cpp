#include "programs/vitralRaytracer/ScreenProgress.h"

const Class ScreenProgress::CLASS = { "ScreenProgress", &ProgressMonitor::CLASS };

const Class *
ScreenProgress::getClass() const
{
    return &CLASS;
}
