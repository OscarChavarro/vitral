#include <cstdlib>
#include <cstring>
#include <exception>
#include <typeinfo>
#if defined(__GNUC__) || defined(__clang__)
#include <cxxabi.h>
#endif

#include "PolyhedralBoundedSolidExample.h"

#include "java/io/File.h"
#include "java/io/FileOutputStream.h"
#include "java/lang/Double.h"
#include "java/lang/Exception.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/Integer.h"
#include "java/lang/NumberFormatException.h"
#include "java/lang/RuntimeException.h"
#include "java/lang/StringBuilder.h"
#include "java/lang/System.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"
#include "InteractiveDebugger.h"
#include "PolyhedralBoundedSolidModelingTools.h"
#include "gui/CameraFaceFocusInteraction.h"
#include "models/DebuggerModel.h"
#include "options/CommandLineOptions.h"
#include "render/Jogl4HeadlessRenderer.h"

static const char* const PREFIX = "[PolyhedralBoundedSolidExample] ";

static void printError(const java::String& message)
{
    java::System::err.println((java::String(PREFIX) + message).c_str());
}

/**
Java: `getClass().getSimpleName()` of a caught exception.
*/
static java::String simpleNameOf(const std::type_info& type)
{
    const char* mangled = type.name();
    java::String name(mangled);
#if defined(__GNUC__) || defined(__clang__)
    int status = 0;
    char* demangled = abi::__cxa_demangle(mangled, nullptr, nullptr, &status);
    if ( status == 0 && demangled != nullptr ) {
        name = demangled;
    }
    std::free(demangled);
#endif
    int scope = name.rfind("::");
    if ( scope != -1 ) {
        name = name.substring(scope + 2);
    }
    return name;
}

int PolyhedralBoundedSolidExample::main(const java::ArrayList<java::String>& args)
{
    DebuggerModel model;
    CommandLineOptions options;

    try {
        options = CommandLineOptions::parse(args);
    }
    catch ( const java::IllegalArgumentException& e ) {
        printError(e.getMessage());
        java::System::err.println(
            "Usage: [--offline] [--screenshot <file.png>] [--faceId <id>] [--edgeIndex <id>] [--appelDump <file.json>]");
        return 0;
    }

    // Keep the debugger process alive and surface fatal kernel issues as exceptions.
    Logger::setWithSystemExit(false);
    Logger::setWithFatalExceptions(true);

    configureInitialModelFromSystemProperty(model);
    applyOptionOverrides(model, options);
    buildSolidWithRecovery(model);

    if ( options.isMotifSweep() ) {
        runMotifSweep(model, options.getOutputPath());
        return 0;
    }
    if ( options.isOffline() ) {
        try {
            applyOfflineFaceSelection(model, options);
        }
        catch ( const java::RuntimeException& e ) {
            printError(e.getMessage());
            java::System::exit(2);
        }
        applyOfflineAppelDisplayMode(model);
        Jogl4HeadlessRenderer renderer(&model, options.getOutputPath());
        renderer.render();
        exportAppelDumpIfRequested(model, options);
        if ( model.isErrorState() ) {
            printError(java::String("BUILD-ERROR: ") + model.getErrorMessage());
            java::System::exit(2);
        }
        return 0;
    }

    InteractiveDebugger::launch(model);
    return 0;
}

void PolyhedralBoundedSolidExample::applyOfflineFaceSelection(
    DebuggerModel& model,
    const CommandLineOptions& options)
{
    const int* requestedFaceId = options.getFaceId();
    int faceIndex;
    _PolyhedralBoundedSolidFace* face;

    if ( requestedFaceId == nullptr ) {
        return;
    }

    faceIndex = findFaceIndexById(model.getSolid(), *requestedFaceId);
    if ( faceIndex < 0 ) {
        throw java::IllegalArgumentException(
            java::String("Face id ") + java::String::valueOf(*requestedFaceId) +
            " not found in current solid");
    }
    model.setFaceIndex(faceIndex);
    model.clampFaceIndex();

    CameraFaceFocusInteraction focus;
    if ( !focus.focusSelectedFace(model) ) {
        throw java::IllegalStateException(
            java::String("Cannot focus camera on selected face id ") +
            java::String::valueOf(*requestedFaceId));
    }

    face = model.getSolid()->getPolygonsList().get(faceIndex);
    java::System::out.println((java::String(PREFIX) + "Selected face id=" +
        java::String::valueOf(face->id) + " index=" +
        java::String::valueOf(faceIndex) + " loops=" +
        java::String::valueOf((long)face->boundariesList.size())).c_str());
}

