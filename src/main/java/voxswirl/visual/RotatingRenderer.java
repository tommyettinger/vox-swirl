package voxswirl.visual;

import com.badlogic.gdx.graphics.Pixmap;
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
        pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmapHalf = new Pixmap(w>>>1, h>>>1, Pixmap.Format.RGBA8888);
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
        final int threshold = 9;
        pixmap.setColor(0);
        pixmap.fill();
        int xSize = Math.min(pixmap.getWidth(), working.length) - 1, ySize = Math.min(pixmap.getHeight(), working[0].length) - 1, depth;
        for (int x = 0; x <= xSize; x++) {
            System.arraycopy(working[x], 0, render[x], 0, ySize);
        }

        int v, vx, vy, vz, fx, fy, fz;
        float hs = (size) * 0.5f;
        final float cYaw = cos_(yaw), sYaw = sin_(yaw);
        final float cPitch = cos_(pitch), sPitch = sin_(pitch);
        final float cRoll = cos_(roll), sRoll = sin_(roll);
        for (int sx = 0; sx <= xSize; sx++) {
            for (int sy = 0; sy <= ySize; sy++) {
                if((v = voxels[sx][sy]) != -1) {
                    vx = v & 0x3FF;
                    vy = v >>> 10 & 0x3FF;
                    vz = v >>> 20 & 0x3FF;
                    final float x1 = (vx-hs) * cYaw - (vy-hs) * sYaw;
                    final float y1 = (vx-hs) * sYaw + (vy-hs) * sYaw;
                    final float z1 = vz - hs;
                    final float x2 = x1;
                    final float y2 = y1 * sPitch - z1 * cPitch;
                    final float z2 = y1 * sPitch + z1 * cPitch;
                    fx = (int)(z2 * sRoll + x2 * cRoll + hs + 4.500f);
                    fy = (int)(y2 + hs + 4.500f);
                    fz = (int)(z2 * cRoll - x2 * sRoll + hs + 4.500f);
                    if (Math.abs(shadeZ[fx][fy] - fz) < 1)
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
                    if (Math.abs(shadeX[fy][fz] - fx) > 1)
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

    public Pixmap blitHalf(float yaw, float pitch, float roll) {
        final int threshold = 8;
        pixmapHalf.setColor(0);
        pixmapHalf.fill();
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
                    if (Math.abs(shadeZ[fx][fy] - fz) < 1)
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
                    if (Math.abs(shadeX[fy][fz] - fx) > 1)
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

    public Pixmap drawSplats(byte[][][] colors, float yaw, float pitch, float roll, IntMap<VoxMaterial> materialMap) {
        this.materialMap = materialMap;
        final int size = colors.length;
        final float hs = (size) * 0.5f;
        final float cYaw = cos_(yaw), sYaw = sin_(yaw);
        final float cPitch = cos_(pitch), sPitch = sin_(pitch);
        final float cRoll = cos_(roll), sRoll = sin_(roll);
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    final byte v = colors[x][y][z];
                    if(v != 0)
                    {
                        final float x1 = (x-hs) * cYaw - (y-hs) * sYaw;
                        final float y1 = (x-hs) * sYaw + (y-hs) * sYaw;
                        final float z1 = z - hs;
                        final float x2 = x1;
                        final float y2 = y1 * sPitch - z1 * cPitch;
                        final float z2 = y1 * sPitch + z1 * cPitch;
                        splat(z2 * sRoll + x2 * cRoll + hs, y2 + hs, z2 * cRoll - x2 * sRoll + hs, x, y, z, v);
                    }
                }
            }
        }
        return blit(yaw, pitch, roll);
    }

    public Pixmap drawSplatsHalf(byte[][][] colors, float yaw, float pitch, float roll, IntMap<VoxMaterial> materialMap) {
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
        return blitHalf(yaw, pitch, roll);
    }

}
