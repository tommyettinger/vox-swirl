package voxswirl.app;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.Array;
import com.github.tommyettinger.anim8.*;
import voxswirl.io.LittleEndianDataInputStream;
import voxswirl.io.VoxIO;
import voxswirl.physical.Tools3D;
import voxswirl.visual.Coloring;
import voxswirl.visual.NextRenderer;
import voxswirl.visual.SplatRenderer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class VoxSwirl extends ApplicationAdapter {
    public static final int SCREEN_WIDTH = 512;//640;
    public static final int SCREEN_HEIGHT = 512;//720;
    private NextRenderer renderer;
    private byte[][][] voxels;
    private String name;
    private String[] inputs;
    private PixmapIO.PNG png;
    private AnimatedGif gif;
    private PNG8 png8;
    private AnimatedPNG apng;
    public VoxSwirl(String[] args){
        if(args != null && args.length > 0)
            inputs = args;
        else 
        {
            System.out.println("INVALID ARGUMENTS. Please supply space-separated absolute paths to .vox models, or use the .bat file.");
            inputs = new String[]{"vox/Eye_Tyrant.vox", "vox/Infantry_Firing.vox", "vox/Lomuk.vox", "vox/Tree.vox", "vox/Bear.vox", "vox/libGDX_BadLogic_Half.vox"};
//            inputs = new String[]{"vox/Eye_Tyrant.vox", "vox/Infantry_Firing.vox", "vox/Lomuk.vox", "vox/Tree.vox", "vox/LAB.vox"};
//            inputs = new String[]{"vox/Lomuk.vox", "vox/Tree.vox", "vox/Eye_Tyrant.vox", "vox/IPT.vox", "vox/LAB.vox"};
//            inputs = new String[]{"vox/Infantry_Firing.vox"};
//            inputs = new String[]{"vox/IPT_No_Pow.vox"};
//            inputs = new String[]{"vox/Box.vox", "vox/Direction_Cube.vox"};
//            inputs = new String[]{"vox/IPT_Original.vox"};
            inputs = new String[]{"vox/IPT.vox"};
//            inputs = new String[]{"vox/LAB.vox"};
//            inputs = new String[]{"vox/Bear.vox"};
//            inputs = new String[]{"vox/libGDX_BadLogic_Logo.vox"};
//            inputs = new String[]{"vox/libGDX_Gray.vox"};
            if(!new File(inputs[0]).exists()) 
                System.exit(0);
        }
    }
    @Override
    public void create() {
        if (inputs == null) Gdx.app.exit();
        Gdx.files.local("out/vox/").mkdirs();
        png = new PixmapIO.PNG();
        png8 = new PNG8();
        gif = new AnimatedGif();
        apng = new AnimatedPNG();
        gif.setDitherAlgorithm(Dithered.DitherAlgorithm.SCATTER);
        png8.setDitherAlgorithm(Dithered.DitherAlgorithm.SCATTER);
        png8.palette = gif.palette = new PaletteReducer(Coloring.HALTONIC255);
        gif.palette.setDitherStrength(0.75f);
        for (String s : inputs) {
            load(s);
            try {
                Pixmap pixmap;
                Array<Pixmap> pm = new Array<>(64);
                for (int i = 0; i < 64; i++) {
                    pixmap = renderer.drawSplats(voxels, (i & 63) * 0x1p-6f, VoxIO.lastMaterials);
                    Pixmap p = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), pixmap.getFormat());
                    p.drawPixmap(pixmap, 0, 0);
                    pm.add(p);
                    png.write(Gdx.files.local("out/" + name + '/' + name + "_angle" + i + ".png"), p);
                    for (int colorCount : new int[]{3, 8, 32, 64, 86, 128, 256}) {
                        png8.palette.exact(Coloring.HALTONIC255, colorCount);
                        png8.write(Gdx.files.local("out/lowColor/" + colorCount + "/" + name + '/' + name + "_angle" + i + ".png"), p, false);
                    }
                    VoxIO.writeVOX("out/vox/" + s.substring(4, s.length() - 4) + "_angle"+i+".vox", renderer.remade, VoxIO.lastPalette);
                }
                for (int colorCount : new int[]{3, 8, 32, 64, 86, 128, 256}) {
                    gif.palette.exact(Coloring.HALTONIC255, colorCount);
                    gif.write(Gdx.files.local("out/lowColor/" + colorCount + "/" + name + '/' + name + ".gif"), pm, 12);
                }
                apng.write(Gdx.files.local("out/" + name + '/' + name + ".png"), pm, 12);
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
        config.disableAudio(true);
        final VoxSwirl app = new VoxSwirl(arg);
        new Lwjgl3Application(app, config);
    }

    public void load(String name) {
        try {
            //// loads a file by its full path, which we get via a command-line arg
            voxels = VoxIO.readVox(new LittleEndianDataInputStream(new FileInputStream(name)));
            if(voxels == null) {
                voxels = new byte[][][]{{{1}}};
                return;
            }
            Tools3D.soakInPlace(voxels);
            int nameStart = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1;
            this.name = name.substring(nameStart, name.indexOf('.', nameStart));
            renderer = new NextRenderer(voxels.length, 48);
            renderer.palette(VoxIO.lastPalette);
            
        } catch (FileNotFoundException e) {
            voxels = new byte[][][]{{{1}}}; 
        }
    }
}