/**
Offline override for the Appel hidden-line display mode (the state cycled
by key [8] in the interactive debugger), via -Dpoly.appelDisplayMode. The
value may be an ordinal (0=OFF, 1=edges+visible+hidden, 2=edges+visible) or
the enum constant name. Useful for scripting per-state screenshots.

@param model debugger model whose display mode is overridden
*/
void PolyhedralBoundedSolidExample::applyOfflineAppelDisplayMode(
    DebuggerModel& model)
{
    java::String value = java::System::getProperty("poly.appelDisplayMode");
    if ( value.trim().isEmpty() ) {
        return;
    }
    value = value.trim();

    const int modesLength = 4;
    try {
        int ordinal = java::Integer::parseInt(value);
        if ( ordinal >= 0 && ordinal < modesLength ) {
            model.setAppelDisplayMode((AppelDisplayMode)ordinal);
            return;
        }
    }
    catch ( const java::NumberFormatException& ) {
        // Not an ordinal; fall through to name parsing.
    }

    AppelDisplayMode mode;
    if ( AppelDisplayModeUtil::valueOf(value.c_str(), &mode) ) {
        model.setAppelDisplayMode(mode);
    }
    else {
        printError(java::String("Ignoring unknown ") +
            "poly.appelDisplayMode='" + value + "'");
    }
}

int PolyhedralBoundedSolidExample::findFaceIndexById(
    PolyhedralBoundedSolid* solid, int faceId)
{
    if ( solid == nullptr ) {
        return -1;
    }
    int i;
    for ( i = 0; i < (int)solid->getPolygonsList().size(); i++ ) {
        if ( solid->getPolygonsList().get(i)->id == faceId ) {
            return i;
        }
    }
    return -1;
}

void PolyhedralBoundedSolidExample::configureInitialModelFromSystemProperty(
    DebuggerModel& model)
{
    java::String modelProperty = java::System::getProperty("polySolidModel");
    if ( modelProperty.trim().isEmpty() ) {
        return;
    }

    try {
        int modelId = java::Integer::parseInt(modelProperty);
        model.setSolidModelName(SolidModelNamesUtil::fromId(modelId));
        return;
    }
    catch ( const java::NumberFormatException& ) {
        // Not a numeric id; continue with enum-name parsing.
    }

    SolidModelNames name;
    if ( SolidModelNamesUtil::valueOf(modelProperty.c_str(), &name) ) {
        model.setSolidModelName(name);
    }
    else {
        printError(java::String("Ignoring unknown ") +
            "polySolidModel='" + modelProperty + "'");
    }
}

void PolyhedralBoundedSolidExample::applyOptionOverrides(
    DebuggerModel& model,
    const CommandLineOptions& options)
{
    RendererConfiguration* quality = model.getQuality();

    if ( options.getSolidModelName() != nullptr ) {
        model.setSolidModelName(*options.getSolidModelName());
    }
    if ( options.getCsgSampleName() != nullptr ) {
        model.setCsgSample(*options.getCsgSampleName());
    }
    if ( options.getDrawPoints() != nullptr ) {
        quality->setPoints(*options.getDrawPoints());
    }
    if ( options.getDrawWires() != nullptr ) {
        quality->setWires(*options.getDrawWires());
    }
    if ( options.getDrawSurfaces() != nullptr ) {
        quality->setSurfaces(*options.getDrawSurfaces());
    }
    if ( options.getShadingType() != nullptr ) {
        quality->setShadingType(*options.getShadingType());
    }
    if ( options.getVertexNormalSmoothingThresholdDegrees() != nullptr ) {
        quality->setVertexNormalSmoothingThresholdDegrees(
            *options.getVertexNormalSmoothingThresholdDegrees());
    }
    if ( options.getEdgeIndex() != nullptr ) {
        model.setEdgeIndex(*options.getEdgeIndex());
    }
    if ( options.getKurlanderBowlMotifIndex() != nullptr ) {
        model.setKurlanderBowlSingleMotifIndex(
            *options.getKurlanderBowlMotifIndex());
    }
}

