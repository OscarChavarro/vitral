#include <cstdio>

#include "InteractiveMarkers.hpp"
#include "gui/KeyboardInteractionTechniques.hpp"
#include "model/MarkersModel.hpp"
#include "render/OpenCVMarkersRenderer.hpp"
#include "vision/MarkerTracker.hpp"
InteractiveMarkers::InteractiveMarkers(MarkersModel* model, MarkerTracker* tracker)
    : model(model), tracker(tracker) {}

int InteractiveMarkers::run() {
    KeyboardInteractionTechniques keyHandler(model);
    OpenCVMarkersRenderer renderer(model);

    tracker->runPreviewLoop();
    return 0;
}
