package my.myusri.kompas;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

/**
 * Simple compass implementation that makes use of magnetic sensor only.
 * Magnetic inclination not compensated.
 */

class SimpleCompassStrategy extends CompassStrategy {

  private float lastVal[];

  SimpleCompassStrategy(SensorManager manager, Listener listener) {
    super(
      manager,
      manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
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
    if (listener == null || ev.sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD)
      return;

    if (Float.isNaN(lastVal[0]))
      System.arraycopy(ev.values, 0, lastVal, 0, 3);

    final float mx = (float) filter(ev.values[0], lastVal[0]);
    final float my = (float) filter(ev.values[1], lastVal[1]);
    final float mz = (float) filter(ev.values[2], lastVal[2]);
    float deg = (float)Math.toDegrees(Math.atan2(mx, my));
    lastVal[0] = mx; lastVal[1] = my; lastVal[2] = mz;
    listener.newData(deg, mx, my, mz);
  }
}