void PolyhedralBoundedSolidExample::runMotifSweep(DebuggerModel& model,
    const java::String& outputPath)
{
    int total = CsgKurlanderBowlFixture::getSingleMotifCount();
    int stars = CsgKurlanderBowlFixture::getSingleMotifStarCount();
    java::String prefix = outputPath;
    int dotIndex = prefix.rfind('.');
    java::String stem = dotIndex < 0 ? prefix : prefix.substring(0, dotIndex);
    java::String ext = dotIndex < 0 ? java::String(".png") : prefix.substring(dotIndex);
    model.setSolidModelName(SolidModelNames::CSG_DIRECT);
    model.setCsgSample(CsgSampleNames::KURLANDER_BOWL_SINGLE_MOTIF);
    int ok = 0;
    int empty = 0;
    int invalid = 0;
    int blackFaces = 0;
    int unchanged = 0;
    int exception = 0;
    int motif;
    for ( motif = 0; motif < total; motif++ ) {
        java::String kind = motif < stars ? "STAR" : "MOON";
        int kindIndex = motif < stars ? motif : motif - stars;
        java::String tag = kind + "[" + java::String::valueOf(kindIndex) + "]";
        java::String filename = stem + "_" + java::String::format("%02d", motif) +
            "_" + kind + java::String::valueOf(kindIndex) + ext;
        int originalBowlFaces = -1;
        try {
            java::ArrayList<PolyhedralBoundedSolid*> preview =
                CsgKurlanderBowlFixture::createBowlAndFirstStarOperands(
                    motif);
            if ( preview.size() > 0 && preview[0] != nullptr ) {
                originalBowlFaces = (int)preview[0]->getPolygonsList().size();
            }
            long int i;
            for ( i = 0; i < preview.size(); i++ ) {
                delete preview[i];
            }
        }
        catch ( ... ) {
            /* fall through */
        }
        model.setKurlanderBowlSingleMotifIndex(motif);
        buildSolidWithRecovery(model);
        try {
            Jogl4HeadlessRenderer renderer(&model, filename);
            renderer.render();
        }
        catch ( const java::Throwable& t ) {
            model.setErrorState(java::String("RenderException: ") +
                simpleNameOf(typeid(t)) + " - " + t.getMessage());
        }
        catch ( const std::exception& t ) {
            model.setErrorState(java::String("RenderException: ") +
                simpleNameOf(typeid(t)) + " - " + t.what());
        }
        java::String status;
        java::String detail;
        if ( model.isErrorState() ) {
            status = "EXCEPTION";
            detail = java::String(" err=") + model.getErrorMessage();
            exception++;
        }
        else {
            PolyhedralBoundedSolid* solid = model.getSolid();
            int faces = solid == nullptr ? 0
                : (int)solid->getPolygonsList().size();
            if ( faces == 0 ) {
                status = "EMPTY";
                detail = "";
                empty++;
            }
            else if ( faces == originalBowlFaces ) {
                status = "UNCHANGED";
                detail = java::String(" faces=") + java::String::valueOf(faces);
                unchanged++;
            }
            else {
                bool valid = false;
                try {
                    valid = PolyhedralBoundedSolidValidationEngine
                        ::validateIntermediate(solid);
                }
                catch ( ... ) {
                    /* leave valid=false */
                }
                if ( !valid ) {
                    status = "INVALID";
                    detail = java::String(" faces=") + java::String::valueOf(faces);
                    invalid++;
                }
                else {
                    java::String orientationMsg;
                    bool orientationOK = true;
                    try {
                        orientationOK =
                            PolyhedralBoundedSolidGeometricValidator
                                ::validateConsistentFaceOrientations(
                                    solid, &orientationMsg);
                    }
                    catch ( ... ) {
                        orientationOK = true;
                    }
                    if ( !orientationOK ) {
                        int firstLine = orientationMsg.indexOf('\n');
                        java::String preview = firstLine > 0
                            ? orientationMsg.substring(0, firstLine).trim()
                            : java::String("(orientation flagged)");
                        status = "BLACK_FACES";
                        detail = java::String(" faces=") +
                            java::String::valueOf(faces) + " " + preview;
                        blackFaces++;
                    }
                    else {
                        status = "OK";
                        detail = java::String(" faces=") +
                            java::String::valueOf(faces) +
                            " bowlFaces=" + java::String::valueOf(originalBowlFaces);
                        ok++;
                    }
                }
            }
        }
        java::System::out.println((java::String("[SWEEP-") + status + "] " + tag +
            " motif=" + java::String::valueOf(motif) + detail).c_str());
    }
    java::System::out.println((java::String("[SWEEP-SUMMARY] ok=") +
        java::String::valueOf(ok) + " empty=" + java::String::valueOf(empty) +
        " invalid=" + java::String::valueOf(invalid) +
        " blackFaces=" + java::String::valueOf(blackFaces) +
        " unchanged=" + java::String::valueOf(unchanged) +
        " exception=" + java::String::valueOf(exception) +
        " total=" + java::String::valueOf(total)).c_str());
}

