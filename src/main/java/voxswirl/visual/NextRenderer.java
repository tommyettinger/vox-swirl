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
import static com.github.tommyettinger.colorful.TrigTools.cos_;
import static com.github.tommyettinger.colorful.TrigTools.sin_;

/**
 * Renders {@code byte[][][]} voxel models to {@link Pixmap}s with arbitrary yaw rotation.
 */
public class NextRenderer {
    public Pixmap pixmap;
    public int[][] depths, render, outlines;
    public VoxMaterial[][] materials;
    public byte[][][] remade;
    public float[][][] lights;
    public float[][] colorI, colorP, colorT;
    public PaletteReducer reducer = new PaletteReducer(Coloring.HALTONIC255);
    private int[] palette;
    public float[] paletteI, paletteP, paletteT;
    public boolean dither = false, outline = true;
    public int size;
    public int quality = 24;
    public float neutral = 1f, bigUp = 1.1f, midUp = 1.04f, midDown = 0.9f,
            smallUp = 1.02f, smallDown = 0.94f, tinyUp = 1.01f, tinyDown = 0.98f;
    public IntMap<VoxMaterial> materialMap;
    public long seed;

    protected NextRenderer() {

    }
    public NextRenderer(final int size) {
        this(size, 24);
    }
    public NextRenderer(final int size, final int quality) {
        this.size = size;
        this.quality = Math.min(Math.max(quality, 0), 64);
        final int w = size * 4 + 4, h = size * 5 + 4;
        pixmap = new Pixmap(w>>>1, h>>>1, Pixmap.Format.RGBA8888);
        render =   new int[w][h];
        outlines = new int[w][h];
        depths =   new int[w][h];
        materials = new VoxMaterial[w][h];
        colorI = fill(-1f, w, h);
        colorP = fill(-1f, w, h);
        colorT = fill(-1f, w, h);
        remade = new byte[size << 2][size << 2][size << 2];
        lights = new float[size << 2][size << 2][size << 2];
        Tools3D.fill(lights, 0.75f);
    }
    
    protected float bn(int x, int y) {
        return (PaletteReducer.RAW_BLUE_NOISE[(x & 63) | (y & 63) << 6] + 128) * 0x1p-8f;
    }

    /**
     * Ranges between -0.75f and 0.75f, both exclusive. Blue-noise distributed, but uniform.
     * @param x x position, will wrap every 64
     * @param y y position, will wrap every 64
     * @return a float between -0.75f and 0.75f
     */
    protected float angle(int x, int y) {
        return (PaletteReducer.RAW_BLUE_NOISE[(x & 63) | (y & 63) << 6] + 0.5f) * 0x3p-9f;
    }

    /**
     * Ranges between -0.75f and 0.75f, both exclusive. Blue-noise distributed and centrally biased.
     * @param x x position, will wrap every 64
     * @param y y position, will wrap every 64
     * @return a float between -0.75f and 0.75f
     */
    protected float biasedAngle(int x, int y) {
        return (PaletteReducer.TRI_BLUE_NOISE[(x & 63) | (y & 63) << 6] - PaletteReducer.RAW_BLUE_NOISE[(x & 63) | (y & 63) << 6]) * 0x3p-10f;
    }

    /**
     * Ranges between -0.75f and 0.75f, both exclusive. Blue-noise distributed and centrally biased.
     * @param x x position, will wrap every 64
     * @param y y position, will wrap every 64
     * @param v variant, typically from 0 to 64
     * @return a float between -0.75f and 0.75f
     */
    protected float biasedAngle(int x, int y, int v) {
//        return (PaletteReducer.TRI_BLUE_NOISE[(x + v * 5 & 63) | (y + v * 3 & 63) << 6] + 0.5f) * 0x3p-9f;
        return (PaletteReducer.TRI_BLUE_NOISE[(x + v & 63) | (y & 63) << 6] - PaletteReducer.RAW_BLUE_NOISE[(x & 63) | (y + v & 63) << 6]) * 0x3p-10f;
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
//        remade[(int) ((xPos + 0x1p23f) - 0x1p23f)][(int) ((yPos + 0x1p23f) - 0x1p23f)][(int) ((zPos + 0x1p23f) - 0x1p23f)] = voxel;
        remade[(int) xPos + size][(int) yPos + size][(int) zPos] = voxel;
//        for (int xp = (int) xPos; xp < xPos + 1f; xp++) {
//            for (int yp = (int) yPos; yp < yPos + 1f; yp++) {
//                for (int zp = (int) zPos; zp < zPos + 1f; zp++) {
//                    remade[xp][yp][zp] = voxel;
//                }
//            }
//        }
    }
    
