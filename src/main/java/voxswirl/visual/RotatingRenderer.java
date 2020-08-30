package voxswirl.visual;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntMap;
import voxswirl.physical.VoxMaterial;

import static voxswirl.meta.ArrayTools.fill;
import static voxswirl.meta.TrigTools.cos_;
import static voxswirl.meta.TrigTools.sin_;

/**
 * Renders {@code byte[][][]} voxel models to {@link Pixmap}s with arbitrary yaw rotation.
 */
public class RotatingRenderer extends SplatRenderer {
    public RotatingRenderer(final int size) {
        this.size = size;
        final int w = size * 6 + 4, h = size * 6 + 4;
        pixmap = new Pixmap(w>>>1, h>>>1, Pixmap.Format.RGBA8888);
        working =  new int[w][h];
        render =   new int[w][h];
        outlines = new int[w][h];
        depths =   new int[w][h];
        materials = new VoxMaterial[w][h];
        voxels = fill(-1, w, h);
        shadeX = fill(-1, size * 3 + 10 >> 1, size * 3 + 10 >> 1);
        shadeZ = fill(-1, size * 3 + 10 >> 1, size * 3 + 10 >> 1);
    }

    public Pixmap blit(float yaw, float pitch, float roll) {
        final int threshold = 8;
        pixmap.setColor(0);
        pixmap.fill();
        int xSize = working.length - 1, ySize = working[0].length - 1, depth;
        for (int x = 0; x <= xSize; x++) {
            System.arraycopy(working[x], 0, render[x], 0, ySize);
        }
        int v, vx, vy, vz, fx, fy, fz;
        float hs = (size) * 0.5f, ox, oy, oz;
        final float cYaw = cos_(yaw), sYaw = sin_(yaw);
        final float cPitch = cos_(pitch), sPitch = sin_(pitch);
        final float cRoll = cos_(roll), sRoll = sin_(roll);
        final float x_x = cYaw * cPitch, y_x = cYaw * sPitch * sRoll - sYaw * cRoll, z_x = cYaw * sPitch * cRoll + sYaw * sRoll;
        final float x_y = sYaw * cPitch, y_y = sYaw * sPitch * sRoll + cYaw * cRoll, z_y = sYaw * sPitch * cRoll - cYaw * sRoll;
        final float x_z = -sPitch, y_z = cPitch * sRoll, z_z = cPitch * cRoll;
        VoxMaterial m;
        boolean direct;
        for (int sx = 0; sx <= xSize; sx++) {
            for (int sy = 0; sy <= ySize; sy++) {
                if((v = voxels[sx][sy]) != -1) {
                    vx = v & 0x3FF;
                    vy = v >>> 10 & 0x3FF;
                    vz = v >>> 20 & 0x3FF;
                    ox = vx - hs;
                    oy = vy - hs;
                    oz = vz - hs;
                    fx = (int)(ox * x_x + oy * y_x + oz * z_x + hs + 4.500f);
                    fy = (int)(ox * x_y + oy * y_y + oz * z_y + hs + 4.500f);
                    fz = (int)(ox * x_z + oy * y_z + oz * z_z + hs + 4.500f);
                    m = materials[sx][sy];
                    direct = false;
                    if (Math.abs(shadeZ[fx][fy] - fz) < 1)
                    {
                        direct = true;
                        render[sx][sy] = Coloring.adjust(render[sx][sy], 1.1f, midUp);
                        float spread = MathUtils.lerp(1.033f, 1f, m.getTrait(VoxMaterial.MaterialTrait._rough));
                        if(sx > 0) render[sx-1][sy] = Coloring.adjust(render[sx-1][sy], spread, smallUp);
                        if(sy > 0) render[sx][sy-1] = Coloring.adjust(render[sx][sy-1], spread, smallUp);
                        if(sx < xSize) render[sx+1][sy] = Coloring.adjust(render[sx+1][sy], spread, smallUp);
                        if(sy < ySize) render[sx][sy+1] = Coloring.adjust(render[sx][sy+1], spread, smallUp);

                        if(sx > 1) render[sx-2][sy] = Coloring.adjust(render[sx-2][sy], spread, smallUp);
                        if(sy > 1) render[sx][sy-2] = Coloring.adjust(render[sx][sy-2], spread, smallUp);
                        if(sx < xSize-1) render[sx+2][sy] = Coloring.adjust(render[sx+2][sy], spread, smallUp);
                        if(sy < ySize-1) render[sx][sy+2] = Coloring.adjust(render[sx][sy+2], spread, smallUp);
                    }
                    if (Math.abs(shadeX[fy][fz] - fx) > 1)
                    {
                        direct = false;
                        render[sx][sy] = Coloring.adjust(render[sx][sy], 0.95f, smallDown);
                        float spread = MathUtils.lerp(0.974f, 1f, m.getTrait(VoxMaterial.MaterialTrait._rough));
                        if(sx > 0) render[sx-1][sy] = Coloring.adjust(render[sx-1][sy], spread, tinyDown);
                        if(sy > 0) render[sx][sy-1] = Coloring.adjust(render[sx][sy-1], spread, tinyDown);
                        if(sx < xSize) render[sx+1][sy] = Coloring.adjust(render[sx+1][sy], spread, tinyDown);
                        if(sy < ySize) render[sx][sy+1] = Coloring.adjust(render[sx][sy+1], spread, tinyDown);

                        if(sx > 1) render[sx-2][sy] = Coloring.adjust(render[sx-2][sy], spread, tinyDown);
                        if(sy > 1) render[sx][sy-2] = Coloring.adjust(render[sx][sy-2], spread, tinyDown);
                        if(sx < xSize-1) render[sx+2][sy] = Coloring.adjust(render[sx+2][sy], spread, tinyDown);
                        if(sy < ySize-1) render[sx][sy+2] = Coloring.adjust(render[sx][sy+2], spread, tinyDown);
                    }
                    if(direct)
                    {
                        render[sx][sy] = Coloring.adjust(render[sx][sy], 0.8f + m.getTrait(VoxMaterial.MaterialTrait._ior), m.getTrait(VoxMaterial.MaterialTrait._metal) * 0.375f + 0.85f);
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
                        if (outlines[x - 1][y] == 0 || depths[x - 2][y] < depth - threshold) {
                            pixmap.drawPixel(hx - 1, hy    , o);
                        }
                        if (outlines[x + 1][y] == 0 || depths[x + 2][y] < depth - threshold) {
                            pixmap.drawPixel(hx + 1, hy    , o);
                        }
                        if (outlines[x][y - 1] == 0 || depths[x][y - 2] < depth - threshold) {
                            pixmap.drawPixel(hx    , hy - 1, o);
                        }
                        if (outlines[x][y + 1] == 0 || depths[x][y + 2] < depth - threshold) {
                            pixmap.drawPixel(hx    , hy + 1, o);
                        }
                    }
                }
            }
        }
        if(dither) {
            reducer.setDitherStrength(0.5f);
            reducer.reduceBlueNoise(pixmap);
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
        return pixmap;
    }

    // To move one x+ in voxels is x + 2, y - 1 in pixels.
    // To move one x- in voxels is x - 2, y + 1 in pixels.
    // To move one y+ in voxels is x - 2, y - 1 in pixels.
    // To move one y- in voxels is x + 2, y + 1 in pixels.
    // To move one z+ in voxels is y + 3 in pixels.
    // To move one z- in voxels is y - 3 in pixels.

    public Pixmap drawSplats(byte[][][] colors, float yaw, float pitch, float roll, IntMap<VoxMaterial> materialMap) {
        this.materialMap = materialMap;
        final int size = colors.length;
        final float hs = (size) * 0.5f;
        float ox, oy, oz; // offset x,y,z
        final float cYaw = cos_(yaw), sYaw = sin_(yaw);
        final float cPitch = cos_(pitch), sPitch = sin_(pitch);
        final float cRoll = cos_(roll), sRoll = sin_(roll);
        final float x_x = cYaw * cPitch, y_x = cYaw * sPitch * sRoll - sYaw * cRoll, z_x = cYaw * sPitch * cRoll + sYaw * sRoll;
        final float x_y = sYaw * cPitch, y_y = sYaw * sPitch * sRoll + cYaw * cRoll, z_y = sYaw * sPitch * cRoll - cYaw * sRoll;
        final float x_z = -sPitch, y_z = cPitch * sRoll, z_z = cPitch * cRoll;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    final byte v = colors[x][y][z];
                    if(v != 0)
                    {
                        ox = x - hs;
                        oy = y - hs;
                        oz = z - hs;
                        splat(ox * x_x + oy * y_x + oz * z_x + hs,
                                ox * x_y + oy * y_y + oz * z_y + hs,
                                ox * x_z + oy * y_z + oz * z_z + hs, x, y, z, v);
                    }
                }
            }
        }
        return blit(yaw, pitch, roll);
    }

}
