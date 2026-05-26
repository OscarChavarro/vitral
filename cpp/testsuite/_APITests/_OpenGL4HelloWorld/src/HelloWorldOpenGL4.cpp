#include "java/lang/String.h"
#include <GL/glew.h>
#include <GLFW/glfw3.h>

#include <cstdio>
#include <cstdlib>
#include <fstream>
#include <string>
#include <vector>
#include <cstring>

class HelloWorldOpenGL4
{
public:
    static HelloWorldOpenGL4* instance;

    HelloWorldOpenGL4()
        : window(nullptr)
        , shaderProgramId(0)
        , vertexArrayId(0)
        , vertexBufferId(0)
        , ready(false)
        , closing(false)
    {
    }

    ~HelloWorldOpenGL4()
    {
        cleanup();
    }

    void init()
    {
        glewExperimental = GL_TRUE;
        GLenum err = glewInit();
        if (err != GLEW_OK) {
            fprintf(stderr, "GLEW init failed: %s\n", glewGetErrorString(err));
            return;
        }

        checkOpenGLVersion();

        shaderProgramId = createShaderProgram();
        if (shaderProgramId == 0) {
            fprintf(stderr, "Failed to create shader program\n");
            return;
        }

        glGenVertexArrays(1, &vertexArrayId);
        glBindVertexArray(vertexArrayId);

        glGenBuffers(1, &vertexBufferId);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferId);

        float vertexData[] = {
            -0.8f, -0.8f, 0.0f,
             0.8f,  0.8f, 0.0f
        };

        glBufferData(
            GL_ARRAY_BUFFER,
            sizeof(vertexData),
            vertexData,
            GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 3 * sizeof(float), (void*)0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        glUseProgram(shaderProgramId);
        setShaderUniforms();
        glUseProgram(0);

        ready = true;
    }

    void display()
    {
        if (!ready) return;

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        glUseProgram(shaderProgramId);
        glBindVertexArray(vertexArrayId);
        glLineWidth(1.0f);
        glDrawArrays(GL_LINES, 0, 2);
        glBindVertexArray(0);
        glUseProgram(0);

        glfwSwapBuffers(window);
    }

    void reshape(int width, int height)
    {
        glViewport(0, 0, width, height);
    }

