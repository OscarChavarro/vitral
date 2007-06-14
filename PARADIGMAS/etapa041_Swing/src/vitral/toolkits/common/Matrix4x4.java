//===========================================================================

package vitral.toolkits.common;

import net.java.games.jogl.GL;

public class Matrix4x4 {
    public double M[][];

    public Matrix4x4()
    {
        M = new double[4][4];
        identity();
    }

    public Matrix4x4(Matrix4x4 B)
    {
        M = new double[4][4];
        int i,j;

        for ( i = 0; i < 4; i++ ) {
            for ( j = 0; j < 4; j++ ) {
                M[i][j] = B.M[i][j];
            }
        }
    }

    public void identity()
    {
        M[0][0]=1.0;M[0][1]=0.0;M[0][2]=0.0;M[0][3]=0.0;
        M[1][0]=0.0;M[1][1]=1.0;M[1][2]=0.0;M[1][3]=0.0;
        M[2][0]=0.0;M[2][1]=0.0;M[2][2]=1.0;M[2][3]=0.0;
        M[3][0]=0.0;M[3][1]=0.0;M[3][2]=0.0;M[3][3]=1.0;
    }

    public void orthogonalProjection(
        double leftPlaneDistance, double rightPlaneDistance,
        double downPlaneDistance, double upPlaneDistance,
        double nearPlaneDistance, double farPlaneDistance)
    {
        double tx, ty, tz;;

        tx = - ( (rightPlaneDistance + leftPlaneDistance) / 
                 (rightPlaneDistance - leftPlaneDistance) );
        ty = - ( (upPlaneDistance + downPlaneDistance) / 
                 (upPlaneDistance - downPlaneDistance) );
        tz = - ( (farPlaneDistance + nearPlaneDistance) / 
                 (farPlaneDistance - nearPlaneDistance) );

        M[0][0] = 2 / (rightPlaneDistance - leftPlaneDistance);
        M[0][1] = 0;
        M[0][2] = 0;
        M[0][3] = tx;

        M[1][0] = 0;
        M[1][1] = 2 / (upPlaneDistance - downPlaneDistance);
        M[1][2] = 0;
        M[1][3] = ty;

        M[2][0] = 0;
        M[2][1] = 0;
        M[2][2] = -2 / (farPlaneDistance - nearPlaneDistance);
        M[2][3] = tz;

        M[3][0] = 0;
        M[3][1] = 0;
        M[3][2] = 0;
        M[3][3] = 1;
    }

    public void frustumProjection(
                 double leftDistance, double rightDistance,
                 double downDistance, double upDistance,
                 double nearPlaneDistance, double farPlaneDistance)
    {
        double A, B, C, D;

        A = (rightDistance + leftDistance) / (rightDistance - leftDistance);
        B = (upDistance + downDistance) / (upDistance - downDistance); 
        C = - ((farPlaneDistance + nearPlaneDistance) / (farPlaneDistance - nearPlaneDistance));
        D = - ((2 * farPlaneDistance * nearPlaneDistance) / (farPlaneDistance - nearPlaneDistance));

        M[0][0] = 2 * nearPlaneDistance / (rightDistance - leftDistance);
        M[0][1] = 0;
        M[0][2] = A;
        M[0][3] = 0;

        M[1][0] = 0;
        M[1][1] = 2 * nearPlaneDistance / (upDistance - downDistance);
        M[1][2] = B;
        M[1][3] = 0;

        M[2][0] = 0;
        M[2][1] = 0;
        M[2][2] = C;
        M[2][3] = D;

        M[3][0] = 0;
        M[3][1] = 0;
        M[3][2] = -1;
        M[3][3] = 0;
    }


    public void
    translation(double transx, double transy, double transz)
    {
        M[0][0]=1.0;M[0][1]=0.0;M[0][2]=0.0;M[0][3]=transx;
        M[1][0]=0.0;M[1][1]=1.0;M[1][2]=0.0;M[1][3]=transy;
        M[2][0]=0.0;M[2][1]=0.0;M[2][2]=1.0;M[2][3]=transz;
        M[3][0]=0.0;M[3][1]=0.0;M[3][2]=0.0;M[3][3]=1.0;
    }

    public void
    translation(Vector3D T)
    {
        M[0][0]=1.0;M[0][1]=0.0;M[0][2]=0.0;M[0][3]=T.x;
        M[1][0]=0.0;M[1][1]=1.0;M[1][2]=0.0;M[1][3]=T.y;
        M[2][0]=0.0;M[2][1]=0.0;M[2][2]=1.0;M[2][3]=T.z;
        M[3][0]=0.0;M[3][1]=0.0;M[3][2]=0.0;M[3][3]=1.0;
    }

    public void
    eulerAnglesRotation(double yaw, double pitch, double roll)
    {
        Matrix4x4 R1, R2, R3;

        R1 = new Matrix4x4();
        R2 = new Matrix4x4();
        R3 = new Matrix4x4();

        R3.axisRotation(yaw, 0, 0, 1);
        R2.axisRotation(pitch, 0, -1, 0);
        R1.axisRotation(roll, 1, 0, 0);

        this.M = R3.multiply(R2.multiply(R1)).M;
    }

