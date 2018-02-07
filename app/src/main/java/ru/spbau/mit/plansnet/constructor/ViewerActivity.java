package ru.spbau.mit.plansnet.constructor;

import android.view.View;
import android.widget.TextView;

import org.andengine.engine.camera.Camera;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.input.touch.TouchEvent;

import java.util.concurrent.Semaphore;

import ru.spbau.mit.plansnet.R;

public class ViewerActivity extends BaseConstructorActivity {

    private void createBorder(Scene pScene) {
        Line[] border = {
                new Line(0, 0, 0, GRID_SIZE * GRID_ROWS,
                        3, getVertexBufferObjectManager()),
                new Line(GRID_SIZE * GRID_COLS, 0,
                        GRID_SIZE * GRID_COLS, GRID_SIZE * GRID_ROWS,
                        3, getVertexBufferObjectManager()),
                new Line(0, 0, GRID_SIZE * GRID_COLS, 0,
                        3, getVertexBufferObjectManager()),
                new Line(0, GRID_SIZE * GRID_ROWS,
                        GRID_SIZE * GRID_COLS, GRID_SIZE * GRID_ROWS,
                        3, getVertexBufferObjectManager())
        };
        for (Line line : border) {
            line.setColor(0.7f, 0.7f, 0.7f);
            pScene.attachChild(line);
        }
    }

    private void initScene(Scene pScene) {
        pScene.setOnSceneTouchListener(new IOnSceneTouchListener() {

            private float mInitialTouchX;
            private float mInitialTouchY;

            @Override
            public boolean onSceneTouchEvent(final Scene pScene, TouchEvent pSceneTouchEvent) {
                mPinchZoomDetector.onTouchEvent(pSceneTouchEvent);
                if (pSceneTouchEvent.isActionDown()) {
                    RoomSprite room = map.getRoomTouched(pSceneTouchEvent);
                    if (room != null) {
                        showParams(room);
                    }
                    mInitialTouchX = pSceneTouchEvent.getX();
                    mInitialTouchY = pSceneTouchEvent.getY();
                } else if (pSceneTouchEvent.isActionMove()){
                    final float touchOffsetX = mInitialTouchX - pSceneTouchEvent.getX();
                    final float touchOffsetY = mInitialTouchY - pSceneTouchEvent.getY();
                    Camera mCamera = getEngine().getCamera();
                    mCamera.setCenter(mCamera.getCenterX() + touchOffsetX,
                            mCamera.getCenterY() + touchOffsetY);
                }
                return false;
            }});
        pScene.setTouchAreaBindingOnActionDownEnabled(true);
        pScene.setTouchAreaBindingOnActionMoveEnabled(true);
        pScene.setOnSceneTouchListenerBindingOnActionDownEnabled(true);
    }


    @Override
    protected Scene onCreateScene() {
        final Scene scene = new Scene();
        scene.setBackground(new Background(0.95f, 0.95f, 1f));
        createBorder(scene);
        initScene(scene);
        initPinchZoomDetector();
        initSprites();
        initMap(scene);
        scene.sortChildren();
        return scene;
    }

    @Override
    protected int getLayoutID() {
        return R.layout.activity_viewer;
    }

    @Override
    protected int getRenderSurfaceViewID() {
        return R.id.viewerView;
    }

    public void showParams(RoomSprite pRoom) {
        Semaphore mutex = new Semaphore(1);
        runOnUiThread(() -> {
            findViewById(R.id.viewerView).setEnabled(false);
            View paramsView = findViewById(R.id.roomPropertiesView);
            paramsView.setVisibility(View.VISIBLE);
            TextView roomName = findViewById(R.id.roomPropertiesName);
            TextView roomDescription = findViewById(R.id.roomPropertiesDescription);
            roomName.setText(pRoom.getTitle());
            roomDescription.setText(pRoom.getDescription());
            paramsView.findViewById(R.id.roomPropertiesOk).setOnClickListener(v1 -> {
                paramsView.setVisibility(View.GONE);
                findViewById(R.id.viewerView).setEnabled(true);
            });
        });
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}