/**
C++ port note: Java catches any `Throwable`; here the port's exceptions
(`java::Throwable`), the C++ library ones (`std::exception`) and, as a last
resort, anything else.
*/
void PolyhedralBoundedSolidExample::buildSolidWithRecovery(DebuggerModel& model)
{
    try {
        model.clearErrorState();
        model.setSolid(PolyhedralBoundedSolidModelingTools::buildSolid(model));
        if ( model.getSolid() == nullptr ) {
            throw java::IllegalStateException("Solid builder returned null");
        }
        model.clampFaceIndex();
    }
    catch ( const java::Throwable& e ) {
        model.setErrorState(formatBuildErrorMessage(model,
            simpleNameOf(typeid(e)), e.getMessage()));
    }
    catch ( const std::exception& e ) {
        model.setErrorState(formatBuildErrorMessage(model,
            simpleNameOf(typeid(e)), e.what()));
    }
    catch ( ... ) {
        model.setErrorState(formatBuildErrorMessage(model, "Throwable", ""));
    }
}

java::String PolyhedralBoundedSolidExample::formatBuildErrorMessage(
    DebuggerModel& model, const java::String& simpleName,
    const java::String& message)
{
    java::String msg;
    msg += "Build error";
    msg += java::String(" [") + SolidModelNamesUtil::name(model.getSolidModelName()) + "]";
    msg += java::String(": ") + simpleName;
    if ( !message.isEmpty() ) {
        msg += java::String(" - ") + message;
    }
    return msg;
}

