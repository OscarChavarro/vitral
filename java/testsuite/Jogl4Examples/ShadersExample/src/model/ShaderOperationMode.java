package model;

public enum ShaderOperationMode
{
    OPENGL_4_1,
    SOFTWARE;

    public ShaderOperationMode next()
    {
        ShaderOperationMode[] values = values();
        int nextIndex = (ordinal() + 1) % values.length;
        return values[nextIndex];
    }
}
