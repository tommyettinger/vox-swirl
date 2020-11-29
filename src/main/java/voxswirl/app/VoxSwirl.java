package voxswirl.app;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.Array;
import com.github.tommyettinger.anim8.*;
import voxswirl.io.LittleEndianDataInputStream;
import voxswirl.io.VoxIO;
import voxswirl.physical.Tools3D;
import voxswirl.visual.SplatRenderer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class VoxSwirl extends ApplicationAdapter {
    public static final int SCREEN_WIDTH = 512;//640;
    public static final int SCREEN_HEIGHT = 512;//720;
    private SplatRenderer renderer;
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
            inputs = new String[]{"vox/Lomuk.vox", "vox/Tree.vox", "vox/Eye_Tyrant.vox", "vox/Infantry_Firing.vox"};
//            inputs = new String[]{"vox/Lomuk.vox", "vox/Tree.vox", "vox/Eye_Tyrant.vox", "vox/IPT.vox", "vox/LAB.vox"};
//            inputs = new String[]{"vox/Infantry_Firing.vox"};
//            inputs = new String[]{"vox/IPT_No_Pow.vox"};
//            inputs = new String[]{"vox/IPT_Original.vox"};
//            inputs = new String[]{"vox/IPT.vox"};
//            inputs = new String[]{"vox/LAB.vox"};
//            inputs = new String[]{"vox/libGDX_BadLogic_Logo.vox"};
//            inputs = new String[]{"vox/libGDX_Gray.vox"};
            if(!new File(inputs[0]).exists()) 
                System.exit(0);
        }
    }
    @Override
    public void create() {
        if(inputs == null) Gdx.app.exit();
        png = new PixmapIO.PNG();
        png8 = new PNG8();
        gif = new AnimatedGif();
        apng = new AnimatedPNG();
        gif.setDitherAlgorithm(Dithered.DitherAlgorithm.SCATTER);
        png8.setDitherAlgorithm(Dithered.DitherAlgorithm.SCATTER);
        final int[] bw = new int[]{0x00000000, 0x000000FF, 0xFFFFFFFF,};
        final int[] grayscale = new int[]{0x00000000, 0x000000FF, 0x666666FF, 0xBBBBBBFF, 0xFFFFFFFF,};
        final int[] gb4 = new int[]{0x00000000, 0x081820FF, 0x346856FF, 0x88C070FF, 0xE0F8D0FF,};
        final int[] gb16 = new int[]{0x00000000,
                0x000000FF, 0x081820FF, 0x132C2DFF, 0x1E403BFF, 0x295447FF, 0x346856FF, 0x497E5BFF, 0x5E9463FF,
                0x73AA69FF, 0x88C070FF, 0x9ECE88FF, 0xB4DCA0FF, 0xCAEAB8FF, 0xE0F8D0FF, 0xEFFBE7FF, 0xFFFFFFFF, };
        final int[] az32 = new int[]{0x00000000,
                0x372B26FF, 0xC37C6BFF, 0xDD997EFF, 0x6E6550FF, 0x9A765EFF, 0xE1AD56FF, 0xC6B5A5FF, 0xE9B58CFF,
                0xEFCBB3FF, 0xF7DFAAFF, 0xFFEDD4FF, 0xBBD18AFF, 0x355525FF, 0x557A41FF, 0x112D19FF, 0x45644FFF,
                0x62966AFF, 0x86BB9AFF, 0x15452DFF, 0x396A76FF, 0x86A2B7FF, 0x92B3DBFF, 0x3D4186FF, 0x6672BFFF,
                0x15111BFF, 0x9A76BFFF, 0x925EA2FF, 0xC7A2CFFF, 0x553549FF, 0xA24D72FF, 0xC38E92FF, 0xE3A6BBFF, };
        final int[] gh63 = new int[]{
                0x00000000, 0x0B080FFF, 0xFAF7F0FF, 0x55809FFF, 0x3118ABFF, 0xACF153FF, 0x113FECFF, 0xA6C581FF,
                0xE85845FF, 0xAC8274FF, 0x4BF9B2FF, 0x27D0AAFF, 0x986F2BFF, 0x8388CCFF, 0x9C21A3FF, 0x5B9C67FF,
                0xB02C22FF, 0xCFA9FAFF, 0xB4F5DEFF, 0x9579D3FF, 0x506036FF, 0x6A222EFF, 0x23D3DAFF, 0xD29045FF,
                0x370186FF, 0xD4A0A4FF, 0x438A3EFF, 0xAFFEEFFF, 0x639CC7FF, 0x477B1BFF, 0xB46584FF, 0xAB5EB5FF,
                0xD7C99DFF, 0x7BBE79FF, 0x5D3CC0FF, 0x3BBA72FF, 0x7E5B13FF, 0x5D1259FF, 0x72804AFF, 0x5679DAFF,
                0xF3DE74FF, 0x3FABFAFF, 0xAD4097FF, 0xAD9F7AFF, 0x076C6CFF, 0x95F1D8FF, 0xA02711FF, 0xA0CF97FF,
                0x718958FF, 0xA87D7CFF, 0x53492FFF, 0x89F67EFF, 0xE58AB4FF, 0x4253ADFF, 0x818DB9FF, 0x45497EFF,
                0xCACDF3FF, 0xACD276FF, 0xB94E9EFF, 0xE5F9FAFF, 0xB0906AFF, 0x2B6C1AFF, 0xC9BAFFFF, 0x5E0D53FF, };
        gif.palette = new PaletteReducer(gh63);
        png8.palette = new PaletteReducer(gh63);
        gif.palette.setDitherStrength(0.5f);
        png8.palette.setDitherStrength(0.5f);
        for(String s : inputs)
        {
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
                    png8.write(Gdx.files.local("out/gh63_" + name + '/' + name + "_angle" + i + ".png"), p, false);
                }
                //gif.palette.setDefaultPalette();
//                gif.palette.analyze(pm, 150);
                gif.write(Gdx.files.local("out/gh63_" + name + '/' + name + ".gif"), pm, 12);
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
            renderer = new SplatRenderer(voxels.length);
            renderer.palette = VoxIO.lastPalette;
            
        } catch (FileNotFoundException e) {
            voxels = new byte[][][]{{{1}}}; 
        }
    }
}
