#include "java/lang/String.h"
#include <GL/glew.h>
#include <GLFW/glfw3.h>

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <stdexcept>
#include <string>
#include <vector>

static java::String readShaderSource(const java::String& shaderFileName)
{
    java::String path = "../../../../etc/glslShaders/" + shaderFileName;
    FILE* file = std::fopen(path.c_str(), "r");
    if (!file) {
        throw std::runtime_error("Shader not found: " + path);
    }

    std::fseek(file, 0, SEEK_END);
    long size = std::ftell(file);
    std::fseek(file, 0, SEEK_SET);

    std::vector<char> buffer((size_t)size + 1u, 0);
    size_t readSize = std::fread(buffer.data(), 1, (size_t)size, file);
    buffer[readSize] = '\0';
    std::fclose(file);
    return java::String(buffer.data());
}

static java::String getShaderInfoLog(GLuint shader)
{
    GLint length = 0;
    glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &length);
    if (length <= 1) return "(no log)";
    std::vector<char> log((size_t)length, 0);
    glGetShaderInfoLog(shader, length, &length, log.data());
    return java::String(log.data());
}

static java::String getProgramInfoLog(GLuint program)
{
    GLint length = 0;
    glGetProgramiv(program, GL_INFO_LOG_LENGTH, &length);
    if (length <= 1) return "(no log)";
    std::vector<char> log((size_t)length, 0);
    glGetProgramInfoLog(program, length, &length, log.data());
    return java::String(log.data());
}

static GLuint compileShader(GLenum shaderType, const java::String& source)
{
    GLuint shader = glCreateShader(shaderType);
    const char* src = source.c_str();
    GLint length = (GLint)source.size();
    glShaderSource(shader, 1, &src, &length);
    glCompileShader(shader);

    GLint status = GL_FALSE;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
    if (status == GL_FALSE) {
        java::String log = getShaderInfoLog(shader);
        glDeleteShader(shader);
        throw std::runtime_error("Shader compile error: " + log);
    }
    return shader;
}

static GLuint createShaderProgram()
{
    java::String vertexSource = readShaderSource("constantVertexShader.glsl");
    java::String fragmentSource = readShaderSource("constantPixelShader.glsl");

    GLuint vs = compileShader(GL_VERTEX_SHADER, vertexSource);
    GLuint fs = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

    GLuint program = glCreateProgram();
    glAttachShader(program, vs);
    glAttachShader(program, fs);
    glBindFragDataLocation(program, 0, "fragColor");
    glLinkProgram(program);

    GLint status = GL_FALSE;
    glGetProgramiv(program, GL_LINK_STATUS, &status);
    if (status == GL_FALSE) {
        java::String log = getProgramInfoLog(program);
        glDeleteShader(vs);
        glDeleteShader(fs);
        glDeleteProgram(program);
        throw std::runtime_error("Program link error: " + log);
    }

    glDetachShader(program, vs);
    glDetachShader(program, fs);
    glDeleteShader(vs);
    glDeleteShader(fs);
    return program;
}

static void setShaderUniforms(GLuint program)
{
    float identity[] = {
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
    };

    GLint mvp = glGetUniformLocation(program, "modelViewProjectionLocal");
    GLint withTexture = glGetUniformLocation(program, "withTexture");
    GLint withVertexColors = glGetUniformLocation(program, "withVertexColors");
    GLint diffuseColor = glGetUniformLocation(program, "diffuseColor");

    if (mvp >= 0) glUniformMatrix4fv(mvp, 1, GL_FALSE, identity);
    if (withTexture >= 0) glUniform1i(withTexture, 0);
    if (withVertexColors >= 0) glUniform1i(withVertexColors, 0);
    if (diffuseColor >= 0) glUniform3f(diffuseColor, 1.0f, 1.0f, 1.0f);
}

static void writeRgbToPpm(const char* outFile, const std::vector<unsigned char>& rgb, int w, int h)
{
    FILE* f = std::fopen(outFile, "wb");
    if (!f) throw std::runtime_error("Could not open output file");
    std::fprintf(f, "P6\n%d %d\n255\n", w, h);

    for (int y = h - 1; y >= 0; y--) {
        const unsigned char* row = &rgb[(size_t)y * (size_t)w * 3u];
        std::fwrite(row, 1, (size_t)w * 3u, f);
    }
    std::fclose(f);
}

int main()
{
    const int width = 320;
    const int height = 240;

    if (!glfwInit()) {
        std::fprintf(stderr, "GLFW init failed\n");
        return 1;
    }

    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_DOUBLEBUFFER, GLFW_FALSE);

    GLFWwindow* window = glfwCreateWindow(width, height, "OpenGL4 Pbuffer Example", nullptr, nullptr);
    if (!window) {
        std::fprintf(stderr, "GLFW hidden window creation failed\n");
        glfwTerminate();
        return 1;
    }

    glfwMakeContextCurrent(window);

    glewExperimental = GL_TRUE;
    GLenum err = glewInit();
    if (err != GLEW_OK) {
        std::fprintf(stderr, "GLEW init failed: %s\n", glewGetErrorString(err));
        glfwDestroyWindow(window);
        glfwTerminate();
        return 1;
    }

    GLint major = 0, minor = 0;
    glGetIntegerv(GL_MAJOR_VERSION, &major);
    glGetIntegerv(GL_MINOR_VERSION, &minor);
    if (major < 4 || (major == 4 && minor < 1)) {
        std::fprintf(stderr, "OpenGL 4.1+ required. Found %d.%d\n", major, minor);
        glfwDestroyWindow(window);
        glfwTerminate();
        return 1;
    }

    GLuint program = 0, vao = 0, vbo = 0;
    try {
        program = createShaderProgram();

        glGenVertexArrays(1, &vao);
        glBindVertexArray(vao);

        glGenBuffers(1, &vbo);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        float vertexData[] = {
            -0.8f, -0.8f, 0.0f,
             0.8f,  0.8f, 0.0f
        };

        glBufferData(GL_ARRAY_BUFFER, sizeof(vertexData), vertexData, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 3 * sizeof(float), (void*)0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        glUseProgram(program);
        setShaderUniforms(program);
        glUseProgram(0);

        glViewport(0, 0, width, height);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        glUseProgram(program);
        glBindVertexArray(vao);
        glDrawArrays(GL_LINES, 0, 2);
        glBindVertexArray(0);
        glUseProgram(0);
        glFinish();

        std::vector<unsigned char> rgb((size_t)width * (size_t)height * 3u, 0u);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glReadPixels(0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, rgb.data());
        writeRgbToPpm("./output.ppm", rgb, width, height);
        std::printf("OpenGL4PbufferExample: exported ./output.ppm\n");
    }
    catch (const std::exception& e) {
        std::fprintf(stderr, "%s\n", e.what());
        if (vbo) glDeleteBuffers(1, &vbo);
        if (vao) glDeleteVertexArrays(1, &vao);
        if (program) glDeleteProgram(program);
        glfwDestroyWindow(window);
        glfwTerminate();
        return 1;
    }

    if (vbo) glDeleteBuffers(1, &vbo);
    if (vao) glDeleteVertexArrays(1, &vao);
    if (program) glDeleteProgram(program);
    glfwDestroyWindow(window);
    glfwTerminate();

    return 0;
}
