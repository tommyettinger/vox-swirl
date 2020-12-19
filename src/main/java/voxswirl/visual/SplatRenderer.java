package voxswirl.visual;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.github.tommyettinger.anim8.PaletteReducer;
import com.github.tommyettinger.colorful.ipt_hq.ColorTools;
import voxswirl.physical.VoxMaterial;

import static voxswirl.meta.ArrayTools.fill;
import static voxswirl.meta.TrigTools.cos_;
import static voxswirl.meta.TrigTools.sin_;

/**
 * Renders {@code byte[][][]} voxel models to {@link Pixmap}s with arbitrary yaw rotation.
 */
public class SplatRenderer {
    public Pixmap pixmap;
    public int[][] depths, voxels, working, render, outlines;
    public VoxMaterial[][] materials;
    public float[][] shadeX, shadeZ, colorI, colorP, colorT;
    public PaletteReducer reducer = new PaletteReducer();
    private int[] palette;
    public float[] paletteI, paletteP, paletteT;
    public boolean dither = false, outline = true;
    public int size;
    public float neutral = 1f, bigUp = 1.1f, midUp = 1.04f, midDown = 0.9f,
            smallUp = 1.02f, smallDown = 0.94f, tinyUp = 1.01f, tinyDown = 0.98f;
    public IntMap<VoxMaterial> materialMap;
    public long seed;

    protected SplatRenderer() {
        
    }
    public SplatRenderer (final int size) {
        this.size = size;
        final int w = size * 4 + 4, h = size * 5 + 4;
        pixmap = new Pixmap(w>>>1, h>>>1, Pixmap.Format.RGBA8888);
        working =  new int[w][h];
        render =   new int[w][h];
        outlines = new int[w][h];
        depths =   new int[w][h];
        materials = new VoxMaterial[w][h];
        colorI = fill(-1f, w, h);
        colorP = fill(-1f, w, h);
        colorT = fill(-1f, w, h);
        voxels = fill(-1, w, h);
        shadeX = fill(-1f, size * 3 + 5, size * 3 + 5);
        shadeZ = fill(-1f, size * 3 + 5, size * 3 + 5);
    }
    
    protected float bn(int x, int y) {
//        final float result = (((x | ~y) & 1) + (x + ~y & 1) + (x & y & 1)) * 0.333f;
//        System.out.println(result);
//        return result;
//        return (((x | ~y) & 1) + (x + ~y & 1) + (x & y & 1)) * 0.333f;
        return (PaletteReducer.RAW_BLUE_NOISE[(x & 63) | (y & 63) << 6] + 128) * 0x1p-8f;
    }

    
    protected float random(){
        return (((seed * 0xAF251AF3B0F025B5L + 0xB564EF22EC7AECE5L) >>> 41) +
                ((seed = seed * 0xD1342543DE82EF95L + 0x632BE59BD9B4E019L) >>> 41)) * 0x1p-21f;
    }

    /**
     * Takes a modifier between -0.5f and 0.2f, and adjusts how this changes saturation accordingly.
     * Negative modifiers will decrease saturation, while positive ones increase it. If positive, any
     * changes are incredibly sensitive, and 0.05 will seem very different from 0.0. If negative, changes
     * are not as sensitive, but most of the noticeable effect will happen close to -0.1.
     * @param saturationModifier a float between -0.5f and 0.2f; negative decreases saturation, positive increases
     * @return this, for chaining
     */
    public SplatRenderer saturation(float saturationModifier) {
        saturationModifier = MathUtils.clamp(saturationModifier, -0.5f, 0.2f);
        neutral = 1f + saturationModifier;
        bigUp = 1.1f + saturationModifier;
        midUp = 1.04f + saturationModifier;
        midDown = 0.9f + saturationModifier;
        smallUp = 1.02f + saturationModifier;
        smallDown = 0.94f + saturationModifier;
        tinyUp = 1.01f + saturationModifier;
        tinyDown = 0.98f + saturationModifier;
        return this;
    }

    public int[] palette() {
        return palette;
    }

    public SplatRenderer palette(PaletteReducer color) {
        return palette(color.paletteArray);
    }

    public SplatRenderer palette(int[] color) {
        this.palette = color;
        if(paletteI == null) paletteI = new float[256];
        if(paletteP == null) paletteP = new float[256];
        if(paletteT == null) paletteT = new float[256];
        for (int i = 0; i < color.length; i++) {
            if ((color[i] & 0x80) == 0) {
                paletteI[i] = -1f;
                paletteP[i] = -1f;
                paletteT[i] = -1f;
            } else {
                float ipt = ColorTools.fromRGBA8888(color[i]);
                paletteI[i] = ColorTools.intensity(ipt);
                paletteP[i] = ColorTools.protan(ipt);
                paletteT[i] = ColorTools.tritan(ipt);
            }
        }
        return this;
    }
    
