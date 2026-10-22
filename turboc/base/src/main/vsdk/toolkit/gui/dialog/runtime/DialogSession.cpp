#include <stddef.h>

#include "vsdk/toolkit/gui/dialog/runtime/DialogSession.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/util/TreeMap.h"

DialogSession::DialogSession(const WidgetDialog* definition)
    : definition(definition), open(true), busy(false), generation(0)
{
    if ( definition == NULL ) {
        throw IllegalArgumentException("Missing dialog definition");
    }
}

const WidgetDialog* DialogSession::getDefinition() const
{
    return definition;
}

void DialogSession::setDraft(const String& binding,
                             const String& value)
{
    drafts.put(binding, value);
}

String DialogSession::getDraft(const String& binding) const
{
    return drafts.getOrDefault(binding, String(""));
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
