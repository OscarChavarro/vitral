#include "gui/KeyboardInteractionTechniques.hpp"

KeyboardInteractionTechniques::KeyboardInteractionTechniques(MarkersModel* model)
    : model_(model) {}

KeyboardInteractionTechniques::KeyAction KeyboardInteractionTechniques::processKey(int key) {
    if (key == 'q' || key == 27) {
        return EXIT;
    }
    return NONE;
}
