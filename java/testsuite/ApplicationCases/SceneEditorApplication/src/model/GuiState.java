package model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
State of the GUI of the editor that does not depend on the GUI technology in
use: the language file, which panels are shown, the full screen mode and the
folders proposed by file dialogs. The
I18N texts themselves are the `ApplicationModel.getI18nContext()`.
*/
public class GuiState
{
    /** Folder with the I18N files (one JSON file per language) used to build the GUI. */
    public static final String GUI_LANGUAGE_FOLDER = "./etc/gui/";
    public static final String JSON_EXTENSION = ".json";

    private String languageGuiFile;
    private boolean modifyPanelSelected;
    private boolean fullScreenGuiMode;
    private String readFolder;
    private String writeFolder;

    public GuiState()
    {
        languageGuiFile = GUI_LANGUAGE_FOLDER + "english" + JSON_EXTENSION;
        modifyPanelSelected = false;
        fullScreenGuiMode = false;
        readFolder = (new File("")).getAbsolutePath() + "/../../../../etc/geometry";
        writeFolder = ".";
    }

    /**
    @return folder proposed when opening files: the one of the last file read
    */
    public String getReadFolder()
    {
        return readFolder;
    }

    /**
    @param readFolder folder to propose when opening files
    */
    public void setReadFolder(String readFolder)
    {
        this.readFolder = readFolder;
    }

    /**
    @return folder proposed when saving files: the one of the last file written
    */
    public String getWriteFolder()
    {
        return writeFolder;
    }

    /**
    @param writeFolder folder to propose when saving files
    */
    public void setWriteFolder(String writeFolder)
    {
        this.writeFolder = writeFolder;
    }

    /**
    @return I18N file the GUI is built from
    */
    public String getLanguageGuiFile()
    {
        return languageGuiFile;
    }

    /**
    @param languageGuiFile I18N file to build the GUI from
    */
    public void setLanguageGuiFile(String languageGuiFile)
    {
        this.languageGuiFile = languageGuiFile;
    }

    /**
    @return true if the panel that edits the selected body is shown
    */
    public boolean isModifyPanelSelected()
    {
        return modifyPanelSelected;
    }

    /**
    @param modifyPanelSelected true if the panel that edits the selected body
    is shown
    */
    public void setModifyPanelSelected(boolean modifyPanelSelected)
    {
        this.modifyPanelSelected = modifyPanelSelected;
    }

    /**
    @return true if only the drawing area is shown, without the rest of the GUI
    */
    public boolean isFullScreenGuiMode()
    {
        return fullScreenGuiMode;
    }

    /**
    @param fullScreenGuiMode true to show only the drawing area
    */
    public void setFullScreenGuiMode(boolean fullScreenGuiMode)
    {
        this.fullScreenGuiMode = fullScreenGuiMode;
    }

    /**
    Switches between showing only the drawing area and showing all the GUI.
    */
    public void toggleFullScreenGuiMode()
    {
        fullScreenGuiMode = !fullScreenGuiMode;
    }

    /**
    @return the identifiers of the languages available for the GUI, sorted:
    the names (without extension) of the JSON files in the GUI language folder
    */
    public static List<String> listLanguages()
    {
        List<String> languages = new ArrayList<>();
        File[] files = new File(GUI_LANGUAGE_FOLDER).listFiles();

        if ( files != null ) {
            for ( File file : files ) {
                String name = file.getName();
                if ( file.isFile() && name.endsWith(JSON_EXTENSION) ) {
                    languages.add(name.substring(0, name.length() - JSON_EXTENSION.length()));
                }
            }
        }
        Collections.sort(languages);
        return languages;
    }

    /**
    @param language identifier of a language (see `listLanguages`)
    @return the I18N file of that language
    */
    public static String languageFile(String language)
    {
        return GUI_LANGUAGE_FOLDER + language + JSON_EXTENSION;
    }

    /**
    @return the identifier of the language currently used by the GUI
    */
    public String getCurrentLanguage()
    {
        String name = new File(languageGuiFile).getName();

        if ( name.endsWith(JSON_EXTENSION) ) {
            name = name.substring(0, name.length() - JSON_EXTENSION.length());
        }
        return name;
    }
}