    public void
    axisRotation(double angle, double x, double y, double z)
    {
        double mag, s, c;
        double xx, yy, zz, xy, yz, zx, xs, ys, zs, one_c;

        s = Math.sin( angle );
        c = Math.cos( angle );

        mag = Math.sqrt(x*x + y*y + z*z);

        // OJO: Propenso a error... deberia ser si es menor a EPSILON
        if ( mag == 0.0 ) {
            identity();
            return;
        }

        x /= mag;
        y /= mag;
        z /= mag;

        //-----------------------------------------------------------------
        xx = x * x;
        yy = y * y;
        zz = z * z;
        xy = x * y;
        yz = y * z;
        zx = z * x;
        xs = x * s;
        ys = y * s;
        zs = z * s;
        one_c = 1 - c;

        M[0][0] = (one_c * xx) + c;
        M[0][1] = (one_c * xy) - zs;
        M[0][2] = (one_c * zx) + ys;
        M[0][3] = 0;

        M[1][0] = (one_c * xy) + zs;
        M[1][1] = (one_c * yy) + c;
        M[1][2] = (one_c * yz) - xs;
        M[1][3] = 0;

        M[2][0] = (one_c * zx) - ys;
        M[2][1] = (one_c * yz) + xs;
        M[2][2] = (one_c * zz) + c;
        M[2][3] = 0;

        M[3][0] = 0;
        M[3][1] = 0;
        M[3][2] = 0;
        M[3][3] = 1;
    }

    public void invert()
    {
        double R[][] = new double[4][4];

        R[0][0] = M[1][1]*M[2][2] - M[2][1]*M[1][2];
        R[0][1] = M[0][2]*M[2][1] - M[2][2]*M[0][1];
        R[0][2] = M[0][1]*M[1][2] - M[1][1]*M[0][2];
        R[0][3] = 0;

        R[1][0] = M[1][2]*M[2][0] - M[2][2]*M[1][0];
        R[1][1] = M[0][0]*M[2][2] - M[2][0]*M[0][2];
        R[1][2] = M[0][2]*M[1][0] - M[1][2]*M[0][0];
        R[1][3] = 0;

        R[2][0] = M[1][0]*M[2][1] - M[2][0]*M[1][1];
        R[2][1] = M[0][1]*M[2][0] - M[2][1]*M[0][0];
        R[2][2] = M[0][0]*M[1][1] - M[1][0]*M[0][1];
        R[2][3] = 0;

        R[3][0] = 0;
        R[3][1] = 0;
        R[3][2] = 0;
        R[3][3] = 1;

        M = R;

        double a = 1/determinant();
        int row, column;

        for ( row = 0; row < 4; row++ ) {
            for ( column = 0; column < 4; column++ ) {
                M[row][column] = a*M[row][column];
            }
        }
    }

    public final Matrix4x4 multiply(double a)
    {
        Matrix4x4 R = new Matrix4x4();
        int row, column;

        for ( row = 0; row < 4; row++ ) {
            for ( column = 0; column < 4; column++ ) {
                R.M[row][column] = a*M[row][column];
            }
        }
        return R;
    }

    public final Vector3D multiply(Vector3D E)
    {
        Vector3D R = new Vector3D();

        R.x = M[0][0] * E.x + M[0][1] * E.y + M[0][2] * E.z + M[0][3];
        R.y = M[1][0] * E.x + M[1][1] * E.y + M[1][2] * E.z + M[1][3];
        R.z = M[2][0] * E.x + M[2][1] * E.y + M[2][2] * E.z + M[2][3];

        return R;
    }

    public Matrix4x4 multiply(Matrix4x4 segunda)
    {
        Matrix4x4 R = new Matrix4x4();
        int row_a, column_b, row_b;
        double acumulado;

        for( row_a = 0; row_a < 4; row_a++ ) {
            for( column_b = 0; column_b < 4; column_b++ ) {
                acumulado = 0;
                for( row_b = 0; row_b < 4; row_b++ ) {
                    acumulado += M[row_a][row_b]*segunda.M[row_b][column_b];
                }
                R.M[row_a][column_b] = acumulado;
            }
        }
        return R;
    }

    public double determinant()
    {
        return M[0][0]*(M[1][1]*M[2][2]-M[2][1]*M[1][2]) - 
               M[0][1]*(M[1][0]*M[2][2]-M[2][0]*M[1][2]) + 
               M[0][2]*(M[1][0]*M[2][1]-M[2][0]*M[1][1]);
    }

    public void activateGL(GL gl)
    {
        double Mgl[] = new double[16];
        int row, column, pos;

        for ( pos = 0, column = 0; column < 4; column++ ) {
            for ( row = 0; row < 4; row++, pos++ ) {
                Mgl[pos] = M[row][column];
            }
        }

        gl.glMultMatrixd(Mgl);
    }

