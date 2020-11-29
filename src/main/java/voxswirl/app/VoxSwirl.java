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
//more saturated
//        final int[] gh63 = new int[]{0x00000000, 0x0B080FFF, 0x353336FF, 0x555555FF, 0x797577FF, 0xAAAAAAFF, 0xC8C8C8FF, 0xE0E0E0FF, 0xFAF7F0FF, 0x507FA5FF, 0x3118ABFF, 0xB2EF53FF, 0x1D3CE9FF, 0x15E420FF, 0xA4C387FF, 0xC9595CFF, 0x986F2BFF, 0xEF157FFF, 0xFFB485FF, 0x3D6BC3FF, 0xE34A38FF, 0x031220FF, 0xF762BBFF, 0xB87E86FF, 0x1726CFFF, 0x75DFE0FF, 0x061383FF, 0x2F9E1DFF, 0x7D93C9FF, 0x366F47FF, 0xD7C99DFF, 0x3BD76FFF, 0xDD0EC9FF, 0x3BB974FF, 0x601451FF, 0x104D6DFF, 0x7E43B4FF, 0xC1F9E3FF, 0x2E5E6EFF, 0xC01C13FF, 0xD8E632FF, 0x94A002FF, 0xC23011FF, 0x9BFC60FF, 0xA393D3FF, 0x62000DFF, 0x352CD4FF, 0xDBC7F4FF, 0xABE34DFF, 0xD04C91FF, 0x78025AFF, 0x4DE571FF, 0x7B7E36FF, 0x999E9FFF, 0xE15603FF, 0xB7F3C8FF, 0x3C859FFF, 0x57A34BFF, 0x602665FF, 0xE44142FF, 0x8AF2FAFF, 0xB67AA7FF, 0x396235FF, 0xACDECCFF, };
//less saturated
        final int[] gh63 = new int[]{0x00000000, 0x0B080FFF, 0x353336FF, 0x555555FF, 0x797577FF, 0xAAAAAAFF, 0xC8C8C8FF, 0xE0E0E0FF, 0xFAF7F0FF, 0x0E7DE0FF, 0x5A09ADFF, 0xDBE156FF, 0x00D25EFF, 0x193DA6FF, 0xB0CC68FF, 0x301153FF, 0x99A1C2FF, 0x986F2BFF, 0xA38CA8FF, 0x353DB3FF, 0x47A75CFF, 0xB8320DFF, 0x9AA1B1FF, 0xABFECFFF, 0x9D75D5FF, 0x4E6035FF, 0x652331FF, 0x30D1D4FF, 0xCA8B59FF, 0xB70C8EFF, 0x310486FF, 0x427728FF, 0xFB4C88FF, 0x705AEFFF, 0xD7C99DFF, 0x870A06FF, 0xADB860FF, 0x5B4E93FF, 0xEED5F7FF, 0x57CC2EFF, 0xD13A21FF, 0x440198FF, 0x07A053FF, 0x3E8EB9FF, 0x60C2A7FF, 0x786071FF, 0xDFEFE5FF, 0x720207FF, 0xCC9C69FF, 0x055F8DFF, 0x7B340FFF, 0x93EA60FF, 0x6D8663FF, 0xB8972FFF, 0x625505FF, 0x6DF597FF, 0x0D233BFF, 0xB486E6FF, 0x026AA9FF, 0x3EB491FF, 0xA12C7BFF, 0x135207FF, 0xADD275FF, 0x6267A7FF, };
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
