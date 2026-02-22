package org.example.demo.map;

import java.util.ArrayList;
import java.util.List;

public class Country
{
    private List<CountryPolygon> polygons;
    private String name;

    protected Country(List<CountryPolygon> polygons, String name) {
        this.polygons = polygons;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<CountryPolygon> getPolygons() {
        return polygons;
    }


}
