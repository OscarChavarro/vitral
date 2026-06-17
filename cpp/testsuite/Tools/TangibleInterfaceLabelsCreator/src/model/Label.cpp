#include "model/Label.h"
Label::Label() {
}

Label::Label(const java::String& title)
    : title_(title) {
}

Label::~Label() {
}

const java::String& Label::getTitle() const {
    return title_;
}

void Label::setTitle(const java::String& title) {
    title_ = title;
}
