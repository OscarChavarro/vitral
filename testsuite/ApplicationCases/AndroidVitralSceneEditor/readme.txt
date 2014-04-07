In order to build this project:
  - Start an IDE such as Eclipse or Netbeans with ADT (Android Development 
    Tools) installed. The prefered method for creating a project on Eclipse
    is to prepare all of the files under the "samples" folder on ADT, and
    later let Eclipse to build a project from the "new project" option.
    On Netbeans, create an empty project with a package name of
    "vitral.application" and a main class called "VitralActivity", do
    clean and build, and delete xml folder and VitralActivity class, to
    later add current project files.
  - On netbeans, the new project creation copies some files. Delete 
    VitralActivity.java and res/layout
  - Make the folder res/raw
  - Copy into res/raw render.png and *.glsl from etc/cgShaders folder on vitral
  - Copy into res/raw miniearth.png etc/textures folder on vitral
  - Copy AndroidManifest.xml from vitral/testsuite/ApplicationCases/
    AndroidVitralSceneEditor/AndroidManifest.xml to project
  - Copy library code from src and src_android folders from vitral to project.
    When building a different application from current template, you should
    relocate the "R" class package from "vitral.application" to your desired
    package.
  - Copy application code from 
    testsuite/ApplicationCases/AndroidVitralSceneEditor to project
  - From here, project should compile and run on Android device with basic
    features. Bump mapping and mesh loading examples are still not available
  - Locate a folder on your device in order to copy "mug.ply" and textures.
    Transfer files there from etc vitral directory. Edit application code
    to reflect current device folder location.
