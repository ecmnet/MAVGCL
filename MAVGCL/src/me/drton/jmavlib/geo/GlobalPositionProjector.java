package me.drton.jmavlib.geo;

import static java.lang.Math.*;

/**
 * User: ton Date: 11.07.13 Time: 22:11
 */
public class GlobalPositionProjector {
    private boolean inited;
    private static double r_earth = 6371000.0;
    private double lat0;
    private double lon0;
    private double alt0;
    private double cos_lat0;
    private double sin_lat0;

    public void reset() {
        inited = false;
    }

    public boolean isInited() {
        return inited;
    }

    public void init(LatLonAlt ref) {
        inited = true;
        lat0 = ref.lat * PI / 180.0;
        lon0 = ref.lon * PI / 180.0;
        alt0 = ref.alt;
        cos_lat0 = cos(lat0);
        sin_lat0 = sin(lat0);
    }

    public double[] project(LatLonAlt p) {
        if (!inited) {
            throw new RuntimeException("Not initialized");
        }
        double lat_rad = p.lat * PI / 180.0;
        double lon_rad = p.lon * PI / 180.0;
        double sin_lat = sin(lat_rad);
        double cos_lat = cos(lat_rad);
        double cos_d_lon = cos(lon_rad - lon0);
        double c = acos(sin_lat0 * sin_lat + cos_lat0 * cos_lat * cos_d_lon);
        double k = (c == 0.0) ? 1.0 : (c / sin(c));
        double y = k * cos_lat * sin(lon_rad - lon0) * r_earth;
        double x = k * (cos_lat0 * sin_lat - sin_lat0 * cos_lat * cos_d_lon) * r_earth;
        double z = alt0 - p.alt;
        return new double[]{x, y, z};
    }

    public LatLonAlt reproject(double[] v) {
        if (!inited) {
            throw new RuntimeException("Not initialized");
        }
        double x_rad = v[0] / r_earth;
        double y_rad = v[1] / r_earth;
        double c = sqrt(x_rad * x_rad + y_rad * y_rad);
        double sin_c = sin(c);
        double cos_c = cos(c);
        double lat_rad;
        double lon_rad;
        if (c != 0.0) {
            lat_rad = asin(cos_c * sin_lat0 + (x_rad * sin_c * cos_lat0) / c);
            lon_rad = (lon0 + atan2(y_rad * sin_c, c * cos_lat0 * cos_c - x_rad * sin_lat0 * sin_c));
        } else {
            lat_rad = lat0;
            lon_rad = lon0;
        }
        return new LatLonAlt(lat_rad * 180.0 / Math.PI, lon_rad * 180.0 / Math.PI, alt0 - v[2]);
    }
}
