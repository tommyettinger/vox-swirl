package voxswirl.visual;

import com.badlogic.gdx.graphics.Pixmap;
import squidpony.ArrayTools;
import squidpony.squidmath.NumberTools;

/**
 * Created by Tommy Ettinger on 12/16/2018.
 */
public class SplatRenderer {
    public Pixmap pixmap, pixmapHalf;
    public int[][] depths, voxels;
    public int[][] shadeX, shadeZ;
    public int[][] working, render, outlines;
    public Colorizer color = Colorizer.ManosColorizer;
    public boolean dither = false, outline = true;
    public int size;

    public SplatRenderer (final int size) {
        this.size = size;
        final int w = size * 4 + 4, h = size * 5 + 4;
        pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmapHalf = new Pixmap(w>>>1, h>>>1, Pixmap.Format.RGBA8888);
        working =  new int[w][h];
        render =   new int[w][h];
        outlines = new int[w][h];
        depths =   new int[w][h];
        voxels   = new int[w][h];
        shadeX = ArrayTools.fill(-1, size<<2, size<<2);
        shadeZ = ArrayTools.fill(-1, size<<2, size<<2);
    }

    public Colorizer colorizer () {
        return color;
    }

    public SplatRenderer colorizer (Colorizer color) {
        this.color = color;
        return this;
    }
    
    public void splat(int xPos, int yPos, int zPos, byte voxel) {
        final int size = shadeZ.length,
                xx = Math.max(0, (size + yPos - xPos) * 2 - 1),
                yy = Math.max(0, (zPos * 3 + size + size - xPos - yPos) - 1),
                depth = (xPos + yPos) * 2 + zPos * 3;
        for (int x = 0, ax = xx; x < 4 && ax < working.length; x++, ax++) {
            for (int y = 0, ay = yy; y < 4 && ay < working[0].length; y++, ay++) {
                //if((x == 0 || x == 3) && (y == 0 || y == 3)) continue;
                working[ax][ay] = color.medium(voxel);
                depths[ax][ay] = depth;
                outlines[ax][ay] = color.dark(voxel);
                voxels[ax][ay] = xPos | yPos << 10 | zPos << 20; 
            }
        }
        shadeZ[xPos][yPos] = Math.max(shadeZ[xPos][yPos], zPos);
        shadeX[yPos][zPos] = Math.max(shadeX[yPos][zPos], xPos);
    }
    
    public void splatTurned(float xPos, float yPos, float zPos, int vx, int vy, int vz, byte voxel) {
        final int 
                xx = (int)(0.5f + Math.max(0, (size + yPos - xPos) * 2 - 1)),
                yy = (int)(0.5f + Math.max(0, (zPos * 3 + size + size - xPos - yPos) - 1)),
                depth = (int)(0.5f + (xPos + yPos) * 2 + zPos * 3);
        boolean drawn = false;
        for (int x = 0, ax = xx; x < 4 && ax < working.length; x++, ax++) {
            for (int y = 0, ay = yy; y < 4 && ay < working[0].length; y++, ay++) {
                if (depth >= depths[ax][ay]) {
                    drawn = true;
                    //if((x == 0 || x == 3) && (y == 0 || y == 3)) continue;
                    working[ax][ay] = color.medium(voxel);
                    depths[ax][ay] = depth;
                    outlines[ax][ay] = color.dark(voxel);
                    voxels[ax][ay] = vx | vy << 10 | vz << 20;
                }
            }
        }
        if(drawn) {
            shadeZ[(int) (6.5f + xPos)][(int) (6.5f + yPos)] = Math.max(shadeZ[(int) (6.5f + xPos)][(int) (6.5f + yPos)], (int) (6.5f + zPos));
            shadeX[(int) (6.5f + yPos)][(int) (6.5f + zPos)] = Math.max(shadeX[(int) (6.5f + yPos)][(int) (6.5f + zPos)], (int) (6.5f + xPos));
        }
    }
    
    public SplatRenderer clear() {
        pixmap.setColor(0);
        pixmap.fill();
        ArrayTools.fill(working, (byte) 0);
        ArrayTools.fill(depths, 0);
        ArrayTools.fill(outlines, (byte) 0);
        ArrayTools.fill(voxels, -1);
        ArrayTools.fill(shadeX, -1);
        ArrayTools.fill(shadeZ, -1);
        return this;
    }
    
