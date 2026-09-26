#include <vector>
#include <unistd.h>

#include "java/io/File.h"
#include "application/GuiEventExecutor.h"
#include "application/XtOpenGL4GuiEventExecutor.h"
#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtFileDialog.h"
#include "io/FileSuffixFilter.h"
#include "model/GuiState.h"
#include "render/opengl4/XtOpenGL4SceneBridge.h"
#include "vsdk/toolkit/common/logging/Logger.h"

namespace {

std::string parentFolderOf(const std::string& path)
{
    size_t separator = path.rfind('/');
    if ( separator == std::string::npos ) {
        return ".";
    }
    return separator == 0 ? "/" : path.substr(0, separator);
}

std::string currentFolder()
{
    char buffer[4096];
    return getcwd(buffer, sizeof(buffer)) != nullptr ? buffer : ".";
}

}

XtOpenGL4GuiEventExecutor::XtOpenGL4GuiEventExecutor(XtApplicationHost* parent)
    : parent(parent)
{
}

bool XtOpenGL4GuiEventExecutor::executeCommand(const java::String& command)
{
    std::string label(command.c_str());
    GuiEventExecutor::CommandResult result =
        parent->getSceneBridge()->executeCommand(label);

    if ( result == GuiEventExecutor::CommandResult::FAILED ) {
        return false;
    }
    if ( result == GuiEventExecutor::CommandResult::NOT_HANDLED &&
         !executeXtCommand(label) ) {
        return false;
    }

    //-----------------------------------------------------------------
    parent->repaintDrawingArea();
    return true;
}

/**
Executes the commands that need Xt.
@return false if the command failed
*/
bool XtOpenGL4GuiEventExecutor::executeXtCommand(const std::string& label)
{
    XtOpenGL4SceneBridge* bridge = parent->getSceneBridge();
    GuiEventExecutor* commands = bridge->getCommands();
    GuiState* guiState = bridge->getGuiState();
    std::string path;

    //- FILE ----------------------------------------------------------
    if ( label == "IDC_FILE_QUIT" ) {
        parent->closeApplication();
    }
    else if ( label == "IDC_IMPORT_OBJECTS_FROM_FILE" ) {
        std::vector<FileSuffixFilter> filters;
        filters.push_back(FileSuffixFilter("3ds", "3ds Kinetix/Discreet 3DStudio/3DStudioMax binary scene file"));
        filters.push_back(FileSuffixFilter("vtk", "vtk Kitware vtk legacy binary file (mesh only)"));
        filters.push_back(FileSuffixFilter("gts", "gts Gts mesh ASCII file"));
        filters.push_back(FileSuffixFilter("obj", "obj Alias/Wavefront text mesh"));
        filters.push_back(FileSuffixFilter("ply", "ply Ply mesh"));

        if ( XtFileDialog::showDialog(parent, "Open",
                 guiState->getReadFolder().c_str(), filters, path) ) {
            if ( !commands->importObjects(java::File(path.c_str())) ) {
                Logger::reportMessage("XtOpenGL4GuiEventExecutor",
                    Logger::WARNING, "executeCommand",
                    java::String("Failed to read file...\n") + path.c_str());
                return false;
            }
            guiState->setReadFolder(parentFolderOf(path).c_str());
        }
    }
    else if ( label == "IDC_EXPORT_OBJECTS_TO_OBJ" ||
              label == "IDC_EXPORT_OBJECTS_TO_GTS" ||
              label == "IDC_EXPORT_OBJECTS_TO_VTK" ) {
        GuiEventExecutor::ExportFormat format =
            label == "IDC_EXPORT_OBJECTS_TO_OBJ" ? GuiEventExecutor::ExportFormat::OBJ :
            label == "IDC_EXPORT_OBJECTS_TO_GTS" ? GuiEventExecutor::ExportFormat::GTS :
            GuiEventExecutor::ExportFormat::VTK;

        if ( XtFileDialog::showDialog(parent, "Save",
                 guiState->getWriteFolder().c_str(),
                 std::vector<FileSuffixFilter>(), path) ) {
            if ( !commands->exportObjects(java::File(path.c_str()), format) ) {
                Logger::reportMessage("XtOpenGL4GuiEventExecutor",
                    Logger::WARNING, "execute",
                    java::String("Failed to write file...\n") + path.c_str());
                return false;
            }
            guiState->setWriteFolder(parentFolderOf(path).c_str());
        }
    }
    //- RENDERING -----------------------------------------------------
    else if ( label == "Select palette for depthmap display" ||
              label == "IDC_RENDERING_SELECTPALETTEDEPTH" ) {
        std::vector<FileSuffixFilter> filters;
        filters.push_back(FileSuffixFilter("gpl", "gpl Gimp Palettes"));
        if ( XtFileDialog::showDialog(parent, "Open",
                 currentFolder() + "/../../../../etc/palettes", filters,
                 path) ) {
            if ( !commands->loadPalette(java::File(path.c_str())) ) {
                Logger::reportMessage("XtOpenGL4GuiEventExecutor",
                    Logger::WARNING, "execute", "Failed to read file");
                return false;
            }
        }
    }
    else if ( label == "IDC_RENDERING_RAYTRACING" ) {
        parent->showStatusMessage(parent->getMessage("IDM_COMPUTING_RAYTRACING"));
        parent->doRaytracingImage();
        parent->showImage(bridge->getRaytracedImage());
    }
    //- CUSTOMIZE -----------------------------------------------------
    else if ( label == "IDC_CUSTOMIZE_LAF_MOTIF" ||
              label == "IDC_CUSTOMIZE_LAF_JAVA" ||
              label == "IDC_CUSTOMIZE_LAF_GTK" ||
              label == "IDC_CUSTOMIZE_LAF_WINDOWS" ) {
        // Swing look and feels: the Xt GUI has only the look of its
        // XResources file
        parent->showStatusMessage("Look and feel changes are not available in the Xt GUI");
    }
    else if ( label == "IDC_CUSTOMIZE_LANGUAGE_ENGLISH" ) {
        parent->setGuiLanguage("english");
    }
    else if ( label == "IDC_CUSTOMIZE_LANGUAGE_SPANISH" ) {
        parent->setGuiLanguage("spanish");
    }
    return true;
}
