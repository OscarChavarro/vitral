#include <cstdio>

#include <java/util/ArrayList.txx>
#include "TangibleInterfaceMarkersCreator.h"
#include "processing/MarkerGenerator.h"
TangibleInterfaceMarkersCreator::TangibleInterfaceMarkersCreator(int argc, char** argv)
    : options(argc, argv),
      pageRenderer(nullptr) {
}

TangibleInterfaceMarkersCreator::~TangibleInterfaceMarkersCreator() {
    if (model.getMarkers() != nullptr) {
        for (long i = 0; i < model.getMarkers()->size(); ++i) {
            Marker* marker = model.getMarkers()->get(i);
            if (marker) {
                delete marker;
            }
        }
        delete model.getMarkers();
        model.setMarkers(nullptr);
    }

    if (pageRenderer != nullptr) {
        delete pageRenderer;
        pageRenderer = nullptr;
    }
}

bool TangibleInterfaceMarkersCreator::init() {
    model.setStartId(options.getStartId());
    model.setMarkerSizeMm(options.getMarkerSizeMm());

    if (model.getMarkerSizeMm() <= 0.0) {
        fprintf(stderr, "[markers_creator] Error: marker size must be a positive value in millimeters (e.g. -size 40mm).\n");
        return false;
    }

    const int maxId = MarkerGenerator::maxId();
    if (model.getStartId() < 0 || model.getStartId() > maxId) {
        fprintf(stderr, "[markers_creator] Error: start id %d is out of range (valid range is 0-%d).\n",
            model.getStartId(), maxId);
        return false;
    }

    pageRenderer = new CairoPdfPageRenderer(model.getMarkerSizeMm());
    const int capacity = pageRenderer->getCapacity();
    if (capacity <= 0) {
        fprintf(stderr,
            "[markers_creator] Error: a %.1fmm marker does not fit within the printable area of an A4 page with the configured margins. Nothing was generated.\n",
            model.getMarkerSizeMm());
        return false;
    }

    model.setCount(capacity);
    if (model.getStartId() + model.getCount() - 1 > maxId) {
        model.setCount(maxId - model.getStartId() + 1);
    }

    model.setOutputPdf(options.getOutputPdf(model.getStartId() + model.getCount() - 1));

    return true;
}

void TangibleInterfaceMarkersCreator::process() {
    MarkerGenerator generator;
    model.setMarkers(new java::ArrayList<Marker*>());

    for (int id = model.getStartId(); id < model.getStartId() + model.getCount(); ++id) {
        model.getMarkers()->add(new Marker(generator.generate(id)));
    }
}

void TangibleInterfaceMarkersCreator::exportPdf() {
    pageRenderer->renderPage(model.getOutputPdf(), model.getMarkers());

    printf("[markers_creator] PDF generated: %s (%d markers, %dx%d grid, %.1fmm)\n",
           model.getOutputPdf(),
           model.getCount(),
           pageRenderer->getColumns(),
           pageRenderer->getRows(),
           model.getMarkerSizeMm());
}
