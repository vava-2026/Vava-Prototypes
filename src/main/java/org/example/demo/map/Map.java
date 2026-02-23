package org.example.demo.map;


import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class Map
{
    private final List<CountryPolygon> countryPolygons;
    private static final String GEOJSON_FILE = "110m_countries.json";

    public Map() throws IOException {
        var inputStream = getClass().getResourceAsStream("/" + GEOJSON_FILE);
        if (inputStream == null) {
            throw new IOException("Resource not found: " + GEOJSON_FILE);
        }
        countryPolygons = GeoJsonParser.parse(inputStream);

        for (CountryPolygon cp : countryPolygons) {
            System.out.println("Loaded country polygon: " + cp.getName() + " with " + cp.getPoints().size() + " points");
        }
    }

    /// Get the list of all country polygons loaded from the GeoJSON file.
    /// Each CountryPolygon contains the name of the country and a list of points that define its borders.
    /// Multiple country polygons can have the same name (e.g. for countries with multiple disconnected territories).
    /// @return list of all country polygons
    public final List<CountryPolygon> getCountryPolygons() {
        return countryPolygons;
    }

    // TODO: get polygons in limited by longtitude/latitude box
    // TODO: for better performance when drawing only a part of the map

    /// Given a country name, return a Country object containing the name and the list of polygons that belong to that country.
    /// @param name the name of the country to search for (case-insensitive)
    ///
    /// TODO: review return
    /// @return Country if a country with the given name is found, or an empty Optional if no such country exists.
    public Optional<Country> getCountryByName(String name) {
        List<CountryPolygon> matching = countryPolygons.stream()
                .filter(cp -> cp.getName().equalsIgnoreCase(name))
                .toList();

        return matching.isEmpty() ? Optional.empty() : Optional.of(new Country(matching, name));
    }

    /// Given a geographic point (latitude and longitude), determine which country it belongs to by checking if the point is inside any of the country polygons.
    /// @param p the geographic point to check
    ///
    /// TODO: review return
    /// @return a Country object containing the name of the country and the list of polygons that contain the point.
    public Optional<Country> getCountryByPoint(Point p)
    {
        return countryPolygons.stream()
                .filter(cp -> cp.contains(p))
                .findFirst()
                .map(CountryPolygon::getName)
                .flatMap(this::getCountryByName);
    }
}
