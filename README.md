# Kompas

A very simple Android compass app. It only explicitly makes use of Android
magnetic field sensor. The sensor data are rotated into they XY plane of the
device and the vector in the XY plane is used to display the compass
direction. Note that magnetic dip or inclination is not taken into account
at the moment.

To reduce compass jitter, first order IIR filter is used. The slider at the
top of the screen is used to change the filter &alpha; coefficient between
0.0 and 1.0. The closer the coefficient to 0.0, the less filtered the
compass direction would be. Conversely, the closer the coefficient to 1.0,
the more filtered the compass direction would be.
