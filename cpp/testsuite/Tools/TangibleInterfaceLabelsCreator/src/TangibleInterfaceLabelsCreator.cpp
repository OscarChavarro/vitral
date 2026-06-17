#include <cstdio>

#include <java/util/ArrayList.txx>
#include "TangibleInterfaceLabelsCreator.h"
#include "model/Label.h"
TangibleInterfaceLabelsCreator::TangibleInterfaceLabelsCreator(int argc, char** argv)
    : options_(argc, argv),
      pageRenderer_(nullptr) {
}

TangibleInterfaceLabelsCreator::~TangibleInterfaceLabelsCreator() {
    if (model_.getLabels() != nullptr) {
        for (long i = 0; i < model_.getLabels()->size(); ++i) {
            Label* label = model_.getLabels()->get(i);
            if (label) {
                delete label;
            }
        }
        delete model_.getLabels();
        model_.setLabels(nullptr);
    }

    if (pageRenderer_ != nullptr) {
        delete pageRenderer_;
        pageRenderer_ = nullptr;
    }
}

bool TangibleInterfaceLabelsCreator::init() {
    model_.setLabelSizeMm(options_.getLabelSizeMm());
    model_.setCircleHoledRadiusMm(options_.getCircleHoledRadiusMm());

    if (model_.getLabelSizeMm() <= 0.0) {
        fprintf(stderr, "[labels_creator] Error: label size must be a positive value in millimeters (e.g. -size 40mm).\n");
        return false;
    }
    if (model_.getCircleHoledRadiusMm() <= 0.0) {
        fprintf(stderr, "[labels_creator] Error: circle hole radius must be a positive value in millimeters.\n");
        return false;
    }
    if (model_.getCircleHoledRadiusMm() * 2.0 >= model_.getLabelSizeMm()) {
        fprintf(stderr, "[labels_creator] Error: circle hole radius %.1fmm is too large for a %.1fmm label.\n",
            model_.getCircleHoledRadiusMm(), model_.getLabelSizeMm());
        return false;
    }

    pageRenderer_ = new CairoPdfPageRenderer(model_.getLabelSizeMm(), model_.getCircleHoledRadiusMm());
    if (pageRenderer_->getCapacity() <= 0) {
        fprintf(stderr,
            "[labels_creator] Error: a %.1fmm label does not fit within the printable area of an A4 page with the configured margins. Nothing was generated.\n",
            model_.getLabelSizeMm());
        return false;
    }

    model_.setOutputPdf(options_.getOutputPdf());

    return true;
}

void TangibleInterfaceLabelsCreator::process() {
    model_.setLabels(new java::ArrayList<Label*>());
    model_.getLabels()->add(new Label(java::String("Ray")));
    model_.getLabels()->add(new Label(java::String("Omni\nLight")));
    model_.getLabels()->add(new Label(java::String("SpotLight")));
    model_.getLabels()->add(new Label(java::String("Camera")));
    model_.getLabels()->add(new Label(java::String("Cutting\nPlane")));
    model_.getLabels()->add(new Label(java::String("Object")));
    model_.setCount(static_cast<int>(model_.getLabels()->size()));
}

void TangibleInterfaceLabelsCreator::exportPdf() {
    pageRenderer_->renderPage(model_.getOutputPdf(), model_.getLabels());

    printf("[labels_creator] PDF generated: %s (%d labels, %dx%d grid, %.1fmm)\n",
           model_.getOutputPdf(),
           model_.getCount(),
           pageRenderer_->getColumns(),
           pageRenderer_->getRows(),
           model_.getLabelSizeMm());
}
