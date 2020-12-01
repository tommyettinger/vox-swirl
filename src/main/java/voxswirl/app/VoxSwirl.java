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
//        final int[] gh63 = new int[]{0x00000000, 0x0B080FFF, 0x353336FF, 0x555555FF, 0x797577FF, 0xAAAAAAFF, 0xC8C8C8FF, 0xE0E0E0FF, 0xFAF7F0FF, 0x0E7DE0FF, 0x5A09ADFF, 0xDBE156FF, 0x00D25EFF, 0x193DA6FF, 0xB0CC68FF, 0x301153FF, 0x99A1C2FF, 0x986F2BFF, 0xA38CA8FF, 0x353DB3FF, 0x47A75CFF, 0xB8320DFF, 0x9AA1B1FF, 0xABFECFFF, 0x9D75D5FF, 0x4E6035FF, 0x652331FF, 0x30D1D4FF, 0xCA8B59FF, 0xB70C8EFF, 0x310486FF, 0x427728FF, 0xFB4C88FF, 0x705AEFFF, 0xD7C99DFF, 0x870A06FF, 0xADB860FF, 0x5B4E93FF, 0xEED5F7FF, 0x57CC2EFF, 0xD13A21FF, 0x440198FF, 0x07A053FF, 0x3E8EB9FF, 0x60C2A7FF, 0x786071FF, 0xDFEFE5FF, 0x720207FF, 0xCC9C69FF, 0x055F8DFF, 0x7B340FFF, 0x93EA60FF, 0x6D8663FF, 0xB8972FFF, 0x625505FF, 0x6DF597FF, 0x0D233BFF, 0xB486E6FF, 0x026AA9FF, 0x3EB491FF, 0xA12C7BFF, 0x135207FF, 0xADD275FF, 0x6267A7FF, };
//mid saturation, central lightness
        final int[] gh63 = new int[]{0x00000000, 0x0B080FFF, 0x353336FF, 0x555555FF, 0x797577FF, 0xAAAAAAFF, 0xC8C8C8FF, 0xE0E0E0FF, 0xFAF7F0FF, 0x0B97A1FF, 0x86022EFF, 0xA99FA5FF, 0x2F48B2FF, 0xF94F48FF, 0xDDF468FF, 0xCBB4EBFF, 0x133D4DFF, 0x1E98D8FF, 0xA100A9FF, 0x26CD3EFF, 0xCB5C83FF, 0x61E0F8FF, 0xE9AE4CFF, 0x486A49FF, 0x4F1856FF, 0x3A76E3FF, 0x209461FF, 0x49126AFF, 0xA7A459FF, 0xA82626FF, 0x8EC0C3FF, 0xA9606DFF, 0xA74244FF, 0x1CB397FF, 0xE5ADBDFF, 0xC24295FF, 0x7EA5D2FF, 0x87EFD5FF, 0x0AA8B5FF, 0x796472FF, 0xD6D7AFFF, 0xE46794FF, 0xFCEDC1FF, 0x63CE99FF, 0x864A91FF, 0x58ECFFFF, 0x6A228AFF, 0xBA51DAFF, 0x245406FF, 0xD0C473FF, 0xAC46CDFF, 0x95AFCEFF, 0x336286FF, 0xD19282FF, 0x8AEEE4FF, 0x96C9B4FF, 0xC95A6AFF, 0xC06F82FF, 0x20306EFF, 0x7B8DBEFF, 0xCA7756FF, 0xADC48DFF, 0x5B723BFF, 0xE8917FFF, };
