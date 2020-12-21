package voxswirl.visual;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.github.tommyettinger.anim8.PaletteReducer;
import com.github.tommyettinger.colorful.ipt_hq.ColorTools;
import voxswirl.physical.Tools3D;
import voxswirl.physical.VoxMaterial;

import static voxswirl.meta.ArrayTools.fill;
import static voxswirl.meta.TrigTools.cos_;
import static voxswirl.meta.TrigTools.sin_;

/**
 * Renders {@code byte[][][]} voxel models to {@link Pixmap}s with arbitrary yaw rotation.
 */
public class NextRenderer {
    public Pixmap pixmap;
    public int[][] depths, voxels, render, outlines;
    public VoxMaterial[][] materials;
    public byte[][][] remade;
    public float[][] colorI, colorP, colorT;
    public PaletteReducer reducer = new PaletteReducer();
    private int[] palette;
    public float[] paletteI, paletteP, paletteT;
    public boolean dither = false, outline = true;
    public int size;
    public float neutral = 1f, bigUp = 1.1f, midUp = 1.04f, midDown = 0.9f,
            smallUp = 1.02f, smallDown = 0.94f, tinyUp = 1.01f, tinyDown = 0.98f;
    public IntMap<VoxMaterial> materialMap;
    public long seed;

    protected NextRenderer() {

    }
    public NextRenderer(final int size) {
        this.size = size;
        final int w = size * 4 + 4, h = size * 5 + 4;
        pixmap = new Pixmap(w>>>1, h>>>1, Pixmap.Format.RGBA8888);
        render =   new int[w][h];
        outlines = new int[w][h];
        depths =   new int[w][h];
        materials = new VoxMaterial[w][h];
        voxels = fill(-1, w, h);
        colorI = fill(-1f, w, h);
        colorP = fill(-1f, w, h);
        colorT = fill(-1f, w, h);
        remade = new byte[size << 1][size << 1][size << 1];
    }
    
    protected float bn(int x, int y) {
        return (PaletteReducer.RAW_BLUE_NOISE[(x & 63) | (y & 63) << 6] + 128) * 0x1p-8f;
    }

