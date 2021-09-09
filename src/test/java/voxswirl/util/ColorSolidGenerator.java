package voxswirl.util;

import voxswirl.io.VoxIO;

public class ColorSolidGenerator {
    private static final int[] OKLAB = {
            0x00007D7D, 0x00007D7D, 0x00128181, 0x00248181, 0x00368181, 0x00498181, 0x005B8181, 0x006D8181,
            0x007F8181, 0x00928181, 0x00A48181, 0x00B68181, 0x00C98181, 0x00DB8181, 0x00ED8181, 0x00FF7D7D,
            0x00418A84, 0x006E8A84, 0x009C8A84, 0x00CA8A84, 0x00478487, 0x00758487, 0x00A48487, 0x00D28487,
            0x00488487, 0x00778487, 0x00A58487, 0x00D48487, 0x004C8187, 0x007C8187, 0x00AB8187, 0x00DB8187,
            0x00577A8A, 0x00877A8A, 0x00B67A8A, 0x00E57A8A, 0x0055748A, 0x0084748A, 0x00B3748A, 0x00E2748A,
            0x004D7087, 0x00757087, 0x009D7087, 0x00C57087, 0x0053747A, 0x0082747A, 0x00B2747A, 0x00E2747A,
            0x00397A6D, 0x00687A6D, 0x00977A6D, 0x00C67A6D, 0x003B876D, 0x006A876D, 0x0098876D, 0x00C7876D,
            0x00408A70, 0x006F8A70, 0x009D8A70, 0x00CC8A70, 0x00458E74, 0x00748E74, 0x00A38E74, 0x00D28E74,
            0x00679E91, 0x008F9E91, 0x00B69E91, 0x006D9494, 0x00969494, 0x00BF9494, 0x00719194, 0x009A9194,
            0x00C49194, 0x00748E94, 0x009D8E94, 0x00C58E94, 0x00778A97, 0x00A08A97, 0x00C98A97, 0x007C8797,
            0x00A58797, 0x00CF8797, 0x00818197, 0x00AC8197, 0x00D68197, 0x008D779B, 0x00B7779B, 0x00E1779B,
            0x00916D9E, 0x00BA6D9E, 0x00E46D9E, 0x008B6A9B, 0x00B66A9B, 0x00E06A9B, 0x0088639B, 0x00B2639B,
            0x00DC639B, 0x00845A9B, 0x00AE5A9B, 0x00D85A9B, 0x0083608A, 0x00A3608A, 0x00C3608A, 0x007F637D,
            0x009B637D, 0x00B7637D, 0x00826A70, 0x00AB6A70, 0x00D56A70, 0x006E7463, 0x00977463, 0x00C07463,
            0x0051814D, 0x0079814D, 0x00A1814D, 0x00568E53, 0x007F8E53, 0x00A98E53, 0x005D9756, 0x00869756,
            0x00AE9756, 0x00609B5A, 0x008A9B5A, 0x00B39B5A, 0x0065A15D, 0x008EA15D, 0x00B8A15D, 0x006AA460,
            0x0094A460, 0x00BDA460, 0x006AAB70, 0x0092AB70, 0x00BAAB70, 0x0066A487, 0x008EA487, 0x00B5A487,
            0x0087BB9E, 0x00A4BB9E, 0x008DB1A1, 0x00AAB1A1, 0x0092A8A4, 0x00B1A8A4, 0x0098A1A4, 0x00B7A1A4,
            0x009A9EA8, 0x00B99EA8, 0x009E9BA8, 0x00BB9BA8, 0x009D9BA8, 0x00BC9BA8, 0x00A094A8, 0x00C194A8,
            0x00A78EAB, 0x00C68EAB, 0x00AD87AB, 0x00CB87AB, 0x00B27DAE, 0x00D37DAE, 0x00BE74B1, 0x00DE74B1,
            0x00CB67B5, 0x00E967B5, 0x00C763B1, 0x00E663B1, 0x00C25DB1, 0x00E25DB1, 0x00C153B1, 0x00DF53B1,
            0x00BC4DB1, 0x00DC4DB1, 0x00BA43AE, 0x00D843AE, 0x00B140A8, 0x00C240A8, 0x00AD498E, 0x00B5498E,
            0x00B2507D, 0x00C3507D, 0x00BF5370, 0x00DD5370, 0x00AC5D63, 0x00CB5D63, 0x009A6756, 0x00B86756,
            0x00747736, 0x00927736, 0x006B872C, 0x0089872C, 0x0071972F, 0x008E972F, 0x0079A836, 0x0097A836,
            0x007CAB39, 0x009AAB39, 0x0081B13C, 0x00A0B13C, 0x0085B840, 0x00A4B840, 0x0089BE46, 0x00A8BE46,
            0x008EC249, 0x00ADC249, 0x0094C84D, 0x00B3C84D, 0x008FC86D, 0x00ABC86D, 0x008AC587, 0x00A7C587,
            0x00A3D5B1, 0x00A8C8B5, 0x00AFBEB8, 0x00B5B5B8, 0x00B7AEB8, 0x00B9AEBB, 0x00B9ABBB, 0x00BCA8BB,
            0x00BEA4BB, 0x00C49EBE, 0x00C897BE, 0x00CD91C2, 0x00D287C2, 0x00DA7DC5, 0x00E674C8, 0x00F167CB,
            0x00F55ACF, 0x00F253CB, 0x00EF4DCB, 0x00EB43CB, 0x00E839CB, 0x00E433C8, 0x00E126C8, 0x00DE1FC2,
            0x00D329AB, 0x00CC3391, 0x00CE3981, 0x00D84074, 0x00DC4663, 0x00CC5056, 0x00BA5A46, 0x00A1672C,
            0x00757A00, 0x007B8E02, 0x0081A108, 0x0089B10F, 0x008FBE15, 0x0093C51C, 0x0096CB1C, 0x009BD222,
            0x009ED526, 0x00A2DC29, 0x00A9E52F, 0x00ADE936, 0x00AFEF49, 0x00A9EC6A, 0x00A5E987, 0x00A2DFA4,
    }
            , RGBA = {
            0x00000000, 0x000000FF, 0x050403FF, 0x0F0D0CFF, 0x1A1817FF, 0x292625FF, 0x393534FF, 0x4A4645FF,
            0x5C5957FF, 0x726E6CFF, 0x878381FF, 0x9D9997FF, 0xB6B2B0FF, 0xCFCAC8FF, 0xE9E4E2FF, 0xFBFFFFFF,
            0x2B1C1BFF, 0x574241FF, 0x8C7472FF, 0xC9ADAAFF, 0x2D231CFF, 0x594D44FF, 0x8F8176FF, 0xCCBCB0FF,
            0x2D241DFF, 0x5B4F46FF, 0x908277FF, 0xCFBEB2FF, 0x2E2820FF, 0x5C554BFF, 0x938B7EFF, 0xD3CABCFF,
            0x323425FF, 0x626451FF, 0x9A9D87FF, 0xD9DCC4FF, 0x293523FF, 0x56644FFF, 0x8C9C83FF, 0xCADCC0FF,
            0x1D2F21FF, 0x405645FF, 0x6A836FFF, 0x9BB6A0FF, 0x1F3335FF, 0x4A6264FF, 0x809B9EFF, 0xBDDCDFFF,
            0x0F1B2BFF, 0x334459FF, 0x637690FF, 0x9BB1CEFF, 0x1D182CFF, 0x45405AFF, 0x777090FF, 0xB2AACFFF,
            0x251B2EFF, 0x4F435BFF, 0x837491FF, 0xBFAFD0FF, 0x2C1D2EFF, 0x5A475CFF, 0x907A93FF, 0xCFB5D2FF,
            0x662F28FF, 0x98584EFF, 0xCE8579FF, 0x663B28FF, 0x986650FF, 0xD0977EFF, 0x68412CFF, 0x9A6C54FF,
            0xD3A085FF, 0x68452FFF, 0x9A7258FF, 0xD0A386FF, 0x6A4A2DFF, 0x9C7756FF, 0xD3AA85FF, 0x6C5131FF,
            0x9E7F5BFF, 0xD7B58DFF, 0x6A5A36FF, 0x9E8B63FF, 0xD7C296FF, 0x6E6C3DFF, 0xA19F6AFF, 0xDAD89EFF,
            0x67753BFF, 0x98A868FF, 0xD0E29CFF, 0x59703BFF, 0x8BA56AFF, 0xC2DF9EFF, 0x4C6F39FF, 0x7BA366FF,
            0xB0DD99FF, 0x366F35FF, 0x64A362FF, 0x97DD94FF, 0x346C4FFF, 0x569271FF, 0x7CBC98FF, 0x2B665DFF,
            0x49877DFF, 0x69AAA0FF, 0x326672FF, 0x5E97A5FF, 0x92CFDFFF, 0x294B6CFF, 0x52789FFF, 0x81ACD7FF,
            0x1D2565FF, 0x3E4D97FF, 0x667ACEFF, 0x312663FF, 0x584E95FF, 0x877ECEFF, 0x422866FF, 0x6C5199FF,
            0x9D7ED0FF, 0x492A66FF, 0x765399FF, 0xA983D1FF, 0x552B67FF, 0x845499FF, 0xBA85D3FF, 0x5E2E69FF,
            0x90599CFF, 0xC789D4FF, 0x692B54FF, 0x9B5382FF, 0xD382B5FF, 0x672B34FF, 0x99535CFF, 0xCF8088FF,
            0xB0342EFF, 0xDB554BFF, 0xB1432EFF, 0xDC654CFF, 0xB0512CFF, 0xDD754EFF, 0xB25D33FF, 0xDE8155FF,
            0xB3612EFF, 0xDF8650FF, 0xB56832FF, 0xDE8B53FF, 0xB36731FF, 0xE08C54FF, 0xB06F35FF, 0xDF975AFF,
            0xB47B35FF, 0xE0A259FF, 0xB4863CFF, 0xDEAD5FFF, 0xB0923AFF, 0xDEBE62FF, 0xB4A740FF, 0xE1D468FF,
            0xB4C048FF, 0xDDEC6FFF, 0xA7BD4CFF, 0xD0EA73FF, 0x95BA47FF, 0xBFE86FFF, 0x82BE46FF, 0xA8E96BFF,
            0x6EBA40FF, 0x96E868FF, 0x50BD47FF, 0x76E86BFF, 0x30B24CFF, 0x47CA5FFF, 0x23A979FF, 0x30B482FF,
            0x2BAD9AFF, 0x44C4B1FF, 0x38BDC1FF, 0x63E7ECFF, 0x319DBCFF, 0x5AC7E8FF, 0x2D80B7FF, 0x51A7E2FF,
            0x1D44B0FF, 0x3967DCFF, 0x322CAFFF, 0x4B50DBFF, 0x4B2BB2FF, 0x684DDDFF, 0x662CB3FF, 0x8850DFFF,
            0x6D2FB3FF, 0x9052DFFF, 0x7A31B5FF, 0xA155E2FF, 0x8631B5FF, 0xAE56E2FF, 0x9431B1FF, 0xBE56DEFF,
            0x9F35B3FF, 0xCA5AE0FF, 0xAE36B6FF, 0xDB5CE3FF, 0xB33581FF, 0xDD56A4FF, 0xB33156FF, 0xDF5376FF,
            0xFD2918FF, 0xFA4411FF, 0xFD580BFF, 0xFC6A1AFF, 0xF8721EFF, 0xFD740EFF, 0xF9770FFF, 0xFA7E17FF,
            0xFA831BFF, 0xFD8F14FF, 0xFB9A1EFF, 0xFDA514FF, 0xF7B321FF, 0xF8C41FFF, 0xFDDC27FF, 0xFBF42CFF,
            0xEDFF21FF, 0xDAFF2FFF, 0xC9FF29FF, 0xAFFF21FF, 0x94FF19FF, 0x7AFF27FF, 0x49FF21FF, 0x00FF3CFF,
            0x00EE6EFF, 0x00DF9BFF, 0x00DFBBFF, 0x00EAE0FF, 0x00EBFFFF, 0x00CEFFFF, 0x00ADFFFF, 0x007EFFFF,
            0x2200FFFF, 0x4300FFFF, 0x5F00FFFF, 0x7900FFFF, 0x8F05FFFF, 0x9B13FEFF, 0xA600FFFF, 0xB510FFFF,
            0xBD15FFFF, 0xCA11FFFF, 0xE10AFFFF, 0xED16FCFF, 0xFD15DFFF, 0xFD1BA4FF, 0xFF1271FF, 0xFE173BFF,
    };

    public static void main(String[] args){
        byte[][][] data = new byte[256][256][256];
        for (int i = 1; i < 256; i++) {
            int oklab = OKLAB[i] & 0xFFFFFF;
            data[oklab & 0xFF][oklab >>> 8 & 0xFF][oklab >>> 16] = (byte) i;
        }
        VoxIO.writeVOX("Yamoe.vox", data, RGBA);
    }
}