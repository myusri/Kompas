# Kompas

A very simple Android compass app. It will try to use rotation vector sensor
(TYPE_ROTATION_VECTOR) if the device has it. Otherwise, the app will fall
back to using magnetic field sensor (TYPE_MAGNETIC_FIELD). The former
compensates for magnetic inclination while the latter does not.

The XYZ magnetic/orientation vector is projected to the XY plane
of the device and is used to display the compass direction. There is the
possibility of the magnetic vector to be virtually perpendicular
to the XY plane, which may result jumpy compass needle. To avoid this,
the user will be notified to make the device less "vertical" when the
projected XY magnitude is too low compared to the XYZ magnitude.

To reduce compass jitter, first order IIR filter is used. The slider at the
top of the screen is used to change the filter &alpha; coefficient between
0.0 and 1.0. The closer the coefficient to 0.0, the less filtered the
compass direction would be. Conversely, the closer the coefficient to 1.0,
the more filtered the compass direction would be.

If rotation vector sensor is used, additional filtering is not necessary
because the sensor is supposed to be very fast and stable. In this case,
you may set the coefficient to zero.