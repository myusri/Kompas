package my.myusri.kompas;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Base class for compass sensor strategy
 */

abstract class CompassStrategy implements SensorEventListener {

  /** Listener to compass strategy. */
  interface Listener {
    /**
     * New compass direction (projected)  and vector (x,y,z) relative to
     * device orientation.
     */
    void newData(float deg, float x, float y, float z);
  }

  // let save some battery, shall we
  private static final int SAMPLING_PERIOD =  500000;

  private SensorManager manager;
  private double alpha;
  Sensor sensor;
  Listener listener;
  boolean valid;

  CompassStrategy(SensorManager manager, Sensor sensor, Listener listener) {
    this.manager = manager;
    this.sensor = sensor;
    this.listener = listener;
  }

  void register() {
    manager.registerListener(this, sensor, SAMPLING_PERIOD);
  }

  void unregister() {
    manager.unregisterListener(this);
  }

  /**
   * Subclass should override to reset computed/stored values to some default.
   * It is also called in this base class constructor. */
  void reset() {}

  /**
   * whether the strategy object is valid. Constructor must set valid to
   * true if the strategy can work.
   */
  boolean isValid() {
    return valid;
  }

  /** Set the coefficient for the 1st-order IIR filter */
  void setAlpha(double alpha) {
    this.alpha = alpha;
  }

  /**
   *
   * first order IIR filter:
   * <br>y0 = (1-alpha) * x0 + alpha * y1
   * <br>where,<ul>
   * <li>y0 - filtered output
   * <li>x0 - raw current value
   * <li>y1 - last filtered value
   * </ul>
   */

  double filter(double x0, double y1) {
    return (1- alpha)*x0 + alpha *y1;
  }

  /** Default to do nothing */
  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
  }
}
