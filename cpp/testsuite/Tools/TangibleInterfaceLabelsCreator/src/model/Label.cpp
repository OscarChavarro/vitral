#include "model/Label.h"
Label::Label() {
}

Label::Label(const java::String& title)
    : title(title) {
}

Label::~Label() {
}

const java::String& Label::getTitle() const {
    return title;
}

void Label::setTitle(const java::String& title) {
    this->title = title;
}
