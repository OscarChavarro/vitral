package vsdk.toolkit.render.androidgles20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Box;

public class AndroidGLES20BoxRenderer extends AndroidGLES20Renderer {
	
	/**
	 * The method to draw the box and its surfaces
	 * x, y, z represents the coordinates of the box center 
	 */
	private static void drawBox(Box nBox, RendererConfiguration nRendererConfiguration)
	{
		//One Quad_Strip for the body
		VSDK.acumulatePrimitiveCount(VSDK.QUAD_STRIP, 1);
		//8 Quads covering the body of the box
		VSDK.acumulatePrimitiveCount(VSDK.QUAD, 8);
		
		//index on the vertex array
		int index;
		//This array will contain the information of every vertex to draw them later
		float vertexDataArray[];
		//THe size of a vertex with all its information
		int vertexFloatElements = 8;
		
		
		//Size of the vertex in byte
		int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES*vertexFloatElements;
		//Defining the size of the vertex array
		
		//------------------------------------------------------------------------
		//  BOX MAIN BODY
		//------------------------------------------------------------------------
		
		//The main body is composed by four faces surrounding the box.
		
		//reserving the space on the vertexArray: 10 vertex to create the envelopping surface
		vertexDataArray = new float[10*vertexFloatElements];
		index = 0;
		Vector3D color = new Vector3D(1, 1, 1);
		//WARNING: UV Mapping coordinates are hard coded!!
		
		//Face 1
		drawVertex( 0.5,  0.5,  0.5, (double)1/3, (double)0, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex( 0.5,  - 0.5,  0.5, (double)2/3, (double)0, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  0.5,  0.5, (double)1/3, (double)1/4, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  - 0.5,  0.5, (double)2/3, (double)1/4, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		//Face 2
		drawVertex(- 0.5,  0.5, - 0.5, (double)1/3, (double)1/2, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  - 0.5, - 0.5, (double)2/3, (double)1/2, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		//Face 3
		drawVertex( 0.5,  0.5, - 0.5, (double)1/3, (double)3/4, color,vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex( 0.5,  - 0.5, - 0.5, (double)2/3, (double)3/4, color,vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		//Face 4
		drawVertex( 0.5,  0.5,  0.5, (double)1/3, (double)1, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex( 0.5,  - 0.5,  0.5, (double)2/3, (double)1, color, vertexDataArray, index, nRendererConfiguration);

		
		
		//Sending the data to the renderer
		
		FloatBuffer verticesBufferedArray;
		
		verticesBufferedArray = ByteBuffer.allocateDirect(vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
			ByteOrder.nativeOrder()).asFloatBuffer();
		
		verticesBufferedArray.put(vertexDataArray);
		
		drawVertices3Position3Normal2Uv(verticesBufferedArray, GLES20.GL_TRIANGLE_STRIP, 10, vertexSizeInBytes);
		
		//------------------------------------------------------------------------
		//  BOX CAPS
		//------------------------------------------------------------------------
		
		//Upper Cap
		
		vertexDataArray = new float[4*vertexFloatElements];
		index = 0;
		
		drawVertex( 0.5,  0.5,  0.5, (double)1/3, (double)1/4, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  0.5,  0.5, (double)1/3, (double)1/2, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex( 0.5,  0.5, - 0.5, (double)0, (double)1/4, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  0.5, - 0.5, (double)0, (double)1/2, color, vertexDataArray, index, nRendererConfiguration);
		
		
		verticesBufferedArray = ByteBuffer.allocateDirect(vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
				ByteOrder.nativeOrder()).asFloatBuffer();
			
		verticesBufferedArray.put(vertexDataArray);
			
		drawVertices3Position3Normal2Uv(verticesBufferedArray, GLES20.GL_TRIANGLE_STRIP, 4, vertexSizeInBytes);		
		
		//Lower Cap

		vertexDataArray = new float[4*vertexFloatElements];
		index = 0;
		
		drawVertex( 0.5,  - 0.5,  0.5, (double)2/3, (double)1/4, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  - 0.5,  0.5, (double)2/3, (double)1/2, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex( 0.5,  - 0.5, - 0.5, (double)1, (double)1/4, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  - 0.5, - 0.5, (double)1, (double)1/2, color, vertexDataArray, index, nRendererConfiguration);
		
		
		
		verticesBufferedArray = ByteBuffer.allocateDirect(vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
				ByteOrder.nativeOrder()).asFloatBuffer();
			
		verticesBufferedArray.put(vertexDataArray);
			
		drawVertices3Position3Normal2Uv(verticesBufferedArray, GLES20.GL_TRIANGLE_STRIP, 4, vertexSizeInBytes);
	}

	/**
	 * Draw a vertex given the primitives
	 */
	private static void drawVertex(double x, double y, double z, double u, double v, Vector3D color, float[] vertexDataArray, int index, RendererConfiguration nRendererConfiguration) {
		
		//Vertex coordinates
		vertexDataArray[index] = (float)x; index++;
		vertexDataArray[index] = (float)y; index++;
		vertexDataArray[index] = (float)z; index++;
		
		/**
		//VertexColor
		vertexDataArray[index] = (float)color.x; index++;
		vertexDataArray[index] = (float)color.y; index++;
		vertexDataArray[index] = (float)color.z; index++;
		**/
		//Vertex normals		
		Vector3D normal = new Vector3D(x, y, z);
		normal.normalize();
		
		vertexDataArray[index] = (float)normal.x; index++;
		vertexDataArray[index] = (float)normal.y; index++;
		vertexDataArray[index] = (float)normal.z; index++;		
		
		//texture coordinates
		
		vertexDataArray[index] = (float)u; index++;
		vertexDataArray[index] = (float)v; index++;
				
	}

	private static void drawWires(Box nBox,
			RendererConfiguration nRendererConfiguration) {
		
		Vector3D color = new Vector3D(1, 0, 0);
		//One Quad_Strip for the body
		VSDK.acumulatePrimitiveCount(VSDK.QUAD_STRIP, 1);
		//8 Quads covering the body of the box
		VSDK.acumulatePrimitiveCount(VSDK.QUAD, 8);
		

		//index on the vertex array
		int index;
		//This array will contain the information of every vertex to draw them later
		float vertexDataArray[];
		//THe size of a vertex with all its information
		int vertexFloatElements = 8;
		
		
		//Size of the vertex in byte
		int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES*vertexFloatElements;
		//Defining the size of the vertex array
		
		//------------------------------------------------------------------------
		//  BOX CAPS
		//------------------------------------------------------------------------
		
		//Upper Cap
		
		vertexDataArray = new float[4*vertexFloatElements];
		index = 0;
		
		drawVertex( 0.5,  0.5,  0.5, (double)1/4, (double)0, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  0.5,  0.5, (double)1/2, (double)0, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  0.5, - 0.5, (double)1/2, (double)2/3, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex( 0.5,  0.5, - 0.5, (double)1/4, (double)1/3, color, vertexDataArray, index, nRendererConfiguration);
		

		FloatBuffer verticesBufferedArray;

		
		verticesBufferedArray = ByteBuffer.allocateDirect(vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
				ByteOrder.nativeOrder()).asFloatBuffer();
			
		verticesBufferedArray.put(vertexDataArray);
			
		drawVertices3Position3Normal2Uv(verticesBufferedArray, GLES20.GL_LINE_LOOP, 4, vertexSizeInBytes);		
		
		//Lower Cap

		vertexDataArray = new float[4*vertexFloatElements];
		index = 0;
		
		drawVertex( 0.5,  - 0.5,  0.5, (double)1/4, (double)1, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  - 0.5,  0.5, (double)1/2, (double)1, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  - 0.5, - 0.5, (double)1/2, (double)2/3, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex( 0.5,  - 0.5, - 0.5, (double)1/4, (double)2/3, color, vertexDataArray, index, nRendererConfiguration);
		
		
		
		verticesBufferedArray = ByteBuffer.allocateDirect(vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
				ByteOrder.nativeOrder()).asFloatBuffer();
			
		verticesBufferedArray.put(vertexDataArray);
			
		drawVertices3Position3Normal2Uv(verticesBufferedArray, GLES20.GL_LINE_LOOP, 4, vertexSizeInBytes);
		
//line 1
		
		vertexDataArray = new float[2*vertexFloatElements];
		index = 0;
		
		drawVertex( 0.5,  0.5,  0.5, (double)1/4, (double)1, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex( 0.5,  - 0.5,  0.5, (double)1/2, (double)1, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
	
		
		
		verticesBufferedArray = ByteBuffer.allocateDirect(vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
				ByteOrder.nativeOrder()).asFloatBuffer();
			
		verticesBufferedArray.put(vertexDataArray);
			
		drawVertices3Position3Normal2Uv(verticesBufferedArray, GLES20.GL_LINE_STRIP, 2, vertexSizeInBytes);

	//line 2

		vertexDataArray = new float[2*vertexFloatElements];
		index = 0;
		
		drawVertex(- 0.5,  0.5,  0.5, (double)1/4, (double)1, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  - 0.5,  0.5, (double)1/2, (double)1, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
	
		
		
		verticesBufferedArray = ByteBuffer.allocateDirect(vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
				ByteOrder.nativeOrder()).asFloatBuffer();
			
		verticesBufferedArray.put(vertexDataArray);
			
		drawVertices3Position3Normal2Uv(verticesBufferedArray, GLES20.GL_LINE_STRIP, 2, vertexSizeInBytes);
		
//line 3
		
		vertexDataArray = new float[2*vertexFloatElements];
		index = 0;
		
		drawVertex(- 0.5,  0.5, - 0.5, (double)1/4, (double)1, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  - 0.5, - 0.5, (double)1/2, (double)1, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
	
		
		
		verticesBufferedArray = ByteBuffer.allocateDirect(vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
				ByteOrder.nativeOrder()).asFloatBuffer();
			
		verticesBufferedArray.put(vertexDataArray);
			
		drawVertices3Position3Normal2Uv(verticesBufferedArray, GLES20.GL_LINE_STRIP, 2, vertexSizeInBytes);
		
//line 4
		
		vertexDataArray = new float[2*vertexFloatElements];
		index = 0;
		
		drawVertex( 0.5,  0.5, - 0.5, (double)1/4, (double)1, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex( 0.5,  - 0.5, - 0.5, (double)1/2, (double)1, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
	
		
		
		verticesBufferedArray = ByteBuffer.allocateDirect(vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
				ByteOrder.nativeOrder()).asFloatBuffer();
			
		verticesBufferedArray.put(vertexDataArray);
			
		drawVertices3Position3Normal2Uv(verticesBufferedArray, GLES20.GL_LINE_STRIP, 2, vertexSizeInBytes);
				
	}

	private static void drawPoints(Box nBox,
			RendererConfiguration nRendererConfiguration) {
		//One Quad_Strip for the body
		VSDK.acumulatePrimitiveCount(VSDK.QUAD_STRIP, 1);
		//8 Quads covering the body of the box
		VSDK.acumulatePrimitiveCount(VSDK.QUAD, 8);
		
		//index on the vertex array
		int index;
		//This array will contain the information of every vertex to draw them later
		float vertexDataArray[];
		//THe size of a vertex with all its information
		int vertexFloatElements = 8;
		
		Vector3D color = new Vector3D(1, 1, 1);
		
		//Size of the vertex in byte
		int vertexSizeInBytes = FLOAT_SIZE_IN_BYTES*vertexFloatElements;
		//Defining the size of the vertex array
		
		//------------------------------------------------------------------------
		//  BOX MAIN BODY
		//------------------------------------------------------------------------
		
		//The main body is composed by four faces surrounding the box.
		
		//reserving the space on the vertexArray: 10 vertex to create the envelopping surface
		vertexDataArray = new float[10*vertexFloatElements];
		index = 0;
		
		//WARNING: UV Mapping coordinates are hard coded!!
		
		//Face 1
		drawVertex( 0.5,  0.5,  0.5, (double)0, (double)1/3, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex( 0.5,  - 0.5,  0.5, (double)0, (double)2/3, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  0.5,  0.5, (double)3/4, (double)2/3, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  - 0.5,  0.5, (double)3/4, (double)1/3, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		//Face 2
		drawVertex(- 0.5,  0.5, - 0.5, (double)1/2, (double)2/3, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  - 0.5, - 0.5, (double)1/2, (double)1/3, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		//Face 3
		drawVertex( 0.5,  0.5, - 0.5, (double)1/4, (double)2/3, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex( 0.5,  - 0.5, - 0.5, (double)1/4, (double)1/3, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		//Face 4
		drawVertex( 0.5,  0.5,  0.5, (double)0, (double)2/3, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex( 0.5,  - 0.5,  0.5, (double)0, (double)2/3, color, vertexDataArray, index, nRendererConfiguration);

		
		
		//Sending the data to the renderer
		
		FloatBuffer verticesBufferedArray;
		
		verticesBufferedArray = ByteBuffer.allocateDirect(vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
			ByteOrder.nativeOrder()).asFloatBuffer();
		
		verticesBufferedArray.put(vertexDataArray);
		
		drawVertices3Position3Normal2Uv(verticesBufferedArray, GLES20.GL_TRIANGLE_STRIP, 10, vertexSizeInBytes);
		
		//------------------------------------------------------------------------
		//  BOX CAPS
		//------------------------------------------------------------------------
		
		//Upper Cap
		
		vertexDataArray = new float[4*vertexFloatElements];
		index = 0;
		
		drawVertex( 0.5,  0.5,  0.5, (double)1/2, (double)0, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  0.5,  0.5, (double)1/4, (double)0, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex( 0.5,  0.5, - 0.5, (double)1/4, (double)2/3, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  0.5, - 0.5, (double)1/2, (double)1/3, color, vertexDataArray, index, nRendererConfiguration);
		
		
		verticesBufferedArray = ByteBuffer.allocateDirect(vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
				ByteOrder.nativeOrder()).asFloatBuffer();
			
		verticesBufferedArray.put(vertexDataArray);
			
		drawVertices3Position3Normal2Uv(verticesBufferedArray, GLES20.GL_TRIANGLE_STRIP, 4, vertexSizeInBytes);		
		
		//Lower Cap

		vertexDataArray = new float[4*vertexFloatElements];
		index = 0;
		
		drawVertex( 0.5,  - 0.5,  0.5, (double)1/2, (double)1, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  - 0.5,  0.5, (double)1/4, (double)1, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex( 0.5,  - 0.5, - 0.5, (double)1/4, (double)2/3, color, vertexDataArray, index, nRendererConfiguration);
		index+=vertexFloatElements;
		drawVertex(- 0.5,  - 0.5, - 0.5, (double)1/2, (double)2/3, color, vertexDataArray, index, nRendererConfiguration);
		
		
		
		verticesBufferedArray = ByteBuffer.allocateDirect(vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
				ByteOrder.nativeOrder()).asFloatBuffer();
			
		verticesBufferedArray.put(vertexDataArray);
			
		drawVertices3Position3Normal2Uv(verticesBufferedArray, GLES20.GL_POINTS, 4, vertexSizeInBytes);
		
	}
	


public static void draw(Box nBox, Camera nCamera, RendererConfiguration nRendererConfiguration)
{
		if(nRendererConfiguration.isPointsSet())
		{
			glDisable(GL_TEXTURE_2D);
			setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
			drawPoints(nBox, nRendererConfiguration);
		}
		if(nRendererConfiguration.isWiresSet())
		{
			glDisable(GL_TEXTURE_2D);
			setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
			drawWires(nBox, nRendererConfiguration);
		}
		if(nRendererConfiguration.isSurfacesSet())
		{
			if(nRendererConfiguration.isTextureSet())
				glEnable(GL_TEXTURE_2D);
			else
				glDisable(GL_TEXTURE_2D);
			
			switch(nRendererConfiguration.getShadingType()){
				case (RendererConfiguration.SHADING_TYPE_NOLIGHT):
					setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
					drawBox(nBox, nRendererConfiguration);
					break;
				case (RendererConfiguration.SHADING_TYPE_FLAT):
					setShadingType(RendererConfiguration.SHADING_TYPE_FLAT);
					drawBox(nBox, nRendererConfiguration);
					break;
				case (RendererConfiguration.SHADING_TYPE_GOURAUD):
					setShadingType(RendererConfiguration.SHADING_TYPE_GOURAUD);
					drawBox(nBox, nRendererConfiguration);
					break;
				default:
					break;
				
			}
		
		}
	}
}



	

