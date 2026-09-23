#include <cstdio>

#include <java/util/ArrayList.txx>
#include "TangibleInterfaceLabelsCreator.h"
#include "model/Label.h"
TangibleInterfaceLabelsCreator::TangibleInterfaceLabelsCreator(int argc, char** argv)
    : options(argc, argv),
      pageRenderer(nullptr) {
}

TangibleInterfaceLabelsCreator::~TangibleInterfaceLabelsCreator() {
    if (model.getLabels() != nullptr) {
        for (long i = 0; i < model.getLabels()->size(); ++i) {
            Label* label = model.getLabels()->get(i);
            if (label) {
                delete label;
            }
        }
        delete model.getLabels();
        model.setLabels(nullptr);
    }

    if (pageRenderer != nullptr) {
        delete pageRenderer;
        pageRenderer = nullptr;
    }
}

bool TangibleInterfaceLabelsCreator::init() {
    model.setLabelSizeMm(options.getLabelSizeMm());
    model.setCircleHoledRadiusMm(options.getCircleHoledRadiusMm());

    if (model.getLabelSizeMm() <= 0.0) {
        fprintf(stderr, "[labels_creator] Error: label size must be a positive value in millimeters (e.g. -size 40mm).\n");
        return false;
    }
    if (model.getCircleHoledRadiusMm() <= 0.0) {
        fprintf(stderr, "[labels_creator] Error: circle hole radius must be a positive value in millimeters.\n");
        return false;
    }
    if (model.getCircleHoledRadiusMm() * 2.0 >= model.getLabelSizeMm()) {
        fprintf(stderr, "[labels_creator] Error: circle hole radius %.1fmm is too large for a %.1fmm label.\n",
            model.getCircleHoledRadiusMm(), model.getLabelSizeMm());
        return false;
    }

    pageRenderer = new CairoPdfPageRenderer(model.getLabelSizeMm(), model.getCircleHoledRadiusMm());
    if (pageRenderer->getCapacity() <= 0) {
        fprintf(stderr,
            "[labels_creator] Error: a %.1fmm label does not fit within the printable area of an A4 page with the configured margins. Nothing was generated.\n",
            model.getLabelSizeMm());
        return false;
    }

    model.setOutputPdf(options.getOutputPdf());

    return true;
}

void TangibleInterfaceLabelsCreator::process() {
    model.setLabels(new java::ArrayList<Label*>());
    model.getLabels()->add(new Label(java::String("Ray")));
    model.getLabels()->add(new Label(java::String("Omni\nLight")));
    model.getLabels()->add(new Label(java::String("SpotLight")));
    model.getLabels()->add(new Label(java::String("Camera")));
    model.getLabels()->add(new Label(java::String("Cutting\nPlane")));
    model.getLabels()->add(new Label(java::String("Object")));
    model.setCount(static_cast<int>(model.getLabels()->size()));
}

void TangibleInterfaceLabelsCreator::exportPdf() {
    pageRenderer->renderPage(model.getOutputPdf(), model.getLabels());

    printf("[labels_creator] PDF generated: %s (%d labels, %dx%d grid, %.1fmm)\n",
           model.getOutputPdf(),
           model.getCount(),
           pageRenderer->getColumns(),
           pageRenderer->getRows(),
           model.getLabelSizeMm());
}
