#include <dirent.h>
#include <sys/stat.h>
#include <unistd.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/io/File.h"
#include "java/util/ArrayList.txx"
#include "model/GuiState.h"

// All the ports share the I18N files of the etc folder of VITRAL
const char* const GuiState::GUI_LANGUAGE_FOLDER = "../../../../etc/gui/";
const char* const GuiState::JSON_EXTENSION = ".json";
const char* const GuiState::APPLICATION_DATA_FOLDER = "../../../../";

static BOOLEAN endsWith(const String& text, const String& suffix)
{
    return text.length() >= suffix.length() &&
        text.substring(text.length() - suffix.length()).equals(suffix);
}

static String currentDirectory()
{
    char buffer[4096];
    if ( getcwd(buffer, sizeof(buffer)) == NULL ) {
        return String(".");
    }
    return String(buffer);
}

GuiState::GuiState()
{
    languageGuiFile = String(GUI_LANGUAGE_FOLDER) + "english" +
        JSON_EXTENSION;
    modifyPanelSelected = FALSE;
    fullScreenGuiMode = FALSE;
    readFolder = currentDirectory() + "/../../../../etc/geometry";
    writeFolder = ".";
}

const String& GuiState::getReadFolder() const
{
    return readFolder;
}

void GuiState::setReadFolder(const String& readFolder)
{
    this->readFolder = readFolder;
}

const String& GuiState::getWriteFolder() const
{
    return writeFolder;
}

void GuiState::setWriteFolder(const String& writeFolder)
{
    this->writeFolder = writeFolder;
}

const String& GuiState::getLanguageGuiFile() const
{
    return languageGuiFile;
}

void GuiState::setLanguageGuiFile(const String& languageGuiFile)
{
    this->languageGuiFile = languageGuiFile;
}

BOOLEAN GuiState::isModifyPanelSelected() const
{
    return modifyPanelSelected;
}

void GuiState::setModifyPanelSelected(BOOLEAN modifyPanelSelected)
{
    this->modifyPanelSelected = modifyPanelSelected;
}

BOOLEAN GuiState::isFullScreenGuiMode() const
{
    return fullScreenGuiMode;
}

void GuiState::setFullScreenGuiMode(BOOLEAN fullScreenGuiMode)
{
    this->fullScreenGuiMode = fullScreenGuiMode;
}

void GuiState::toggleFullScreenGuiMode()
{
    fullScreenGuiMode = !fullScreenGuiMode;
}

ArrayList<String> GuiState::listLanguages()
{
    ArrayList<String> languages;
    String extension(JSON_EXTENSION);
    DIR* directory = opendir(GUI_LANGUAGE_FOLDER);

    if ( directory != NULL ) {
        struct dirent* entry;
        while ( (entry = readdir(directory)) != NULL ) {
            String name(entry->d_name);
            String path = String(GUI_LANGUAGE_FOLDER) + name;
            struct stat status;
            if ( stat(path.c_str(), &status) == 0 &&
                 S_ISREG(status.st_mode) && endsWith(name, extension) ) {
                languages.add(name.substring(0,
                    name.length() - extension.length()));
            }
        }
        closedir(directory);
    }

    // Sorted by insertion (the list is short)
    long i;
    long j;
    for ( i = 1; i < languages.size(); i++ ) {
        String key = languages.get(i);
        for ( j = i - 1; j >= 0 && key < languages.get(j); j-- ) {
            languages.set(j + 1, languages.get(j));
        }
        languages.set(j + 1, key);
    }
    return languages;
}

String GuiState::languageFile(const String& language)
{
    return String(GUI_LANGUAGE_FOLDER) + language + JSON_EXTENSION;
}

String GuiState::getCurrentLanguage() const
{
    String name = File(languageGuiFile).getName();
    String extension(JSON_EXTENSION);

    if ( endsWith(name, extension) ) {
        name = name.substring(0, name.length() - extension.length());
    }
    return name;
}
