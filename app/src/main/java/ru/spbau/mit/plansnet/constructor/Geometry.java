package ru.spbau.mit.plansnet.constructor;

import android.graphics.PointF;

import com.earcutj.Earcut;

import org.andengine.entity.primitive.Line;

import java.util.ArrayList;
import java.util.List;

public class Geometry {

    public static void swap(Line l1, Line l2) {
        Line tmp = copy(l1);
        l1.setPosition(l2.getX1(), l2.getY1(), l2.getX2(), l2.getY2());
        l2.setPosition(tmp.getX1(), tmp.getY1(), tmp.getX2(), tmp.getY2());
    }

    public static Line copy(Line line) {
        return new Line(line.getX1(), line.getY1(), line.getX2(), line.getY2(),
                line.getVertexBufferObjectManager());
    }

    public static float getMinX(Line line) {
        return Math.min(line.getX1(), line.getX2());
    }

    public static float getMinY(Line line) {
        return Math.min(line.getY1(), line.getY2());
    }

    public static float getMaxX(Line line) {
        return Math.max(line.getX1(), line.getX2());
    }

    public static float getMaxY(Line line) {
        return Math.max(line.getY1(), line.getY2());
    }

    public static boolean linesParallel(Line l1, Line l2) {
        PointF v1 = new PointF(l1.getX2() - l1.getX1(), l1.getY2() - l1.getY1());
        PointF v2 = new PointF(l2.getX2() - l2.getX1(), l2.getY2() - l2.getY1());
        return v1.x * v2.y - v1.y * v2.x == 0;
    }

    public static boolean linesIntersect(Line l1, Line l2) {
        PointF a = new PointF(l1.getX1(), l1.getY1());
        PointF b = new PointF(l1.getX2(), l1.getY2());
        return linesJoinable(l1, l2) || l1.collidesWith(l2) && !linesParallel(l1, l2) &&
                !lineStartsWith(l2, a) && !lineEndsWith(l2, a) &&
                !lineStartsWith(l2, b) && !lineEndsWith(l2, b);
    }

    public static boolean linesJoinable(Line l1, Line l2) {
        if (!linesParallel(l1, l2) || !l1.collidesWith(l2)) {
            return false;
        }
        if (isVertical(l1)) {
            return getMinY(l1) != getMaxY(l2) && getMaxY(l1) != getMinY(l2);
        }
        return getMinX(l1) != getMaxX(l2) && getMaxX(l1) != getMinX(l2);
    }

    public static boolean isVertical(Line line) {
        return line.getX1() == line.getX2();
    }

    public static boolean isHorizontal(Line line) {
        return line.getY1() == line.getY2();
    }

    public static boolean lineStartsWith(Line line, PointF PointF) {
        return PointF.equals(line.getX1(), line.getY1());
    }
    public static boolean lineEndsWith(Line line, PointF PointF) {
        return PointF.equals(line.getX2(), line.getY2());
    }

    public static boolean isPointAtCorner(PointF point, PointF ld, PointF ru, double bounds) {
        return (point.x <= ld.x + bounds || point.x >= ru.x - bounds) &&
               (point.y <= ld.y + bounds || point.y >= ru.y - bounds);
    }

    public static float distance(PointF p1, PointF p2) {
        return length(new Line(p1.x, p1.y, p2.x, p2.y, null));
    }

    public static float length(Line line) {
        return (float) Math.sqrt((line.getX2() - line.getX1()) * (line.getX2() - line.getX1()) +
                (line.getY2() - line.getY1()) * (line.getY2() - line.getY1()));
    }

    public static Line normalize(Line line) {
        float len = length(line);
        return new Line(0, 0, line.getX2() / len, line.getY2() / len,
                line.getVertexBufferObjectManager());
    }

    public static float getAngle(Line l1, Line l2) {
        l1 = new Line(0, 0, l1.getX2() - l1.getX1(),
                l1.getY2() - l1.getY1(), null);
        l2 = new Line(0, 0, l2.getX2() - l2.getX1(),
                l2.getY2() - l2.getY1(), null);
        if (isOnePoint(l1) || isOnePoint(l2) || linesJoinable(l1, l2)) {
            return 0;
        }
        l1 = normalize(l1);
        l2 = normalize(l2);
        float result = (float) Math.acos(l1.getX2() * l2.getX2() + l1.getY2() * l2.getY2());
        if (l1.getX2() * l2.getY2() - l1.getY2() * l2.getX2() < 0) {
            result *= -1;
        }
        return result;
    }

