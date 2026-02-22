package org.example.demo.map;

import java.util.ArrayList;
import java.util.List;

public class CountryPolygon
{
    private Polygon polygon;
    private String name;

    public CountryPolygon(List<Point> polygon, String name) {
        this(new Polygon(polygon), name);
    }

    public CountryPolygon(Polygon polygon, String name) {
        this.polygon = polygon;
        this.name = name;
    }

    public final Polygon getPolygon() {
        return polygon;
    }

    public final String getName() {
        return name;
    }


}