    public Pixmap blit() {
        final int threshold = 9;
        pixmap.setColor(0);
        pixmap.fill();
        int xSize = Math.min(pixmap.getWidth(), working.length) - 1, ySize = Math.min(pixmap.getHeight(), working[0].length) - 1, depth;
        for (int x = 0; x <= xSize; x++) {
            System.arraycopy(working[x], 0, render[x], 0, ySize);
        }
        //for (int x = 0; x < xSize; x++) {
        //    for (int y = 0; y < ySize; y++) {
        //        render[x][y] = Coloring.darken(working[x][y], 0.1f);
        //    }
        //}
        int v, vx, vy, vz;
        for (int sx = 0; sx <= xSize; sx++) {
            for (int sy = 0; sy <= ySize; sy++) {
                if((v = voxels[sx][sy]) != -1) {
                    vx = v & 0x3FF;
                    vy = v >>> 10 & 0x3FF;
                    vz = v >>> 20 & 0x3FF;
                    if (shadeX[vy][vz] != vx)
                        render[sx][sy] = Coloring.darken(render[sx][sy], 0.15f);
                    if (shadeZ[vx][vy] == vz)
                        render[sx][sy] = Coloring.lighten(render[sx][sy], 0.2f);
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
//                        if (outlines[x - 1][y] == 0 && outlines[x][y - 1] == 0) {
//                            pixmap.drawPixel(x - 1, y    , o);
//                            pixmap.drawPixel(x    , y - 1, o);
//                            pixmap.drawPixel(x    , y    , o);
//                        } else if (outlines[x + 1][y] == 0 && outlines[x][y - 1] == 0) {
//                            pixmap.drawPixel(x + 1, y    , o);
//                            pixmap.drawPixel(x    , y - 1, o);
//                            pixmap.drawPixel(x    , y    , o); 
//                        } else if (outlines[x - 1][y] == 0 && outlines[x][y + 1] == 0) {
//                            pixmap.drawPixel(x - 1, y    , o);
//                            pixmap.drawPixel(x    , y + 1, o);
//                            pixmap.drawPixel(x    , y    , o);
//                        } else if (outlines[x + 1][y] == 0 && outlines[x][y + 1] == 0) {
//                            pixmap.drawPixel(x + 1, y    , o);
//                            pixmap.drawPixel(x    , y + 1, o);
//                            pixmap.drawPixel(x    , y    , o);
//                        } else {
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
            color.reducer.setDitherStrength(0.3125f);
            color.reducer.reduceFloydSteinberg(pixmap);
//            color.reducer.reduceKnollRoberts(pixmap);
//            color.reducer.reduceSierraLite(pixmap);
//            color.reducer.reduceJimenez(pixmap);

        }

        ArrayTools.fill(render, (byte) 0);
        ArrayTools.fill(working, (byte) 0);
        ArrayTools.fill(depths, 0);
        ArrayTools.fill(outlines, (byte) 0);
        ArrayTools.fill(voxels, -1);
        ArrayTools.fill(shadeX, -1);
        ArrayTools.fill(shadeZ, -1);
        return pixmap;
    }
    public Pixmap blit(float turns) {
        final int threshold = 9;
        pixmap.setColor(0);
        pixmap.fill();
        int xSize = Math.min(pixmap.getWidth(), working.length) - 1, ySize = Math.min(pixmap.getHeight(), working[0].length) - 1, depth;
        for (int x = 0; x <= xSize; x++) {
            System.arraycopy(working[x], 0, render[x], 0, ySize);
        }
        //for (int x = 0; x < xSize; x++) {
        //    for (int y = 0; y < ySize; y++) {
        //        render[x][y] = Coloring.darken(working[x][y], 0.1f);
        //    }
        //}
        int v, vx, vy, vz, fx, fy;
        float hs = size * 0.5f, c = NumberTools.cos_(turns), s = NumberTools.sin_(turns);
        for (int sx = 0; sx <= xSize; sx++) {
            for (int sy = 0; sy <= ySize; sy++) {
                if((v = voxels[sx][sy]) != -1) {
                    vx = v & 0x3FF;
                    vy = v >>> 10 & 0x3FF;
                    vz = v >>> 20 & 0x3FF;
                    fx = (int)((vx-hs) * c - (vy-hs) * s + hs + 6.5f);
                    fy = (int)((vx-hs) * s + (vy-hs) * c + hs + 6.5f);
                    if (Math.abs(shadeX[fy][vz] - fx) > 1)
                        render[sx][sy] = Coloring.darken(render[sx][sy], 0.15f);
                    if (shadeZ[fx][fy] == vz)
                        render[sx][sy] = Coloring.lighten(render[sx][sy], 0.2f);
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
//                        if (outlines[x - 1][y] == 0 && outlines[x][y - 1] == 0) {
//                            pixmap.drawPixel(x - 1, y    , o);
//                            pixmap.drawPixel(x    , y - 1, o);
//                            pixmap.drawPixel(x    , y    , o);
//                        } else if (outlines[x + 1][y] == 0 && outlines[x][y - 1] == 0) {
//                            pixmap.drawPixel(x + 1, y    , o);
//                            pixmap.drawPixel(x    , y - 1, o);
//                            pixmap.drawPixel(x    , y    , o); 
//                        } else if (outlines[x - 1][y] == 0 && outlines[x][y + 1] == 0) {
//                            pixmap.drawPixel(x - 1, y    , o);
//                            pixmap.drawPixel(x    , y + 1, o);
//                            pixmap.drawPixel(x    , y    , o);
//                        } else if (outlines[x + 1][y] == 0 && outlines[x][y + 1] == 0) {
//                            pixmap.drawPixel(x + 1, y    , o);
//                            pixmap.drawPixel(x    , y + 1, o);
//                            pixmap.drawPixel(x    , y    , o);
//                        } else {
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
            color.reducer.setDitherStrength(0.3125f);
            color.reducer.reduceFloydSteinberg(pixmap);
//            color.reducer.reduceKnollRoberts(pixmap);
//            color.reducer.reduceSierraLite(pixmap);
//            color.reducer.reduceJimenez(pixmap);

        }

        ArrayTools.fill(render, (byte) 0);
        ArrayTools.fill(working, (byte) 0);
        ArrayTools.fill(depths, 0);
        ArrayTools.fill(outlines, (byte) 0);
        ArrayTools.fill(voxels, -1);
        ArrayTools.fill(shadeX, -1);
        ArrayTools.fill(shadeZ, -1);
        return pixmap;
    }

    public Pixmap blitHalf() {
        final int threshold = 8;
        pixmapHalf.setColor(0);
        pixmapHalf.fill();
        int xSize = working.length - 1, ySize = working[0].length - 1, depth;
        for (int x = 0; x <= xSize; x++) {
            System.arraycopy(working[x], 0, render[x], 0, ySize);
        }
        int v, vx, vy, vz;
        for (int sx = 0; sx <= xSize; sx++) {
            for (int sy = 0; sy <= ySize; sy++) {
                if((v = voxels[sx][sy]) != -1)
                {
                    vx = v & 0x3FF;
                    vy = v >>> 10 & 0x3FF;
                    vz = v >>> 20 & 0x3FF;
                    if (shadeX[vy][vz] != vx)
                        render[sx][sy] = Coloring.darken(render[sx][sy], 0.15f);
                    if (shadeZ[vx][vy] == vz)
                        render[sx][sy] = Coloring.lighten(render[sx][sy], 0.2f);
                }
            }
        }

        for (int x = 0; x <= xSize; x++) {
//            final int choice = (x & 2) >>> 1 | (x & 1) << 1;
            for (int y = 0; y <= ySize; y++) {
//                if((y & 3) != choice) continue;
                if (render[x][y] != 0) {
                    pixmapHalf.drawPixel(x >>> 1, y >>> 1, render[x][y]);
                }
            }
        }
        if (outline) {
            int o;
            for (int x = 1; x < xSize; x++) {
//                final int choice = (x & 2) >>> 1 | (x & 1) << 1;
                final int hx = x >>> 1;
                for (int y = 1; y < ySize; y++) {
//                    if((y & 3) != choice) continue;
                    int hy = y >>> 1;
                    if ((o = outlines[x][y]) != 0) {
                        depth = depths[x][y];
//                        if (outlines[x - 1][y] == 0 && outlines[x][y - 1] == 0) {
//                            pixmapHalf.drawPixel(hx - 1, hy    , o);
//                            pixmapHalf.drawPixel(hx    , hy - 1, o);
//                            pixmapHalf.drawPixel(hx    , hy    , o);
//                        } else if (outlines[x + 1][y] == 0 && outlines[x][y - 1] == 0) {
//                            pixmapHalf.drawPixel(hx + 1, hy    , o);
//                            pixmapHalf.drawPixel(hx    , hy - 1, o);
//                            pixmapHalf.drawPixel(hx    , hy    , o); 
//                        } else if (outlines[x - 1][y] == 0 && outlines[x][y + 1] == 0) {
//                            pixmapHalf.drawPixel(hx - 1, hy    , o);
//                            pixmapHalf.drawPixel(hx    , hy + 1, o);
//                            pixmapHalf.drawPixel(hx    , hy    , o);
//                        } else if (outlines[x + 1][y] == 0 && outlines[x][y + 1] == 0) {
//                            pixmapHalf.drawPixel(hx + 1, hy    , o);
//                            pixmapHalf.drawPixel(hx    , hy + 1, o);
//                            pixmapHalf.drawPixel(hx    , hy    , o);
//                        } else {
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
//                        }
                    }
                }
            }
        }
        if(dither) {
            color.reducer.setDitherStrength(0.3125f);
            color.reducer.reduceFloydSteinberg(pixmapHalf);
//            color.reducer.reduceKnollRoberts(pixmapHalf);
//            color.reducer.reduceSierraLite(pixmapHalf);
//            color.reducer.reduceJimenez(pixmapHalf);
        }

        ArrayTools.fill(render, (byte) 0);
        ArrayTools.fill(working, (byte) 0);
        ArrayTools.fill(depths, 0);
        ArrayTools.fill(outlines, (byte) 0);
        ArrayTools.fill(voxels, -1);
        ArrayTools.fill(shadeX, -1);
        ArrayTools.fill(shadeZ, -1);
        return pixmapHalf;
    }

    public Pixmap drawSplats(byte[][][] colors) {
        // To move one x+ in voxels is x + 2, y - 1 in pixels.
        // To move one x- in voxels is x - 2, y + 1 in pixels.
        // To move one y+ in voxels is x - 2, y - 1 in pixels.
        // To move one y- in voxels is x + 2, y + 1 in pixels.
        // To move one z+ in voxels is y + 3 in pixels.
        // To move one z- in voxels is y - 3 in pixels.
        final int size = colors.length;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    final byte v = colors[x][y][z];
                    if(v != 0)
                        splat(x, y, z, v);
                }
            }
        }
        return blit();
    }

    public Pixmap drawSplats(byte[][][] colors, float angleTurns) {
        // To move one x+ in voxels is x + 2, y - 1 in pixels.
        // To move one x- in voxels is x - 2, y + 1 in pixels.
        // To move one y+ in voxels is x - 2, y - 1 in pixels.
        // To move one y- in voxels is x + 2, y + 1 in pixels.
        // To move one z+ in voxels is y + 3 in pixels.
        // To move one z- in voxels is y - 3 in pixels.
        final int size = colors.length;
        final float hs = size * 0.5f;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    final byte v = colors[x][y][z];
                    if(v != 0)
                    {
                        final float c = NumberTools.cos_(angleTurns), s = NumberTools.sin_(angleTurns);
                        splatTurned((x-hs) * c - (y-hs) * s + hs, (x-hs) * s + (y-hs) * c + hs, z, x, y, z, v);
                    }
                }
            }
        }
        return blit(angleTurns);
    }

    public Pixmap drawSplatsHalf(byte[][][] colors) { 
        final int size = colors.length;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    final byte v = colors[x][y][z];
                    if(v != 0)
                        splat(x, y, z, v);
                }
            }
        }
        return blitHalf();
    }

}
