#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <exception>

#include <GL/glew.h>
#include <GL/glx.h>
#include <X11/Intrinsic.h>
#include <X11/Shell.h>
#include <X11/StringDefs.h>
#include <X11/keysym.h>

#include "java/lang/String.h"
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/common/logging/Logger.h"

#ifdef __APPLE__
#error "SceneEditorApplication Xt + GLX + OpenGL 4.1 is supported only on Linux/X11, not macOS/XQuartz."
#endif

#ifndef GLX_CONTEXT_MAJOR_VERSION_ARB
#define GLX_CONTEXT_MAJOR_VERSION_ARB 0x2091
#endif
#ifndef GLX_CONTEXT_MINOR_VERSION_ARB
#define GLX_CONTEXT_MINOR_VERSION_ARB 0x2092
#endif
#ifndef GLX_CONTEXT_PROFILE_MASK_ARB
#define GLX_CONTEXT_PROFILE_MASK_ARB 0x9126
#endif
#ifndef GLX_CONTEXT_CORE_PROFILE_BIT_ARB
#define GLX_CONTEXT_CORE_PROFILE_BIT_ARB 0x00000001
#endif
#ifndef GLX_CONTEXT_FLAGS_ARB
#define GLX_CONTEXT_FLAGS_ARB 0x2094
#endif
#ifndef GLX_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB
#define GLX_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB 0x00000002
#endif

typedef GLXContext (*CreateContextAttribsARBProc)(
    Display*,
    GLXFBConfig,
    GLXContext,
    Bool,
    const int*);
typedef void (*SwapIntervalEXTProc)(Display*, GLXDrawable, int);

class SceneEditorApplication
{
public:
    SceneEditorApplication()
        : appContext(nullptr)
        , display(nullptr)
        , shell(nullptr)
        , window(0)
        , fbConfig(nullptr)
        , context(nullptr)
        , wmDeleteWindow(None)
        , shaderProgramId(0)
        , vertexArrayId(0)
        , vertexBufferId(0)
        , width(640)
        , height(480)
        , ready(false)
        , closing(false)
    {
    }

    ~SceneEditorApplication()
    {
        cleanupOpenGL();
        if (context != nullptr) {
            glXMakeCurrent(display, None, nullptr);
            glXDestroyContext(display, context);
            context = nullptr;
        }
        if (shell != nullptr) {
            XtDestroyWidget(shell);
            shell = nullptr;
        }
        if (display != nullptr) {
            XtCloseDisplay(display);
            display = nullptr;
        }
    }

    int run(int argc, char** argv)
    {
        try {
            createWindow(argc, argv);
            createContext();
            initOpenGL();
            redraw();
            XtAppMainLoop(appContext);
        }
        catch (const VSDKFatalException& ex) {
            fprintf(stderr, "SceneEditorApplication fatal error: %s\n", ex.what());
            return 1;
        }
        catch (const std::exception& ex) {
            fprintf(stderr, "SceneEditorApplication error: %s\n", ex.what());
            return 1;
        }
        return 0;
    }

private:
    XtAppContext appContext;
    Display* display;
    Widget shell;
    Window window;
    GLXFBConfig fbConfig;
    GLXContext context;
    Atom wmDeleteWindow;
    GLuint shaderProgramId;
    GLuint vertexArrayId;
    GLuint vertexBufferId;
    int width;
    int height;
    bool ready;
    bool closing;