    /**
     * Ranges between -0.5f and 0.5f, both exclusive. Centrally biased.
     * @param x x position, will wrap every 64
     * @param y y position, will wrap every 64
     * @return a float between -0.5f and 0.5f
     */
    protected float bnt(int x, int y) {
        return (PaletteReducer.TRI_BLUE_NOISE[(x & 63) | (y & 63) << 6] + 0.5f) * 0x1p-8f;
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
    public NextRenderer saturation(float saturationModifier) {
        saturationModifier = MathUtils.clamp(saturationModifier, -1f, 0.5f);
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

    public NextRenderer palette(PaletteReducer color) {
        return palette(color.paletteArray);
    }

    public NextRenderer palette(int[] color) {
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
    
    public void splat(float xPos, float yPos, float zPos, byte voxel) {
        for (int xp = (int) xPos; xp < xPos + 0.25f; xp++) {
            for (int yp = (int) yPos; yp < yPos + 0.25f; yp++) {
                for (int zp = (int) zPos; zp < zPos + 0.25f; zp++) {
                    remade[xp][yp][zp] = voxel;
                }
            }
        }
    }
    
    public NextRenderer clear() {
        pixmap.setColor(0);
        pixmap.fill();
        fill(depths, 0);
        fill(render, 0);
        fill(outlines, (byte) 0);
        fill(voxels, -1);
        fill(colorI, -1f);
        fill(colorP, -1f);
        fill(colorT, -1f);
        return this;
    }

    /**
     * Compiles all of the individual voxels drawn with {@link #splat(float, float, float, byte)} into a
     * single Pixmap and returns it.
     * @return {@link #pixmap}, edited to contain the render of all the voxels put in this with {@link #splat(float, float, float, byte)}
     */
    public Pixmap blit() {
        final int threshold = 8;
        pixmap.setColor(0);
        pixmap.fill();
        int xSize = render.length - 1, ySize = render[0].length - 1, xx, yy, depth;
        VoxMaterial m;
        for (int z = 0; z < remade[0][0].length; z++) {
            for (int y = 0; y < remade[0].length; y++) {
                for (int x = 0; x < remade.length; x++) {
                    xx = (size + y - x) * 2 + 1;
                    if(xx < 0 || xx > xSize) continue;
                    yy = (z * 3 + size * 4 - x - y) + 1;
                    if(yy < 0 || yy > ySize) continue;
                    int voxel = remade[x][y][z] & 255;
                    if(voxel == 0) continue;
                    depth = (x + y) * 2 + z * 3;
                    m = materialMap.get(voxel);
                    final float emit = m.getTrait(VoxMaterial.MaterialTrait._emit) * 1.25f;
                    final float alpha = m.getTrait(VoxMaterial.MaterialTrait._alpha);
                    for (int lx = 0, ax = xx; lx < 4 && ax <= xSize; lx++, ax++) {
                        for (int ly = 0, ay = yy; ly < 4 && ay <= ySize; ly++, ay++) {
                            if (depth >= depths[ax][ay] && (alpha == 0f || bn(ax >>> 1, ay >>> 1) >= alpha)) {
                                colorI[ax][ay] = paletteI[voxel];
                                colorP[ax][ay] = paletteP[voxel];
                                colorT[ax][ay] = paletteT[voxel];
                                depths[ax][ay] = depth;
                                materials[ax][ay] = m;
                                voxels[ax][ay] = x | y << 10 | z << 20;
                                if(alpha == 0f)
                                    outlines[ax][ay] = Coloring.adjust(palette[voxel], 0.625f + emit, bigUp);
                            }
                        }
                    }

                }
            }
        }

        int starting = remade[0][0].length - 1;
        for (int y = 0; y < remade[0].length; y++) {
            for (int x = 0; x < remade.length; x++) {
                float ox = x, oy = y;
                for (int z = starting; z >= 0; z--, ox += bnt(x, y), oy -= bnt(x + 23, y + 41)) {

                }
            }
        }

        for (int x = 0; x <= xSize; x++) {
            for (int y = 0; y <= ySize; y++) {
                if (colorP[x][y] >= 0f) {
                    pixmap.drawPixel(x >>> 1, y >>> 1, render[x][y] = ColorTools.toRGBA8888(ColorTools.ipt(
                            Math.min(Math.max(colorI[x][y] - 0.11f, 0f), 1f),
                            (colorP[x][y] - 0.5f) * neutral + 0.5f,
                            (colorT[x][y] - 0.5f) * neutral + 0.5f, 1f)));
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
                        depth = depths[x][y] - threshold;
                        if (outlines[x - 1][y] == 0 || depths[x - 1][y] < depth) {
                            pixmap.drawPixel(hx, hy    , o);
                        }
                        else if (outlines[x + 1][y] == 0 || depths[x + 1][y] < depth) {
                            pixmap.drawPixel(hx, hy    , o);
                        }
                        else if (outlines[x][y - 1] == 0 || depths[x][y - 1] < depth) {
                            pixmap.drawPixel(hx    , hy, o);
                        }
                        else if (outlines[x][y + 1] == 0 || depths[x][y + 1] < depth) {
                            pixmap.drawPixel(hx    , hy, o);
                        }
                    }
                }
            }
        }
        if(dither) {
            reducer.setDitherStrength(0.75f);
            reducer.reduceScatter(pixmap);
//            color.reducer.reduceJimenez(pixmapHalf);
        }

        fill(render, 0);
        fill(depths, 0);
        fill(outlines, 0);
        fill(voxels, -1);
        fill(colorI, -1f);
        fill(colorP, -1f);
        fill(colorT, -1f);
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
        Tools3D.fill(remade, 0);
        seed += TimeUtils.millis() * 0x632BE59BD9B4E019L;
//        seed = Tools3D.hash64(colors);
        final int size = colors.length;
        final float hs = (size) * 0.5f;
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    final byte v = colors[x][y][z];
                    if(v != 0)
                    {
                        final float c = cos_(angleTurns), s = sin_(angleTurns);
                        final float xPos = (x-hs) * c - (y-hs) * s + size;
                        final float yPos = (x-hs) * s + (y-hs) * c + size;
                        minX = Math.min(minX, xPos);
                        maxX = Math.max(maxX, xPos);
                        minY = Math.min(minY, yPos);
                        maxY = Math.max(maxY, yPos);
                        splat(xPos, yPos, z, v);
                    }
                }
            }
        }
        return blit();
    }

}
