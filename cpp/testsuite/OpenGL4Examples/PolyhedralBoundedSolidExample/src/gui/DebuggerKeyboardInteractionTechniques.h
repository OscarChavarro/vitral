#ifndef __POLYHEDRAL_EXAMPLE_DEBUGGER_KEYBOARD_INTERACTION_TECHNIQUES__
#define __POLYHEDRAL_EXAMPLE_DEBUGGER_KEYBOARD_INTERACTION_TECHNIQUES__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "gui/CameraFaceFocusInteraction.h"

class DebuggerModel;
class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;

/// C++ port of Java's `gui.DebuggerKeyboardInteractionTechniques`
class DebuggerKeyboardInteractionTechniques {
public:
    /// Java: interface `Actions`
    class Actions {
    public:
        virtual ~Actions() {}
        virtual void requestExit() = 0;
        virtual void rebuildSolid() = 0;
        virtual void toggleFullscreen() = 0;
        virtual void toggleSolidAnimation() = 0;
        virtual void requestScreenshot() = 0;
        virtual void requestStlExport() = 0;
    };

    DebuggerKeyboardInteractionTechniques();

    bool processPressed(DebuggerModel& model, const KeyEvent& event,
        Actions& actions);

private:
    CameraFaceFocusInteraction cameraFaceFocusInteraction;

    static void printSolidForCurrentFaceSelection(DebuggerModel& model);
    /// Java: a `LinkedHashSet`, here a list without repetitions
    static java::ArrayList<_PolyhedralBoundedSolidFace*>
        collectSelectedAndNeighborFaces(_PolyhedralBoundedSolidFace* selectedFace);
    static java::String buildFacesSubsetDump(
        PolyhedralBoundedSolid* solid,
        const java::ArrayList<_PolyhedralBoundedSolidFace*>& facesToPrint,
        _PolyhedralBoundedSolidFace* selectedFace);
    static java::String intPreSpaces(int a, int n);
};

#endif
