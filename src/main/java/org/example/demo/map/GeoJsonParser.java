package org.example.demo.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GeoJsonParser {
    public static List<CountryPolygon> parse(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(inputStream);

        JsonNode features = root.get("features");
        if (features == null || !features.isArray()) {
            throw new IllegalArgumentException("Invalid GeoJSON: no features array");
        }

        List<CountryPolygon> result = new ArrayList<>();

        for (JsonNode feature : features) {

            JsonNode properties = feature.get("properties");
            String name = properties != null && properties.has("SOVEREIGNT")
                    ? properties.get("SOVEREIGNT").asText()
                    : "UNKNOWN";

            JsonNode geometry = feature.get("geometry");
            if (geometry == null) continue;

            String type = geometry.get("type").asText();
            JsonNode coordinates = geometry.get("coordinates");

            if ("Polygon".equals(type)) {
                List<Point> polygon = parsePolygon(coordinates);
                result.add(new CountryPolygon(polygon, name));
            }
            else if ("MultiPolygon".equals(type)) {
                for (JsonNode polygonNode : coordinates) {
                    List<Point> polygon = parsePolygon(polygonNode);
                    result.add(new CountryPolygon(polygon, name));
                }
            }
            else {
                System.out.println("Unsupported geometry type: " + type);
                continue; // ignore other types
            }
        }

        return result;
    }

    // TODO: handle holes (inner rings) if needed
    private static List<Point> parsePolygon(JsonNode polygonCoords) {
        List<Point> result = new ArrayList<>();
        // polygonCoords[0] is the outer ring, [1..n] are holes — skip them
        JsonNode outerRing = polygonCoords.get(0);
        if (outerRing == null) return result;

        for (JsonNode point : outerRing) {
            double lon = point.get(0).asDouble();
            double lat = point.get(1).asDouble();
            result.add(new Point(lon, lat));
        }
        return result;
    }
}
