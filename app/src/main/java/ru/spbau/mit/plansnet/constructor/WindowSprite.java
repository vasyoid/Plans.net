package ru.spbau.mit.plansnet.constructor;


import org.andengine.opengl.texture.region.ITextureRegion;

public class WindowSprite extends MapObjectLinear {

    private static ITextureRegion textureRegion;

    public MapObjectSprite copy() {
        MapObjectSprite result = new WindowSprite();
        result.setPosition(getPosition());
        return result;
    }

    public WindowSprite() {
        super(textureRegion);
    }

/*    public WindowSprite(Window window) {
        super(textureRegion);
        setPosition(pWall.getX(), pWall.getY(), pWall.getX2(), pWall.getY2());
    }
*/
    public static void setTexture(ITextureRegion texture) {
        textureRegion = texture;
    }

}
