package ru.spbau.mit.plansnet.constructor.objects;

import android.graphics.PointF;
import android.support.annotation.NonNull;

import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;

import ru.spbau.mit.plansnet.constructor.Geometry;
import ru.spbau.mit.plansnet.constructor.Map;
import ru.spbau.mit.plansnet.constructor.constructorController.ConstructorActivity;
import ru.spbau.mit.plansnet.data.objects.Sticker;

import static org.andengine.input.touch.TouchEvent.ACTION_DOWN;
import static org.andengine.input.touch.TouchEvent.ACTION_MOVE;
import static org.andengine.input.touch.TouchEvent.ACTION_UP;
import static ru.spbau.mit.plansnet.constructor.constructorController.BaseConstructorActivity.MAP_HEIGHT;
import static ru.spbau.mit.plansnet.constructor.constructorController.BaseConstructorActivity.MAP_WIDTH;

public class StickerSprite extends MapObjectSprite {

    private final static int TEXTURE_SIZE = 400;

    private static ITextureRegion[] textureRegions;

    private final StickerType mType;
    private PointF mPosition;
    private PointF mFirstTouch = null;
    private PointF mPreviousTouch = null;
    private boolean mZooming = false;
    private float mSize;
    private float mCurrentSize;

    public StickerSprite(@NonNull StickerType pType, @NonNull PointF pPosition) {
        this(pType, pPosition, 1.0f);
    }

    public StickerSprite(@NonNull Sticker pSticker) {
        this(StickerType.fromValue(pSticker.getType()),
                new PointF(pSticker.getPosition().getX(), pSticker.getPosition().getY()),
                pSticker.getSize());
    }

    public StickerSprite(@NonNull StickerType pType, @NonNull PointF pPosition, float pSize) {
        super(textureRegions[pType.getValue()], vertexBufferObjectManager);
        mType = pType;
        mSize = pSize;
        mPosition = new PointF(pPosition.x, pPosition.y);
        mPosition.x = Geometry.bringValueToBounds(mPosition.x,
                getWidthScaled() / 2, MAP_WIDTH - getWidthScaled() / 2);
        mPosition.y = Geometry.bringValueToBounds(mPosition.y,
                getHeightScaled() / 2, MAP_HEIGHT - getHeightScaled() /2);
        setScaleCenter(TEXTURE_SIZE / 2, TEXTURE_SIZE / 2);
        setScale(mSize);
        super.setPosition(mPosition.x - TEXTURE_SIZE / 2,
                mPosition.y - TEXTURE_SIZE / 2);
        setZIndex(4);
    }

    public static void setTextureRegions(@NonNull ITextureRegion[] pTextureRegions) {
        textureRegions = pTextureRegions;
    }

    public float getSize() {
        return mSize;
    }

    public int getType() {
        return mType.getValue();
    }

    public @NonNull PointF getPosition() {
        return mPosition;
    }

    @Override
    public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
                                 float pTouchAreaLocalX, float pTouchAreaLocalY) {
        if (MAP.getTouchState() == ConstructorActivity.ActionState.MOVE_OBJECT) {
            switch (pSceneTouchEvent.getAction()) {
                case ACTION_DOWN:
                    mFirstTouch = mPreviousTouch = new PointF(pSceneTouchEvent.getX(),
                            pSceneTouchEvent.getY());
                    if (Geometry.isPointAtCorner(new PointF(pTouchAreaLocalX, pTouchAreaLocalY),
                            new PointF(0, 0), new PointF(TEXTURE_SIZE, TEXTURE_SIZE),
                            Map.getGridSize() / 2.0 / mSize)) {
                        mZooming = true;
                        mCurrentSize = mSize;
                    } else {
                        setScale(mSize + 0.05f);
                    }
                    break;
                case ACTION_MOVE:
                    if (mFirstTouch == null || mPreviousTouch == null) {
                        return false;
                    }
                    PointF currentTouch = new PointF(pSceneTouchEvent.getX(),
                            pSceneTouchEvent.getY());
                    if (mZooming) {
                        mCurrentSize = mSize * Geometry.distance(mPosition, currentTouch) /
                                Geometry.distance(mPosition, mFirstTouch);
                        mCurrentSize = Geometry.bringValueToBounds(mCurrentSize,
                                0.7f, 1.5f);
                        setScale(mCurrentSize);
                    } else {
                        mPosition.offset(currentTouch.x - mPreviousTouch.x,
                                currentTouch.y - mPreviousTouch.y);
                        mPosition.x = Geometry.bringValueToBounds(mPosition.x,
                                getWidthScaled() / 2, MAP_WIDTH - getWidthScaled() / 2);
                        mPosition.y = Geometry.bringValueToBounds(mPosition.y,
                                getHeightScaled() / 2,
                                MAP_HEIGHT - getHeightScaled() /2);
                        super.setPosition(mPosition.x - TEXTURE_SIZE / 2,
                                mPosition.y - TEXTURE_SIZE / 2);
                        mPreviousTouch = currentTouch;
                    }
                    break;
                case ACTION_UP:
                    if (mZooming) {
                        mSize = mCurrentSize;
                        mZooming = false;
                    }
                    setScale(mSize);
                    mPosition.x = Geometry.bringValueToBounds(mPosition.x,
                            getWidthScaled() / 2, MAP_WIDTH - getWidthScaled() / 2);
                    mPosition.y = Geometry.bringValueToBounds(mPosition.y,
                            getHeightScaled() / 2, MAP_HEIGHT - getHeightScaled() /2);
                    super.setPosition(mPosition.x - TEXTURE_SIZE / 2,
                            mPosition.y - TEXTURE_SIZE / 2);
                    mFirstTouch = mPreviousTouch = null;
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

        EXIT(0), LIFT(1), STAIRS(2), WC(3), FIRE(4), SMOKE(5), VOLTAGE(6);
        private int value;

        StickerType(int pValue) {
            value = pValue;
        }

        public int getValue() {
            return value;
        }

        public static @NonNull StickerType fromValue(int pValue) {
            switch (pValue) {
                case 0:
                    return EXIT;
                case 1:
                    return LIFT;
                case 2:
                    return STAIRS;
                case 3:
                    return WC;
                case 4:
                    return FIRE;
                case 5:
                    return SMOKE;
                case 6:
                    return VOLTAGE;
                default:
                    throw new IllegalArgumentException("Illegal sticker type");
            }
        }

    }

}
