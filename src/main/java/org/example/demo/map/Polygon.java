package org.example.demo.map;

import java.util.List;

public class Polygon
{
    private final List<Point> points;

    Polygon(List<Point> points) {
        this.points = points;
    }

    public List<Point> getPoints() {
        return points;
    }

    public boolean contains(Point point) {
        int n = points.size();
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Point pi = points.get(i);
            Point pj = points.get(j);
            if ((pi.getLat() > point.getLat()) != (pj.getLat() > point.getLat()) &&
                (point.getLon() < (pj.getLon() - pi.getLon()) * (point.getLat() - pi.getLat()) / (pj.getLat() - pi.getLat()) + pi.getLon())) {
                inside = !inside;
            }
        }
        return inside;
    }
}
