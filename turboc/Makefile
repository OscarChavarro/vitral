# Build of the turboc port with GNU make only, for legacy systems without
# CMake (e.g. Debian 1.1 with gcc 2.7.2): the base library and the offline
# examples, which need no window system, (scene-editor) the libraries of the
# Athena variant of SceneEditorApplication and the application,
# (scene-editor-motif) its Motif variant, and
# (glut-examples) the GLUT programs of testsuite/OpenGL1Examples and
# testsuite/_APITests. The options are the ones of legacy.mk, i.e.
#   make WITH_POSIX_THREADS=1
#   make scene-editor GL_DIR=$HOME/mesa/usr
# scripts/compileWithMake.sh runs it. The CMake build (scripts/compile.sh)
# remains the one for the full port.

all:
	cd base && $(MAKE)
	cd testsuite/OfflineExamples && $(MAKE)

# Needs X11 (Xt, Xaw, Xmu) and OpenGL 1.1 or later
scene-editor: all
	cd opengl1 && $(MAKE)
	cd xt && $(MAKE)
	cd xaw && $(MAKE)
	cd testsuite/ApplicationCases/SceneEditorApplication && $(MAKE)

# The Motif variant of SceneEditorApplication: needs Motif (i.e. LessTif)
scene-editor-motif: scene-editor
	cd xm && $(MAKE)
	cd testsuite/ApplicationCases/SceneEditorApplication && \
	    $(MAKE) VARIANT=xm_opengl1

# Needs GLUT and OpenGL 1.1 or later
glut-examples: all
	cd opengl1 && $(MAKE)
	cd glut && $(MAKE)
	cd testsuite/OpenGL1Examples && $(MAKE)
	cd testsuite/_APITests && $(MAKE)

clean:
	cd base && $(MAKE) clean
	cd testsuite/OfflineExamples && $(MAKE) clean
	cd opengl1 && $(MAKE) clean
	cd xt && $(MAKE) clean
	cd xaw && $(MAKE) clean
	cd xm && $(MAKE) clean
	cd testsuite/ApplicationCases/SceneEditorApplication && $(MAKE) clean
	cd glut && $(MAKE) clean
	cd testsuite/OpenGL1Examples && $(MAKE) clean
	cd testsuite/_APITests && $(MAKE) clean

.PHONY: all scene-editor scene-editor-motif glut-examples clean