    public static boolean isOnePoint(Line line) {
        return line.getX1() == line.getX2() && line.getY1() == line.getY2();
    }

    public static PointF getIntersectionPoint(Line l1, Line l2) {
        l1 = copy(l1);
        l2 = copy(l2);
        if (!l1.collidesWith(l2) || linesJoinable(l1, l2)) {
            return null;
        }
        PointF result = new PointF();
        if (linesParallel(l1, l2)) {
            if (getMaxX(l1) == getMinX(l2)) {
                result.x = getMaxX(l1);
            } else {
                result.x = getMinX(l1);
            }
            if (getMaxY(l1) == getMinY(l2)) {
                result.y = getMaxY(l1);
            } else {
                result.y = getMinY(l1);
            }
            return result;
        }
        if (isVertical(l2)) {
            Geometry.swap(l1, l2);
        }
        if (isVertical(l1)) {
            result.set(l1.getX1(),
                    (l2.getY2() - l2.getY1()) /
                            (l2.getX2() - l2.getX1()) *
                            (l1.getX1() - l2.getX1()) + l2.getY1());
            return result;
        }
        if (isHorizontal(l2)) {
            Geometry.swap(l1, l2);
        }
        if (isHorizontal(l1)) {
            result.set((l2.getX2() - l2.getX1()) /
                    (l2.getY2() - l2.getY1()) *
                    (l1.getY1() - l2.getY1()) + l2.getX1(), l1.getY1());
            return result;
        }
        float t1 = (l1.getX2() - l1.getX1()) / (l1.getY2() - l1.getY1());
        float t2 = (l2.getX2() - l2.getX1()) / (l2.getY2() - l2.getY1());
        float y = (l2.getX1() - l1.getX1() + t1 * l1.getY1() - t2 * l2.getY1()) / (t1 - t2);
        float x = t1 * (y - l1.getY1()) + l1.getX1();
        result.set(x, y);
        return result;
    }

    public static List<PointF> roomPolygon(List<MapObjectSprite> objects, PointF PointF) {
        List<PointF> polygon = new ArrayList<>();
        MapObjectLinear currentObject = null;
        float curX = -1e5f;
        Line ray = new Line(-1e5f, PointF.y, PointF.x, PointF.y, null);
        for (MapObjectSprite o : objects) {
            if (!(o instanceof MapObjectLinear)) {
                continue;
            }
            MapObjectLinear ol = (MapObjectLinear) o;
            PointF tmp = getIntersectionPoint(ray, ol.getPosition());
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
            for (MapObjectSprite o : objects) {
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

    public static boolean isPointInsidePolygon(List<PointF> polygon, PointF PointF) {
        int cntIntersections = 0;
        Line line = new Line(PointF.x, PointF.y,
                PointF.x + 0.5f, -1e5f, null);
        for (int i = 0; i < polygon.size(); i++) {
            Line side = new Line(polygon.get(i).x, polygon.get(i).y,
                    polygon.get((i + 1) % polygon.size()).x,
                    polygon.get((i + 1) % polygon.size()).y, null);
            if (side.collidesWith(line)) {
                cntIntersections++;
            }
        }
        return cntIntersections % 2 == 1;
    }

    public static float[] makeTriangles(List<PointF> polygon) {
        float[][][] vertices = new float[1][polygon.size()][2];
        for (int i = 0; i < polygon.size(); i++) {
            vertices[0][i][0] = polygon.get(i).x;
            vertices[0][i][1] = polygon.get(i).y;
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

    public static PointF getPointInside(float[] pBufferData) {
        PointF v1 = new PointF(pBufferData[3] - pBufferData[0],
                pBufferData[4] - pBufferData[1]);
        PointF v2 = new PointF(pBufferData[6] - pBufferData[0],
                pBufferData[7] - pBufferData[1]);
        v1.offset(v2.x, v2.y);
        v1.set(v1.x / 4, v1.y / 4);
        v1.offset(pBufferData[0], pBufferData[1]);
        return v1;
    }

}