    public NextRenderer clear() {
        pixmap.setColor(0);
        pixmap.fill();
        fill(depths, 0);
        fill(render, 0);
        fill(outlines, (byte) 0);
        fill(colorI, -1f);
        fill(colorP, -1f);
        fill(colorT, -1f);
        Tools3D.fill(remade, 0);
        Tools3D.fill(lights, 0.75f);
        return this;
    }

    /**
     * Compiles all of the individual voxels drawn with {@link #splat(float, float, float, byte)} into a
     * single Pixmap and returns it.
     * @return {@link #pixmap}, edited to contain the render of all the voxels put in this with {@link #splat(float, float, float, byte)}
     */
    public Pixmap blit() {
        final int threshold = 15;
        final int lightPasses = quality;
        final float strongMain = 0.025f * 12f / lightPasses,
        strongMinor = 0.08f * 12f / lightPasses,
                weakMain = 0.015f * 12f / lightPasses,
                weakMinor = 0.05f * 12f / lightPasses;
        pixmap.setColor(0);
        pixmap.fill();
        final int xSize = render.length - 1, ySize = render[0].length - 1, size2 = size << 1,
                rmxLength = remade.length, rmyLength = remade[0].length, rmzLength = remade[0][0].length,
                startRegion = size, endRegion = rmxLength - (size);
        int xx, yy, depth, voxel, vx, vy, vz;
        VoxMaterial m;

        // top lighting
        int starting = size * 3;
        for (int y = startRegion; y < endRegion; y++) {
            for (int x = startRegion; x < endRegion; x++) {
                for (int p = 0; p < lightPasses; p++) {
                    float ox = x, oy = y, xAngle = biasedAngle(x, y, p), yAngle = biasedAngle(x + 23, y + 41, -p);
                    for (int z = starting;
                         z >= 0 && ox >= startRegion && oy >= startRegion && ox < endRegion && oy < endRegion;
                         z--, ox += xAngle, oy += yAngle) {
                        if(z >= size2) continue;
                        vx = (int)(ox + 0.5f);
                        vy = (int)(oy + 0.5f);
                        if((voxel = remade[vx][vy][z] & 255) != 0){
                            m = materialMap.get(voxel);
                            float carry = strongMinor * m.getTrait(VoxMaterial.MaterialTrait._rough);
                            lights[vx][vy][z] += strongMain;
                            for (int vxx = x - 1; vxx <= x + 1; vxx++) {
                                for (int vyy = vy - 1; vyy <= vy + 1; vyy++) {
                                    for (int vzz = Math.max(0, z - 1); vzz <= Math.min(starting, z + 1); vzz++) {
                                        lights[vxx][vyy][vzz] += carry;
                                    }
                                }
                            }
//
//                            lights[x+1][vy][z] += 0.02f;
//                            lights[x-1][vy][z] += 0.02f;
//                            lights[x][vy+1][z] += 0.02f;
//                            lights[x][vy-1][z] += 0.02f;

                            break;
                        }
                    }
                }
            }
        }
        // side lighting
        starting = endRegion;
        for (int z = 0; z < size2; z++) {
            for (int y = startRegion; y < endRegion; y++) {
                for (int p = 0; p < lightPasses; p++) {
                    float oz = z, oy = y, zAngle= biasedAngle(y + 11, z + 47, -p), yAngle = biasedAngle(y + 19, z + 13, p);
                    for (int x = starting;
                         x >= 0 && oz >= 0 && oy >= startRegion && oz < size2 && oy < endRegion;
                         x--, oz += zAngle, oy += yAngle) {
                        vz = (int)(oz + 0.5f);
                        vy = (int)(oy + 0.5f);
                        if((voxel = remade[x][vy][vz] & 255) != 0){
                            m = materialMap.get(voxel);
                            float carry = weakMinor * m.getTrait(VoxMaterial.MaterialTrait._rough);
                            lights[x][vy][vz] += weakMain;
                            for (int vxx = x - 1; vxx <= x + 1; vxx++) {
                                for (int vyy = vy - 1; vyy <= vy + 1; vyy++) {
                                    for (int vzz = Math.max(0, vz - 1); vzz <= Math.min(size2, vz + 1); vzz++) {
                                        lights[vxx][vyy][vzz] += carry;
                                    }
                                }
                            }
//                            if(vz + 1 < size) lights[x][vy][vz+1] += 0.00625f;
//                            if(vz > 0) lights[x][vy][vz-1] += 0.00625f;
//                            lights[x][vy+1][vz] += 0.00625f;
//                            lights[x][vy-1][vz] += 0.00625f;
                            break;
                        }
                    }
                }
            }
        }

        for (int z = 0; z < remade[0][0].length; z++) {
            for (int y = 0; y < remade[0].length; y++) {
                for (int x = 0; x < remade.length; x++) {
//                    xx = (size + y - x) * 2 + 1;
                    xx = (size + y - x) + 1;
                    if(xx < 0 || xx > xSize) continue;
//                    yy = (z * 3 + size * 3 - x - y) + 1;
                    yy = (z * 3 + size * 5 - x - y) + 2 >> 1;
                    if(yy < 0 || yy > ySize) continue;
                    voxel = remade[x][y][z] & 255;
                    if(voxel == 0) continue;
                    depth = (x + y) * 2 + z * 3;
                    m = materialMap.get(voxel);
                    final float emit = m.getTrait(VoxMaterial.MaterialTrait._emit) * 1.25f;
                    final float alpha = m.getTrait(VoxMaterial.MaterialTrait._alpha);
                    final float reflect = m.getTrait(VoxMaterial.MaterialTrait._ior) + 0.9375f;
//                    final int ax = xx, ay = yy;
                    for (int lx = 0, ax = xx; lx < 2 && ax <= xSize; lx++, ax++) {
                        for (int ly = 0, ay = yy; ly < 2 && ay <= ySize; ly++, ay++) {
                            if (depth >= depths[ax][ay] && (alpha == 0f || bn(ax >>> 1, ay >>> 1) >= alpha)) {
                                colorI[ax][ay] = (float)Math.pow(paletteI[voxel] * (float) Math.sqrt(lights[x][y][z]), reflect);
                                colorP[ax][ay] = paletteP[voxel];
                                colorT[ax][ay] = paletteT[voxel];
                                depths[ax][ay] = depth;
                                materials[ax][ay] = m;
                                if(alpha == 0f)
                                    outlines[ax][ay] = ColorTools.toRGBA8888(ColorTools.ipt(
                                            Math.max(0f, Math.min(1f, paletteI[voxel] - 0.2f + emit)),
                                            Math.max(0f, Math.min(1f, (paletteP[voxel] - 0.5f) * (neutral + 0.125f) + 0.5f)),
                                            Math.max(0f, Math.min(1f, (paletteT[voxel] - 0.5f) * (neutral + 0.125f) + 0.5f)),
                                            1f
                                    ));
                            }
                        }
                    }

                }
            }
        }

        for (int x = 0; x <= xSize; x++) {
            for (int y = 0; y <= ySize; y++) {
                if (colorP[x][y] >= 0f) {
//                    pixmap.drawPixel(x >>> 1, y >>> 1, render[x][y] = ColorTools.toRGBA8888(ColorTools.ipt(
                    pixmap.drawPixel(x, y, render[x][y] = ColorTools.toRGBA8888(ColorTools.ipt(
                            Math.min(Math.max(colorI[x][y], 0f), 1f),
                            (colorP[x][y] - 0.5f) * neutral + 0.5f,
                            (colorT[x][y] - 0.5f) * neutral + 0.5f, 1f)));
                }
            }
        }
        if (outline) {
            int o;
            for (int x = 2; x < xSize - 1; x++) {
                final int hx = x;
//                final int hx = x >>> 1;
                for (int y = 2; y < ySize - 1; y++) {
                    final int hy = y;
//                    int hy = y >>> 1;
                    if ((o = outlines[x][y]) != 0) {
                        depth = depths[x][y] - (threshold << 1);
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
        fill(colorI, -1f);
        fill(colorP, -1f);
        fill(colorT, -1f);
        Tools3D.fill(lights, 0.75f);
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
        final float hs = size * 0.5f;
        final float c = cos_(angleTurns), s = sin_(angleTurns);
//        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
//        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    final byte v = colors[x][y][z];
                    if(v != 0)
                    {
                        for (int m = 0; m < 2; m++) {
                            for (int n = 0; n < 2; n++) {
                                for (int o = 0; o < 2; o++) {
                                    final float xPos = (x+m-hs) * c - (y+n-hs) * s + size;
                                    final float yPos = (x+m-hs) * s + (y+n-hs) * c + size;
                                    splat(xPos, yPos, z + o, v);
                                }
                            }
                        }
//                        minX = Math.min(minX, xPos);
//                        maxX = Math.max(maxX, xPos);
//                        minY = Math.min(minY, yPos);
//                        maxY = Math.max(maxY, yPos);
//                        splat(xPos, yPos, z, v);
                    }
                }
            }
        }
        return blit();
    }

}
