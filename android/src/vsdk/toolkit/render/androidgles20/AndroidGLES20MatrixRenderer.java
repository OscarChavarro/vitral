package vsdk.toolkit.render.androidgles20;

// VSDK classes
import android.opengl.Matrix;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import static vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer.GL_MODELVIEW;
import static vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer.GL_PROJECTION;
import static vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer.copyMatrix;
import static vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer.currentMatrixMode;
import static vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer.modelViewMatrix;
import static vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer.originalMatrix;
import static vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer.projectionMatrix;
import static vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer.transientMatrix;

public class AndroidGLES20MatrixRenderer extends AndroidGLES20Renderer {
    
    /**
    Multiply current OpenGL matrix with matrix R.
    @param R
    */
    public static void activate(Matrix4x4 R)
    {
        int i, j, index;
        
        transientMatrix = new float[16];
        index = 0;
        for ( j = 0; j < 4; j++ ) {
            for ( i = 0; i < 4; i++ ) {
                transientMatrix[index] = (float)R.M[i][j];
                index++;
            }
        }
        
        switch ( currentMatrixMode ) {
          case GL_MODELVIEW:
            copyMatrix(originalMatrix, modelViewMatrix);
            Matrix.multiplyMM(modelViewMatrix, 0,
                              originalMatrix, 0, transientMatrix, 0);
            break;
          case GL_PROJECTION:
            copyMatrix(originalMatrix, projectionMatrix);
            Matrix.multiplyMM(projectionMatrix, 0,
                              originalMatrix, 0, transientMatrix, 0);
            break;
        }
        activateTransformationMatrices();
    }
}