    public void splat(float xPos, float yPos, float zPos, int vx, int vy, int vz, byte voxel) {
        final int 
                xx = (int)(0.5f + Math.max(0, (size + yPos - xPos) * 2 + 1)),
                yy = (int)(0.5f + Math.max(0, (zPos * 3 + size * 2 - xPos - yPos) + 1)),
                depth = (int)(0.5f + (xPos + yPos) * 2 + zPos * 3);
        //depth - yy == 3 * xPos + 3 * yPos - 3 * size
        //depth - yy * 2 = 4 * xPos + 4 * yPos - 6 * size
        //depth - yy * 2 - xx * 2 = 8 * xPos - 4 * size
        //depth - yy - yy - xx - xx + size * 4 >> 3 == xPos

        //yy - depth = xPos + yPos + size * 3
        //yy - xx - depth = xPos * 2 + size * 2
        //(yy - xx - depth >> 1) - size = xPos

        //yy - depth = xPos + yPos + size * 3
        //yy + xx - depth = yPos * 2 + size * 4
        //(yy + xx - depth >> 1) - size - size = yPos
        boolean drawn = false;
        final VoxMaterial m = materialMap.get(voxel & 255);
        final float emit = m.getTrait(VoxMaterial.MaterialTrait._emit) * 1.25f;
        final float alpha = m.getTrait(VoxMaterial.MaterialTrait._alpha);
        for (int x = 0, ax = xx; x < 4 && ax < working.length; x++, ax++) {
            for (int y = 0, ay = yy; y < 4 && ay < working[0].length; y++, ay++) {
                if (depth >= depths[ax][ay] && (alpha == 0f || bn(ax >>> 1, ay >>> 1) >= alpha)) {
                    drawn = true;
                    working[ax][ay] = Coloring.adjust(palette[voxel & 255], 1f, neutral);
                    colorI[ax][ay] = paletteI[voxel & 255];
                    colorP[ax][ay] = paletteP[voxel & 255];
                    colorT[ax][ay] = paletteT[voxel & 255];
                    depths[ax][ay] = depth;
                    materials[ax][ay] = m;
                    if(alpha == 0f)
                        outlines[ax][ay] = Coloring.adjust(palette[voxel & 255], 0.625f + emit, bigUp);
                    voxels[ax][ay] = vx | vy << 10 | vz << 20;
                }
            }
        }
        if(xPos < -4.5 || yPos < -4.5 || zPos < -4.5 || xPos + 4.5 > shadeZ.length || yPos + 4.5 > shadeZ[0].length || zPos + 4.5 > shadeX[0].length)
            System.out.println(xPos + ", " + yPos + ", " + zPos + " is out of bounds");
        else if(drawn) {
            shadeZ[(int) (4.500f + xPos)][(int) (4.500f + yPos)] = Math.max(shadeZ[(int) (4.500f + xPos)][(int) (4.500f + yPos)], (4.500f + zPos));
            shadeX[(int) (4.500f + yPos)][(int) (4.500f + zPos)] = Math.max(shadeX[(int) (4.500f + yPos)][(int) (4.500f + zPos)], (4.500f + xPos));
        }
    }
    
    public SplatRenderer clear() {
        pixmap.setColor(0);
        pixmap.fill();
        fill(working, (byte) 0);
        fill(depths, 0);
        fill(outlines, (byte) 0);
        fill(voxels, -1);
        fill(shadeX, -1f);
        fill(shadeZ, -1f);
        fill(colorI, -1f);
        fill(colorP, -1f);
        fill(colorT, -1f);
        return this;
    }

    /**
     * Compiles all of the individual voxels drawn with {@link #splat(float, float, float, int, int, int, byte)} into a
     * single Pixmap and returns it.
     * @param turns yaw in turns; like turning your head or making a turn in a car
     * @return {@link #pixmap}, edited to contain the render of all the voxels put in this with {@link #splat(float, float, float, int, int, int, byte)}
     */
    public Pixmap blit(float turns) {
        return blit(turns, 0f, 0f);
    }

