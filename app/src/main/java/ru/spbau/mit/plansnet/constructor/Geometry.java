package ru.spbau.mit.plansnet.constructor;

import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.earcutj.Earcut;

import org.andengine.entity.primitive.Line;

import java.util.ArrayList;
import java.util.List;

import ru.spbau.mit.plansnet.constructor.objects.MapObjectLinear;
import ru.spbau.mit.plansnet.constructor.objects.MapObjectSprite;

/**
 * Util class implementing methods to work with lines, points and polygons.
 */
public class Geometry {

    /**
     * Swaps tho lines.
     * @param pLine1 first line.
     * @param pLine2 second line.
     */
    public static void swap(@NonNull Line pLine1, @NonNull Line pLine2) {
        Line tmp = copy(pLine1);
        pLine1.setPosition(pLine2.getX1(), pLine2.getY1(), pLine2.getX2(), pLine2.getY2());
        pLine2.setPosition(tmp.getX1(), tmp.getY1(), tmp.getX2(), tmp.getY2());
    }

    /**
     * Deep copies a line.
     * @param pLine line to copy.
     * @return copy of the line.
     */
    public static @NonNull Line copy(@NonNull Line pLine) {
        return new Line(pLine.getX1(), pLine.getY1(), pLine.getX2(), pLine.getY2(),
                pLine.getVertexBufferObjectManager());
    }

    /**
     * Returns the minimum x coordinate of a given line.
     * @param pLine a line.
     * @return the minimum x coordinate.
     */
    public static float getMinX(@NonNull Line pLine) {
        return Math.min(pLine.getX1(), pLine.getX2());
    }

    /**
     * Returns the minimum y coordinate of a given line.
     * @param pLine a line.
     * @return the minimum y coordinate.
     */
    public static float getMinY(@NonNull Line pLine) {
        return Math.min(pLine.getY1(), pLine.getY2());
    }

    /**
     * Returns the maximum x coordinate of a given line.
     * @param pLine a line.
     * @return the maximum x coordinate.
     */
    public static float getMaxX(@NonNull Line pLine) {
        return Math.max(pLine.getX1(), pLine.getX2());
    }

    /**
     * Returns the maximum y coordinate of a given line.
     * @param pLine a line.
     * @return the maximum y coordinate.
     */
    public static float getMaxY(@NonNull Line pLine) {
        return Math.max(pLine.getY1(), pLine.getY2());
    }

    /**
     * Says if two lines are parallel.
     * @param pLine1 first line.
     * @param pLine2 second line.
     * @return true if the lines are parallel, false otherwise.
     */
    public static boolean linesParallel(@NonNull Line pLine1, @NonNull Line pLine2) {
        PointF v1 = new PointF(pLine1.getX2() - pLine1.getX1(),
                pLine1.getY2() - pLine1.getY1());
        PointF v2 = new PointF(pLine2.getX2() - pLine2.getX1(),
                pLine2.getY2() - pLine2.getY1());
        return v1.x * v2.y - v1.y * v2.x == 0;
    }

    /**
     * Says if two lines have intersections.
     * @param pLine1 first line.
     * @param pLine2 second line.
     * @return true if the lines intersect, false otherwise.
     */
    public static boolean linesIntersect(@NonNull Line pLine1, @NonNull Line pLine2) {
        PointF a = new PointF(pLine1.getX1(), pLine1.getY1());
        PointF b = new PointF(pLine1.getX2(), pLine1.getY2());
        return linesJoinable(pLine1, pLine2) || pLine1.collidesWith(pLine2) &&
                !linesParallel(pLine1, pLine2) &&
                !lineStartsWith(pLine2, a) && !lineEndsWith(pLine2, a) &&
                !lineStartsWith(pLine2, b) && !lineEndsWith(pLine2, b);
    }