    void createWindow(int argc, char** argv)
    {
        XtToolkitInitialize();
        appContext = XtCreateApplicationContext();
        display = XtOpenDisplay(
            appContext,
            nullptr,
            "scene-editor",
            "SceneEditorApplication",
            nullptr,
            0,
            &argc,
            argv);
        if (display == nullptr) {
            throw VSDKFatalException("Could not open X display. Is DISPLAY set?");
        }

        XVisualInfo* visualInfo = chooseVisual();
        Colormap colormap = XCreateColormap(
            display,
            RootWindow(display, visualInfo->screen),
            visualInfo->visual,
            AllocNone);

        Arg args[8];
        Cardinal n = 0;
        XtSetArg(args[n], XtNvisual, visualInfo->visual); n++;
        XtSetArg(args[n], XtNcolormap, colormap); n++;
        XtSetArg(args[n], XtNdepth, visualInfo->depth); n++;
        XtSetArg(args[n], XtNwidth, width); n++;
        XtSetArg(args[n], XtNheight, height); n++;
        XtSetArg(args[n], XtNtitle, "VITRAL Scene Editor - Xt GLX OpenGL 4"); n++;

        shell = XtAppCreateShell(
            "sceneEditor",
            "SceneEditorApplication",
            topLevelShellWidgetClass,
            display,
            args,
            n);
        if (shell == nullptr) {
            XFree(visualInfo);
            throw VSDKFatalException("Could not create Xt shell");
        }

        XtAddEventHandler(
            shell,
            ExposureMask | StructureNotifyMask | KeyPressMask,
            False,
            &SceneEditorApplication::eventHandler,
            reinterpret_cast<XtPointer>(this));

        XtRealizeWidget(shell);
        window = XtWindow(shell);
        wmDeleteWindow = XInternAtom(display, "WM_DELETE_WINDOW", False);
        XSetWMProtocols(display, window, &wmDeleteWindow, 1);
        XtAddEventHandler(
            shell,
            NoEventMask,
            True,
            &SceneEditorApplication::eventHandler,
            reinterpret_cast<XtPointer>(this));

        XFree(visualInfo);
    }

    XVisualInfo* chooseVisual()
    {
        const int fbAttributes[] = {
            GLX_X_RENDERABLE, True,
            GLX_DRAWABLE_TYPE, GLX_WINDOW_BIT,
            GLX_RENDER_TYPE, GLX_RGBA_BIT,
            GLX_X_VISUAL_TYPE, GLX_TRUE_COLOR,
            GLX_RED_SIZE, 8,
            GLX_GREEN_SIZE, 8,
            GLX_BLUE_SIZE, 8,
            GLX_ALPHA_SIZE, 8,
            GLX_DEPTH_SIZE, 24,
            GLX_DOUBLEBUFFER, True,
            None
        };

        int count = 0;
        GLXFBConfig* configs = glXChooseFBConfig(
            display,
            DefaultScreen(display),
            fbAttributes,
            &count);
        if (configs == nullptr || count == 0) {
            throw VSDKFatalException("No suitable GLX framebuffer config found");
        }

        XVisualInfo* visualInfo = nullptr;
        for (int i = 0; i < count; ++i) {
            visualInfo = glXGetVisualFromFBConfig(display, configs[i]);
            if (visualInfo != nullptr) {
                fbConfig = configs[i];
                break;
            }
        }

        XFree(configs);
        if (visualInfo == nullptr) {
            throw VSDKFatalException("No X visual found for GLX framebuffer config");
        }
        return visualInfo;
    }

    void createContext()
    {
        CreateContextAttribsARBProc createContextAttribs =
            reinterpret_cast<CreateContextAttribsARBProc>(
                glXGetProcAddressARB(
                    reinterpret_cast<const GLubyte*>("glXCreateContextAttribsARB")));

        if (createContextAttribs == nullptr) {
            throw VSDKFatalException("GLX_ARB_create_context is not available");
        }

        const int contextAttributes[] = {
            GLX_CONTEXT_MAJOR_VERSION_ARB, 4,
            GLX_CONTEXT_MINOR_VERSION_ARB, 1,
            GLX_CONTEXT_PROFILE_MASK_ARB, GLX_CONTEXT_CORE_PROFILE_BIT_ARB,
            GLX_CONTEXT_FLAGS_ARB, GLX_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB,
            None
        };

        context = createContextAttribs(
            display,
            fbConfig,
            nullptr,
            True,
            contextAttributes);
        if (context == nullptr) {
            throw VSDKFatalException("Could not create an OpenGL 4.1 core GLX context");
        }

        if (!glXMakeCurrent(display, window, context)) {
            throw VSDKFatalException("Could not make GLX context current");
        }

        SwapIntervalEXTProc swapInterval =
            reinterpret_cast<SwapIntervalEXTProc>(
                glXGetProcAddressARB(
                    reinterpret_cast<const GLubyte*>("glXSwapIntervalEXT")));
        if (swapInterval != nullptr) {
            swapInterval(display, window, 1);
        }
    }

