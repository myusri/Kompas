package my.myusri.kompas;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.opengl.Matrix;

/**
 * Compass strategy based on rotation vector sensor (TYPE_ROTATION_VECTOR).
 * This sensor compensates for magnetic inclination. Since
 * TYPE_ROTATION_VECTOR is an Android composite/fused sensor, ideally we
 * do not need additional filtering. It is done anyway for consistency with
 * other compass strategy implementations.
 *
 * (Wanted to use TYPE_GEOMAGNETIC_ROTATION_VECTOR but it doesn't seem to
 * work.)
 */

class RotationCompassStrategy extends CompassStrategy {

  private final static float[] Y_AXIS = { 0, 1, 0, 0 };
  private float[] R = new float[16];
  private float lastVal[];

  RotationCompassStrategy(SensorManager manager, Listener listener) {
    super(
      manager,
      manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
      listener);
    valid = sensor != null;
    lastVal = new float[3];
    reset();
  }

  @Override
  void reset() {
    lastVal[0] = lastVal[1] = lastVal[2] = Float.NaN;
  }

  @Override
  public void onSensorChanged(SensorEvent ev) {
    if (listener == null || ev.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR)
      return;

    if (Float.isNaN(lastVal[0]))
      System.arraycopy(ev.values, 0, lastVal, 0, 3);

    // The rotation matrix R transform a vector from the device coordinate
    // system to the world coordinate system where Y is flat against the
    // ground and pointing to magnetic North, Z pointing up and X is
    // vector product Y.Z, roughly pointing East.

    // Device coordinate system is defined relative to its default
    // orientation. X is the horizontal axis, Y is the vertical axis and
    // Z is pointing away from the screen.

    // To get the compass vector pointing to magnetic north, the world Y
    // axis, we need to rotate the device Y axis to it.

    float[] N = new float[4];
    SensorManager.getRotationMatrixFromVector(R, ev.values);
    Matrix.multiplyMV(N, 0, R, 0, Y_AXIS, 0);

    final float mx = (float) filter(N[0], lastVal[0]);
    final float my = (float) filter(N[1], lastVal[1]);
    final float mz = (float) filter(N[2], lastVal[2]);
    float deg = (float)Math.toDegrees(Math.atan2(mx, my));
    lastVal[0] = mx; lastVal[1] = my; lastVal[2] = mz;
    listener.newData(deg, mx, my, mz);
  }
}
