#include "CommandLineOptions.h"

#include <algorithm>
#include <cctype>
#include <cstdlib>
#include <stdexcept>

#include "vsdk/toolkit/environment/material/RendererConfiguration.h"

CommandLineOptions::CommandLineOptions()
    : offline(false),
      method(ShaderOperationMode::OPENGL_4_1),
      hasRotation(false), rotationDegrees(0.0),
      hasLightRotation(false), lightRotationDegrees(0.0),
      hasWithTexture(false), withTexture(true),
      hasWithBumpMap(false), withBumpMap(true),
      hasShadingType(false), shadingType(RendererConfiguration::SHADING_TYPE_PHONG),
      hasTextureFilter(false), textureFilter(TextureFilterOption::LINEAR),
      hasMeridians(false), meridians(100),
      hasParallels(false), parallels(50),
      hasCpuTextureOffsetU(false), cpuTextureOffsetUTexels(-0.5),
      hasCpuTextureOffsetV(false), cpuTextureOffsetVTexels(-0.5),
      showHud(false), width(1100), height(900) {}

static java::String lower(const java::String& s)
{
    java::String out = s;
    std::transform(out.begin(), out.end(), out.begin(), [](unsigned char c){ return (char)std::tolower(c); });
    return out;
}

static bool startsWith(const java::String& s, const java::String& p)
{
    return s.size() >= p.size() && s.substr(0, p.size()) == p;
}

static ShaderOperationMode parseMethod(const java::String& raw)
{
    const java::String v = lower(raw);
    if (v == "opengl" || v == "opengl_4_1") return ShaderOperationMode::OPENGL_4_1;
    if (v == "software") return ShaderOperationMode::SOFTWARE;
    throw std::invalid_argument("Unknown --method value: " + raw + ". Use opengl or software.");
}

static int parseShading(const java::String& raw)
{
    const java::String v = lower(raw);
    if (v == "constant" || v == "nolight") return RendererConfiguration::SHADING_TYPE_NOLIGHT;
    if (v == "flat") return RendererConfiguration::SHADING_TYPE_FLAT;
    if (v == "gouraud") return RendererConfiguration::SHADING_TYPE_GOURAUD;
    if (v == "phong") return RendererConfiguration::SHADING_TYPE_PHONG;
    if (v == "cook" || v == "cook_torrance" || v == "cook-torrance" || v == "cooktorrance") {
        return RendererConfiguration::SHADING_TYPE_COOK_TERRANCE;
    }
    throw std::invalid_argument("Unknown --shading value: " + raw);
}

static CommandLineOptions::TextureFilterOption parseTextureFilter(const java::String& raw)
{
    const java::String v = lower(raw);
    if (v == "linear") return CommandLineOptions::TextureFilterOption::LINEAR;
    if (v == "nearest") return CommandLineOptions::TextureFilterOption::NEAREST;
    throw std::invalid_argument("Unknown --texture-filter value: " + raw);
}

static void applyFeatureSwitches(CommandLineOptions& o, const java::String& csv, bool enabled)
{
    size_t b = 0;
    while (b < csv.size()) {
        size_t e = csv.find(',', b);
        if (e == java::String::npos) e = csv.size();
        const java::String t0 = lower(csv.substr(b, e - b));
        java::String t;
        for (size_t i = 0; i < t0.size(); ++i) {
            const unsigned char c = (unsigned char)t0[i];
            if (!std::isspace(c)) {
                char oneChar[2] = { t0[i], '\0' };
                t += oneChar;
            }
        }
        if (t == "texture" || t == "textures") {
            o.hasWithTexture = true;
            o.withTexture = enabled;
        }
        else if (t == "bumpmap" || t == "bump" || t == "normalmap") {
            o.hasWithBumpMap = true;
            o.withBumpMap = enabled;
        }
        else if (!t.empty()) {
            throw std::invalid_argument("Unknown feature in --with/--without: " + t);
        }
        b = e + 1;
    }
}

