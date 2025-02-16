package me.nulldoubt.advancedtilemaps.gwt;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GwtAdvTilemaps extends ApplicationAdapter {

    private static final Vector2 temp = new Vector2();

    private Texture dirt;
    private Texture dirtOverlay;
    private TileLayer dirtLayer;

    private Texture grass;
    private Texture grassOverlay;
    private TileLayer grassLayer;

    private SpriteBatch batch;
    private ShaderProgram shader;

    private Viewport worldViewport;
    private OrthographicCamera worldCamera;

    private final Vector2 cameraVelocity = new Vector2();
    private final float cameraSpeed = 21f;

    private float targetZoom;
    private final float zoomSpeed = 21f;

    private boolean _touchDown;
    private boolean _buttonRight;

    @Override
    public void create() {

        Gdx.app.setLogLevel(Application.LOG_DEBUG);

        dirt = new Texture("Dirt.png");
        dirtOverlay = new Texture("DirtOverlay.png");
        dirtOverlay.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        dirtOverlay.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        grass = new Texture("Grass.png");
        grassOverlay = new Texture("GrassOverlay.png");
        grassOverlay.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        grassOverlay.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        shader = new ShaderProgram(
            Gdx.files.internal("shaders/overlay.vert").readString(),
            Gdx.files.internal("shaders/overlay.frag").readString()
        );

        Gdx.app.log("GwtAdvTilemaps", shader.getLog());

        if (!shader.isCompiled())
            throw new RuntimeException("Unable to compile shader: " + shader.getLog());

        batch = new SpriteBatch(3000);

        worldViewport = new FitViewport(40f, 22.5f);
        worldCamera = (OrthographicCamera) worldViewport.getCamera();
        worldCamera.zoom = targetZoom = 1f / 2.5f;

        dirtLayer = new TileLayer(64, 64, 16f, 16f, 1f / 16f, true);
        dirtLayer.setTileSet(new TextureRegion(dirt));
        dirtLayer.setOverlay(dirtOverlay, shader);

        grassLayer = new TileLayer(64, 64, 16f, 16f, 1f / 16f, false);
        grassLayer.setTileSet(new TextureRegion(grass));
        grassLayer.setOverlay(grassOverlay, shader);

        Gdx.input.setInputProcessor(new InputAdapter() {

            @Override
            public boolean scrolled(float amountX, float amountY) {
                targetZoom += (amountY * 0.25f);
                targetZoom = MathUtils.clamp(targetZoom, 0.1f, 1.5f);
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (!_touchDown)
                    return false;
                handleTile(screenX, screenY);
                return true;
            }

            @Override
            public boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {
                _buttonRight = (button == Input.Buttons.RIGHT);
                _touchDown = true;
                handleTile(screenX, screenY);
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                _touchDown = false;
                return true;
            }
        });
    }

    private void handleTile(int screenX, int screenY) {
        final Vector2 touch = worldViewport.unproject(temp.set(screenX, screenY));
        grassLayer.tileAt(
            (int) (touch.x - 1f),
            (int) (touch.y - 1f),
            !_buttonRight
        );
    }

    @Override
    public void render() {
        ScreenUtils.clear(Color.BLACK);

        final float delta = Gdx.graphics.getDeltaTime();

        if (targetZoom != worldCamera.zoom)
            worldCamera.zoom = MathUtils.lerp(worldCamera.zoom, targetZoom, delta * zoomSpeed);

        final float width = (worldViewport.getWorldWidth() / 2f) * worldCamera.zoom;
        final float height = (worldViewport.getWorldHeight() / 2f) * worldCamera.zoom;

        if (Gdx.input.isKeyPressed(Input.Keys.W))
            cameraVelocity.y++;
        if (Gdx.input.isKeyPressed(Input.Keys.A))
            cameraVelocity.x--;
        if (Gdx.input.isKeyPressed(Input.Keys.S))
            cameraVelocity.y--;
        if (Gdx.input.isKeyPressed(Input.Keys.D))
            cameraVelocity.x++;
        float length = cameraVelocity.len();
        if (length > 1f)
            cameraVelocity.nor();

        worldCamera.position.x += cameraVelocity.x * cameraSpeed * delta;
        worldCamera.position.y += cameraVelocity.y * cameraSpeed * delta;
        cameraVelocity.setZero();

        worldCamera.position.set(
            MathUtils.clamp(worldCamera.position.x, width + 1f, dirtLayer.getTilesX() * dirtLayer.getTileWidth() * dirtLayer.getUnitScale() - width),
            MathUtils.clamp(worldCamera.position.y, height + 1f, dirtLayer.getTilesY() * dirtLayer.getTileHeight() * dirtLayer.getUnitScale() - height),
            0f
        );

        worldViewport.apply();
        if (length > 0.01f && _touchDown)
            handleTile(Gdx.input.getX(), Gdx.input.getY());

        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin(); // begin the batch.
        dirtLayer.setView(worldCamera);
        dirtLayer.render(batch); // first comes the dirt.
        grassLayer.setView(worldCamera);
        grassLayer.render(batch); // then comes the grass.
        batch.end(); // end the batch.
    }

    @Override
    public void resize(final int width, final int height) {
        worldViewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        dirt.dispose();
        dirtOverlay.dispose();
        grass.dispose();
        grassOverlay.dispose();
        shader.dispose();
        batch.dispose();
    }

}
