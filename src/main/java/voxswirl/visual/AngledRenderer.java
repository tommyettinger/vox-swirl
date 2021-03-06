package voxswirl.visual;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.github.tommyettinger.anim8.PaletteReducer;
import com.github.tommyettinger.colorful.oklab.ColorTools;
import voxswirl.physical.Tools3D;
import voxswirl.physical.VoxMaterial;

import static com.github.tommyettinger.colorful.TrigTools.cos_;
import static com.github.tommyettinger.colorful.TrigTools.sin_;
import static voxswirl.meta.ArrayTools.fill;

/**
 * Renders {@code byte[][][]} voxel models to {@link Pixmap}s with arbitrary yaw rotation.
 * Calculates the approximate slope of visible voxels and uses only that to calculate (faked) lighting.
 */
public class AngledRenderer {
    public Pixmap pixmap;
    public int[][] depths, voxels, render, outlines;
    public VoxMaterial[][] materials;
    public float[][] shadeX, shadeZ, colorL, colorA, colorB;
    public PaletteReducer reducer = new PaletteReducer(Coloring.HALTONIC255);
    private int[] palette;
    public float[] paletteL, paletteA, paletteB;
    public boolean dither = false, outline = true;
    public int size;
    public float neutral = 1f;
    public IntMap<VoxMaterial> materialMap;
    public long seed;
    public byte[][][] remade;

    protected AngledRenderer() {

    }
    public AngledRenderer(final int size) {
        this.size = size;
        final int w = size * 4 + 4, h = size * 5 + 4;
        pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
//        pixmap = new Pixmap(w>>>1, h>>>1, Pixmap.Format.RGBA8888);
        render =   new int[w][h];
        outlines = new int[w][h];
        depths =   new int[w][h];
        materials = new VoxMaterial[w][h];
        voxels = fill(-1, w, h);
        shadeX = fill(-1f, size * 4, size * 4);
        shadeZ = fill(-1f, size * 4, size * 4);
        colorL = fill(-1f, w, h);
        colorA = fill(-1f, w, h);
        colorB = fill(-1f, w, h);
        remade = new byte[size << 2][size << 2][size << 2];
    }
    
    protected float bn(int x, int y) {
//        final float result = (((x | ~y) & 1) + (x + ~y & 1) + (x & y & 1)) * 0.333f;
//        System.out.println(result);
//        return result;
//        return (((x | ~y) & 1) + (x + ~y & 1) + (x & y & 1)) * 0.333f;
        return (PaletteReducer.TRI_BLUE_NOISE[(x & 63) | (y & 63) << 6] + 128) * 0x1p-8f;
    }

    /**
     * Takes a modifier between -0.5f and 0.2f, and adjusts how this changes saturation accordingly.
     * Negative modifiers will decrease saturation, while positive ones increase it. If positive, any
     * changes are incredibly sensitive, and 0.05 will seem very different from 0.0. If negative, changes
     * are not as sensitive, but most of the noticeable effect will happen close to -0.1.
     * @param saturationModifier a float between -0.5f and 0.2f; negative decreases saturation, positive increases
     * @return this, for chaining
     */
    public AngledRenderer saturation(float saturationModifier) {
        neutral = 1f + MathUtils.clamp(saturationModifier, -1f, 0.5f);;
        return this;
    }

    public int[] palette() {
        return palette;
    }

    public AngledRenderer palette(PaletteReducer color) {
        return palette(color.paletteArray);
    }

    public AngledRenderer palette(int[] color) {
        this.palette = color;
        if(paletteL == null) paletteL = new float[256];
        if(paletteA == null) paletteA = new float[256];
        if(paletteB == null) paletteB = new float[256];
        for (int i = 0; i < color.length; i++) {
            if ((color[i] & 0x80) == 0) {
                paletteL[i] = -1f;
                paletteA[i] = -1f;
                paletteB[i] = -1f;
            } else {
                float lab = ColorTools.fromRGBA8888(color[i]);
                paletteL[i] = ColorTools.channelL(lab);
                paletteA[i] = ColorTools.channelA(lab);
                paletteB[i] = ColorTools.channelB(lab);
            }
        }
        return this;
    }
    
