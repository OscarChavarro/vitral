#ifndef __GUI_EVENT_EXECUTOR__
#define __GUI_EVENT_EXECUTOR__

#include "java/io/File.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/gui/CommandListener.h"

class ApplicationModel;
class Scene;
class SimpleBody;

/**
Executes the commands of the GUI of the editor (identified by the `IDC_*`
names of the I18N GUI definition) that only work over the application model:
creation of objects and lights, capture requests, interaction modes,
viewport and debugging toggles. It also offers the persistence operations
whose files are chosen by the GUI. It does not depend on any GUI or
rendering technology: each GUI technology executes here what it does not
present itself (file dialogs, windows, look and feel...), and presents the
status messages requested through its `Presenter`.

What the commands change in the scene (i.e. objects and lights created,
objects imported) is recorded in the scene history of the model, so it can
be undone.
*/
class GuiEventExecutor : public CommandListener {
public:
    /**
    Result of executing a command.
    */
    enum class CommandResult {
        /** The command was executed */
        DONE,
        /** The command was recognized, but it could not be executed */
        FAILED,
        /** The command is not executed by this class */
        NOT_HANDLED
    };

    /**
    Formats to export the objects of the scene.
    */
    enum class ExportFormat {
        OBJ,
        GTS,
        VTK
    };

    /**
    What the GUI technology presents for this executor.
    */
    class Presenter {
    public:
        virtual ~Presenter() {}

        /**
        @param message text to show in the status bar
        */
        virtual void showStatusMessage(const java::String& message) = 0;
    };

private:
    ApplicationModel* model;
    Presenter* presenter;

    Scene* scene() const;
    CommandResult executeModelCommand(const java::String& label);
    SimpleBody* addDebugSphere(SimpleBody* voxelBody, int groupIndex,
                               const Vector3Dd& cm, double averageDistance);
    java::String message(const char* id) const;

public:
    /**
    @param model application model the commands work over
    @param presenter presents the status messages of the commands
    */
    GuiEventExecutor(ApplicationModel* model, Presenter* presenter);
    virtual ~GuiEventExecutor() {}

    /**
    @param label identifier of the command (`IDC_*`)
    @return true if the command was executed
    */
    virtual bool executeCommand(const java::String& label) override;

    /**
    @param label identifier of the command (`IDC_*`)
    @return whether the command was executed, failed, or is not one of the
    commands of this class
    */
    CommandResult execute(const java::String& label);

    /**
    Adds the objects of a file to the scene.
    @param file obj or ply file (the formats read by the C++ port)
    @return false if the file can not be read (the Java version throws)
    */
    bool importObjects(const java::File& file);

    /**
    Writes the objects of the scene to a file.
    @param file destination file
    @param format format of the file
    @return false if the file can not be written (the Java version throws)
    */
    bool exportObjects(const java::File& file, ExportFormat format);

    /**
    Replaces the palette used to present depth maps.
    @param file Gimp palette (gpl) file
    @return false if the file can not be read (the Java version throws)
    */
    bool loadPalette(const java::File& file);
};

#endif
