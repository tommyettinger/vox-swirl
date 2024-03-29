package voxswirl.util;

import voxswirl.io.VoxIO;

public class ColorSolidGenerator {
    private static final int[] OKLAB = {
            0x00007D7D, 0x00007D7D, 0x00128181, 0x00248181, 0x00368181, 0x00498181, 0x005B8181, 0x006D8181,
            0x007F8181, 0x00928181, 0x00A48181, 0x00B68181, 0x00C98181, 0x00DB8181, 0x00ED8181, 0x00FF7D7D,
            0x0041978A, 0x006E978A, 0x009C978A, 0x00CA978A, 0x00478E8E, 0x00758E8E, 0x00A48E8E, 0x00D28E8E,
            0x00488A8E, 0x00778A8E, 0x00A58A8E, 0x00D48A8E, 0x004C8191, 0x007C8191, 0x00AB8191, 0x00DB8191,
            0x00577494, 0x00877494, 0x00B67494, 0x00E57494, 0x00556D94, 0x00846D94, 0x00B36D94, 0x00E26D94,
            0x004D6391, 0x00756391, 0x009D6391, 0x00C56391, 0x00536D77, 0x00826D77, 0x00B26D77, 0x00E26D77,
            0x00397A5D, 0x00687A5D, 0x00977A5D, 0x00C67A5D, 0x003B8E60, 0x006A8E60, 0x00988E60, 0x00C78E60,
            0x00409763, 0x006F9763, 0x009D9763, 0x00CC9763, 0x00459E6A, 0x00749E6A, 0x00A39E6A, 0x00D29E6A,
            0x0067AB9B, 0x008FAB9B, 0x00B6AB9B, 0x006D9E9E, 0x00969E9E, 0x00BF9E9E, 0x0071979E, 0x009A979E,
            0x00C4979E, 0x0074949E, 0x009D949E, 0x00C5949E, 0x007791A1, 0x00A091A1, 0x00C991A1, 0x007C8AA1,
            0x00A58AA1, 0x00CF8AA1, 0x008181A4, 0x00AC81A4, 0x00D681A4, 0x008D74A8, 0x00B774A8, 0x00E174A8,
            0x00916AA8, 0x00BA6AA8, 0x00E46AA8, 0x008B60A8, 0x00B660A8, 0x00E060A8, 0x008856A4, 0x00B256A4,
            0x00DC56A4, 0x00844DA4, 0x00AE4DA4, 0x00D84DA4, 0x0083538E, 0x00A3538E, 0x00C3538E, 0x007F5A7A,
            0x009B5A7A, 0x00B75A7A, 0x0082636D, 0x00AB636D, 0x00D5636D, 0x006E6D5A, 0x00976D5A, 0x00C06D5A,
            0x00518139, 0x00798139, 0x00A18139, 0x00569443, 0x007F9443, 0x00A99443, 0x005DA146, 0x0086A146,
            0x00AEA146, 0x0060A84D, 0x008AA84D, 0x00B3A84D, 0x0065AE50, 0x008EAE50, 0x00B8AE50, 0x006AB553,
            0x0094B553, 0x00BDB553, 0x006ABB6A, 0x0092BB6A, 0x00BABB6A, 0x0066B58A, 0x008EB58A, 0x00B5B58A,
            0x0087C5A4, 0x00A4C5A4, 0x008DB8A8, 0x00AAB8A8, 0x0092AEA8, 0x00B1AEA8, 0x0098A4AB, 0x00B7A4AB,
            0x009AA4AB, 0x00B9A4AB, 0x009EA1AE, 0x00BBA1AE, 0x009D9EAE, 0x00BC9EAE, 0x00A097AE, 0x00C197AE,
            0x00A791B1, 0x00C691B1, 0x00AD8AB1, 0x00CB8AB1, 0x00B27DB5, 0x00D37DB5, 0x00BE74B8, 0x00DE74B8,
            0x00CB67BB, 0x00E967BB, 0x00C75DBB, 0x00E65DBB, 0x00C256B8, 0x00E256B8, 0x00C14DB8, 0x00DF4DB8,
            0x00BC43B8, 0x00DC43B8, 0x00BA39B5, 0x00D839B5, 0x00B136AE, 0x00C236AE, 0x00AD4391, 0x00B54391,
            0x00B2497D, 0x00C3497D, 0x00BF5070, 0x00DD5070, 0x00AC5A60, 0x00CB5A60, 0x009A6350, 0x00B86350,
            0x00747429, 0x00927429, 0x006B871F, 0x0089871F, 0x00719B26, 0x008E9B26, 0x0079AE2C, 0x0097AE2C,
            0x007CB12F, 0x009AB12F, 0x0081BB33, 0x00A0BB33, 0x0085C239, 0x00A4C239, 0x0089C53C, 0x00A8C53C,
            0x008ECF40, 0x00ADCF40, 0x0094D246, 0x00B3D246, 0x008FD56A, 0x00ABD56A, 0x008ACF8A, 0x00A7CF8A,
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
            0x361513FF, 0x663B38FF, 0x9F6B67FF, 0xDFA49EFF, 0x361E14FF, 0x66473AFF, 0x9F7A6BFF, 0xDFB5A3FF,
            0x352115FF, 0x654B3CFF, 0x9D7E6CFF, 0xDDBAA6FF, 0x322813FF, 0x61553CFF, 0x998B6EFF, 0xDACAA9FF,
            0x303617FF, 0x5F6742FF, 0x96A076FF, 0xD5E0B1FF, 0x273716FF, 0x536740FF, 0x88A072FF, 0xC5E0ADFF,
            0x0D3315FF, 0x325B37FF, 0x5B8960FF, 0x8ABD8FFF, 0x113539FF, 0x3D6569FF, 0x729EA3FF, 0xAEE0E6FF,
            0x09183BFF, 0x2C416DFF, 0x5974A8FF, 0x90B0EAFF, 0x1F1338FF, 0x483B6AFF, 0x796CA3FF, 0xB4A5E4FF,
            0x2B133AFF, 0x583B6BFF, 0x8D6CA4FF, 0xCBA6E5FF, 0x361438FF, 0x673D68FF, 0xA06FA2FF, 0xE1AAE2FF,
            0x752317FF, 0xAA4D3DFF, 0xE37A66FF, 0x733316FF, 0xA85E3EFF, 0xE38F6BFF, 0x723B1AFF, 0xA66743FF,
            0xE29A72FF, 0x73401DFF, 0xA76D46FF, 0xDF9E73FF, 0x754519FF, 0xA97243FF, 0xE2A571FF, 0x744E1EFF,
            0xA87C49FF, 0xE2B179FF, 0x71581CFF, 0xA68A4AFF, 0xE0C17BFF, 0x716C21FF, 0xA49F50FF, 0xDED983FF,
            0x697626FF, 0x99A954FF, 0xD1E487FF, 0x53731FFF, 0x84A950FF, 0xBAE483FF, 0x3C7425FF, 0x6AA953FF,
            0x9FE485FF, 0x1F7321FF, 0x50A84FFF, 0x84E381FF, 0x0B714AFF, 0x3A986DFF, 0x61C393FF, 0x006962FF,
            0x2C8B83FF, 0x4FAFA6FF, 0x1D6877FF, 0x4D9AABFF, 0x81D3E5FF, 0x114C79FF, 0x3E7AAEFF, 0x6DAEE8FF,
            0x1A1B7CFF, 0x3646B2FF, 0x5D75ECFF, 0x341B75FF, 0x5A46ABFF, 0x8976E8FF, 0x471B79FF, 0x7246AFFF,
            0xA275E9FF, 0x511C75FF, 0x8048ABFF, 0xB478E5FF, 0x5C1D76FF, 0x8D48ABFF, 0xC57AE7FF, 0x681D78FF,
            0x9D4BAEFF, 0xD67BE9FF, 0x741B5BFF, 0xAA468AFF, 0xE474BEFF, 0x751C2FFF, 0xAB4656FF, 0xE47382FF,
            0xBB241FFF, 0xE8493EFF, 0xBA3B1EFF, 0xE65D3EFF, 0xB84A24FF, 0xE66F46FF, 0xB85922FF, 0xE67E46FF,
            0xBB5B25FF, 0xE98048FF, 0xBF6120FF, 0xEA8543FF, 0xBA631FFF, 0xE78844FF, 0xB76B23FF, 0xE7944BFF,
            0xBB7722FF, 0xE89F49FF, 0xBC832AFF, 0xE7AA4FFF, 0xB49126FF, 0xE2BD51FF, 0xB9A72CFF, 0xE5D357FF,
            0xB9C034FF, 0xE2EB5DFF, 0xA3C02FFF, 0xCDEC5AFF, 0x8FBD33FF, 0xB8EB5EFF, 0x7BC133FF, 0xA1EC5BFF,
            0x5EBF2CFF, 0x87ED57FF, 0x3AC135FF, 0x63EC5BFF, 0x00B63CFF, 0x2ACE51FF, 0x00AC74FF, 0x0CB77DFF,
            0x00B09BFF, 0x27C7B1FF, 0x29BEC2FF, 0x58E9ECFF, 0x1C9EC2FF, 0x4CC9EEFF, 0x1681C1FF, 0x41A7EDFF,
            0x0A3FC2FF, 0x2964F0FF, 0x3121C1FF, 0x4749EFFF, 0x4D20BFFF, 0x6946EBFF, 0x6A1EC0FF, 0x8C46EEFF,
            0x7122C0FF, 0x9549EDFF, 0x811FC2FF, 0xA849F0FF, 0x8E21BEFF, 0xB74AECFF, 0x9725BFFF, 0xC14DEDFF,
            0xA81FC0FF, 0xD54AEEFF, 0xB626BFFF, 0xE450EDFF, 0xBE2285FF, 0xE947A8FF, 0xBE2250FF, 0xEB4870FF,
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
        VoxIO.writeVOX("Yamof.vox", data, RGBA);
    }
}
