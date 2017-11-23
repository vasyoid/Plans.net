package ru.spbau.mit.plansnet.constructor;

import android.graphics.PointF;
import android.util.Log;

import org.andengine.entity.primitive.Line;

import java.util.List;

public class LineHelper {

    public static void swap(Line l1, Line l2) {
        Line tmp = copy(l1);
        l1.setPosition(l2);
        l2.setPosition(tmp);
    }

    public static Line copy(Line line) {
        return new Line(line.getX1(), line.getY1(), line.getX2(), line.getY2(),
                line.getVertexBufferObjectManager());
    }

    public static void changeDirection(Line line) {
        line.setPosition(line.getX2(), line.getY2(), line.getX1(), line.getY1());
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
        return l1.collidesWith(l2) && !linesParallel(l1, l2) &&
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

    public static Line join(Line l1, Line l2) {
        if (!linesJoinable(l1, l2)) {
            return null;
        }
        Line result = new Line(Math.min(getMinX(l1), getMinX(l2)),
                Math.min(getMinY(l1), getMinY(l2)),
                Math.max(getMaxX(l1), getMaxX(l2)),
                Math.max(getMaxY(l1), getMaxY(l2)),
                l1.getVertexBufferObjectManager());
        if (!linesParallel(result, l1)) {
            result.setPosition(result.getX1(), result.getY2(), result.getX2(), result.getY1());
        }
        return result;
    }

    public static boolean isVertical(Line line) {
        return line.getX1() == line.getX2();
    }

    public static boolean isHorizontal(Line line) {
        return line.getY1() == line.getY2();
    }

    public static boolean isPointAbove(Line line, PointF point) {
        PointF tmp = getIntersectionPoint(new Line(point.x, -1e5f,
                point.x, 1e5f, null), line, false);
        return tmp != null && point.y > tmp.y;
    }

    public static boolean lineStartsWith(Line line, PointF point) {
        return point.equals(line.getX1(), line.getY1());
    }
    public static boolean lineEndsWith(Line line, PointF point) {
        return point.equals(line.getX2(), line.getY2());
    }

    public static boolean isPointRightward(Line line, PointF point) {
        PointF tmp = getIntersectionPoint(new Line(-1e5f, point.y,
                1e5f, point.y, null), line, false);
        return tmp != null && point.x > tmp.x;
    }

    public static float length(Line line) {
        return (float) Math.sqrt((line.getX2() - line.getX1()) * (line.getX2() - line.getX1()) +
                (line.getY2() - line.getY1()) * (line.getY2() - line.getY1()));
    }

    public static void normalize(Line line) {
        float len = length(line);
        line.setPosition(0, 0, line.getX2() / len, line.getY2() / len);
    }

    public static float getAngle(Line l1, Line l2) {
        l1 = new Line(0, 0, l1.getX2() - l1.getX1(),
                l1.getY2() - l1.getY1(), null);
        l2 = new Line(0, 0, l2.getX2() - l2.getX1(),
                l2.getY2() - l2.getY1(), null);
        if (isOnePoint(l1) || isOnePoint(l2) || linesJoinable(l1, l2)) {
            return 0;
        }
        normalize(l1);
        normalize(l2);
        float result = (float) Math.acos(l1.getX2() * l2.getX2() + l1.getY2() * l2.getY2());
        if (l1.getX2() * l2.getY2() - l1.getY2() * l2.getX2() < 0) {
            result *= -1;
        }
        return result;
    }

    public static boolean isOnePoint(Line line) {
        return line.getX1() == line.getX2() && line.getY1() == line.getY2();
    }

    public static PointF getIntersectionPoint(Line l1, Line l2, boolean checkCollision) {
        l1 = copy(l1);
        l2 = copy(l2);
        if (checkCollision && !l1.collidesWith(l2) || linesJoinable(l1, l2)) {
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
            LineHelper.swap(l1, l2);
        }
        if (isVertical(l1)) {
            result.set(l1.getX1(),
                    (l2.getY2() - l2.getY1()) /
                            (l2.getX2() - l2.getX1()) *
                            (l1.getX1() - l2.getX1()) + l2.getY1());
            return result;
        }
        if (isHorizontal(l2)) {
            LineHelper.swap(l1, l2);
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

}
