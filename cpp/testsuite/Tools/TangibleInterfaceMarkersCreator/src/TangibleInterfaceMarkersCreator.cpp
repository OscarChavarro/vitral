#include "TangibleInterfaceMarkersCreator.h"

#include <cstdio>
#include <java/util/ArrayList.txx>

#include "processing/MarkerGenerator.h"

TangibleInterfaceMarkersCreator::TangibleInterfaceMarkersCreator(int argc, char** argv)
    : options_(argc, argv),
      pageRenderer_(nullptr) {
}

TangibleInterfaceMarkersCreator::~TangibleInterfaceMarkersCreator() {
    if (model_.getMarkers() != nullptr) {
        for (long i = 0; i < model_.getMarkers()->size(); ++i) {
            Marker* marker = model_.getMarkers()->get(i);
            if (marker) {
                delete marker;
            }
        }
        delete model_.getMarkers();
        model_.setMarkers(nullptr);
    }

    if (pageRenderer_ != nullptr) {
        delete pageRenderer_;
        pageRenderer_ = nullptr;
    }
}

bool TangibleInterfaceMarkersCreator::init() {
    model_.setStartId(options_.getStartId());
    model_.setMarkerSizeMm(options_.getMarkerSizeMm());

    if (model_.getMarkerSizeMm() <= 0.0) {
        fprintf(stderr, "[markers_creator] Error: marker size must be a positive value in millimeters (e.g. -size 40mm).\n");
        return false;
    }

    const int maxId = MarkerGenerator::maxId();
    if (model_.getStartId() < 0 || model_.getStartId() > maxId) {
        fprintf(stderr, "[markers_creator] Error: start id %d is out of range (valid range is 0-%d).\n",
            model_.getStartId(), maxId);
        return false;
    }

    pageRenderer_ = new CairoPdfPageRenderer(model_.getMarkerSizeMm());
    const int capacity = pageRenderer_->getCapacity();
    if (capacity <= 0) {
        fprintf(stderr,
            "[markers_creator] Error: a %.1fmm marker does not fit within the printable area of an A4 page with the configured margins. Nothing was generated.\n",
            model_.getMarkerSizeMm());
        return false;
    }

    model_.setCount(capacity);
    if (model_.getStartId() + model_.getCount() - 1 > maxId) {
        model_.setCount(maxId - model_.getStartId() + 1);
    }

    model_.setOutputPdf(options_.getOutputPdf(model_.getStartId() + model_.getCount() - 1));

    return true;
}

void TangibleInterfaceMarkersCreator::process() {
    MarkerGenerator generator;
    model_.setMarkers(new java::ArrayList<Marker*>());

    for (int id = model_.getStartId(); id < model_.getStartId() + model_.getCount(); ++id) {
        model_.getMarkers()->add(new Marker(generator.generate(id)));
    }
}

void TangibleInterfaceMarkersCreator::exportPdf() {
    pageRenderer_->renderPage(model_.getOutputPdf(), model_.getMarkers());

    printf("[markers_creator] PDF generated: %s (%d markers, %dx%d grid, %.1fmm)\n",
           model_.getOutputPdf(),
           model_.getCount(),
           pageRenderer_->getColumns(),
           pageRenderer_->getRows(),
           model_.getMarkerSizeMm());
}