    /**
     * Compiles all of the individual voxels drawn with {@link #splat(float, float, float, int, int, int, byte)} into a
     * single Pixmap and returns it.
     * <br>
     * Although this is in SplatRenderer (which only handles yaw rotation), this allows specifying pitch and roll as
     * well in order to have one consistent implementation, used by {@link RotatingRenderer}.
     * @param yaw in turns; like turning your head or making a turn in a car
     * @param pitch in turns; like looking up or down or making a nosedive in a plane
     * @param roll in turns; like tilting your head to one side or doing a barrel roll in a starship
     * @return {@link #pixmap}, edited to contain the render of all the voxels put in this with {@link #splat(float, float, float, int, int, int, byte)}
     */
    public Pixmap blit(float yaw, float pitch, float roll) {
        final int threshold = 8;
        pixmap.setColor(0);
        pixmap.fill();
        int xSize = working.length - 1, ySize = working[0].length - 1, depth;
//        for (int x = 0; x <= xSize; x++) {
//            System.arraycopy(working[x], 0, render[x], 0, ySize);
//        }
        int v, vx, vy, vz, fx, fy, fz;
        float hs = (size) * 0.5f, ox, oy, oz, tx, ty, tz;
        final float cYaw = cos_(yaw), sYaw = sin_(yaw);
        final float cPitch = cos_(pitch), sPitch = sin_(pitch);
        final float cRoll = cos_(roll), sRoll = sin_(roll);
        final float x_x = cYaw * cPitch, y_x = cYaw * sPitch * sRoll - sYaw * cRoll, z_x = cYaw * sPitch * cRoll + sYaw * sRoll;
        final float x_y = sYaw * cPitch, y_y = sYaw * sPitch * sRoll + cYaw * cRoll, z_y = sYaw * sPitch * cRoll - cYaw * sRoll;
        final float x_z = -sPitch, y_z = cPitch * sRoll, z_z = cPitch * cRoll;
        VoxMaterial m;
//        int deepest = size * 7 + 5;
//        // light wave from top going down
//        for (int sx = 0; sx <= xSize; sx++) {
//            for (int d = 0; d <= deepest; d++) {
//                for (int sy = ySize; sy >= 0; sy--) {
//                    if(depths[sx][sy] == d + sy)
//                    {
//                        colorI[sx][sy] += 0.175f;
//                        break;
//                    }
//                }
//            }
//        }
//        // light wave from front left going back and right
//        int gx, gy, gz;
//        for (int sx = 0; sx <= xSize; sx++) {
//            for (int sy = 0; sy <= ySize; sy++) {
//                gx = (int)(size + 4.5f + hs);
//                gy = (int) (sx * 0.5f + 4.5f + hs);
//                gz = (int) (sy + 4.5f);
//
//                for (int px = sx, py = sy; px <= xSize && py <= ySize; ++px, py += (px & 1)) {
////                    if ((depth = depths[px][py]) != 0 && (depth - px + py) == sx) {
////                    if ((depth = depths[px][py]) != 0 && (py + px - depth) == sx)
//                    if ((v = voxels[px][py]) != -1) {
//                        vx = v & 0x3FF;
//                        vy = v >>> 10 & 0x3FF;
//                        vz = v >>> 20 & 0x3FF;
//                        ox = vx - hs;
//                        oy = vy - hs;
//                        oz = vz - hs;
//                        tx = ox * x_x + oy * y_x + oz * z_x + hs + 4.500f;
//                        fx = (int) (tx);
//                        ty = ox * x_y + oy * y_y + oz * z_y + hs + 4.500f;
//                        fy = (int) (ty);
//                        tz = ox * x_z + oy * y_z + oz * z_z + hs + 4.500f;
//                        fz = (int) (tz);
//                        if(gy == fy && gz == fz) {
//                            colorI[px][py] += 0.125f;
////                for (int d = deepest; d >= 0; d--) {
////                    if (depths[sx][sy] == d + sy - sx) {
////                        colorI[sx][sy] += 0.125f;
//                            colorT[px][py] = 1.0f;
//                            break;
//                        }
//                    }
//                }
//            }
//        }
//        for (int sx = 0; sx <= xSize; sx++) {
//            for (int sy = 0; sy <= ySize; sy++) {
//                if((working[sx][sy] & 0x80) == 0x80)
//                    render[sx][sy] = ColorTools.toRGBA8888(ColorTools.ipt(Math.min(Math.max(colorI[sx][sy] - 0.09375f, 0f), 1f), colorP[sx][sy], colorT[sx][sy], 1f));
//            }
//        }
        for (int sx = 0; sx <= xSize; sx++) {
            for (int sy = 0; sy <= ySize; sy++) {
                if((v = voxels[sx][sy]) != -1) {
                    vx = v & 0x3FF;
                    vy = v >>> 10 & 0x3FF;
                    vz = v >>> 20 & 0x3FF;
                    ox = vx - hs;
                    oy = vy - hs;
                    oz = vz - hs;
                    tx = ox * x_x + oy * y_x + oz * z_x + hs + 4.500f;
                    fx = (int)(tx);
                    ty = ox * x_y + oy * y_y + oz * z_y + hs + 4.500f;
                    fy = (int)(ty);
                    tz = ox * x_z + oy * y_z + oz * z_z + hs + 4.500f;
                    fz = (int)(tz);
                    m = materials[sx][sy];
                    double limit = 2 + (PaletteReducer.TRI_BLUE_NOISE[(sx & 63) + (sy << 6) + (fx + fy + fz >>> 2) & 4095] + 0.5) * 0x1p-7;
                    if (Math.abs(shadeX[fy][fz] - tx) <= limit || ((fy > 1 && Math.abs(shadeX[fy - 2][fz] - tx) <= limit) || (fy < shadeX.length - 2 && Math.abs(shadeX[fy + 2][fz] - tx) <= limit))) {
                        colorI[sx][sy] += 0.06f;
                        float spread = MathUtils.lerp(0.016f, 0.0018f, m.getTrait(VoxMaterial.MaterialTrait._rough));
                        if (sx > 0) colorI[sx - 1][sy] += spread;
                        if (sy > 0) colorI[sx][sy - 1] += spread;
                        if (sx < xSize) colorI[sx + 1][sy] += spread;
                        if (sy < ySize) colorI[sx][sy + 1] += spread;

                        if (sx > 1) colorI[sx - 2][sy] += spread;
                        if (sy > 1) colorI[sx][sy - 2] += spread;
                        if (sx < xSize - 1) colorI[sx + 2][sy] += spread;
                        if (sy < ySize - 1) colorI[sx][sy + 2] += spread;

                    }
//                    if ((shadeX[fy][fz] - tx) > limit || ((fy > 1 && shadeX[fy - 2][fz] - tx > limit) || (fy < shadeX.length - 2 && shadeX[fy + 2][fz] - tx > limit))) {
                        if (Math.abs(shadeZ[fx][fy] - tz) < 1) {
                            colorI[sx][sy] += 0.075f;
                            float spread = MathUtils.lerp(0.02f, 0.002f, m.getTrait(VoxMaterial.MaterialTrait._rough));
                            if (sx > 0) colorI[sx - 1][sy] += spread;
                            if (sy > 0) colorI[sx][sy - 1] += spread;
                            if (sx < xSize) colorI[sx + 1][sy] += spread;
                            if (sy < ySize) colorI[sx][sy + 1] += spread;

                            if (sx > 1) colorI[sx - 2][sy] += spread;
                            if (sy > 1) colorI[sx][sy - 2] += spread;
                            if (sx < xSize - 1) colorI[sx + 2][sy] += spread;
                            if (sy < ySize - 1) colorI[sx][sy + 2] += spread;
//                            render[sx][sy] = Coloring.adjust(working[sx][sy], 1.25f, midUp);
//                            float spread = MathUtils.lerp(1.22f, 1.03f, m.getTrait(VoxMaterial.MaterialTrait._rough));
//                            if (sx > 0) render[sx - 1][sy] = Coloring.adjust(working[sx - 1][sy], spread, smallUp);
//                            if (sy > 0) render[sx][sy - 1] = Coloring.adjust(working[sx][sy - 1], spread, smallUp);
//                            if (sx < xSize) render[sx + 1][sy] = Coloring.adjust(working[sx + 1][sy], spread, smallUp);
//                            if (sy < ySize) render[sx][sy + 1] = Coloring.adjust(working[sx][sy + 1], spread, smallUp);
//
//                            if (sx > 1) render[sx - 2][sy] = Coloring.adjust(working[sx - 2][sy], spread, smallUp);
//                            if (sy > 1) render[sx][sy - 2] = Coloring.adjust(working[sx][sy - 2], spread, smallUp);
//                            if (sx < xSize - 1)
//                                render[sx + 2][sy] = Coloring.adjust(working[sx + 2][sy], spread, smallUp);
//                            if (sy < ySize - 1)
//                                render[sx][sy + 2] = Coloring.adjust(working[sx][sy + 2], spread, smallUp);
//                        } else {
//                            colorI[sx][sy] += -0.35f;
//                            float spread = MathUtils.lerp(-0.34f, -0.12f, m.getTrait(VoxMaterial.MaterialTrait._rough));
//                            if (sx > 0) colorI[sx - 1][sy] += spread;
//                            if (sy > 0) colorI[sx][sy - 1] += spread;
//                            if (sx < xSize) colorI[sx + 1][sy] += spread;
//                            if (sy < ySize) colorI[sx][sy + 1] += spread;
//
//                            if (sx > 1) colorI[sx - 2][sy] += spread;
//                            if (sy > 1) colorI[sx][sy - 2] += spread;
//                            if (sx < xSize - 1) colorI[sx + 2][sy] += spread;
//                            if (sy < ySize - 1) colorI[sx][sy + 2] += spread;
//
////                            render[sx][sy] = Coloring.adjust(working[sx][sy], 0.65f, smallDown);
////                            float spread = MathUtils.lerp(0.66f, 0.88f, m.getTrait(VoxMaterial.MaterialTrait._rough));
////                            if (sx > 0) render[sx - 1][sy] = Coloring.adjust(working[sx - 1][sy], spread, smallDown);
////                            if (sy > 0) render[sx][sy - 1] = Coloring.adjust(working[sx][sy - 1], spread, smallDown);
////                            if (sx < xSize)
////                                render[sx + 1][sy] = Coloring.adjust(working[sx + 1][sy], spread, smallDown);
////                            if (sy < ySize)
////                                render[sx][sy + 1] = Coloring.adjust(working[sx][sy + 1], spread, smallDown);
////
////                            if (sx > 1) render[sx - 2][sy] = Coloring.adjust(working[sx - 2][sy], spread, smallDown);
////                            if (sy > 1) render[sx][sy - 2] = Coloring.adjust(working[sx][sy - 2], spread, smallDown);
////                            if (sx < xSize - 1)
////                                render[sx + 2][sy] = Coloring.adjust(working[sx + 2][sy], spread, smallDown);
////                            if (sy < ySize - 1)
////                                render[sx][sy + 2] = Coloring.adjust(working[sx][sy + 2], spread, smallDown);
//                        }
//                    }
//                    else if (Math.abs(shadeZ[fx][fy] - tz) < 1)
//                    {
//                        colorI[sx][sy] += 0.25f;
//                        float spread = MathUtils.lerp(0.22f, 0.03f, m.getTrait(VoxMaterial.MaterialTrait._rough));
//                        if (sx > 0) colorI[sx - 1][sy] += spread;
//                        if (sy > 0) colorI[sx][sy - 1] += spread;
//                        if (sx < xSize) colorI[sx + 1][sy] += spread;
//                        if (sy < ySize) colorI[sx][sy + 1] += spread;
//
//                        if (sx > 1) colorI[sx - 2][sy] += spread;
//                        if (sy > 1) colorI[sx][sy - 2] += spread;
//                        if (sx < xSize - 1) colorI[sx + 2][sy] += spread;
//                        if (sy < ySize - 1) colorI[sx][sy + 2] += spread;
//
////                        render[sx][sy] = Coloring.adjust(working[sx][sy], 1.3f, bigUp);
////                        float spread = MathUtils.lerp(1.27f, 1.05f, m.getTrait(VoxMaterial.MaterialTrait._rough));
////                        if(sx > 0) render[sx-1][sy] = Coloring.adjust(working[sx-1][sy], spread, midUp);
////                        if(sy > 0) render[sx][sy-1] = Coloring.adjust(working[sx][sy-1], spread, midUp);
////                        if(sx < xSize) render[sx+1][sy] = Coloring.adjust(working[sx+1][sy], spread, midUp);
////                        if(sy < ySize) render[sx][sy+1] = Coloring.adjust(working[sx][sy+1], spread, midUp);
////
////                        if(sx > 1) render[sx-2][sy] = Coloring.adjust(working[sx-2][sy], spread, midUp);
////                        if(sy > 1) render[sx][sy-2] = Coloring.adjust(working[sx][sy-2], spread, midUp);
////                        if(sx < xSize-1) render[sx+2][sy] = Coloring.adjust(working[sx+2][sy], spread, midUp);
////                        if(sy < ySize-1) render[sx][sy+2] = Coloring.adjust(working[sx][sy+2], spread, midUp);
////
////                        render[sx][sy] = Coloring.adjust(render[sx][sy], 0.85f + m.getTrait(VoxMaterial.MaterialTrait._ior) * 0.5f, m.getTrait(VoxMaterial.MaterialTrait._metal) * 0.375f + 1f);
                    }
                }
            }
        }

        for (int x = 0; x <= xSize; x++) {
            for (int y = 0; y <= ySize; y++) {
                if (colorP[x][y] >= 0f) {
                    pixmap.drawPixel(x >>> 1, y >>> 1, render[x][y] = ColorTools.toRGBA8888(ColorTools.ipt(Math.min(Math.max(colorI[x][y] - 0.11f, 0f), 1f), colorP[x][y], colorT[x][y], 1f)));
                }
            }
        }
        if (outline) {
            int o;
            for (int x = 2; x < xSize - 1; x++) {
                final int hx = x >>> 1;
                for (int y = 2; y < ySize - 1; y++) {
                    int hy = y >>> 1;
                    if ((o = outlines[x][y]) != 0) {
                        depth = depths[x][y];
//                        if (outlines[x - 1][y] == 0) {
//                            pixmap.drawPixel(hx - 1, hy    , o);
//                        }
//                        if (outlines[x + 1][y] == 0) {
//                            pixmap.drawPixel(hx + 1, hy    , o);
//                        }
//                        if (outlines[x][y - 1] == 0) {
//                            pixmap.drawPixel(hx    , hy - 1, o);
//                        }
//                        if (outlines[x][y + 1] == 0) {
//                            pixmap.drawPixel(hx    , hy + 1, o);
//                        }

                        if (outlines[x - 1][y] == 0 || depths[x - 1][y] < depth - threshold) {
                            pixmap.drawPixel(hx, hy    , o);
                        }
                        else if (outlines[x + 1][y] == 0 || depths[x + 1][y] < depth - threshold) {
                            pixmap.drawPixel(hx, hy    , o);
                        }
                        else if (outlines[x][y - 1] == 0 || depths[x][y - 1] < depth - threshold) {
                            pixmap.drawPixel(hx    , hy, o);
                        }
                        else if (outlines[x][y + 1] == 0 || depths[x][y + 1] < depth - threshold) {
                            pixmap.drawPixel(hx    , hy, o);
                        }

//                        if (outlines[x - 1][y] == 0 || depths[x - 2][y] < depth - threshold) {
//                            pixmap.drawPixel(hx, hy    , o);
//                        }
//                        else if (outlines[x + 1][y] == 0 || depths[x + 2][y] < depth - threshold) {
//                            pixmap.drawPixel(hx, hy    , o);
//                        }
//                        else if (outlines[x][y - 1] == 0 || depths[x][y - 2] < depth - threshold) {
//                            pixmap.drawPixel(hx    , hy, o);
//                        }
//                        else if (outlines[x][y + 1] == 0 || depths[x][y + 2] < depth - threshold) {
//                            pixmap.drawPixel(hx    , hy, o);
//                        }
                        
//                        if (outlines[x - 1][y] == 0 || depths[x - 2][y] < depth - threshold) {
//                            pixmap.drawPixel(hx - 1, hy    , o);
//                        }
//                        if (outlines[x + 1][y] == 0 || depths[x + 2][y] < depth - threshold) {
//                            pixmap.drawPixel(hx + 1, hy    , o);
//                        }
//                        if (outlines[x][y - 1] == 0 || depths[x][y - 2] < depth - threshold) {
//                            pixmap.drawPixel(hx    , hy - 1, o);
//                        }
//                        if (outlines[x][y + 1] == 0 || depths[x][y + 2] < depth - threshold) {
//                            pixmap.drawPixel(hx    , hy + 1, o);
//                        }
                    }
                }
            }
        }
        if(dither) {
            reducer.setDitherStrength(0.5f);
            reducer.reduceBlueNoise(pixmap);
//            color.reducer.reduceJimenez(pixmapHalf);
        }

        fill(render, 0);
        fill(working, 0);
        fill(depths, 0);
        fill(outlines, 0);
        fill(voxels, -1);
        fill(shadeX, -1);
        fill(shadeZ, -1);
        fill(colorI, -1f);
        fill(colorP, -1f);
        fill(colorT, -1f);
        return pixmap;
    }

