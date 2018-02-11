package ru.spbau.mit.plansnet.constructor;

import android.graphics.PointF;

import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;

import ru.spbau.mit.plansnet.data.objects.Sticker;

import static org.andengine.input.touch.TouchEvent.ACTION_DOWN;
import static org.andengine.input.touch.TouchEvent.ACTION_MOVE;
import static org.andengine.input.touch.TouchEvent.ACTION_UP;

public class StickerSprite extends MapObjectSprite {

    private final static int TEXTURE_SIZE = 400;
    private static ITextureRegion[] textureRegions;
    private final StickerType type;
    private PointF position;
    private PointF firstTouch = null;
    private PointF previousTouch = null;
    private boolean zooming = false;
    private float size;
    private float currentSize;

    public StickerSprite(StickerType pType, PointF pPosition) {
        this(pType, pPosition, 1.0f);
    }

    public StickerSprite(Sticker pSticker) {
        this(StickerType.fromValue(pSticker.getType()),
                new PointF(pSticker.getPosition().getX(), pSticker.getPosition().getY()),
                pSticker.getSize());
    }

    public StickerSprite(StickerType pType, PointF pPosition, float pSize) {
        super(textureRegions[pType.getValue()], vertexBufferObjectManager);
        type = pType;
        size = pSize;
        position = new PointF(pPosition.x, pPosition.y);
        setScaleCenter(TEXTURE_SIZE / 2, TEXTURE_SIZE / 2);
        setScale(size);
        super.setPosition(position.x - TEXTURE_SIZE / 2,
                position.y - TEXTURE_SIZE / 2);
        setZIndex(4);
    }

    public static void setTextureRegions(ITextureRegion[] pTextureRegions) {
        textureRegions = pTextureRegions;
    }

    public float getSize() {
        return size;
    }

    public int getType() {
        return type.getValue();
    }

    public PointF getPosition() {
        return position;
    }

    @Override
    public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
                                 float pTouchAreaLocalX, float pTouchAreaLocalY) {
        if (MAP.getTouchState() == ConstructorActivity.ActionState.MOVE_STICKER) {
            switch (pSceneTouchEvent.getAction()) {
                case ACTION_DOWN:
                    firstTouch = previousTouch = new PointF(pSceneTouchEvent.getX(),
                            pSceneTouchEvent.getY());
                    if (Geometry.isPointAtCorner(new PointF(pTouchAreaLocalX, pTouchAreaLocalY),
                            new PointF(0, 0), new PointF(TEXTURE_SIZE, TEXTURE_SIZE),
                            Map.getGridSize() / 2.0 / size)) {
                        zooming = true;
                        currentSize = size;
                    } else {
                        setScale(size + 0.05f);
                    }
                    break;
                case ACTION_MOVE:
                    if (firstTouch == null || previousTouch == null) {
                        break;
                    }
                    PointF currentTouch = new PointF(pSceneTouchEvent.getX(),
                            pSceneTouchEvent.getY());
                    if (zooming) {
                        currentSize = size * Geometry.distance(position, currentTouch) /
                                Geometry.distance(position, firstTouch);
                        currentSize = Math.min(currentSize, 1.5f);
                        currentSize = Math.max(currentSize, 0.7f);
                        setScale(currentSize);
                    } else {
                        position.offset(currentTouch.x - previousTouch.x,
                                currentTouch.y - previousTouch.y);
                        super.setPosition(position.x - TEXTURE_SIZE / 2,
                                position.y - TEXTURE_SIZE / 2);
                        previousTouch = currentTouch;
                    }
                    break;
                case ACTION_UP:
                    if (zooming) {
                        size = currentSize;
                        zooming = false;
                    }
                    setScale(size);
                    firstTouch = previousTouch = null;
                    break;
                default:
                    break;
            }
            return true;
        } else if (MAP.getTouchState() == ConstructorActivity.ActionState.DEL &&
                pSceneTouchEvent.getAction() == ACTION_DOWN) {
            MAP.removeObject(this);
            return true;
        }
        return false;
    }

    public enum StickerType {

        EXIT(0), LIFT(1), STAIRS(2), WC(3);
        private int value;

        StickerType(int pValue) {
            value = pValue;
        }

        public int getValue() {
            return value;
        }

        public static StickerType fromValue(int pValue) {
            switch (pValue) {
                case 0:
                    return EXIT;
                case 1:
                    return LIFT;
                case 2:
                    return STAIRS;
                case 3:
                    return WC;
                default:
                    return null;
            }
        }

    }

}
