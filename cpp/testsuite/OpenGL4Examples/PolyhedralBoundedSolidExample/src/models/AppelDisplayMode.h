#ifndef __POLYHEDRAL_EXAMPLE_APPEL_DISPLAY_MODE__
#define __POLYHEDRAL_EXAMPLE_APPEL_DISPLAY_MODE__

/**
Display states for the Appel hidden-line algorithm, cycled with key [8]:
OFF disables it, EDGES_VISIBLE_HIDDEN draws contour, visible and hidden lines,
EDGES_VISIBLE draws only contour and visible lines (the hidden ones are
suppressed), and EDGES_ONLY draws only the contour (border) lines (both the
visible non-contour and the hidden lines are suppressed).

C++ port of Java enum `models.AppelDisplayMode`.
*/
enum class AppelDisplayMode {
    OFF,
    EDGES_VISIBLE_HIDDEN,
    EDGES_VISIBLE,
    EDGES_ONLY
};

class AppelDisplayModeUtil {
public:
    static const char* name(AppelDisplayMode value);
    static const char* getLabel(AppelDisplayMode value);
    static AppelDisplayMode nextCircular(AppelDisplayMode value);
    /**
    Java: `AppelDisplayMode.valueOf(name)`
    @return false when no value has that name (Java throws
    `IllegalArgumentException`)
    */
    static bool valueOf(const char* name, AppelDisplayMode* out);

private:
    AppelDisplayModeUtil() {}
};

#endif
