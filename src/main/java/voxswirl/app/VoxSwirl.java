package voxswirl.app;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.viewport.Viewport;
import voxswirl.io.LittleEndianDataInputStream;
import voxswirl.io.VoxIO;
import voxswirl.physical.ModelMaker;
import voxswirl.visual.Colorizer;
import voxswirl.visual.SplatRenderer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class VoxSwirl extends ApplicationAdapter {
    public static final int SCREEN_WIDTH = 512;//640;
    public static final int SCREEN_HEIGHT = 512;//720;
    public static final int VIRTUAL_WIDTH = SCREEN_WIDTH;
    public static final int VIRTUAL_HEIGHT = SCREEN_HEIGHT;
    protected SpriteBatch batch;
    protected Viewport worldView;
    protected Viewport screenView;
    protected BitmapFont font;
    protected FrameBuffer buffer;
    protected Texture screenTexture, pmTexture;
    protected ModelMaker maker;
    private SplatRenderer renderer;
    private byte[][][] voxels;
    private Colorizer colorizer;
    private String name;
    private String[] inputs;
    private PixmapIO.PNG png;
    public VoxSwirl(String[] args){
        if(args != null && args.length > 0)
            inputs = args;
        else 
        {
            System.out.println("INVALID ARGUMENTS. Please supply space-separated absolute paths to .vox models, or use the .bat file.");
            inputs = new String[]{"D:/Tree.vox"};
        }
    }
    @Override
    public void create() {
        if(inputs == null) Gdx.app.exit();
        png = new PixmapIO.PNG();
        renderer = new SplatRenderer(80);
//        renderer.dither = true;
        maker = new ModelMaker(-1L, colorizer);
        for(String s : inputs)
        {
            load(s);
            try {
                for (int i = 0; i < 32; i++) {
                    png.write(Gdx.files.local("out/" + name + '/' + name + "_angle" + i + ".png"), renderer.drawSplats(voxels, i * 0x1p-5f));
                }
//                png.write(Gdx.files.local("out/" + name + '/' + name + "_SW" + ".png"), renderer.drawSplats(voxels));
//                Tools3D.clockwiseInPlace(voxels);
//                png.write(Gdx.files.local("out/" + name + '/' + name + "_NW" + ".png"), renderer.drawSplats(voxels));
//                Tools3D.clockwiseInPlace(voxels);
//                png.write(Gdx.files.local("out/" + name + '/' + name + "_NE" + ".png"), renderer.drawSplats(voxels));
//                Tools3D.clockwiseInPlace(voxels);
//                png.write(Gdx.files.local("out/" + name + '/' + name + "_SE" + ".png"), renderer.drawSplats(voxels));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Gdx.app.exit();
    }

    @Override
    public void render() {
    }


    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Writing Test");
        config.setWindowedMode(SCREEN_WIDTH, SCREEN_HEIGHT);
        config.setIdleFPS(10);
        config.useVsync(true);
        config.setResizable(false);
        final VoxSwirl app = new VoxSwirl(arg);
        new Lwjgl3Application(app, config);
    }

    public void load(String name) {
        try {
            //// loads a file by its full path, which we get via drag+drop
            voxels = VoxIO.readVox(new LittleEndianDataInputStream(new FileInputStream(name)));
            if(voxels == null) {
                voxels = maker.shipSmoothColorized();
                return;
            }
            int nameStart = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1;
            this.name = name.substring(nameStart, name.indexOf('.', nameStart));
            renderer = new SplatRenderer(voxels.length);
            renderer.colorizer(Colorizer.arbitraryColorizer(VoxIO.lastPalette));
            //Tools3D.clockwiseInPlace(voxels);
            //VoxIO.writeVOX(name + ".vox", voxels, maker.getColorizer().getReducer().paletteArray);
            
        } catch (FileNotFoundException e) {
            voxels = maker.shipSmoothColorized();
            renderer.colorizer(colorizer);
        }
    }
}
