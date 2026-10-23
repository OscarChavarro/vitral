#include <stddef.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/dialog/runtime/DialogSession.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/util/TreeMap.h"

DialogSession::DialogSession(const WidgetDialog* definition)
    : definition(definition), open(TRUE), busy(FALSE), generation(0)
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

BOOLEAN DialogSession::isOpen() const
{
    return open;
}

BOOLEAN DialogSession::isBusy() const
{
    return busy;
}

long DialogSession::beginAction()
{
    if ( !open || busy ) {
        return -1;
    }
    busy = TRUE;
    return ++generation;
}

BOOLEAN DialogSession::completeAction(long request)
{
    if ( !open || !busy || request != generation ) {
        return FALSE;
    }
    busy = FALSE;
    return TRUE;
}

void DialogSession::close()
{
    open = FALSE;
    busy = FALSE;
    ++generation;
}