    /**
     * Says if two lines combined give another line.
     * It means that they are parallel and have one common point.
     * @param pLine1 first line.
     * @param pLine2 second line.
     * @return true if the lines are joinable, false otherwise.
     */
    public static boolean linesJoinable(@NonNull Line pLine1, @NonNull Line pLine2) {
        if (!linesParallel(pLine1, pLine2) || !pLine1.collidesWith(pLine2)) {
            return false;
        }
        if (isVertical(pLine1)) {
            return getMinY(pLine1) != getMaxY(pLine2) && getMaxY(pLine1) != getMinY(pLine2);
        }
        return getMinX(pLine1) != getMaxX(pLine2) && getMaxX(pLine1) != getMinX(pLine2);
    }

    /**
     * Says if a line is vertical
     * @param pLine line.
     * @return true the line is vertical, false otherwise.
     */
    public static boolean isVertical(@NonNull Line pLine) {
        return pLine.getX1() == pLine.getX2();
    }

    /**
     * Says if a line is horizontal
     * @param pLine line.
     * @return true the line is horizontal, false otherwise.
     */
    public static boolean isHorizontal(@NonNull Line pLine) {
        return pLine.getY1() == pLine.getY2();
    }

    /**
     * Says if a line's first point is a given one.
     * @param pLine line.
     * @param PointF point.
     * @return true the line starts with the point, false otherwise.
     */
    public static boolean lineStartsWith(@NonNull Line pLine, @NonNull PointF PointF) {
        return PointF.equals(pLine.getX1(), pLine.getY1());
    }

    /**
     * Says if a line's second point is a given one.
     * @param pLine line.
     * @param PointF point.
     * @return true the line ends with the point, false otherwise.
     */
    public static boolean lineEndsWith(@NonNull Line pLine, @NonNull PointF PointF) {
        return PointF.equals(pLine.getX2(), pLine.getY2());
    }

    /**
     * Says if a point is at a corner of a rectangle.
     * @param pPoint point to check.
     * @param pLeftDown left down corner of the rectangle
     * @param pRightUp right up corner of the rectangle
     * @param bounds the maximum distance the point can be from two of the rectangle sides.
     * @return true the point is at a corner, false otherwise.
     */
    public static boolean isPointAtCorner(@NonNull PointF pPoint,
                                          @NonNull PointF pLeftDown,
                                          @NonNull PointF pRightUp, double bounds) {
        return (pPoint.x <= pLeftDown.x + bounds || pPoint.x >= pRightUp.x - bounds) &&
               (pPoint.y <= pLeftDown.y + bounds || pPoint.y >= pRightUp.y - bounds);
    }

    /**
     * Returns the distance between two points.
     * @param pPoint1 first point.
     * @param pPoin2 second point.
     * @return the distance between the points.
     */
    public static float distance(@NonNull PointF pPoint1, @NonNull PointF pPoin2) {
        return length(new Line(pPoint1.x, pPoint1.y,
                pPoin2.x, pPoin2.y, null));
    }

    /**
     * Returns the length of a line.
     * @param pLine line.
     * @return the line's length.
     */
    public static float length(@NonNull Line pLine) {
        return (float) Math.sqrt((pLine.getX2() - pLine.getX1()) * (pLine.getX2() - pLine.getX1()) +
                (pLine.getY2() - pLine.getY1()) * (pLine.getY2() - pLine.getY1()));
    }

    /**
     * Changes the the line so that its length is 1.
     * @param pLine line to normalize.
     * @return new line.
     */
    public static @NonNull Line normalize(@NonNull Line pLine) {
        float len = length(pLine);
        return new Line(0, 0, pLine.getX2() / len, pLine.getY2() / len,
                pLine.getVertexBufferObjectManager());
    }

    /**
     * returns the smaller angle between two lines.
     * @param pLine1 first line.
     * @param pLine2 second line.
     * @return angle between the lines.
     */
    public static float getAngle(@NonNull Line pLine1, @NonNull Line pLine2) {
        pLine1 = new Line(0, 0, pLine1.getX2() - pLine1.getX1(),
                pLine1.getY2() - pLine1.getY1(), null);
        pLine2 = new Line(0, 0, pLine2.getX2() - pLine2.getX1(),
                pLine2.getY2() - pLine2.getY1(), null);
        if (isOnePoint(pLine1) || isOnePoint(pLine2) || linesJoinable(pLine1, pLine2)) {
            return 0;
        }
        pLine1 = normalize(pLine1);
        pLine2 = normalize(pLine2);
        float result = (float) Math.acos(pLine1.getX2() * pLine2.getX2() +
                pLine1.getY2() * pLine2.getY2());
        if (pLine1.getX2() * pLine2.getY2() - pLine1.getY2() * pLine2.getX2() < 0) {
            result *= -1;
        }
        return result;
    }