    public void splat(float xPos, float yPos, float zPos, int vx, int vy, int vz, byte voxel) {
        if(xPos <= -1f || yPos <= -1f || zPos <= -1f
                || xPos >= size * 2 || yPos >= size * 2 || zPos >= size * 2)
            return;
        final int 
                xx = (int)(0.5f + Math.max(0, (size + yPos - xPos) * 2 + 1)),
                yy = (int)(0.5f + Math.max(0, (zPos * 3 + size * 3 - xPos - yPos) + 1)),
                depth = (int)(0.5f + (xPos + yPos) * 2 + zPos * 3);
        boolean drawn = false;
        final VoxMaterial m = materialMap.get(voxel & 255);
        final float emit = m.getTrait(VoxMaterial.MaterialTrait._emit) * 1.25f;
        final float alpha = m.getTrait(VoxMaterial.MaterialTrait._alpha);
        final float hs = size * 0.5f;
        final float hx = hs + xPos, hy = hs + yPos, hz = hs + zPos;
        for (int x = 0, ax = xx; x < 4 && ax < render.length; x++, ax++) {
            for (int y = 0, ay = yy; y < 4 && ay < render[0].length; y++, ay++) {
                if (depth >= depths[ax][ay] && (alpha == 0f || bn(ax, ay) >= alpha)) {
                    drawn = true;
                    colorL[ax][ay] = paletteL[voxel & 255];
                    colorA[ax][ay] = paletteA[voxel & 255];
                    colorB[ax][ay] = paletteB[voxel & 255];
                    depths[ax][ay] = depth;
                    materials[ax][ay] = m;
                    if(alpha == 0f)
                        outlines[ax][ay] =
                                Coloring.darken(palette[voxel & 255], 0.375f - emit);
//                                Coloring.adjust(palette[voxel & 255], 0.625f + emit, neutral);
                    else
                        outlines[ax][ay] = palette[voxel & 255];
                    voxels[ax][ay] = vx | vy << 10 | vz << 20;
                    for (int xp = (int)hx; xp < hx + 0.5f; xp++) {
                        for (int yp = (int) hy; yp < hy + 0.5f; yp++) {
                            for (int zp = (int) hz; zp < hz + 0.5f; zp++) {
                                remade[xp][yp][zp] = voxel;
                            }
                        }
                    }
                }
            }
        }
        if(xPos < -hs || yPos < -hs || zPos < -hs || xPos + hs > shadeZ.length || yPos + hs > shadeZ[0].length || zPos + hs > shadeX[0].length)
            System.out.println(xPos + ", " + yPos + ", " + zPos + " is out of bounds");
        else if(drawn) {
            shadeZ[(int) (hs + xPos)][(int) (hs + yPos)] = Math.max(shadeZ[(int) (hs + xPos)][(int) (hs + yPos)], (hs + zPos));
            shadeX[(int) (hs + yPos)][(int) (hs + zPos)] = Math.max(shadeX[(int) (hs + yPos)][(int) (hs + zPos)], (hs + xPos));
        }
    }
    
