package voxswirl.visual;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.github.tommyettinger.anim8.PaletteReducer;
import voxswirl.physical.VoxMaterial;

import static voxswirl.meta.ArrayTools.fill;
import static voxswirl.meta.TrigTools.cos_;
import static voxswirl.meta.TrigTools.sin_;

/**
 * Renders {@code byte[][][]} voxel models to {@link Pixmap}s with arbitrary yaw rotation.
 */
public class SplatRenderer {
    public Pixmap pixmap, pixmapHalf;
    public int[][] depths, voxels, working, render, outlines;
    public VoxMaterial[][] materials;
    public int[][] shadeX, shadeZ;
    public PaletteReducer color = new PaletteReducer();
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
        pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmapHalf = new Pixmap(w>>>1, h>>>1, Pixmap.Format.RGBA8888);
        working =  new int[w][h];
        render =   new int[w][h];
        outlines = new int[w][h];
        depths =   new int[w][h];
        materials = new VoxMaterial[w][h];
        voxels = fill(-1, w, h);
        shadeX = fill(-1, size + 5 << 1, size + 5 << 1);
        shadeZ = fill(-1, size + 5 << 1, size + 5 << 1);
    }
    protected static float hash(final int x, final int y, final int z, final int w, final int u) {
        final int s = x * 0x1C3360 ^ y * 0x18DA3A ^ z * 0x15E6DA ^ w * 0x134D28 ^ u * 0x110280;
        return ((s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >>> 8) * 0x1p-24f;
    }
    
    protected float random(){
        return (((seed = seed * 0xD1342543DE82EF95L + 0x632BE59BD9B4E019L) >>> 41) +
                ((seed = seed * 0xD1342543DE82EF95L + 0x632BE59BD9B4E019L) >>> 41)) * 0x1p-24f;
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

    public PaletteReducer palette() {
        return color;
    }

    public SplatRenderer palette(PaletteReducer color) {
        this.color = color;
        return this;
    }

    public void splat(float xPos, float yPos, float zPos, int vx, int vy, int vz, byte voxel) {
        final int 
                xx = (int)(0.5f + Math.max(0, (size + yPos - xPos) * 2 + 1)),
                yy = (int)(0.5f + Math.max(0, (zPos * 3 + size + size - xPos - yPos) + 1)),
                depth = (int)(0.5f + (xPos + yPos) * 2 + zPos * 3);
        boolean drawn = false;
        final VoxMaterial m = materialMap.get(voxel & 255);
        final float emit = m.getTrait(VoxMaterial.MaterialTrait._emit) * 1.25f;
        final float alpha = m.getTrait(VoxMaterial.MaterialTrait._alpha);
        for (int x = 0, ax = xx; x < 4 && ax < working.length; x++, ax++) {
            for (int y = 0, ay = yy; y < 4 && ay < working[0].length; y++, ay++) {
                if (depth >= depths[ax][ay] && (alpha == 0f || random() >= alpha)) {
                    drawn = true;
                    working[ax][ay] = Coloring.adjust(color.paletteArray[voxel & 255], 1f, neutral);
                    depths[ax][ay] = depth;
                    materials[ax][ay] = m;
                    outlines[ax][ay] = alpha > 0f ? 0 : Coloring.adjust(color.paletteArray[voxel & 255], 0.625f + emit, bigUp);
                    voxels[ax][ay] = vx | vy << 10 | vz << 20;
                }
            }
        }
        if(drawn) {
            shadeZ[(int) (4.500f + xPos)][(int) (4.500f + yPos)] = Math.max(shadeZ[(int) (4.500f + xPos)][(int) (4.500f + yPos)], (int) (4.500f + zPos));
            shadeX[(int) (4.500f + yPos)][(int) (4.500f + zPos)] = Math.max(shadeX[(int) (4.500f + yPos)][(int) (4.500f + zPos)], (int) (4.500f + xPos));
        }
    }
    
    public SplatRenderer clear() {
        pixmap.setColor(0);
        pixmap.fill();
        fill(working, (byte) 0);
        fill(depths, 0);
        fill(outlines, (byte) 0);
        fill(voxels, -1);
        fill(shadeX, -1);
        fill(shadeZ, -1);
        return this;
    }

    public Pixmap blit(float turns) {
        final int threshold = 9;
        pixmap.setColor(0);
        pixmap.fill();
        int xSize = Math.min(pixmap.getWidth(), working.length) - 1, ySize = Math.min(pixmap.getHeight(), working[0].length) - 1, depth;
        for (int x = 0; x <= xSize; x++) {
            System.arraycopy(working[x], 0, render[x], 0, ySize);
        }

        int v, vx, vy, vz, fx, fy;
        float hs = (size) * 0.5f, c = cos_(turns), s = sin_(turns);
        for (int sx = 0; sx <= xSize; sx++) {
            for (int sy = 0; sy <= ySize; sy++) {
                if((v = voxels[sx][sy]) != -1) {
                    vx = v & 0x3FF;
                    vy = v >>> 10 & 0x3FF;
                    vz = v >>> 20 & 0x3FF;
                    fx = (int)((vx-hs) * c - (vy-hs) * s + hs + 4.500f);
                    fy = (int)((vx-hs) * s + (vy-hs) * c + hs + 4.500f);
                    if (shadeZ[fx][fy] == vz+4)
                    {
                        render[sx][sy] = Coloring.adjust(render[sx][sy], 1.1f, midUp);
                        if(sx > 0) render[sx-1][sy] = Coloring.adjust(render[sx-1][sy], 1.030f, smallUp);
                        if(sy > 0) render[sx][sy-1] = Coloring.adjust(render[sx][sy-1], 1.030f, smallUp);
                        if(sx < xSize) render[sx+1][sy] = Coloring.adjust(render[sx+1][sy], 1.030f, smallUp);
                        if(sy < ySize) render[sx][sy+1] = Coloring.adjust(render[sx][sy+1], 1.030f, smallUp);

                        if(sx > 1) render[sx-2][sy] = Coloring.adjust(render[sx-2][sy], 1.030f, smallUp);
                        if(sy > 1) render[sx][sy-2] = Coloring.adjust(render[sx][sy-2], 1.030f, smallUp);
                        if(sx < xSize-1) render[sx+2][sy] = Coloring.adjust(render[sx+2][sy], 1.030f, smallUp);
                        if(sy < ySize-1) render[sx][sy+2] = Coloring.adjust(render[sx][sy+2], 1.030f, smallUp);
                    }
                    if (Math.abs(shadeX[fy][vz + 4] - fx) > 1)
                    {
                        render[sx][sy] = Coloring.adjust(render[sx][sy], 0.95f, smallDown);
                        if(sx > 0) render[sx-1][sy] = Coloring.adjust(render[sx-1][sy], 0.977f, tinyDown);
                        if(sy > 0) render[sx][sy-1] = Coloring.adjust(render[sx][sy-1], 0.977f, tinyDown);
                        if(sx < xSize) render[sx+1][sy] = Coloring.adjust(render[sx+1][sy], 0.977f, tinyDown);
                        if(sy < ySize) render[sx][sy+1] = Coloring.adjust(render[sx][sy+1], 0.977f, tinyDown);

                        if(sx > 1) render[sx-2][sy] = Coloring.adjust(render[sx-2][sy], 0.977f, tinyDown);
                        if(sy > 1) render[sx][sy-2] = Coloring.adjust(render[sx][sy-2], 0.977f, tinyDown);
                        if(sx < xSize-1) render[sx+2][sy] = Coloring.adjust(render[sx+2][sy], 0.977f, tinyDown);
                        if(sy < ySize-1) render[sx][sy+2] = Coloring.adjust(render[sx][sy+2], 0.977f, tinyDown);
                    }

                }
            }
        }

        for (int x = 0; x <= xSize; x++) {
            for (int y = 0; y <= ySize; y++) {
                if (render[x][y] != 0) {
                    pixmap.drawPixel(x, y, render[x][y]);
                }
            }
        }
        if (outline) {
            int o;
            for (int x = 1; x < xSize; x++) {
                for (int y = 1; y < ySize; y++) {
                    if ((o = outlines[x][y]) != 0) {
                        depth = depths[x][y];
                            if (outlines[x - 1][y] == 0 || depths[x - 1][y] < depth - threshold) {
                                pixmap.drawPixel(x - 1, y    , o);
                            }
                            if (outlines[x + 1][y] == 0 || depths[x + 1][y] < depth - threshold) {
                                pixmap.drawPixel(x + 1, y    , o);
                            }
                            if (outlines[x][y - 1] == 0 || depths[x][y - 1] < depth - threshold) {
                                pixmap.drawPixel(x    , y - 1, o);
                            }
                            if (outlines[x][y + 1] == 0 || depths[x][y + 1] < depth - threshold) {
                                pixmap.drawPixel(x    , y + 1, o);
                            }
//                        }
                    }
                }
            }
        }
        if(dither) {
            color.setDitherStrength(0.3125f);
            color.reduceBlueNoise(pixmapHalf);
//            color.reduceFloydSteinberg(pixmapHalf);
//            color.reducer.reduceKnollRoberts(pixmap);
//            color.reducer.reduceSierraLite(pixmap);
//            color.reducer.reduceJimenez(pixmap);

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

    public Pixmap blitHalf(float turns) {
        final int threshold = 8;
        pixmapHalf.setColor(0);
        pixmapHalf.fill();
        int xSize = working.length - 1, ySize = working[0].length - 1, depth;
        for (int x = 0; x <= xSize; x++) {
            System.arraycopy(working[x], 0, render[x], 0, ySize);
        }
        int v, vx, vy, vz, fx, fy;
        float hs = (size) * 0.5f, c = cos_(turns), s = sin_(turns);
        for (int sx = 0; sx <= xSize; sx++) {
            for (int sy = 0; sy <= ySize; sy++) {
                if((v = voxels[sx][sy]) != -1) {
                    vx = v & 0x3FF;
                    vy = v >>> 10 & 0x3FF;
                    vz = v >>> 20 & 0x3FF;
                    fx = (int)((vx-hs) * c - (vy-hs) * s + hs + 4.500f);
                    fy = (int)((vx-hs) * s + (vy-hs) * c + hs + 4.500f);
                    if (shadeZ[fx][fy] == vz+4)
                    {
                        render[sx][sy] = Coloring.adjust(render[sx][sy], 1.1f, midUp);
                        if(sx > 0) render[sx-1][sy] = Coloring.adjust(render[sx-1][sy], 1.030f, smallUp);
                        if(sy > 0) render[sx][sy-1] = Coloring.adjust(render[sx][sy-1], 1.030f, smallUp);
                        if(sx < xSize) render[sx+1][sy] = Coloring.adjust(render[sx+1][sy], 1.030f, smallUp);
                        if(sy < ySize) render[sx][sy+1] = Coloring.adjust(render[sx][sy+1], 1.030f, smallUp);

                        if(sx > 1) render[sx-2][sy] = Coloring.adjust(render[sx-2][sy], 1.030f, smallUp);
                        if(sy > 1) render[sx][sy-2] = Coloring.adjust(render[sx][sy-2], 1.030f, smallUp);
                        if(sx < xSize-1) render[sx+2][sy] = Coloring.adjust(render[sx+2][sy], 1.030f, smallUp);
                        if(sy < ySize-1) render[sx][sy+2] = Coloring.adjust(render[sx][sy+2], 1.030f, smallUp);
                    }
                    if (Math.abs(shadeX[fy][vz + 4] - fx) > 1)
                    {
                        render[sx][sy] = Coloring.adjust(render[sx][sy], 0.95f, smallDown);
                        if(sx > 0) render[sx-1][sy] = Coloring.adjust(render[sx-1][sy], 0.977f, tinyDown);
                        if(sy > 0) render[sx][sy-1] = Coloring.adjust(render[sx][sy-1], 0.977f, tinyDown);
                        if(sx < xSize) render[sx+1][sy] = Coloring.adjust(render[sx+1][sy], 0.977f, tinyDown);
                        if(sy < ySize) render[sx][sy+1] = Coloring.adjust(render[sx][sy+1], 0.977f, tinyDown);

                        if(sx > 1) render[sx-2][sy] = Coloring.adjust(render[sx-2][sy], 0.977f, tinyDown);
                        if(sy > 1) render[sx][sy-2] = Coloring.adjust(render[sx][sy-2], 0.977f, tinyDown);
                        if(sx < xSize-1) render[sx+2][sy] = Coloring.adjust(render[sx+2][sy], 0.977f, tinyDown);
                        if(sy < ySize-1) render[sx][sy+2] = Coloring.adjust(render[sx][sy+2], 0.977f, tinyDown);
                    }
                }
            }
        }

        for (int x = 0; x <= xSize; x++) {
            for (int y = 0; y <= ySize; y++) {
                if (render[x][y] != 0) {
                    pixmapHalf.drawPixel(x >>> 1, y >>> 1, render[x][y]);
                }
            }
        }
        if (outline) {
            int o;
            for (int x = 1; x < xSize; x++) { 
                final int hx = x >>> 1;
                for (int y = 1; y < ySize; y++) {
                    int hy = y >>> 1;
                    if ((o = outlines[x][y]) != 0) {
                        depth = depths[x][y];
                        if (outlines[x - 1][y] == 0 || depths[x - 1][y] < depth - threshold) {
                            pixmapHalf.drawPixel(hx - 1, hy    , o);
                        }
                        if (outlines[x + 1][y] == 0 || depths[x + 1][y] < depth - threshold) {
                            pixmapHalf.drawPixel(hx + 1, hy    , o);
                        }
                        if (outlines[x][y - 1] == 0 || depths[x][y - 1] < depth - threshold) {
                            pixmapHalf.drawPixel(hx    , hy - 1, o);
                        }
                        if (outlines[x][y + 1] == 0 || depths[x][y + 1] < depth - threshold) {
                            pixmapHalf.drawPixel(hx    , hy + 1, o);
                        }
                    }
                }
            }
        }
        if(dither) {
            color.setDitherStrength(0.3125f);
            color.reduceBlueNoise(pixmapHalf);
//            color.reduceFloydSteinberg(pixmapHalf);
//            color.reducer.reduceKnollRoberts(pixmapHalf);
//            color.reducer.reduceSierraLite(pixmapHalf);
//            color.reducer.reduceJimenez(pixmapHalf);
        }

        fill(render, 0);
        fill(working, 0);
        fill(depths, 0);
        fill(outlines, 0);
        fill(voxels, -1);
        fill(shadeX, -1);
        fill(shadeZ, -1);
        return pixmapHalf;
    }

    // To move one x+ in voxels is x + 2, y - 1 in pixels.
    // To move one x- in voxels is x - 2, y + 1 in pixels.
    // To move one y+ in voxels is x - 2, y - 1 in pixels.
    // To move one y- in voxels is x + 2, y + 1 in pixels.
    // To move one z+ in voxels is y + 3 in pixels.
    // To move one z- in voxels is y - 3 in pixels.

    public Pixmap drawSplats(byte[][][] colors, float angleTurns, IntMap<VoxMaterial> materialMap) {
        this.materialMap = materialMap;
        seed = (TimeUtils.millis() >>> 5) * 0x632BE59BD9B4E019L;
//        seed = Tools3D.hash64(colors);
        final int size = colors.length;
        final float hs = (size) * 0.5f;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    final byte v = colors[x][y][z];
                    if(v != 0)
                    {
                        final float c = cos_(angleTurns), s = sin_(angleTurns);
                        splat((x-hs) * c - (y-hs) * s + hs, (x-hs) * s + (y-hs) * c + hs, z, x, y, z, v);
                    }
                }
            }
        }
        return blit(angleTurns);
    }

    public Pixmap drawSplatsHalf(byte[][][] colors, float angleTurns, IntMap<VoxMaterial> materialMap) {
        this.materialMap = materialMap;
        seed = (TimeUtils.millis() >>> 5) * 0x632BE59BD9B4E019L;
//        seed = Tools3D.hash64(colors);
        final int size = colors.length;
        final float hs = (size) * 0.5f;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    final byte v = colors[x][y][z];
                    if(v != 0)
                    {
                        final float c = cos_(angleTurns), s = sin_(angleTurns);
                        splat((x-hs) * c - (y-hs) * s + hs, (x-hs) * s + (y-hs) * c + hs, z, x, y, z, v);
                    }
                }
            }
        }
        return blitHalf(angleTurns);
    }

}
