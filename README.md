# Kompas

A very simple Android compass app. It only explicitly makes use of Android
magnetic field sensor. The XYZ magnetic vector is projected to the XY plane
of the device and is used to display the compass direction. There is the
possibility of the magnetic vector to be virtually perpendicular
to the XY plane, which may result jumpy compass needle. To avoid this,
the user will be notified to make the device less "vertical" when the
projected XY magnitude is too low compared to the XYZ magnitude.

Note that magnetic dip or inclination is not taken into account at the moment
(so, "vertical" might not be totally vertical.)

To reduce compass jitter, first order IIR filter is used. The slider at the
top of the screen is used to change the filter &alpha; coefficient between
0.0 and 1.0. The closer the coefficient to 0.0, the less filtered the
compass direction would be. Conversely, the closer the coefficient to 1.0,
the more filtered the compass direction would be.