    void initOpenGL()
    {
        glewExperimental = GL_TRUE;
        GLenum err = glewInit();
        if (err != GLEW_OK) {
            fprintf(stderr, "GLEW init failed: %s\n", glewGetErrorString(err));
            throw VSDKFatalException("GLEW init failed");
        }
        glGetError();

        checkOpenGLVersion();
        shaderProgramId = createShaderProgram();

        glGenVertexArrays(1, &vertexArrayId);
        glBindVertexArray(vertexArrayId);

        glGenBuffers(1, &vertexBufferId);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferId);

        const float vertexData[] = {
            -0.8f, -0.8f, 0.0f,
             0.8f,  0.8f, 0.0f
        };

        glBufferData(GL_ARRAY_BUFFER, sizeof(vertexData), vertexData, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 3 * sizeof(float), nullptr);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        glUseProgram(shaderProgramId);
        setShaderUniforms();
        glUseProgram(0);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glViewport(0, 0, width, height);
        ready = true;
    }

    void checkOpenGLVersion()
    {
        const char* versionStr = reinterpret_cast<const char*>(glGetString(GL_VERSION));
        const char* rendererStr = reinterpret_cast<const char*>(glGetString(GL_RENDERER));
        const char* vendorStr = reinterpret_cast<const char*>(glGetString(GL_VENDOR));
        printf("OpenGL vendor: %s\n", vendorStr != nullptr ? vendorStr : "(unknown)");
        printf("OpenGL renderer: %s\n", rendererStr != nullptr ? rendererStr : "(unknown)");
        printf("OpenGL version string: %s\n", versionStr != nullptr ? versionStr : "(unknown)");

        GLint major = 0;
        GLint minor = 0;
        glGetIntegerv(GL_MAJOR_VERSION, &major);
        glGetIntegerv(GL_MINOR_VERSION, &minor);
        if ((major == 0 && minor == 0) && versionStr != nullptr) {
            sscanf(versionStr, "%d.%d", &major, &minor);
        }
        printf("OpenGL parsed version: %d.%d\n", major, minor);

        if (major < 4 || (major == 4 && minor < 1)) {
            Logger::reportMessage(
                "SceneEditorApplication",
                Logger::ERROR,
                "checkOpenGLVersion",
                "OpenGL version too old");
            throw VSDKFatalException("This example requires OpenGL 4.1+");
        }
    }

    GLuint createShaderProgram()
    {
        const java::String vertexSource =
            "#version 410 core\n"
            "layout(location = 0) in vec3 vertexPosition;\n"
            "uniform mat4 modelViewProjectionLocal;\n"
            "void main() {\n"
            "    gl_Position = modelViewProjectionLocal * vec4(vertexPosition, 1.0);\n"
            "}\n";
        const java::String fragmentSource =
            "#version 410 core\n"
            "uniform vec3 diffuseColor;\n"
            "out vec4 fragColor;\n"
            "void main() {\n"
            "    fragColor = vec4(diffuseColor, 1.0);\n"
            "}\n";

        GLuint vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource);
        GLuint fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        GLuint program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glBindFragDataLocation(program, 0, "fragColor");
        glLinkProgram(program);

        GLint linkStatus = GL_FALSE;
        glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
        if (linkStatus == GL_FALSE) {
            java::String log = getProgramInfoLog(program);
            fprintf(stderr, "Program link error: %s\n", log.c_str());
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            glDeleteProgram(program);
            throw VSDKFatalException("Shader program link failed");
        }

        glDetachShader(program, vertexShader);
        glDetachShader(program, fragmentShader);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    GLuint compileShader(GLenum shaderType, const java::String& source)
    {
        GLuint shader = glCreateShader(shaderType);
        const char* src = source.c_str();
        GLint length = source.length();

        glShaderSource(shader, 1, &src, &length);
        glCompileShader(shader);

        GLint compileStatus = GL_FALSE;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compileStatus);
        if (compileStatus == GL_FALSE) {
            java::String log = getShaderInfoLog(shader);
            fprintf(stderr, "Shader compile error: %s\n", log.c_str());
            glDeleteShader(shader);
            throw VSDKFatalException("Shader compile failed");
        }
        return shader;
    }

    void setShaderUniforms()
    {
        const float identity[] = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };

        GLint mvpLoc = glGetUniformLocation(shaderProgramId, "modelViewProjectionLocal");
        GLint diffuseColorLoc = glGetUniformLocation(shaderProgramId, "diffuseColor");

        if (mvpLoc >= 0) {
            glUniformMatrix4fv(mvpLoc, 1, GL_FALSE, identity);
        }
        if (diffuseColorLoc >= 0) {
            glUniform3f(diffuseColorLoc, 1.0f, 1.0f, 1.0f);
        }
    }

    void redraw()
    {
        if (!ready || display == nullptr || context == nullptr) {
            return;
        }
        glXMakeCurrent(display, window, context);
        glViewport(0, 0, width, height);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        glUseProgram(shaderProgramId);
        glBindVertexArray(vertexArrayId);
        glLineWidth(1.0f);
        glDrawArrays(GL_LINES, 0, 2);
        glBindVertexArray(0);
        glUseProgram(0);

        glXSwapBuffers(display, window);
    }

    void requestClose()
    {
        if (closing) {
            return;
        }
        closing = true;
        XtAppSetExitFlag(appContext);
    }

    static void eventHandler(
        Widget,
        XtPointer clientData,
        XEvent* event,
        Boolean*)
    {
        SceneEditorApplication* self =
            reinterpret_cast<SceneEditorApplication*>(clientData);
        if (self == nullptr) {
            return;
        }

        switch (event->type) {
        case Expose:
            if (event->xexpose.count == 0) {
                self->redraw();
            }
            break;
        case ConfigureNotify:
            self->width = event->xconfigure.width;
            self->height = event->xconfigure.height;
            self->redraw();
            break;
        case KeyPress: {
            KeySym key = XLookupKeysym(&event->xkey, 0);
            if (key == XK_Escape || key == XK_q || key == XK_Q) {
                self->requestClose();
            }
            break;
        }
        case ClientMessage:
            if (static_cast<Atom>(event->xclient.data.l[0]) == self->wmDeleteWindow) {
                self->requestClose();
            }
            break;
        default:
            break;
        }
    }

    java::String getShaderInfoLog(GLuint shader)
    {
        GLint length = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &length);
        if (length <= 1) {
            return "(no log)";
        }

        char* log = new char[length]();
        glGetShaderInfoLog(shader, length, &length, log);
        java::String result(log);
        delete[] log;
        return result;
    }

    java::String getProgramInfoLog(GLuint program)
    {
        GLint length = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &length);
        if (length <= 1) {
            return "(no log)";
        }

        char* log = new char[length]();
        glGetProgramInfoLog(program, length, &length, log);
        java::String result(log);
        delete[] log;
        return result;
    }

    void cleanupOpenGL()
    {
        if (display != nullptr && context != nullptr) {
            glXMakeCurrent(display, window, context);
        }
        if (vertexBufferId != 0) {
            glDeleteBuffers(1, &vertexBufferId);
            vertexBufferId = 0;
        }
        if (vertexArrayId != 0) {
            glDeleteVertexArrays(1, &vertexArrayId);
            vertexArrayId = 0;
        }
        if (shaderProgramId != 0) {
            glDeleteProgram(shaderProgramId);
            shaderProgramId = 0;
        }
        ready = false;
    }
};

int main(int argc, char** argv)
{
    SceneEditorApplication app;
    return app.run(argc, argv);
}
