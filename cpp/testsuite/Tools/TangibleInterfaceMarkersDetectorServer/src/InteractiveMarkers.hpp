#ifndef INTERACTIVE_MARKERS_HPP
#define INTERACTIVE_MARKERS_HPP

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