void PolyhedralBoundedSolidExample::exportAppelDumpIfRequested(
    DebuggerModel& model,
    const CommandLineOptions& options)
{
    if ( options.getAppelDumpPath() == nullptr ||
         options.getAppelDumpPath()->trim().isEmpty() ||
         options.getEdgeIndex() == nullptr ||
         *options.getEdgeIndex() != -3 ||
         model.getSolid() == nullptr ) {
        return;
    }

    java::ArrayList<SimpleBody*> bodies = createDebugBodyScene(model);

    HiddenLineRenderer::AppelAlgorithmDump dump;
    Calligraphic2DBuffer contourLines;
    Calligraphic2DBuffer visibleLines;
    Calligraphic2DBuffer hiddenLines;
    HiddenLineRenderer::executeAppelAlgorithmWithDiagnostics(
        bodies, model.getCamera(), &contourLines, &visibleLines, &hiddenLines,
        dump);
    for ( long int i = 0; i < bodies.size(); i++ ) {
        delete bodies.get(i);
    }

    java::String dumpPath = *options.getAppelDumpPath();
    Jogl4DebuggerRenderer::ensureParentFolder(dumpPath);

    try {
        java::String json = serializeAppelDump(model, dump);
        java::FileOutputStream out(dumpPath.c_str());
        out.write((const unsigned char*)json.c_str(), 0, json.length());
        out.close();
        java::System::out.println((java::String(PREFIX) + "Exported " +
            dumpPath).c_str());
    }
    catch ( const java::Exception& ) {
        throw java::IllegalStateException(
            java::String("Cannot write appel dump to '") + dumpPath + "'");
    }
}

/**
@return new bodies, owned by the caller
*/
java::ArrayList<SimpleBody*> PolyhedralBoundedSolidExample::createDebugBodyScene(
    DebuggerModel& model)
{
    java::ArrayList<SimpleBody*> bodies;
    SimpleBody* body = new SimpleBody();
    Matrix4x4d modelMatrix = model.getSolidModelMatrix();
    // C++ port note: the solid stays owned by the model
    body->setGeometryReference(model.getSolid());
    body->setPosition(Vector3Dd());
    body->setRotation(modelMatrix);
    body->setRotationInverse(modelMatrix.inverse());
    bodies.add(body);
    return bodies;
}

