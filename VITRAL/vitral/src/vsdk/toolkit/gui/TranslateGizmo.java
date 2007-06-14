//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 16 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.gui;

import java.util.ArrayList;
import java.util.Iterator;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;

import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.RayableObject;

public class TranslateGizmo {
  /// Internal transfirmation state
  private Matrix4x4 T;

  private Camera camera;

  /// Geometric model based in primitive instancing: primitive concretions
  private Arrow arrowModel;
  private Cone cylinderModel;

  /// Geometric model based in primitive instancing: primitive instances
  /// This list is always of size 9, and its elements follow the order 
  /// indicated in the values of the *_ELEMENT constants of this class.
  ArrayList<RayableObject> elementInstances;

  /// Internal element selection state
  public static final int X_AXIS_ELEMENT = 1;
  public static final int Y_AXIS_ELEMENT = 2;
  public static final int Z_AXIS_ELEMENT = 3;
  public static final int XYY_SEGMENT_ELEMENT = 4;
  public static final int XYX_SEGMENT_ELEMENT = 5;
  public static final int YZZ_SEGMENT_ELEMENT = 6;
  public static final int YZY_SEGMENT_ELEMENT = 7;
  public static final int XZZ_SEGMENT_ELEMENT = 8;
  public static final int XZX_SEGMENT_ELEMENT = 9;

  public static final int NULL_GROUP = 0;
  public static final int X_AXIS_GROUP = 1;
  public static final int Y_AXIS_GROUP = 2;
  public static final int Z_AXIS_GROUP = 3;
  public static final int XY_PLANE_GROUP = 4;
  public static final int YZ_PLANE_GROUP = 5;
  public static final int XZ_PLANE_GROUP = 6;

  public static final int MODEL_FOR_GRAVITY = 1;
  public static final int MODEL_FOR_DISPLAY = 1;

  /// Interaction state
  private int persistentSelection;
  private int volatileSelection;

  public TranslateGizmo(Camera cam)
  {
      persistentSelection = X_AXIS_GROUP;
      volatileSelection = NULL_GROUP;
      arrowModel = new Arrow(0.5, 0.3, 0.025, 0.05);
      cylinderModel = new Cone(0.05, 0.05, 0.5);

      elementInstances = new ArrayList<RayableObject>();
      for ( int i = 0; i < 9; i++ ) {
      RayableObject r = new RayableObject();
          elementInstances.add(r);
      }

      setCamera(cam);
  }

  public void setCamera(Camera cam)
  {
      camera = cam;
  }

  public Camera getCamera()
  {
      return camera;
  }

  public ArrayList<RayableObject> getElements()
  {
      return elementInstances;
  }

  private Material createMaterial(double r, double g, double b)
  {
       Material m = new Material();

       m.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
       m.setDiffuse(new ColorRgb(r, g, b));
       m.setSpecular(new ColorRgb(1, 1, 1));
       return m;
  }

  /**
  This method updates the data structure contained in the `elementInstances`
  array starting from the given paramenters.
    - translation is te position of the center of the gizmo
    - rotation is the rotation matrix containing the orientation of the gizmo
    - if autosize is false, initialdu, initialdv and camera parameters are not
      used, and the gizmo doesn't change its current size. If autosize is true,
      the gizmo size is changed such as from the current camera, the gizmo
      projection fit a 2D area of initialdu * initialdv pixels.
    - modelType must be one of the following values: MODEL_FOR_GRAVITY or
      MODEL_FOR_DISPLAY. Depending on this value the size of current
      geometric elements could change.
  */
  public void calculateGeometryState(Vector3D translation, Matrix4x4 rotation,
    boolean autosize, int initialdu, int initialdv, Camera camera, 
    int modelType)
  {
      int index;
      Iterator i;
      Material red = createMaterial(1, 0, 0);
      Material green = createMaterial(0, 1, 0);
      Material blue = createMaterial(0, 0, 1);
      Material yellow = createMaterial(1, 1, 0);

      Matrix4x4 R = new Matrix4x4(T);
      R.M[3][0] = 0.0;
      R.M[3][1] = 0.0;
      R.M[3][2] = 0.0;
      R.M[0][3] = 0.0;
      R.M[1][3] = 0.0;
      R.M[2][3] = 0.0;
      R.M[3][3] = 1.0;
      Matrix4x4 subR = new Matrix4x4();
      Matrix4x4 eleR;
      Vector3D subP;
      Vector3D eleP;

      index = 0;
      for ( i = elementInstances.iterator(); 
            index < 9 && i.hasNext(); index++ ) {
          RayableObject r = (RayableObject)i.next();
          r.setGeometry(null);
          switch ( index ) {
         case X_AXIS_ELEMENT:
              // Basic model
              r.setGeometry(arrowModel);
              r.setMaterial(red);
              // Rotation
              subR.axisRotation(Math.toRadians(90.0), 0, -1, 0);
              eleR = R.multiply(subR);
              r.setRotation(eleR);
              eleR.invert();
              r.setRotationInverse(eleR);
              // Translation
              subP = new Vector3D(0, 0, 0.2);
              eleP = eleR.multiply(subP).add(getPosition());
              r.setPosition(eleP);
          break;
         case Y_AXIS_ELEMENT:
              // Basic model
              r.setGeometry(arrowModel);
              r.setMaterial(green);
              // Rotation
              subR.axisRotation(Math.toRadians(90.0), 1, 0, 0);
              eleR = R.multiply(subR);
              r.setRotation(eleR);
              eleR.invert();
              r.setRotationInverse(eleR);
              // Translation
              subP = new Vector3D(0, 0, 0.2);
              eleP = eleR.multiply(subP).add(getPosition());
              r.setPosition(eleP);
          break;
         case Z_AXIS_ELEMENT:
              // Basic model
              r.setGeometry(arrowModel);
              r.setMaterial(blue);
              // Rotation
              subR = new Matrix4x4();
              eleR = R.multiply(subR);
              r.setRotation(eleR);
              eleR.invert();
              r.setRotationInverse(eleR);
              // Translation
              subP = new Vector3D(0, 0, 0.2);
              eleP = eleR.multiply(subP).add(getPosition());
              r.setPosition(eleP);
          break;
         case XYY_SEGMENT_ELEMENT:
          break;
         case XYX_SEGMENT_ELEMENT:
          break;
         case YZZ_SEGMENT_ELEMENT:
          break;
         case YZY_SEGMENT_ELEMENT:
          break;
         case XZZ_SEGMENT_ELEMENT:
          break;
         case XZX_SEGMENT_ELEMENT:
          break;
      }
      }
  }