    /**
     * Compiles all of the individual voxels drawn with {@link #splat(float, float, float, int, int, int, byte)} into a
     * single Pixmap and returns it.
     * <br>
     * Although this is in SplatRenderer (which only handles yaw rotation), this allows specifying pitch and roll as
     * well in order to have one consistent implementation, used by {@link RotatingRenderer}.
     * @param yaw in turns; like turning your head or making a turn in a car
     * @param pitch in turns; like looking up or down or making a nosedive in a plane
     * @param roll in turns; like tilting your head to one side or doing a barrel roll in a starship
     * @return {@link #pixmap}, edited to contain the render of all the voxels put in this with {@link #splat(float, float, float, int, int, int, byte)}
     */
    public Pixmap blitOld(float yaw, float pitch, float roll) {
        final int threshold = 8;
        pixmap.setColor(0);
        pixmap.fill();
        int xSize = working.length - 1, ySize = working[0].length - 1, depth;
        for (int x = 0; x <= xSize; x++) {
            System.arraycopy(working[x], 0, render[x], 0, ySize);
        }
        int v, vx, vy, vz, fx, fy, fz;
        float hs = (size) * 0.5f, ox, oy, oz, tx, tz;
        final float cYaw = cos_(yaw), sYaw = sin_(yaw);
        final float cPitch = cos_(pitch), sPitch = sin_(pitch);
        final float cRoll = cos_(roll), sRoll = sin_(roll);
        final float x_x = cYaw * cPitch, y_x = cYaw * sPitch * sRoll - sYaw * cRoll, z_x = cYaw * sPitch * cRoll + sYaw * sRoll;
        final float x_y = sYaw * cPitch, y_y = sYaw * sPitch * sRoll + cYaw * cRoll, z_y = sYaw * sPitch * cRoll - cYaw * sRoll;
        final float x_z = -sPitch, y_z = cPitch * sRoll, z_z = cPitch * cRoll;
        VoxMaterial m;
        for (int sx = 0; sx <= xSize; sx++) {
            for (int sy = 0; sy <= ySize; sy++) {
                if((v = voxels[sx][sy]) != -1) {
                    vx = v & 0x3FF;
                    vy = v >>> 10 & 0x3FF;
                    vz = v >>> 20 & 0x3FF;
                    ox = vx - hs;
                    oy = vy - hs;
                    oz = vz - hs;
                    tx = ox * x_x + oy * y_x + oz * z_x + size + 4.500f;
                    fx = (int)(tx);
                    fy = (int)(ox * x_y + oy * y_y + oz * z_y + size + 4.500f);
                    tz = ox * x_z + oy * y_z + oz * z_z + hs + 4.500f;
                    fz = (int)(tz);
                    m = materials[sx][sy];
                    double limit = 2 + (PaletteReducer.TRI_BLUE_NOISE[(sx & 63) + (sy << 6) + (fx + fy + fz >>> 2) & 4095] + 0.5) * 0x1p-7;
                    if ((shadeX[fy][fz] - tx) > limit || ((fy > 1 && shadeX[fy - 2][fz] - tx > limit) || (fy < shadeX.length - 2 && shadeX[fy + 2][fz] - tx > limit))) {
                        if (Math.abs(shadeZ[fx][fy] - tz) < 1) {
                            render[sx][sy] = Coloring.adjust(working[sx][sy], 1.25f, midUp);
                            float spread = MathUtils.lerp(1.22f, 1.03f, m.getTrait(VoxMaterial.MaterialTrait._rough));
                            if (sx > 0) render[sx - 1][sy] = Coloring.adjust(working[sx - 1][sy], spread, smallUp);
                            if (sy > 0) render[sx][sy - 1] = Coloring.adjust(working[sx][sy - 1], spread, smallUp);
                            if (sx < xSize) render[sx + 1][sy] = Coloring.adjust(working[sx + 1][sy], spread, smallUp);
                            if (sy < ySize) render[sx][sy + 1] = Coloring.adjust(working[sx][sy + 1], spread, smallUp);

                            if (sx > 1) render[sx - 2][sy] = Coloring.adjust(working[sx - 2][sy], spread, smallUp);
                            if (sy > 1) render[sx][sy - 2] = Coloring.adjust(working[sx][sy - 2], spread, smallUp);
                            if (sx < xSize - 1)
                                render[sx + 2][sy] = Coloring.adjust(working[sx + 2][sy], spread, smallUp);
                            if (sy < ySize - 1)
                                render[sx][sy + 2] = Coloring.adjust(working[sx][sy + 2], spread, smallUp);
                        } else {
                            render[sx][sy] = Coloring.adjust(working[sx][sy], 0.65f, smallDown);
                            float spread = MathUtils.lerp(0.66f, 0.88f, m.getTrait(VoxMaterial.MaterialTrait._rough));
                            if (sx > 0) render[sx - 1][sy] = Coloring.adjust(working[sx - 1][sy], spread, smallDown);
                            if (sy > 0) render[sx][sy - 1] = Coloring.adjust(working[sx][sy - 1], spread, smallDown);
                            if (sx < xSize)
                                render[sx + 1][sy] = Coloring.adjust(working[sx + 1][sy], spread, smallDown);
                            if (sy < ySize)
                                render[sx][sy + 1] = Coloring.adjust(working[sx][sy + 1], spread, smallDown);

                            if (sx > 1) render[sx - 2][sy] = Coloring.adjust(working[sx - 2][sy], spread, smallDown);
                            if (sy > 1) render[sx][sy - 2] = Coloring.adjust(working[sx][sy - 2], spread, smallDown);
                            if (sx < xSize - 1)
                                render[sx + 2][sy] = Coloring.adjust(working[sx + 2][sy], spread, smallDown);
                            if (sy < ySize - 1)
                                render[sx][sy + 2] = Coloring.adjust(working[sx][sy + 2], spread, smallDown);
                        }
                    }
                    else if (Math.abs(shadeZ[fx][fy] - tz) < 1)
                    {
                        render[sx][sy] = Coloring.adjust(working[sx][sy], 1.3f, bigUp);
                        float spread = MathUtils.lerp(1.27f, 1.05f, m.getTrait(VoxMaterial.MaterialTrait._rough));
                        if(sx > 0) render[sx-1][sy] = Coloring.adjust(working[sx-1][sy], spread, midUp);
                        if(sy > 0) render[sx][sy-1] = Coloring.adjust(working[sx][sy-1], spread, midUp);
                        if(sx < xSize) render[sx+1][sy] = Coloring.adjust(working[sx+1][sy], spread, midUp);
                        if(sy < ySize) render[sx][sy+1] = Coloring.adjust(working[sx][sy+1], spread, midUp);

                        if(sx > 1) render[sx-2][sy] = Coloring.adjust(working[sx-2][sy], spread, midUp);
                        if(sy > 1) render[sx][sy-2] = Coloring.adjust(working[sx][sy-2], spread, midUp);
                        if(sx < xSize-1) render[sx+2][sy] = Coloring.adjust(working[sx+2][sy], spread, midUp);
                        if(sy < ySize-1) render[sx][sy+2] = Coloring.adjust(working[sx][sy+2], spread, midUp);

                        render[sx][sy] = Coloring.adjust(render[sx][sy], 0.85f + m.getTrait(VoxMaterial.MaterialTrait._ior) * 0.5f, m.getTrait(VoxMaterial.MaterialTrait._metal) * 0.375f + 1f);
                    }
                }
            }
        }

        for (int x = 0; x <= xSize; x++) {
            for (int y = 0; y <= ySize; y++) {
                if (render[x][y] != 0) {
                    pixmap.drawPixel(x >>> 1, y >>> 1, render[x][y]);
                }
            }
        }
        if (outline) {
            int o;
            for (int x = 2; x < xSize - 1; x++) {
                final int hx = x >>> 1;
                for (int y = 2; y < ySize - 1; y++) {
                    int hy = y >>> 1;
                    if ((o = outlines[x][y]) != 0) {
                        depth = depths[x][y];
//                        if (outlines[x - 1][y] == 0) {
//                            pixmap.drawPixel(hx - 1, hy    , o);
//                        }
//                        if (outlines[x + 1][y] == 0) {
//                            pixmap.drawPixel(hx + 1, hy    , o);
//                        }
//                        if (outlines[x][y - 1] == 0) {
//                            pixmap.drawPixel(hx    , hy - 1, o);
//                        }
//                        if (outlines[x][y + 1] == 0) {
//                            pixmap.drawPixel(hx    , hy + 1, o);
//                        }

                        if (outlines[x - 1][y] == 0 || depths[x - 1][y] < depth - threshold) {
                            pixmap.drawPixel(hx, hy    , o);
                        }
                        else if (outlines[x + 1][y] == 0 || depths[x + 1][y] < depth - threshold) {
                            pixmap.drawPixel(hx, hy    , o);
                        }
                        else if (outlines[x][y - 1] == 0 || depths[x][y - 1] < depth - threshold) {
                            pixmap.drawPixel(hx    , hy, o);
                        }
                        else if (outlines[x][y + 1] == 0 || depths[x][y + 1] < depth - threshold) {
                            pixmap.drawPixel(hx    , hy, o);
                        }

//                        if (outlines[x - 1][y] == 0 || depths[x - 2][y] < depth - threshold) {
//                            pixmap.drawPixel(hx, hy    , o);
//                        }
//                        else if (outlines[x + 1][y] == 0 || depths[x + 2][y] < depth - threshold) {
//                            pixmap.drawPixel(hx, hy    , o);
//                        }
//                        else if (outlines[x][y - 1] == 0 || depths[x][y - 2] < depth - threshold) {
//                            pixmap.drawPixel(hx    , hy, o);
//                        }
//                        else if (outlines[x][y + 1] == 0 || depths[x][y + 2] < depth - threshold) {
//                            pixmap.drawPixel(hx    , hy, o);
//                        }

//                        if (outlines[x - 1][y] == 0 || depths[x - 2][y] < depth - threshold) {
//                            pixmap.drawPixel(hx - 1, hy    , o);
//                        }
//                        if (outlines[x + 1][y] == 0 || depths[x + 2][y] < depth - threshold) {
//                            pixmap.drawPixel(hx + 1, hy    , o);
//                        }
//                        if (outlines[x][y - 1] == 0 || depths[x][y - 2] < depth - threshold) {
//                            pixmap.drawPixel(hx    , hy - 1, o);
//                        }
//                        if (outlines[x][y + 1] == 0 || depths[x][y + 2] < depth - threshold) {
//                            pixmap.drawPixel(hx    , hy + 1, o);
//                        }
                    }
                }
            }
        }
        if(dither) {
            reducer.setDitherStrength(0.5f);
            reducer.reduceBlueNoise(pixmap);
//            color.reducer.reduceJimenez(pixmapHalf);
        }

        fill(render, 0);
        fill(working, 0);
        fill(depths, 0);
        fill(outlines, 0);
        fill(voxels, -1);
        fill(shadeX, -1);
        fill(shadeZ, -1);
        return pixmap;
    }

