package voxswirl;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import voxswirl.io.LittleEndianDataInputStream;
import voxswirl.io.VoxIO;
import voxswirl.physical.Tools3D;
import voxswirl.physical.VoxMaterial;
import voxswirl.visual.Coloring;
import voxswirl.visual.NextRenderer;
import voxswirl.visual.SplatRenderer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class NextVisualizer extends ApplicationAdapter {
    public static final int SCREEN_WIDTH = 800;//640;
    public static final int SCREEN_HEIGHT = 800;//720;
    public static final int VIRTUAL_WIDTH = SCREEN_WIDTH;
    public static final int VIRTUAL_HEIGHT = SCREEN_HEIGHT;
    protected SpriteBatch batch;
    protected Viewport worldView;
    protected Viewport screenView;
    protected FrameBuffer buffer;
    protected Texture screenTexture, pmTexture;
    private NextRenderer renderer;
    private byte[][][] voxels;
    private float saturation;
    private int[] colorCounts = {3, 8, 32, 64, 86, 128, 256};
    private int countIndex = 6;
    private float time;
    private boolean play = true;
    
    @Override
    public void create() {
        batch = new SpriteBatch();
        saturation = 0f;
        time = (TimeUtils.millis() & 4095) * 0x1p-12f;
        worldView = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        screenView = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        buffer = new FrameBuffer(Pixmap.Format.RGBA8888, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, false, false);
        screenView.getCamera().position.set(VIRTUAL_WIDTH / 2, VIRTUAL_HEIGHT / 2, 0);
        screenView.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.enableBlending();
        pmTexture = new Texture(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, Pixmap.Format.RGBA8888);
//        load("vox/Tree.vox");
//        load("vox/IPT_No_Pow.vox");
//        load("vox/libGDX_BadLogic_Logo.vox");
        load("vox/Infantry_Firing.vox");
//        load("vox/Lomuk.vox");
//        load("vox/CrazyBox.vox");
//        renderer.dither = true;

        for(IntMap.Entry<VoxMaterial> m : VoxIO.lastMaterials){
            if(m.value.type != VoxMaterial.MaterialType._diffuse){
                System.out.println(m.key + ": " + m.value);
            }
        }

        Gdx.input.setInputProcessor(inputProcessor());
    }

    @Override
    public void render() {
//        model.setFrame((int)(TimeUtils.millis() >>> 7) & 15);
//        boom.setFrame((int)(TimeUtils.millis() >>> 7) & 15);
        if(play) time = (TimeUtils.millis() & 4095) * 0x1p-12f;
        buffer.begin();
        
        Gdx.gl.glClearColor(0.4f, 0.75f, 0.3f, 1f);
        // for GB_GREEN palette
//        Gdx.gl.glClearColor(0xE0 / 255f, 0xF8 / 255f, 0xD0 / 255f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        worldView.apply();
        worldView.getCamera().position.set(VIRTUAL_WIDTH / 2, VIRTUAL_HEIGHT / 2, 0);
        worldView.update(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        batch.setProjectionMatrix(screenView.getCamera().combined);
        batch.begin();
        pmTexture.draw(renderer.drawSplats(voxels, time, VoxIO.lastMaterials), 0, 0);
        batch.draw(pmTexture,
                0,
                0);
        //batch.setColor(-0x1.fffffep126f); // white as a packed float, resets any color changes that the renderer made
        batch.end();
        buffer.end();
        Gdx.gl.glClearColor(0, 0, 0, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        screenView.apply();
        batch.setProjectionMatrix(screenView.getCamera().combined);
        batch.begin();
        screenTexture = buffer.getColorBufferTexture();
        screenTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        batch.draw(screenTexture, 0, 0);
//        font.setColor(1f, 1f, 1f, 1f);
//        font.draw(batch, Gdx.graphics.getFramesPerSecond() + " FPS", 0, 20);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        screenView.update(width, height);
    }

    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Splat Drawing Test");
        config.setWindowedMode(SCREEN_WIDTH, SCREEN_HEIGHT);
        config.setIdleFPS(10);
        config.useVsync(false);
        config.setResizable(true);
        config.disableAudio(true);
        final NextVisualizer app = new NextVisualizer();
        config.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public void filesDropped(String[] files) {
                if (files != null && files.length > 0) {
                    if (files[0].endsWith(".vox"))
                        app.load(files[0]);
                }
            }
        });
        new Lwjgl3Application(app, config);
    }

    public InputProcessor inputProcessor() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                    case Input.Keys.D: // dither
                        renderer.dither = !renderer.dither;
                        break;
                    case Input.Keys.F: // fringe, affects outline/edge
                        renderer.outline = !renderer.outline;
                        break;
                    case Input.Keys.UP:
                        renderer.saturation(saturation = Math.min(1f, saturation + 0.01f));
                        System.out.println(saturation);
                        break;
                    case Input.Keys.DOWN:
                        renderer.saturation(saturation = Math.max(-1f, saturation - 0.01f));
                        System.out.println(saturation);
                        break;
                    case Input.Keys.LEFT:
                        if(renderer.dither){
                            countIndex = (countIndex + 6) % 7;
                            renderer.reducer.exact(Coloring.HALTONIC255, colorCounts[countIndex]);
                        }
                        break;
                    case Input.Keys.RIGHT:
                        if(renderer.dither){
                            countIndex = (countIndex + 1) % 7;
                            renderer.reducer.exact(Coloring.HALTONIC255, colorCounts[countIndex]);
                        }
                        break;
                    case Input.Keys.SPACE:
                        play = !play;
                        break;
                    case Input.Keys.ESCAPE:
                        Gdx.app.exit();
                        break;
                }
                return true;
            }
        };
    }
    public void load(String name) {
        if(pmTexture != null) pmTexture.dispose();
        pmTexture = new Texture(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, Pixmap.Format.RGBA8888);
        try {
            //// loads a file by its full path, which we get via drag+drop
            byte[][][] v = VoxIO.readVox(new LittleEndianDataInputStream(new FileInputStream(name)));
            if(v == null) {
                voxels = new byte[][][]{{{1}}};
                return;
            }
            Tools3D.soakInPlace(v);
//            voxels = new byte[v.length * 3 >> 1][v.length * 3 >> 1][v.length * 3 >> 1];
//            Tools3D.translateCopyInto(v, voxels, v.length >> 2, v.length >> 2, 0);
            voxels = v;
            renderer = new NextRenderer(voxels.length);
            renderer.palette(VoxIO.lastPalette);
        } catch (FileNotFoundException e) {
            voxels = new byte[][][]{{{1}}};
        }
    }
}
