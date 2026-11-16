#ifndef __POLYHEDRAL_BOUNDED_SOLID_EXAMPLE__
#define __POLYHEDRAL_BOUNDED_SOLID_EXAMPLE__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.h"

class CommandLineOptions;
class DebuggerModel;
class Matrix4x4d;
class PolyhedralBoundedSolid;
class SimpleBody;
namespace java { class StringBuilder; }
class Vector3Dd;

/**
C++ port of Java's `PolyhedralBoundedSolidExample` (its `main` is the C++
`main`).
*/
class PolyhedralBoundedSolidExample {
public:
    static int main(const java::ArrayList<java::String>& args);
    static void buildSolidWithRecovery(DebuggerModel& model);

private:
    static void applyOfflineFaceSelection(DebuggerModel& model,
        const CommandLineOptions& options);
    static void applyOfflineAppelDisplayMode(DebuggerModel& model);
    static int findFaceIndexById(PolyhedralBoundedSolid* solid, int faceId);
    static void configureInitialModelFromSystemProperty(DebuggerModel& model);
    static void applyOptionOverrides(DebuggerModel& model,
        const CommandLineOptions& options);
    static void runMotifSweep(DebuggerModel& model,
        const java::String& outputPath);
    static java::String formatBuildErrorMessage(DebuggerModel& model,
        const java::String& simpleName, const java::String& message);
    static void exportAppelDumpIfRequested(DebuggerModel& model,
        const CommandLineOptions& options);
    static java::String serializeAppelDump(DebuggerModel& model,
        const HiddenLineRenderer::AppelAlgorithmDump& dump);
    static java::ArrayList<SimpleBody*> createDebugBodyScene(
        DebuggerModel& model);
    static void appendJsonString(java::StringBuilder& json,
        const java::String& value);
    static void appendJsonVector(java::StringBuilder& json,
        const java::String& name, const Vector3Dd& vector, int indent);
    static void appendJsonMatrix(java::StringBuilder& json,
        const java::String& name, const Matrix4x4d& matrix, int indent);
    static void appendJsonIntArray(java::StringBuilder& json,
        const java::ArrayList<int>& values);
    static java::String formatDouble(double value);
    static void indent(java::StringBuilder& json, int spaces);
};

#endif
