package me.drton.jmavlib.conversion;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 * User: ton Date: 02.06.13 Time: 20:20
 */
public class RotationConversion {
    public static double[] rotationMatrixByEulerAngles(double roll, double pitch, double yaw) {
        return new double[]{
                cos(pitch) * cos(yaw),
                sin(roll) * sin(pitch) * cos(yaw) - cos(roll) * sin(yaw),
                cos(roll) * sin(pitch) * cos(yaw) + sin(roll) * sin(yaw),
                cos(pitch) * sin(yaw),
                sin(roll) * sin(pitch) * sin(yaw) + cos(roll) * cos(yaw),
                cos(roll) * sin(pitch) * sin(yaw) - sin(roll) * cos(yaw),
                -sin(pitch),
                sin(roll) * cos(pitch),
                cos(roll) * cos(pitch)
        };
    }

    public static double[] eulerAnglesByQuaternion(double[] q) {
        return new double[]{
                Math.atan2(2.0 * (q[0] * q[1] + q[2] * q[3]), 1.0 - 2.0 * (q[1] * q[1] + q[2] * q[2])),
                Math.asin(2 * (q[0] * q[2] - q[3] * q[1])),
                Math.atan2(2.0 * (q[0] * q[3] + q[1] * q[2]), 1.0 - 2.0 * (q[2] * q[2] + q[3] * q[3])),
        };
    }
}
