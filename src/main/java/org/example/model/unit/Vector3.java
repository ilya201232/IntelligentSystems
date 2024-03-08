package org.example.model.unit;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Vector3 {

    private double x;
    private double y;
    private double z;

    public Vector3(Vector2 vector2) {
        this.x = vector2.getX();
        this.y = vector2.getY();
        this.z = 0;
    }

    public double getDistance(Vector3 another) {
        return Math.sqrt(Math.pow((x - another.getX()), 2) + Math.pow((y - another.getY()), 2) + Math.pow((z - another.getZ()), 2));
    }

    public static Vector3 crossProduct(Vector3 first, Vector3 second) {
        return new Vector3(
                first.y * second.z - first.z * second.y,
                first.z * second.x - first.x * second.z,
                first.x * second.y - first.y * second.x
        );
    }

    public static Vector3 crossProduct(Vector2 first, Vector2 second) {
        return crossProduct(new Vector3(first), new Vector3(second));
    }

    public static double dotProduct(Vector3 first, Vector3 second) {
        return first.x * second.x + first.y * second.y + first.z * second.z;
    }
}
