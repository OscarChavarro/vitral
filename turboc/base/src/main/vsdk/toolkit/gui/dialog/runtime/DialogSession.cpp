#include <stddef.h>
#include <stdexcept>

#include "vsdk/toolkit/gui/dialog/runtime/DialogSession.h"

DialogSession::DialogSession(const WidgetDialog* definition)
    : definition(definition), open(true), busy(false), generation(0)
{
    if ( definition == NULL ) {
        throw std::invalid_argument("Missing dialog definition");
    }
}

const WidgetDialog* DialogSession::getDefinition() const
{
    return definition;
}

void DialogSession::setDraft(const String& binding,
                             const String& value)
{
    drafts[binding] = value;
}

String DialogSession::getDraft(const String& binding) const
{
    std::map<String, String>::const_iterator draft =
        drafts.find(binding);
    return draft != drafts.end() ? draft->second : String("");
}

bool DialogSession::isOpen() const
{
    return open;
}

bool DialogSession::isBusy() const
{
    return busy;
}

long DialogSession::beginAction()
{
    if ( !open || busy ) {
        return -1;
    }
    busy = true;
    return ++generation;
}

bool DialogSession::completeAction(long request)
{
    if ( !open || !busy || request != generation ) {
        return false;
    }
    busy = false;
    return true;
}

void DialogSession::close()
{
    open = false;
    busy = false;
    ++generation;
}
