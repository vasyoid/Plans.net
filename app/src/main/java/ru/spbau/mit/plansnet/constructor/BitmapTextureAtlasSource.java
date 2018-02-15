package ru.spbau.mit.plansnet.constructor;

import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.source.BaseTextureAtlasSource;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;

public class BitmapTextureAtlasSource extends BaseTextureAtlasSource
        implements IBitmapTextureAtlasSource {

    private final int[] mColors;

    public BitmapTextureAtlasSource(Bitmap pBitmap, Rect pArea) {
        super(0,0, pArea.width(), pArea.height());
        mColors = new int[mTextureWidth * mTextureHeight];
        for (int y = 0; y < pArea.height(); y++) {
            for  (int x = 0; x < pArea.width(); x++) {
                mColors[x + y * mTextureWidth] = pBitmap.getPixel(x + pArea.left,
                        y + pArea.top);
            }
        }
    }

    public BitmapTextureAtlasSource(Bitmap pBitmap) {
        this(pBitmap, new Rect(0, 0, pBitmap.getWidth(), pBitmap.getHeight()));
    }

    @Override
    public Bitmap onLoadBitmap(Config pBitmapConfig) {
        return Bitmap.createBitmap(mColors, mTextureWidth, mTextureHeight, Bitmap.Config.ARGB_8888);
    }

    @Override
    public IBitmapTextureAtlasSource deepCopy() {
        return new BitmapTextureAtlasSource(Bitmap.createBitmap(mColors, mTextureWidth, mTextureHeight, Bitmap.Config.ARGB_8888));
    }

}