    /**
     * Says if a line's ends coincide.
     * This means that the length of the line is 0.
     * @param pLine line.
     * @return true if the length of the line is 0, false otherwise.
     */
    public static boolean isOnePoint(@NonNull Line pLine) {
        return pLine.getX1() == pLine.getX2() && pLine.getY1() == pLine.getY2();
    }

    /**
     * Returns the intersection of two lines if there is exactly one.
     * @param pLine1 first line
     * @param pLine2 second line
     * @return the intersection point if there is one, null otherwise.
     */
    public static @Nullable PointF getIntersectionPointOrNull(@NonNull Line pLine1,
                                                              @NonNull Line pLine2) {
        pLine1 = copy(pLine1);
        pLine2 = copy(pLine2);
        if (!pLine1.collidesWith(pLine2) || linesJoinable(pLine1, pLine2)) {
            return null;
        }
        PointF result = new PointF();
        if (linesParallel(pLine1, pLine2)) {
            if (getMaxX(pLine1) == getMinX(pLine2)) {
                result.x = getMaxX(pLine1);
            } else {
                result.x = getMinX(pLine1);
            }
            if (getMaxY(pLine1) == getMinY(pLine2)) {
                result.y = getMaxY(pLine1);
            } else {
                result.y = getMinY(pLine1);
            }
            return result;
        }
        if (isVertical(pLine2)) {
            Geometry.swap(pLine1, pLine2);
        }
        if (isVertical(pLine1)) {
            result.set(pLine1.getX1(),
                    (pLine2.getY2() - pLine2.getY1()) /
                            (pLine2.getX2() - pLine2.getX1()) *
                            (pLine1.getX1() - pLine2.getX1()) + pLine2.getY1());
            return result;
        }
        if (isHorizontal(pLine2)) {
            Geometry.swap(pLine1, pLine2);
        }
        if (isHorizontal(pLine1)) {
            result.set((pLine2.getX2() - pLine2.getX1()) /
                    (pLine2.getY2() - pLine2.getY1()) *
                    (pLine1.getY1() - pLine2.getY1()) + pLine2.getX1(), pLine1.getY1());
            return result;
        }
        float t1 = (pLine1.getX2() - pLine1.getX1()) / (pLine1.getY2() - pLine1.getY1());
        float t2 = (pLine2.getX2() - pLine2.getX1()) / (pLine2.getY2() - pLine2.getY1());
        float y = (pLine2.getX1() - pLine1.getX1() + t1 * pLine1.getY1() - t2 * pLine2.getY1())
                / (t1 - t2);
        float x = t1 * (y - pLine1.getY1()) + pLine1.getX1();
        result.set(x, y);
        return result;
    }

