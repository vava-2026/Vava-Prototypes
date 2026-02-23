package org.example.demo.map;

import java.util.ArrayList;
import java.util.List;

public class CountryPolygon extends Polygon
{
    private String name;

    public CountryPolygon(List<Point> polygon, String name) {
        super(polygon);
        this.name = name;
    }

//
//    public CountryPolygon(Polygon polygon, String name) {
//        super(Polygon);
//        // this.polygon = polygon;
//        this.name = name;
//    }

    public final String getName() {
        return name;
    }


}