    // To move one x+ in voxels is x + 2, y - 1 in pixels.
    // To move one x- in voxels is x - 2, y + 1 in pixels.
    // To move one y+ in voxels is x - 2, y - 1 in pixels.
    // To move one y- in voxels is x + 2, y + 1 in pixels.
    // To move one z+ in voxels is y + 3 in pixels.
    // To move one z- in voxels is y - 3 in pixels.

    public Pixmap drawSplats(byte[][][] colors, float angleTurns, IntMap<VoxMaterial> materialMap) {
        this.materialMap = materialMap;
        seed += TimeUtils.millis() * 0x632BE59BD9B4E019L;
//        seed = Tools3D.hash64(colors);
        final int size = colors.length;
        final float hs = (size) * 0.5f;
        System.out.printf("%dx%dx%d model:\n", size, size, size);
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    final byte v = colors[x][y][z];
                    if(v != 0)
                    {
                        final float c = cos_(angleTurns), s = sin_(angleTurns);
                        final float xPos = (x-hs) * c - (y-hs) * s + hs;
                        final float yPos = (x-hs) * s + (y-hs) * c + hs;
                        minX = Math.min(minX, xPos);
                        maxX = Math.max(maxX, xPos);
                        minY = Math.min(minY, yPos);
                        maxY = Math.max(maxY, yPos);
                        splat(xPos, yPos, z, x, y, z, v);
                    }
                }
            }
        }
        System.out.printf("min X: %f, max X: %f, min Y: %f, max Y: %f\n", minX, maxX, minY, maxY);
        return blit(angleTurns);
    }

}