    void keyboard(int key, int scancode, int action, int mods)
    {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            requestClose();
        }
    }

    void run()
    {
        if (!glfwInit()) {
            fprintf(stderr, "GLFW init failed\n");
            return;
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        window = glfwCreateWindow(640, 480, "VITRAL concept test - OpenGL4 Hello World", nullptr, nullptr);
        if (!window) {
            fprintf(stderr, "GLFW window creation failed\n");
            glfwTerminate();
            return;
        }

        glfwMakeContextCurrent(window);
        glfwSetWindowUserPointer(window, this);
        glfwSwapInterval(1);

        glfwSetKeyCallback(window, [](GLFWwindow* win, int key, int scancode, int action, int mods) {
            HelloWorldOpenGL4* self = static_cast<HelloWorldOpenGL4*>(glfwGetWindowUserPointer(win));
            if (self) self->keyboard(key, scancode, action, mods);
        });

        glfwSetFramebufferSizeCallback(window, [](GLFWwindow* win, int width, int height) {
            HelloWorldOpenGL4* self = static_cast<HelloWorldOpenGL4*>(glfwGetWindowUserPointer(win));
            if (self) self->reshape(width, height);
        });

        init();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        while (!glfwWindowShouldClose(window) && !closing) {
            display();
            glfwPollEvents();
        }

        cleanup();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    void requestClose()
    {
        if (closing) return;
        closing = true;
        if (window) {
            glfwSetWindowShouldClose(window, GLFW_TRUE);
        }
    }

private:
    GLFWwindow* window;
    GLuint shaderProgramId;
    GLuint vertexArrayId;
    GLuint vertexBufferId;
    bool ready;
    bool closing;

    void checkOpenGLVersion()
    {
        const char* versionStr = (const char*)glGetString(GL_VERSION);
        printf("OpenGL version string: %s\n", versionStr);

        GLint major = 0, minor = 0;
        glGetIntegerv(GL_MAJOR_VERSION, &major);
        glGetIntegerv(GL_MINOR_VERSION, &minor);

        printf("OpenGL parsed version: %d.%d\n", major, minor);

        if (major < 4 || (major == 4 && minor < 1)) {
            fprintf(stderr, "This example requires OpenGL 4.1+. Current context is %d.%d\n", major, minor);
            throw std::runtime_error("OpenGL version too old");
        }
    }

    GLuint createShaderProgram()
    {
        java::String vertexSource = readShaderSource("constantVertexShader.glsl");
        java::String fragmentSource = readShaderSource("constantPixelShader.glsl");

        GLuint vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource);
        GLuint fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        if (vertexShader == 0 || fragmentShader == 0) {
            return 0;
        }

        GLuint program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glBindFragDataLocation(program, 0, "fragColor");
        glLinkProgram(program);

        GLint linkStatus;
        glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
        if (linkStatus == GL_FALSE) {
            java::String log = getProgramInfoLog(program);
            fprintf(stderr, "Program link error: %s\n", log.c_str());
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            glDeleteProgram(program);
            return 0;
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

        GLint compileStatus;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compileStatus);
        if (compileStatus == GL_FALSE) {
            java::String log = getShaderInfoLog(shader);
            fprintf(stderr, "Shader compile error: %s\n", log.c_str());
            glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    void setShaderUniforms()
    {
        float identity[] = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };

        GLint modelViewProjectionLocalLoc = glGetUniformLocation(shaderProgramId, "modelViewProjectionLocal");
        GLint withTextureLoc = glGetUniformLocation(shaderProgramId, "withTexture");
        GLint withVertexColorsLoc = glGetUniformLocation(shaderProgramId, "withVertexColors");
        GLint diffuseColorLoc = glGetUniformLocation(shaderProgramId, "diffuseColor");

        if (modelViewProjectionLocalLoc >= 0) {
            glUniformMatrix4fv(modelViewProjectionLocalLoc, 1, GL_FALSE, identity);
        }
        if (withTextureLoc >= 0) {
            glUniform1i(withTextureLoc, 0);
        }
        if (withVertexColorsLoc >= 0) {
            glUniform1i(withVertexColorsLoc, 0);
        }
        if (diffuseColorLoc >= 0) {
            glUniform3f(diffuseColorLoc, 1.0f, 1.0f, 1.0f);
        }
    }

    java::String readShaderSource(const java::String& shaderFileName)
    {
        java::String path = "../../../../etc/glslShaders/" + shaderFileName;
        FILE* file = fopen(path.c_str(), "r");
        if (!file) {
            fprintf(stderr, "Shader not found: %s\n", shaderFileName.c_str());
            throw std::runtime_error("Shader file not found: " + shaderFileName);
        }

        fseek(file, 0, SEEK_END);
        long size = ftell(file);
        fseek(file, 0, SEEK_SET);

        char* buffer = new char[size + 1];
        size_t read_size = fread(buffer, 1, size, file);
        buffer[read_size] = '\0';
        fclose(file);

        printf("Loaded shader from: %s\n", path.c_str());
        java::String result(buffer);
        delete[] buffer;
        return result;
    }

    java::String getShaderInfoLog(GLuint shader)
    {
        GLint length;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &length);
        if (length <= 1) {
            return "(no log)";
        }

        std::vector<char> log(length);
        glGetShaderInfoLog(shader, length, &length, log.data());
        return java::String(log.data());
    }

    java::String getProgramInfoLog(GLuint program)
    {
        GLint length;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &length);
        if (length <= 1) {
            return "(no log)";
        }

        std::vector<char> log(length);
        glGetProgramInfoLog(program, length, &length, log.data());
        return java::String(log.data());
    }

    void cleanup()
    {
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

HelloWorldOpenGL4* HelloWorldOpenGL4::instance = nullptr;

int main(int argc, char* argv[])
{
    HelloWorldOpenGL4 app;
    app.run();
    return 0;
}