java::String PolyhedralBoundedSolidExample::serializeAppelDump(
    DebuggerModel& model,
    const HiddenLineRenderer::AppelAlgorithmDump& dump)
{
    java::StringBuilder json;
    json.append("{\n");
    json.append("  \"solidModel\": ");
    appendJsonString(json, SolidModelNamesUtil::name(model.getSolidModelName()));
    json.append(",\n");
    json.append("  \"camera\": {\n");
    appendJsonVector(json, "position", model.getCamera()->getPosition(), 4);
    json.append(",\n");
    appendJsonVector(json, "front", model.getCamera()->getFront(), 4);
    json.append(",\n");
    appendJsonVector(json, "up", model.getCamera()->getUp(), 4);
    json.append(",\n");
    appendJsonMatrix(json, "rotation", model.getCamera()->getRotation(), 4);
    json.append("\n  },\n");
    json.append("  \"edgeIndex\": ").append(model.getEdgeIndex()).append(",\n");
    json.append("  \"edgeCount\": ").append((int)dump.edges.size()).append(",\n");
    json.append("  \"edges\": [\n");
    for ( int i = 0; i < (int)dump.edges.size(); i++ ) {
        const HiddenLineRenderer::AppelEdgeDump& edge = dump.edges.get(i);
        json.append("    {\n");
        json.append("      \"edgeIndex\": ").append(edge.edgeIndex).append(",\n");
        json.append("      \"edgeType\": ");
        appendJsonString(json, edge.edgeTypeName);
        json.append(",\n");
        json.append("      \"faceIds\": [")
            .append(edge.face1Id).append(", ")
            .append(edge.face2Id).append("],\n");
        appendJsonVector(json, "start", edge.start, 6);
        json.append(",\n");
        appendJsonVector(json, "end", edge.end, 6);
        json.append(",\n");
        json.append("      \"initialQI\": ")
            .append(edge.initialQuantitativeInvisibility).append(",\n");
        json.append("      \"events\": [\n");
        for ( int j = 0; j < (int)edge.events.size(); j++ ) {
            const HiddenLineRenderer::AppelEventDump& event = edge.events.get(j);
            json.append("        {\"t\": ")
                .append(formatDouble(event.lineParameter))
                .append(", \"deltaQI\": ")
                .append(event.deltaQI)
                .append(", \"contourEdgeIndex\": ")
                .append(event.contourEdgeIndex)
                .append(", \"visibleFaceId\": ")
                .append(event.visibleFaceId)
                .append("}");
            if ( j + 1 < (int)edge.events.size() ) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("      ],\n");
        json.append("      \"segments\": [\n");
        for ( int j = 0; j < (int)edge.segments.size(); j++ ) {
            const HiddenLineRenderer::AppelSegmentDump& segment = edge.segments.get(j);
            json.append("        {\n");
            json.append("          \"t\": [")
                .append(formatDouble(segment.tStart)).append(", ")
                .append(formatDouble(segment.tEnd)).append("],\n");
            appendJsonVector(json, "start", segment.start, 10);
            json.append(",\n");
            appendJsonVector(json, "end", segment.end, 10);
            json.append(",\n");
            appendJsonVector(json, "midpoint", segment.midpoint, 10);
            json.append(",\n");
            json.append("          \"midpointQI\": ")
                .append(segment.midpointQuantitativeInvisibility).append(",\n");
            json.append("          \"midpointContributorFaceIds\": ");
            appendJsonIntArray(json, segment.midpointContributorFaceIds);
            json.append(",\n");
            json.append("          \"classification\": ");
            appendJsonString(json, segment.classification);
            json.append("\n        }");
            if ( j + 1 < (int)edge.segments.size() ) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("      ]\n");
        json.append("    }");
        if ( i + 1 < (int)dump.edges.size() ) {
            json.append(",");
        }
        json.append("\n");
    }
    json.append("  ]\n");
    json.append("}\n");
    return json.toString();
}

void PolyhedralBoundedSolidExample::appendJsonString(java::StringBuilder& json,
    const java::String& value)
{
    json.append("\"")
        .append(value.replace("\\", "\\\\").replace("\"", "\\\""))
        .append("\"");
}

void PolyhedralBoundedSolidExample::appendJsonVector(
    java::StringBuilder& json,
    const java::String& name,
    const Vector3Dd& vector,
    int indentSpaces)
{
    indent(json, indentSpaces);
    json.append("\"").append(name).append("\": [")
        .append(formatDouble(vector.x())).append(", ")
        .append(formatDouble(vector.y())).append(", ")
        .append(formatDouble(vector.z())).append("]");
}

void PolyhedralBoundedSolidExample::appendJsonMatrix(
    java::StringBuilder& json,
    const java::String& name,
    const Matrix4x4d& matrix,
    int indentSpaces)
{
    indent(json, indentSpaces);
    json.append("\"").append(name).append("\": [");
    for ( int row = 0; row < 4; row++ ) {
        if ( row > 0 ) {
            json.append(", ");
        }
        json.append("[");
        for ( int col = 0; col < 4; col++ ) {
            if ( col > 0 ) {
                json.append(", ");
            }
            json.append(formatDouble(matrix.get(row, col)));
        }
        json.append("]");
    }
    json.append("]");
}

void PolyhedralBoundedSolidExample::appendJsonIntArray(
    java::StringBuilder& json,
    const java::ArrayList<int>& values)
{
    json.append("[");
    for ( int i = 0; i < (int)values.size(); i++ ) {
        if ( i > 0 ) {
            json.append(", ");
        }
        json.append(values.get(i));
    }
    json.append("]");
}

java::String PolyhedralBoundedSolidExample::formatDouble(double value)
{
    return java::Double::toString(value);
}

void PolyhedralBoundedSolidExample::indent(java::StringBuilder& json,
    int spaces)
{
    for ( int i = 0; i < spaces; i++ ) {
        json.append(' ');
    }
}

/**
C++ port note: the `-Dkey=value` arguments are the JVM options of Java's
command line: they set system properties and are not program arguments.
*/
int main(int argc, char** argv)
{
    java::System::setPropertiesFromArguments(argc, argv);
    java::ArrayList<java::String> args;
    int i;
    for ( i = 1; i < argc; i++ ) {
        if ( std::strncmp(argv[i], "-D", 2) == 0 ) {
            continue;
        }
        args.add(java::String(argv[i]));
    }
    return PolyhedralBoundedSolidExample::main(args);
}
