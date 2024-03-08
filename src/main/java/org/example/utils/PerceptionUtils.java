package org.example.utils;

import org.example.model.Perception;
import org.example.model.object.Marker;
import org.example.model.object.Player;

import java.util.Optional;

public class PerceptionUtils {

    public static Optional<Player> getTeammateWithNumber(Perception perception, int uniformNumber) {
        return perception.getTeammatesSaw().stream()
                .filter(player -> player.getUniformNumber() != null && player.getUniformNumber() == uniformNumber)
                .findFirst();
    }

    public static Optional<Marker> getMarker(Perception perception, String markerName) {
        return perception.getMarkersSaw().stream()
                .filter(marker -> marker.getId().equals(markerName))
                .findFirst();
    }

}
