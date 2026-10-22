#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"

const Class ProgressMonitor::CLASS = { "ProgressMonitor", &PresentationElement::CLASS };

const Class *
ProgressMonitor::getClass() const
{
    return &CLASS;
}