    public AngledRenderer clear() {
        pixmap.setColor(0);
        pixmap.fill();
        fill(depths, 0);
        fill(render, 0);
        fill(outlines, (byte) 0);
        fill(voxels, -1);
        fill(shadeX, -1f);
        fill(shadeZ, -1f);
        fill(colorL, -1f);
        fill(colorA, -1f);
        fill(colorB, -1f);
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
        final int threshold = 12;
        pixmap.setColor(0);
        pixmap.fill();
        int xSize = render.length - 1, ySize = render[0].length - 1, depth;
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
        for (int sx = 0; sx <= xSize; sx++) {
            for (int sy = 0; sy <= ySize; sy++) {
                if((v = voxels[sx][sy]) != -1) {
                    vx = v & 0x3FF;
                    vy = v >>> 10 & 0x3FF;
                    vz = v >>> 20 & 0x3FF;
                    ox = vx - hs;
                    oy = vy - hs;
                    oz = vz - hs;
                    tx = ox * x_x + oy * y_x + oz * z_x + size + hs;
                    fx = (int)(tx);
                    ty = ox * x_y + oy * y_y + oz * z_y + size + hs;
                    fy = (int)(ty);
                    tz = ox * x_z + oy * y_z + oz * z_z + hs + hs;
                    fz = (int)(tz);
                    m = materials[sx][sy];
                    float rough = m.getTrait(VoxMaterial.MaterialTrait._rough);
                    float emit = m.getTrait(VoxMaterial.MaterialTrait._emit);
                    final float limit = 2;

                    float top = 0f, side = 0f, refract = 0f;
                    int depthA = -4, depthB = -4, depthC = -4, depthD = -4;
                    int frontA = -4, frontB = -4, frontC = -4, frontD = -4;
                    if(fx > 1) {
                        for (int probe = 3; probe >= -3; probe--) {
                            if(fz + probe < 0) break;
                            if(fz + probe >= remade.length) continue;
                            if(remade[fx - 2][fy][fz + probe] != 0) {
                                depthA = probe;
                                break;
                            }
                        }
                    }
                    if(fx > 0) {
                        for (int probe = 3; probe >= -3; probe--) {
                            if(fz + probe < 0) break;
                            if(fz + probe >= remade.length) continue;
                            if(remade[fx - 1][fy][fz + probe] != 0) {
                                depthB = probe;
                                break;
                            }
                        }
                    }
                    if(fx < remade.length - 1) {
                        for (int probe = 3; probe >= -3; probe--) {
                            if(fz + probe < 0) break;
                            if(fz + probe >= remade.length) continue;
                            if(remade[fx + 1][fy][fz + probe] != 0) {
                                depthC = probe;
                                break;
                            }
                        }
                    }
                    if(fx < remade.length - 2) {
                        for (int probe = 3; probe >= -3; probe--) {
                            if(fz + probe < 0) break;
                            if(fz + probe >= remade.length) continue;
                            if(remade[fx + 2][fy][fz + probe] != 0) {
                                depthD = probe;
                                break;
                            }
                        }
                    }
                    top = (depthB - depthC) * 0.375f + (depthA - depthD) * 0.1875f + 0.375f;

                    if(depthD < -1 && depthC < 0 && depthB < 1 && depthA < 1)
                        refract = m.getTrait(VoxMaterial.MaterialTrait._ior) * 0.5f;

                    if(fy > 1) {
                        for (int probe = 3; probe >= -3; probe--) {
                            if(fx + probe < 0) break;
                            if(fx + probe >= remade.length) continue;
                            if(remade[fx + probe][fy - 2][fz] != 0) {
                                frontA = probe;
                                break;
                            }
                        }
                    }
                    if(fy > 0) {
                        for (int probe = 3; probe >= -3; probe--) {
                            if(fx + probe < 0) break;
                            if(fx + probe >= remade.length) continue;
                            if(remade[fx + probe][fy - 1][fz] != 0) {
                                frontB = probe;
                                break;
                            }
                        }
                    }
                    if(fy < remade.length - 1) {
                        for (int probe = 3; probe >= -3; probe--) {
                            if(fx + probe < 0) break;
                            if(fx + probe >= remade.length) continue;
                            if(remade[fx + probe][fy + 1][fz] != 0) {
                                frontC = probe;
                                break;
                            }
                        }
                    }
                    if(fy < remade.length - 2) {
                        for (int probe = 3; probe >= -3; probe--) {
                            if(fx + probe < 0) break;
                            if(fx + probe >= remade.length) continue;
                            if(remade[fx + probe][fy + 2][fz] != 0) {
                                frontD = probe;
                                break;
                            }
                        }
                    }

                    side = Math.max(top, (frontB - frontC) * 0.25f + (frontA - frontD) * 0.125f + 0.25f);

                    if(side > 0f){
                        float spread = MathUtils.lerp(0.001f, 0.00025f, rough) * side;
                        if(bn(sx, sy) > 0.75f)
                            colorL[sx][sy] += refract;
                        int dist;
                        for (int i = -3, si = sx + i; i <= 3; i++, si++) {
                            for (int j = -3, sj = sy + j; j <= 3; j++, sj++) {
                                if((dist = Math.abs(i) + Math.abs(j)) > 3 || si < 0 || sj < 0 || si > xSize || sj > ySize) continue;
                                colorL[si][sj] += spread * (4 - dist);
                            }
                        }
                    }
                    if (emit > 0) {
                        float spread = emit * 0.1f;
                        for (int i = -5, si = sx + i; i <= 5; i++, si++) {
                            for (int j = -5, sj = sy + j; j <= 5; j++, sj++) {
                                if(Math.abs(i) + Math.abs(j) > 5 || si < 0 || sj < 0 || si > xSize || sj > ySize) continue;
                                colorL[si][sj] += spread;
                            }
                        }
                    }
                }
            }
        }

        for (int x = 0; x <= xSize; x++) {
            for (int y = 0; y <= ySize; y++) {
                if (colorA[x][y] >= 0f) {
                    pixmap.drawPixel(x, y, render[x][y] = ColorTools.toRGBA8888(ColorTools.limitToGamut(
                            Math.min(Math.max(colorL[x][y], 0f), 1f),
                            (colorA[x][y] - 0.5f) * neutral + 0.5f,
                            (colorB[x][y] - 0.5f) * neutral + 0.5f, 1f)));
                }
            }
        }
//        for (int x = 0; x <= xSize; x++) {
//            for (int y = 0; y <= ySize; y++) {
//                if (colorA[x][y] >= 0f) {
//                    pixmap.drawPixel(x >>> 1, y >>> 1, render[x][y] = ColorTools.toRGBA8888(ColorTools.limitToGamut(
//                            Math.min(Math.max(colorL[x][y] - 0.125f, 0f), 1f),
//                            (colorA[x][y] - 0.5f) * neutral + 0.5f,
//                            (colorB[x][y] - 0.5f) * neutral + 0.5f, 1f)));
//                }
//            }
//        }
        if (outline) {
            int o;
            for (int x = 2; x < xSize - 1; x++) {
                final int hx = x;
//                final int hx = x >>> 1;
                for (int y = 2; y < ySize - 1; y++) {
                    final int hy = y;
//                    int hy = y >>> 1;
                    if ((o = outlines[x][y]) != 0) {
                        depth = depths[x][y];
                        if (outlines[x - 1][y] == 0 || depths[x - 1][y] < depth - threshold) {
                            pixmap.drawPixel(hx - 1, hy    , o);
                        }
                        if (outlines[x + 1][y] == 0 || depths[x + 1][y] < depth - threshold) {
                            pixmap.drawPixel(hx + 1, hy    , o);
                        }
                        if (outlines[x][y - 1] == 0 || depths[x][y - 1] < depth - threshold) {
                            pixmap.drawPixel(hx    , hy - 1, o);
                        }
                        if (outlines[x][y + 1] == 0 || depths[x][y + 1] < depth - threshold) {
                            pixmap.drawPixel(hx    , hy + 1, o);
                        }
                    }
                }
            }
        }
        if(dither) {
            reducer.setDitherStrength(0.75f);
            reducer.reduceScatter(pixmap);
        }

        fill(render, 0);
        fill(depths, 0);
        fill(outlines, 0);
        fill(voxels, -1);
        fill(shadeX, -1);
        fill(shadeZ, -1);
        fill(colorL, -1f);
        fill(colorA, -1f);
        fill(colorB, -1f);
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
//        System.out.printf("%dx%dx%d model:\n", size, size, size);
//        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
//        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    final byte v = colors[x][y][z];
                    if(v != 0)
                    {
                        final float c = cos_(angleTurns), s = sin_(angleTurns);
                        final float xPos = (x-hs) * c - (y-hs) * s + size;
                        final float yPos = (x-hs) * s + (y-hs) * c + size;
//                        minX = Math.min(minX, xPos);
//                        maxX = Math.max(maxX, xPos);
//                        minY = Math.min(minY, yPos);
//                        maxY = Math.max(maxY, yPos);
                        splat(xPos, yPos, z, x, y, z, v);
                    }
                }
            }
        }
//        final int size2 = size << 1;
//        if(minX < 0 || minY < 0 || maxX >= size2 || maxY >= size2) {
//            System.out.println("UH OH on angle " + angleTurns + " for size " + size + " model!");
//            System.out.printf("min X: %f, max X: %f, min Y: %f, max Y: %f\n", minX, maxX, minY, maxY);
//        }
        return blit(angleTurns);
    }

}
