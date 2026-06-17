#include <cstdio>

#include "InteractiveMarkers.hpp"
#include "gui/KeyboardInteractionTechniques.hpp"
#include "model/MarkersModel.hpp"
#include "render/OpenCVMarkersRenderer.hpp"
#include "vision/MarkerTracker.hpp"
InteractiveMarkers::InteractiveMarkers(MarkersModel* model, MarkerTracker* tracker)
    : model_(model), tracker_(tracker) {}

int InteractiveMarkers::run() {
    KeyboardInteractionTechniques keyHandler(model_);
    OpenCVMarkersRenderer renderer(model_);

    tracker_->runPreviewLoop();
    return 0;
}