// disrupted sequence
        final int[] ghd63 = new int[]{
                0x00000000, 0x0B080FFF, 0x353336FF, 0x555555FF, 0x797577FF, 0xAAAAAAFF, 0xC8C8C8FF, 0xE0E0E0FF,
                0xFAF7F0FF, 0x0897A4FF, 0x880130FF, 0xD7DBD4FF, 0xAF8FC7FF, 0x134DBCFF, 0xAD6E3CFF, 0x2D344EFF,
                0x70192EFF, 0x569AA6FF, 0x502E77FF, 0xE69996FF, 0x596929FF, 0x69A668FF, 0xAA6093FF, 0xA1C9FCFF,
                0xB0A68EFF, 0x476750FF, 0x501658FF, 0x3179E2FF, 0x20945FFF, 0x63086BFF, 0x88B24FFF, 0x264D97FF,
                0x76C8C2FF, 0x8A7C41FF, 0x091D5FFF, 0x1B2800FF, 0x915528FF, 0x5D92B6FF, 0x30405AFF, 0xFEBE7FFF,
                0x943BCCFF, 0x8EA5C5FF, 0x83EFDAFF, 0x6A86BAFF, 0xC7A592FF, 0xAA5274FF, 0xBED4CAFF, 0x877DA8FF,
                0xFCEDC1FF, 0x5B4C21FF, 0xABB2A4FF, 0x6047B8FF, 0x7FFFC6FF, 0x8BDEF9FF, 0x782C66FF, 0x6C6CD6FF,
                0x3E4614FF, 0xAE6287FF, 0x5FCDB0FF, 0x495B87FF, 0xBD948EFF, 0x8DF0DBFF, 0x78D3B2FF, 0x837D4DFF,
        };
        final int[] ghr63 = new int[]{
                0x00000000, 0x0B080FFF, 0x353336FF, 0x555555FF, 0x797577FF, 0xAAAAAAFF, 0xC8C8C8FF, 0xE0E0E0FF,
                0xFAF7F0FF, 0xA01948FF, 0x18CA91FF, 0x076C12FF, 0xD1B8E1FF, 0xC768D7FF, 0x8F6730FF, 0x44BF74FF,
                0x76450FFF, 0x4569B5FF, 0x1A0635FF, 0xC5706FFF, 0x007A43FF, 0x98C5B6FF, 0x9B39B2FF, 0x9CF1EFFF,
                0x48262FFF, 0xFAB8C0FF, 0x0A2815FF, 0x275ECCFF, 0x6A1E7DFF, 0x778E35FF, 0xBD3819FF, 0x9BE1DEFF,
                0x8C0F0AFF, 0xACB1A0FF, 0xA394C4FF, 0x68070CFF, 0x507394FF, 0x1B5264FF, 0xCB8C55FF, 0x8534B8FF,
                0xB6C6E4FF, 0x261286FF, 0x37D5E2FF, 0x0A3744FF, 0xE758CBFF, 0xBF3C6BFF, 0xA1A75EFF, 0x945B87FF,
                0xB9AA7FFF, 0x8F3D3FFF, 0xC0D4C2FF, 0x1D0372FF, 0xF277C4FF, 0x41C18CFF, 0xA7EFFCFF, 0xC52EC5FF,
                0x525124FF, 0xE46884FF, 0x0F1475FF, 0x6072D6FF, 0xCDBDC1FF, 0x25226CFF, 0xCBDBD1FF, 0x7A0C28FF,
        };
        final int[] gh127 = new int[]{
                0x00000000, 0x0B080FFF, 0x353336FF, 0x555555FF, 0x797577FF, 0xAAAAAAFF, 0xC8C8C8FF, 0xE0E0E0FF,
                0xFAF7F0FF, 0x0897A4FF, 0x880130FF, 0xD7DBD4FF, 0xAF8FC7FF, 0x134DBCFF, 0xAD6E3CFF, 0x2D344EFF,
                0x70192EFF, 0x569AA6FF, 0x502E77FF, 0xE69996FF, 0x596929FF, 0x69A668FF, 0xAA6093FF, 0xA1C9FCFF,
                0xB0A68EFF, 0x476750FF, 0x501658FF, 0x3179E2FF, 0x20945FFF, 0x63086BFF, 0x88B24FFF, 0x264D97FF,
                0x76C8C2FF, 0x8A7C41FF, 0x091D5FFF, 0x1B2800FF, 0x915528FF, 0x5D92B6FF, 0x30405AFF, 0xFEBE7FFF,
                0x943BCCFF, 0x8EA5C5FF, 0x83EFDAFF, 0x6A86BAFF, 0xC7A592FF, 0xAA5274FF, 0xBED4CAFF, 0x877DA8FF,
                0xFCEDC1FF, 0x5B4C21FF, 0xABB2A4FF, 0x6047B8FF, 0x7FFFC6FF, 0x8BDEF9FF, 0x782C66FF, 0x6C6CD6FF,
                0x3E4614FF, 0xAE6287FF, 0x5FCDB0FF, 0x495B87FF, 0xBD948EFF, 0x8DF0DBFF, 0x78D3B2FF, 0x837D4DFF,
                0xA0885FFF, 0x1D375FFF, 0x4FA1B1FF, 0xAB727CFF, 0xB6CE6EFF, 0x60762EFF, 0xC3B446FF, 0x79482DFF,
                0x7A8A68FF, 0x5B68C3FF, 0xBC8E5BFF, 0x654E10FF, 0x720635FF, 0x6F6F3FFF, 0x94B566FF, 0x3B407BFF,
                0x3F9597FF, 0x19613BFF, 0x608A63FF, 0xD59CDFFF, 0x3F0B64FF, 0x8E95A9FF, 0x614B53FF, 0x82E380FF,
                0xD1BAF5FF, 0x356AC4FF, 0x63F5DFFF, 0x68211AFF, 0x6E9866FF, 0x501546FF, 0xCEA196FF, 0xBD5534FF,
                0xC6D2E9FF, 0x3088A0FF, 0xF6C0EBFF, 0xC8F078FF, 0x661293FF, 0xB01125FF, 0x2B735CFF, 0x94AAC5FF,
                0xA12F97FF, 0x9BAB52FF, 0xE699B4FF, 0xAE497CFF, 0x9870C8FF, 0x6D748AFF, 0xD788E6FF, 0xA35631FF,
                0xAFEEBCFF, 0x834A74FF, 0xA684BEFF, 0xD43864FF, 0x2AD5FBFF, 0x1276C7FF, 0xBA7865FF, 0x65003EFF,
                0x458311FF, 0xF5886CFF, 0x4F502EFF, 0x94124EFF, 0x8ACECBFF, 0xB77242FF, 0xF1AC82FF, 0x2F084EFF,
        };
        final int[] gh255 = new int[]{
                0x00000000, 0x0B080FFF, 0x353336FF, 0x555555FF, 0x797577FF, 0xAAAAAAFF, 0xC8C8C8FF, 0xE0E0E0FF,
                0xFAF7F0FF, 0x0B97A1FF, 0x86022EFF, 0xAA9EA7FF, 0x2D48B2FF, 0xFB4F48FF, 0xB3DACDFF, 0xCCB3EDFF,
                0x0F3F4DFF, 0x1798DFFF, 0xA000ABFF, 0x2CD131FF, 0xCD568FFF, 0x55E3FBFF, 0xFFA945FF, 0x48752DFF,
                0x780368FF, 0x1FA781FF, 0x1C985AFF, 0xCCBB59FF, 0x30547FFF, 0x78CCBCFF, 0x6B056CFF, 0x86B34DFF,
                0xB83100FF, 0x7FC5C2FF, 0xD85B55FF, 0x061B67FF, 0x9D5B0EFF, 0x479EA9FF, 0x183685FF, 0xD8DADAFF,
                0x243009FF, 0xF1B5A0FF, 0x5C6399FF, 0x8BAEB1FF, 0x350639FF, 0x99E7DBFF, 0x4195B8FF, 0x346706FF,
                0xE99B90FF, 0xAA5274FF, 0xB3D9C6FF, 0x213022FF, 0xB467BBFF, 0xFCEDC1FF, 0x61530BFF, 0x87C396FF,
                0x6145BBFF, 0x7FFFC6FF, 0x97DFECFF, 0x7B2E5EFF, 0xD647DCFF, 0xB44EB3FF, 0x5EB1F5FF, 0x87CEB3FF,
                0xD4624EFF, 0xE16483FF, 0x24433DFF, 0x3795E2FF, 0xD97256FF, 0xB5BAA2FF, 0x6B7D14FF, 0xBE9596FF,
                0xAC2F41FF, 0x48A944FF, 0x456A82FF, 0x6B807BFF, 0xA8818CFF, 0x8F4308FF, 0x210D65FF, 0x61685CFF,
                0x63B787FF, 0x723960FF, 0x0E86E4FF, 0x19613BFF, 0xF6BEB0FF, 0x15AE47FF, 0x390E62FF, 0x9096A4FF,
                0x8BD796FF, 0x1F819EFF, 0x68F9D2FF, 0xA4052EFF, 0x59A84DFF, 0x501546FF, 0x1A2F78FF, 0xC6484DFF,
                0x2688A9FF, 0xE7C5EAFF, 0xAFE0B4FF, 0xA0155AFF, 0xB01125FF, 0xE8A265FF, 0x726F2BFF, 0x7CA5E6FF,
                0xB126A0FF, 0x74B94FFF, 0xF1E48FFF, 0xE695C0FF, 0xBC437EFF, 0x8F849EFF, 0x2979B5FF, 0xD788E6FF,
                0x6C5F48FF, 0x98F7B9FF, 0x834386FF, 0xC776C5FF, 0x254B4FFF, 0x8C672BFF, 0xAEB4E0FF, 0x1477C3FF,
                0xA38063FF, 0x070678FF, 0x597C13FF, 0xE0A634FF, 0x535C0DFF, 0x981050FF, 0x82785EFF, 0xE0BB6AFF,
                0xC370BFFF, 0x2E4D62FF, 0xEC99B8FF, 0x1F1600FF, 0x78F198FF, 0x0679AFFF, 0x756DD7FF, 0x18A84AFF,
                0x7BB341FF, 0xD6EEF7FF, 0xA71F7AFF, 0xDCC562FF, 0x5F843CFF, 0xD3FFCAFF, 0x018112FF, 0xA56AC3FF,
                0x552E52FF, 0x59DCE4FF, 0x0F69AEFF, 0xBB7DE6FF, 0x520010FF, 0x94FEC8FF, 0x390B0FFF, 0xE7A8FAFF,
                0x6477A5FF, 0x809E58FF, 0x85391EFF, 0x71BF71FF, 0xFA9BCEFF, 0x309E08FF, 0x13C5BFFF, 0xB459D1FF,
                0x249D46FF, 0xFFDA6BFF, 0x6494C3FF, 0x6C3A6FFF, 0x4C4CD1FF, 0x8AAF92FF, 0xA54235FF, 0x86F760FF,
                0x013139FF, 0x369D7AFF, 0x6FAE9AFF, 0x9E376DFF, 0x131F2BFF, 0x9CEDBEFF, 0xD77C28FF, 0x65F4D4FF,
                0x163B55FF, 0x9163BFFF, 0xF0D4DCFF, 0x5E283BFF, 0x5AA0F0FF, 0x416270FF, 0x2CA04AFF, 0xDEBFF8FF,
                0xC7D4B1FF, 0x4D4717FF, 0x242E41FF, 0x5EA5AAFF, 0x624F50FF, 0xA0ADA6FF, 0x217051FF, 0x9484AAFF,
                0x6FD36DFF, 0x2F5B9CFF, 0x989A4EFF, 0xEC5367FF, 0x5CB5D0FF, 0x5C4F72FF, 0xEBB6D4FF, 0x046242FF,
                0x27BD8CFF, 0x960B85FF, 0x95553FFF, 0x6F7B63FF, 0x4C1658FF, 0xFDE3DFFF, 0x5B98B4FF, 0x7E0D9FFF,
                0xA10833FF, 0x82A9D6FF, 0xBAF5E2FF, 0x241823FF, 0xD78A65FF, 0x74462DFF, 0x64B3D1FF, 0x5C824BFF,
                0x804061FF, 0x3791BFFF, 0x592E2DFF, 0x186C1EFF, 0x6FBFBDFF, 0x9D3EA9FF, 0xD5E5F4FF, 0x3E416EFF,
                0x28C834FF, 0x285509FF, 0xA5BDACFF, 0xCB4956FF, 0x62C277FF, 0x030828FF, 0x65FF72FF, 0xEF6941FF,
                0x1C8FC2FF, 0x562F53FF, 0xF29F42FF, 0xFAA99FFF, 0xAF32A2FF, 0x1FB089FF, 0xB4AA73FF, 0x469090FF,
                0x9E44A9FF, 0xB1C1DFFF, 0xFC6E8DFF, 0x2A476AFF, 0xC6F8FFFF, 0x7F101EFF, 0x747144FF, 0xC376E8FF,
                0xCF925BFF, 0xB83E39FF, 0x92EB9EFF, 0x089994FF, 0xF09BDCFF, 0x0E4117FF, 0x39251DFF, 0x1C1059FF,
        };

        gif.palette = new PaletteReducer(ghr63);
        png8.palette = new PaletteReducer(ghr63);
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
                    png8.write(Gdx.files.local("out/ghr63_" + name + '/' + name + "_angle" + i + ".png"), p, false);
                }
                //gif.palette.setDefaultPalette();
//                gif.palette.analyze(pm, 150);
                gif.write(Gdx.files.local("out/ghr63_" + name + '/' + name + ".gif"), pm, 12);
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