    public String toString()
    {
        String msg;

        msg = "\n------------------------------\n";
        int row, column, pos;

        for ( row = 0; row < 4; row++, pos++ ) {
            for ( pos = 0, column = 0; column < 4; column++ ) {
                msg = msg + M[row][column] + " ";
        }
            msg = msg + "\n";
    }
        msg = msg + "------------------------------\n";
        return msg;
    }

    public Quaternion exportToQuaternion()
    {
        Quaternion quat = new Quaternion();
        double tr, s;
        double q[] = new double[4];
        int i, j, k;
        int nxt[] = new int[3];

        nxt[0] = 1;
        nxt[1] = 2;
        nxt[2] = 0;

        tr = M[0][0] + M[1][1] + M[2][2];

        // check the diagonal
        if ( tr > 0.0 ) {
            s = Math.sqrt(tr + 1.0);
            quat.magnitude = s / 2.0;
            s = 0.5 / s;
            quat.direction.x = (M[2][1] - M[1][2]) * s;
            quat.direction.y = (M[0][2] - M[2][0]) * s;
            quat.direction.z = (M[1][0] - M[0][1]) * s;
          }
          else {                
            // diagonal is negative
            i = 0;
            if (M[1][1] > M[0][0]) i = 1;
            if (M[2][2] > M[i][i]) i = 2;
            j = nxt[i];
            k = nxt[j];

            s = Math.sqrt ((M[i][i] - (M[j][j] + M[k][k])) + 1.0);

            q[i] = s * 0.5;

            if (s != 0.0) s = 0.5 / s;

            q[3] = (M[k][j] - M[j][k]) * s;
            q[j] = (M[j][i] + M[i][j]) * s;
            q[k] = (M[k][i] + M[i][k]) * s;

            quat.direction.x = q[0];
            quat.direction.y = q[1];
            quat.direction.z = q[2];
            quat.magnitude = q[3];
        }

        return quat;
    }

    public void importFromQuaternion(Quaternion a)
    {
        double sx, sy, sz, xx, yy, yz, xy, xz, zz, x2, y2, z2;

        x2 = a.direction.x + a.direction.x;
        y2 = a.direction.y + a.direction.y; 
        z2 = a.direction.z + a.direction.z;
        xx = a.direction.x * x2;
        xy = a.direction.x * y2;
        xz = a.direction.x * z2;
        yy = a.direction.y * y2;
        yz = a.direction.y * z2;
        zz = a.direction.z * z2;
        sx = a.magnitude * x2;
        sy = a.magnitude * y2;   sz = a.magnitude * z2;

        M[0][0] = 1-(yy+zz);
        M[0][1] = xy-sz;
        M[0][2] = xz+sy;
        M[0][3] = 0;

        M[1][0] = xy+sz;
        M[1][1] = 1-(xx+zz);
        M[1][2] = yz-sx;
        M[1][3] = 0;

        M[2][0] = xz-sy;
        M[2][1] = yz+sx;
        M[2][2] = 1-(xx+yy);
        M[2][3] = 0;

        M[3][0] = 0;
        M[3][1] = 0;
        M[3][2] = 0;
        M[3][3] = 1;
    }

    public double obtainEulerYawAngle()
    {
        Vector3D dir = new Vector3D(1, 0, 0);
        double yaw, pitch;
    double EPSILON = 0.0004;

        pitch = obtainEulerPitchAngle();

    dir = multiply(dir);
    dir.z = 0;

        if ( Math.abs(Math.toRadians(90) - pitch) < EPSILON ) {
            dir.x = 0;  dir.y = 0;  dir.z = -1;
            dir = multiply(dir);
        }
        if ( Math.abs(Math.toRadians(-90) - pitch) < EPSILON ) {
            dir.x = 0;  dir.y = 0;  dir.z = 1;
            dir = multiply(dir);
        }
        dir.normalize();
        if ( dir.y <= 0 ) yaw = Math.asin(dir.x) - Math.toRadians(90);
        else yaw = Math.toRadians(90) - Math.asin(dir.x);

        return yaw;
    }
    public double obtainEulerPitchAngle()
    {
        Vector3D dir = new Vector3D(1, 0, 0);

    dir = multiply(dir);
    dir.normalize();
        return Math.toRadians(90) - Math.acos(dir.z);
    }
    public double obtainEulerRollAngle()
    {
        Matrix4x4 R1, R2, R3;
        double yaw, pitch, roll;

        pitch = obtainEulerPitchAngle();
        yaw = obtainEulerYawAngle();

        R3 = new Matrix4x4();
        R2 = new Matrix4x4();

        R3.axisRotation(yaw, 0, 0, 1);
        R2.axisRotation(pitch, 0, -1, 0);
        R3.invert();
    R2.invert();

        R1 = R2.multiply(R3.multiply(this));

        Quaternion q = R1.exportToQuaternion();
        q.normalize();
        R1.importFromQuaternion(q);

        if ( R1.M[2][1] >= 0 ) {  // R1.M[2][1] ::= sin(r)
            // Nuestro angulo esta entre 0 y 180 grados
            roll = Math.acos(R1.M[1][1]);
          }
          else {
            // Nuestro angulo esta entre 180 y 360 grados
            roll = -Math.acos(R1.M[1][1]);
        }

        return roll;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