  public Vector3D getPosition()
  {
      Vector3D p = new Vector3D(T.M[0][3], T.M[1][3], T.M[2][3]);
      return p;
  }

  public void setTransformationMatrix(Matrix4x4 T)
  {
      this.T = T;

      Matrix4x4 R = new Matrix4x4(T);
      R.M[3][0] = 0.0;
      R.M[3][1] = 0.0;
      R.M[3][2] = 0.0;
      R.M[0][3] = 0.0;
      R.M[1][3] = 0.0;
      R.M[2][3] = 0.0;
      R.M[3][3] = 1.0;

      calculateGeometryState(getPosition(), 
                             R, false, 100, 100, camera, MODEL_FOR_DISPLAY);
  }

  public Matrix4x4 getTransformationMatrix()
  {
      return T;
  }

  public boolean processMouseEventAwt(MouseEvent mouseEvent)
  {
      return false;
  }

  public boolean processKeyPressedEventAwt(KeyEvent keyEvent)
  {
      char unicode_id;
      int keycode;
      double deltaMov = 0.1;
      boolean updateNeeded = false;

      unicode_id = keyEvent.getKeyChar();
      keycode = keyEvent.getKeyCode();

      Matrix4x4 R = new Matrix4x4(T);
      R.M[3][0] = 0.0;
      R.M[3][1] = 0.0;
      R.M[3][2] = 0.0;
      R.M[0][3] = 0.0;
      R.M[1][3] = 0.0;
      R.M[2][3] = 0.0;
      R.M[3][3] = 1.0;

      calculateGeometryState(new Vector3D(T.M[0][3], T.M[1][3], T.M[2][3]), 
                             R, false, 100, 100, camera, MODEL_FOR_DISPLAY);

      if ( unicode_id != keyEvent.CHAR_UNDEFINED ) {
            switch ( unicode_id ) {
              // Position
              case 'x':
                T.M[0][3] -= deltaMov;
                updateNeeded = true;
                break;
              case 'X':
                T.M[0][3] += deltaMov;
                updateNeeded = true;
                break;
              case 'y':
                T.M[1][3] -= deltaMov;
                updateNeeded = true;
                break;
              case 'Y':
                T.M[1][3] += deltaMov;
                updateNeeded = true;
                break;
              case 'z':
                T.M[2][3] -= deltaMov;
                updateNeeded = true;
                break;
              case 'Z':
                T.M[2][3] += deltaMov;
                updateNeeded = true;
                break; 
        }
      }

      return updateNeeded;
  }

  public boolean processKeyReleasedEventAwt(KeyEvent mouseEvent)
  {
      return false;
  }

  public boolean processMousePressedEventAwt(MouseEvent e)
  {
      return false;
  }

  public boolean processMouseReleasedEventAwt(MouseEvent e)
  {
      return false;
  }

  public boolean processMouseClickedEventAwt(MouseEvent e)
  {
      return false;
  }

  public boolean processMouseMovedEventAwt(MouseEvent e)
  {
      return false;
  }

  public boolean processMouseDraggedEventAwt(MouseEvent e)
  {
      return false;
  }

  public boolean processMouseWheelEventAwt(MouseWheelEvent e)
  {
      return false;
  }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
