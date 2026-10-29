#ifndef __PROTECTED_286_PROGRESS_MONITOR__
#define __PROTECTED_286_PROGRESS_MONITOR__

#include "Protected286Types.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"

class Protected286ProgressMonitor : public ProgressMonitor {
private:
    Protected286Word lastPercent;
    BOOLEAN active;

    void draw(Protected286Word percent, const char* status);

public:
    static const Class CLASS;

    Protected286ProgressMonitor();
    virtual ~Protected286ProgressMonitor() {}
    virtual void begin();
    virtual void end();
    virtual void update(double minValue, double maxValue, double currentValue);
    virtual double getCurrentPercent();
    virtual const Class* getClass() const;
};

#endif