CommandLineOptions CommandLineOptions::parse(int argc, char** argv)
{
    CommandLineOptions o;
    for (int i = 1; i < argc; i++) {
        java::String arg(argv[i]);
        auto readValue = [&](const java::String& name)->java::String {
            if (i + 1 >= argc) throw std::invalid_argument(name + " requires a value");
            return java::String(argv[++i]);
        };

        if (arg == "--offline") { o.offline = true; o.offlineOutputPath = readValue("--offline"); continue; }
        if (startsWith(arg, "--offline=")) { o.offline = true; o.offlineOutputPath = arg.substr(10); continue; }
        if (arg == "--method") { o.method = parseMethod(readValue("--method")); continue; }
        if (startsWith(arg, "--method=")) { o.method = parseMethod(arg.substr(9)); continue; }
        if (arg == "--rotation") { o.hasRotation = true; o.rotationDegrees = std::atof(readValue("--rotation").c_str()); continue; }
        if (startsWith(arg, "--rotation=")) { o.hasRotation = true; o.rotationDegrees = std::atof(arg.substr(11).c_str()); continue; }
        if (arg == "--light-rotation") { o.hasLightRotation = true; o.lightRotationDegrees = std::atof(readValue("--light-rotation").c_str()); continue; }
        if (startsWith(arg, "--light-rotation=")) { o.hasLightRotation = true; o.lightRotationDegrees = std::atof(arg.substr(17).c_str()); continue; }
        if (arg == "--with") { applyFeatureSwitches(o, readValue("--with"), true); continue; }
        if (startsWith(arg, "--with=")) { applyFeatureSwitches(o, arg.substr(7), true); continue; }
        if (arg == "--without") { applyFeatureSwitches(o, readValue("--without"), false); continue; }
        if (startsWith(arg, "--without=")) { applyFeatureSwitches(o, arg.substr(10), false); continue; }
        if (arg == "--shading") { o.hasShadingType = true; o.shadingType = parseShading(readValue("--shading")); continue; }
        if (startsWith(arg, "--shading=")) { o.hasShadingType = true; o.shadingType = parseShading(arg.substr(10)); continue; }
        if (arg == "--texture-filter") { o.hasTextureFilter = true; o.textureFilter = parseTextureFilter(readValue("--texture-filter")); continue; }
        if (startsWith(arg, "--texture-filter=")) { o.hasTextureFilter = true; o.textureFilter = parseTextureFilter(arg.substr(17)); continue; }
        if (arg == "--meridians") { o.hasMeridians = true; o.meridians = std::max(3, std::atoi(readValue("--meridians").c_str())); continue; }
        if (startsWith(arg, "--meridians=")) { o.hasMeridians = true; o.meridians = std::max(3, std::atoi(arg.substr(12).c_str())); continue; }
        if (arg == "--parallels") { o.hasParallels = true; o.parallels = std::max(2, std::atoi(readValue("--parallels").c_str())); continue; }
        if (startsWith(arg, "--parallels=")) { o.hasParallels = true; o.parallels = std::max(2, std::atoi(arg.substr(12).c_str())); continue; }
        if (arg == "--cpu-texture-offset-u") { o.hasCpuTextureOffsetU = true; o.cpuTextureOffsetUTexels = std::atof(readValue("--cpu-texture-offset-u").c_str()); continue; }
        if (startsWith(arg, "--cpu-texture-offset-u=")) { o.hasCpuTextureOffsetU = true; o.cpuTextureOffsetUTexels = std::atof(arg.substr(23).c_str()); continue; }
        if (arg == "--cpu-texture-offset-v") { o.hasCpuTextureOffsetV = true; o.cpuTextureOffsetVTexels = std::atof(readValue("--cpu-texture-offset-v").c_str()); continue; }
        if (startsWith(arg, "--cpu-texture-offset-v=")) { o.hasCpuTextureOffsetV = true; o.cpuTextureOffsetVTexels = std::atof(arg.substr(23).c_str()); continue; }
        if (arg == "--width") { o.width = std::max(1, std::atoi(readValue("--width").c_str())); continue; }
        if (startsWith(arg, "--width=")) { o.width = std::max(1, std::atoi(arg.substr(8).c_str())); continue; }
        if (arg == "--height") { o.height = std::max(1, std::atoi(readValue("--height").c_str())); continue; }
        if (startsWith(arg, "--height=")) { o.height = std::max(1, std::atoi(arg.substr(9).c_str())); continue; }
        if (arg == "--hud") { o.showHud = true; continue; }
        if (startsWith(arg, "--hud=")) { o.showHud = lower(arg.substr(6)) != "off"; continue; }
        throw std::invalid_argument("Unknown option: " + arg);
    }

    if (o.offline && o.offlineOutputPath.empty()) {
        throw std::invalid_argument("--offline requires an output file path");
    }
    return o;
}
