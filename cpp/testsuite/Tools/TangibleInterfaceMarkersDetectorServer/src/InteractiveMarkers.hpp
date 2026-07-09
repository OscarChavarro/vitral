#ifndef __INTERACTIVE_MARKERS__
#define __INTERACTIVE_MARKERS__

class MarkersModel;
class MarkerTracker;

class InteractiveMarkers {
public:
    explicit InteractiveMarkers(MarkersModel* model, MarkerTracker* tracker);

    int run();

private:
    MarkersModel* model_;
    MarkerTracker* tracker_;
};

#endif
