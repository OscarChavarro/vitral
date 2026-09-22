package render.jogl;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.render.jogl.Jogl4FrameBufferReader;

import render.FrameBufferSource;

/**
Frame buffers of an OpenGL 4 context.
*/
public class Jogl4FrameBufferSource implements FrameBufferSource
{
    private final GL4 gl;

    /**
    @param gl context whose current frame buffers are read
    */
    public Jogl4FrameBufferSource(GL4 gl)
    {
        this.gl = gl;
    }

    @Override
    public RGBImageUncompressed readColor()
    {
        return Jogl4FrameBufferReader.readColor(gl);
    }

    @Override
    public ZBuffer readDepth()
    {
        return Jogl4FrameBufferReader.readDepth(gl);
    }
}