    /**
     * Returns a polygon covering a point and consisting of linear objects if exists.
     * @param pObjects list of linear objects.
     * @param pPoint point.
     * @return the polygon if it exists, null otherwise.
     */
    public static @Nullable List<PointF> roomPolygonOrNull(@NonNull List<MapObjectSprite> pObjects,
                                                           @NonNull PointF pPoint) {
        List<PointF> polygon = new ArrayList<>();
        MapObjectLinear currentObject = null;
        float curX = -1e5f;
        Line ray = new Line(-1e5f, pPoint.y, pPoint.x, pPoint.y, null);
        for (MapObjectSprite o : pObjects) {
            if (!(o instanceof MapObjectLinear)) {
                continue;
            }
            MapObjectLinear ol = (MapObjectLinear) o;
            PointF tmp = getIntersectionPointOrNull(ray, ol.getPosition());
            if (tmp == null) {
                continue;
            }
            if (curX < tmp.x) {
                currentObject = (MapObjectLinear) o;
                curX = tmp.x;
            }
        }
        if (currentObject == null) {
            return null;
        }
        if (currentObject.getPosition().getY1() > currentObject.getPosition().getY2()) {
            currentObject.changeDirection();
        }
        polygon.add(currentObject.getPoint1());
        while (true) {
            PointF curPoint = currentObject.getPoint2();
            if (curPoint.equals(polygon.get(0))) {
                break;
            }
            polygon.add(curPoint);
            MapObjectLinear nextObject = null;
            float currentAngle = 10;
            for (MapObjectSprite o : pObjects) {
                if (!(o instanceof MapObjectLinear)) {
                    continue;
                }
                MapObjectLinear ol = (MapObjectLinear) o;
                if (linesJoinable(ol.getPosition(), currentObject.getPosition())) {
                    continue;
                }
                if (lineEndsWith(ol.getPosition(), curPoint)) {
                    ol.changeDirection();
                }
                if (!lineStartsWith(ol.getPosition(), curPoint)) {
                    continue;
                }
                if (currentAngle > getAngle(currentObject.getPosition(), ol.getPosition())) {
                    currentAngle = getAngle(currentObject.getPosition(), ol.getPosition());
                    nextObject = (MapObjectLinear) o;
                }
            }
            if (nextObject == null) {
                return null;
            }
            currentObject = nextObject;
        }
        return polygon;
    }

    /**
     * Says if a point is inside a polygon.
     * @param pPolygon polygon
     * @param pPoint point.
     * @return true if the point is inside the polygon, false otherwise.
     */
    public static boolean isPointInsidePolygon(@NonNull List<PointF> pPolygon,
                                               @NonNull PointF pPoint) {
        int cntIntersections = 0;
        Line line = new Line(pPoint.x, pPoint.y,
                pPoint.x + 0.5f, -1e5f, null);
        for (int i = 0; i < pPolygon.size(); i++) {
            Line side = new Line(pPolygon.get(i).x, pPolygon.get(i).y,
                    pPolygon.get((i + 1) % pPolygon.size()).x,
                    pPolygon.get((i + 1) % pPolygon.size()).y, null);
            if (side.collidesWith(line)) {
                cntIntersections++;
            }
        }
        return cntIntersections % 2 == 1;
    }

    /**
     * Takes a polygon and triangulates it.
     * @param pPolygon polygon.
     * @return array of coordinates representing the triangulation.
     */
    public static @NonNull float[] makeTriangles(@NonNull List<PointF> pPolygon) {
        float[][][] vertices = new float[1][pPolygon.size()][2];
        for (int i = 0; i < pPolygon.size(); i++) {
            vertices[0][i][0] = pPolygon.get(i).x;
            vertices[0][i][1] = pPolygon.get(i).y;
        }
        List<float[][]> triangles = Earcut.earcut(vertices, true);
        float[] vertexData = new float[triangles.size() * 9];
        for (int i = 0; i < triangles.size(); i++) {
            for (int t = 0; t < 3; ++t) {
                vertexData[9 * i + 3 * t] = triangles.get(i)[t][0];
                vertexData[9 * i + 3 * t + 1] = triangles.get(i)[t][1];
            }
        }
        return vertexData;
    }

    /**
     * Returns a point inside a polygon.
     * @param pBufferData array of coordinates representing the polygon.
     * @return a point strictly inside the polygon.
     */
    public static @NonNull PointF getPointInside(@NonNull float[] pBufferData) {
        PointF v1 = new PointF(pBufferData[3] - pBufferData[0],
                pBufferData[4] - pBufferData[1]);
        PointF v2 = new PointF(pBufferData[6] - pBufferData[0],
                pBufferData[7] - pBufferData[1]);
        v1.offset(v2.x, v2.y);
        v1.set(v1.x / 4, v1.y / 4);
        v1.offset(pBufferData[0], pBufferData[1]);
        return v1;
    }

    /**
     * Brings a numeric value into a numeric segment.
     * @param pValue value.
     * @param pMin segment min value.
     * @param pMax segment max value.
     * @return value closest to the given that belongs to the segment.
     */
    public static float bringValueToBounds(float pValue, float pMin, float pMax) {
        return Math.min(pMax, Math.max(pMin, pValue));
    }

}
