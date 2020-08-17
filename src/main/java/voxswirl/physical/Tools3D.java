package voxswirl.physical;

import java.util.Arrays;

/**
 * Just laying some foundation for 3D array manipulation.
 * Created by Tommy Ettinger on 11/2/2017.
 */
public class Tools3D {

    public static byte[][][] deepCopy(byte[][][] voxels)
    {
        int xs, ys, zs;
        byte[][][] next = new byte[xs = voxels.length][ys = voxels[0].length][zs = voxels[0][0].length];
        for (int x = 0; x < xs; x++) {
            for (int y = 0; y < ys; y++) {
                System.arraycopy(voxels[x][y], 0, next[x][y], 0, zs);
            }
        }
        return next;
    }

    public static byte[][][] deepCopyInto(byte[][][] voxels, byte[][][] target)
    {
        final int xs = voxels.length, ys = voxels[0].length, zs = voxels[0][0].length;
        for (int x = 0; x < xs; x++) {
            for (int y = 0; y < ys; y++) {
                System.arraycopy(voxels[x][y], 0, target[x][y], 0, zs);
            }
        }
        return target;
    }
    public static void fill(byte[][][] array3d, int value) {
        final int depth = array3d.length;
        final int breadth = depth == 0 ? 0 : array3d[0].length;
        final int height = breadth == 0 ? 0 : array3d[0][0].length;
        if(depth > 0 && breadth > 0) {
            Arrays.fill(array3d[0][0], (byte)value);
        }
        for (int y = 1; y < breadth; y++) {
            System.arraycopy(array3d[0][0], 0, array3d[0][y], 0, height);
        }
        for (int x = 1; x < depth; x++) {
            for (int y = 0; y < breadth; y++) {
                System.arraycopy(array3d[0][0], 0, array3d[x][y], 0, height);
            }
        }
    }

    public static byte[][][] rotate(byte[][][] voxels, int turns)
    {
        int xs, ys, zs;
        byte[][][] next = new byte[xs = voxels.length][ys = voxels[0].length][zs = voxels[0][0].length];
        switch (turns & 3)
        {
            case 0:
                return deepCopy(voxels);
            case 1:
            {
                for (int x = 0; x < xs; x++) {
                    for (int y = 0; y < ys; y++) {
                        System.arraycopy(voxels[y][xs - 1 - x], 0, next[x][y], 0, zs);
                    }
                }
            }
            break;
            case 2:
            {
                for (int x = 0; x < xs; x++) {
                    for (int y = 0; y < ys; y++) {
                        System.arraycopy(voxels[xs - 1 - x][ys - 1 - y], 0, next[x][y], 0, zs);
                    }
                }
            }
            break;
            case 3:
            {
                for (int x = 0; x < xs; x++) {
                    for (int y = 0; y < ys; y++) {
                        System.arraycopy(voxels[ys - 1 - y][x], 0, next[x][y], 0, zs);
                    }
                }
            }
            break;
        }
        return next;
    }

    public static byte[][][] clockwiseInPlace(byte[][][] data) {
        final int size = data.length - 1, halfSizeXYOdd = size + 2 >>> 1, halfSizeXYEven = size + 1 >>> 1;
        byte c;
        for (int z = 0; z <= size; z++) {
            for (int x = 0; x < halfSizeXYOdd; x++) {
                for (int y = 0; y < halfSizeXYEven; y++) {

                    c = data[x][y][z];
                    data[x][y][z] = data[y][size - x][z];
                    data[y][size - x][z] = data[size - x][size - y][z];
                    data[size - x][size - y][z] = data[size - y][x][z];
                    data[size - y][x][z] = c;
                }
            }
        }
        return data;
    }
    
    public static byte[][][] mirrorX(byte[][][] voxels)
    {
        int xs, ys, zs;
        byte[][][] next = new byte[(xs = voxels.length) << 1][ys = voxels[0].length][zs = voxels[0][0].length];
        for (int x = 0; x < xs; x++) {
            for (int y = 0; y < ys; y++) {
                System.arraycopy(voxels[x][y], 0, next[x][y], 0, zs);
                System.arraycopy(voxels[x][y], 0, next[(xs << 1) - 1 - x][y], 0, zs);
            }
        }
        return next;
    }

    public static byte[][][] mirrorY(byte[][][] voxels)
    {
        int xs, ys, zs;
        byte[][][] next = new byte[xs = voxels.length][(ys = voxels[0].length) << 1][zs = voxels[0][0].length];
        for (int x = 0; x < xs; x++) {
            for (int y = 0; y < ys; y++) {
                System.arraycopy(voxels[x][y], 0, next[x][y], 0, zs);
                System.arraycopy(voxels[x][y], 0, next[x][(ys << 1) - 1 - y], 0, zs);
            }
        }
        return next;
    }

    public static byte[][][] mirrorXY(byte[][][] voxels)
    {
        int xs, ys, zs;
        byte[][][] next = new byte[(xs = voxels.length) << 1][(ys = voxels[0].length) << 1][zs = voxels[0][0].length];
        for (int x = 0; x < xs; x++) {
            for (int y = 0; y < ys; y++) {
                System.arraycopy(voxels[x][y], 0, next[x][y], 0, zs);
                System.arraycopy(voxels[x][y], 0, next[(xs << 1) - 1 - x][y], 0, zs);
                System.arraycopy(voxels[x][y], 0, next[x][(ys << 1) - 1 - y], 0, zs);
                System.arraycopy(voxels[x][y], 0, next[(xs << 1) - 1 - x][(ys << 1) - 1 - y], 0, zs);
            }
        }
        return next;
    }


    public static int countNot(byte[][][] voxels, int avoid)
    {
        final int xs = voxels.length, ys = voxels[0].length, zs = voxels[0][0].length;
        int c = 0;
        for (int x = 0; x < xs; x++) {
            for (int y = 0; y < ys; y++) {
                for (int z = 0; z < zs; z++) {
                    if(voxels[x][y][z] != avoid) ++c;
                }
            }
        }
        return c;
    }
    public static int count(byte[][][] voxels)
    {
        return countNot(voxels, 0);
    }
    public static int count(byte[][][] voxels, int match)
    {
        final int xs = voxels.length, ys = voxels[0].length, zs = voxels[0][0].length;
        int c = 0;
        byte m = (byte)match;
        for (int x = 0; x < xs; x++) {
            for (int y = 0; y < ys; y++) {
                for (int z = 0; z < zs; z++) {
                    if(voxels[x][y][z] == m) ++c;
                }
            }
        }
        return c;
    }
    public static byte[][][] runCA(byte[][][] voxels, int smoothLevel)
    {
        if(smoothLevel < 1)
            return voxels;
        final int xs = voxels.length, ys = voxels[0].length, zs = voxels[0][0].length;
        //Dictionary<byte, int> colorCount = new Dictionary<byte, int>();
        int[] colorCount = new int[256];
        byte[][][] vs0 = deepCopy(voxels), vs1 = new byte[xs][ys][zs];
        for(int v = 0; v < smoothLevel; v++)
        {
            if(v >= 1)
            {
                deepCopyInto(vs1, vs0);
                //fetch(vs1, (byte) 0);
            }
            for(int x = 0; x < xs; x++)
            {
                for(int y = 0; y < ys; y++)
                {
                    for(int z = 0; z < zs; z++)
                    {
                        Arrays.fill(colorCount, 0);
                        if(x == 0 || y == 0 || z == 0 || x == xs - 1 || y == ys - 1 || z == zs - 1 || vs0[x][y][z] == 2)
                        {
                            colorCount[vs0[x][y][z] & 255] = 10000;
                            colorCount[0] = -100000;
                        }
                        else
                        {
                            for(int xx = -1; xx < 2; xx++)
                            {
                                for(int yy = -1; yy < 2; yy++)
                                {
                                    for(int zz = -1; zz < 2; zz++)
                                    {
                                        byte smallColor = vs0[x + xx][y + yy][z + zz];
                                        colorCount[smallColor & 255]++;
                                    }
                                }
                            }
                        }
                        if(colorCount[0] >= 23)
                        {
                            vs1[x][y][z] = 0;
                        }
                        else
                        {
                            byte max = 0;
                            int cc = colorCount[0] / 3, tmp;
                            for(byte idx = 1; idx != 0; idx++)
                            {
                                tmp = colorCount[idx & 255];
                                if(tmp > 0 && tmp > cc)
                                {
                                    cc = tmp;
                                    max = idx;
                                }
                            }
                            vs1[x][y][z] = max;
                        }
                    }
                }
            }
        }
        return vs1;
    }
    
    private static void writeSlope(byte[][][] voxels, int x, int y, int z, int slope, byte color){
        voxels[x<<1][y<<1][z<<1] = ((slope & 1) != 0) ? color : 0;
        voxels[x<<1|1][y<<1][z<<1] = ((slope & 2) != 0) ? color : 0;
        voxels[x<<1][y<<1|1][z<<1] = ((slope & 4) != 0) ? color : 0;
        voxels[x<<1|1][y<<1|1][z<<1] = ((slope & 8) != 0) ? color : 0;
        voxels[x<<1][y<<1][z<<1|1] = ((slope & 16) != 0) ? color : 0;
        voxels[x<<1|1][y<<1][z<<1|1] = ((slope & 32) != 0) ? color : 0;
        voxels[x<<1][y<<1|1][z<<1|1] = ((slope & 64) != 0) ? color : 0;
        voxels[x<<1|1][y<<1|1][z<<1|1] = ((slope & 128) != 0) ? color : 0;
    }
    
    public static byte[][][] smoothScale(byte[][][] voxels){
        final int limitX = voxels.length - 1;
        final int limitY = voxels[0].length - 1;
        final int limitZ = voxels[0][0].length - 1;
        byte[][][] nextColors = new byte[limitX+1][limitY+1][limitZ+1];
        byte[][][] nextSlopes = new byte[limitX+1][limitY+1][limitZ+1];
        byte[][][] result = new byte[limitX+1<<1][limitY+1<<1][limitZ+1<<1];
        final int[] neighbors = new int[6];
        for (int x = 0; x <= limitX; x++) {
            for (int y = 0; y <= limitY; y++) {
                PER_CELL:
                for (int z = 0; z <= limitZ; z++) {
                    if(voxels[x][y][z] == 0)
                    {
                        int slope = 0;
                        if((neighbors[0] = x == 0 ? 0 : (voxels[x-1][y][z] & 255)) != 0) slope      |= 0x55;
                        if((neighbors[1] = y == 0 ? 0 : (voxels[x][y-1][z] & 255)) != 0) slope      |= 0x33;
                        if((neighbors[2] = z == 0 ? 0 : (voxels[x][y][z-1] & 255)) != 0) slope      |= 0x0F;
                        if((neighbors[3] = x == limitX ? 0 : (voxels[x+1][y][z] & 255)) != 0) slope |= 0xAA;
                        if((neighbors[4] = y == limitY ? 0 : (voxels[x][y+1][z] & 255)) != 0) slope |= 0xCC;
                        if((neighbors[5] = z == limitZ ? 0 : (voxels[x][y][z+1] & 255)) != 0) slope |= 0xF0;
                        if(Integer.bitCount(slope) < 5) // surrounded by empty or next to only one voxel
                        {
                            nextSlopes[x][y][z] = 0;
                            continue;
                        }
                        int bestIndex = -1;
                        for (int i = 0; i < 6; i++) {
                            if(neighbors[i] == 0) continue;
                            if(bestIndex == -1) bestIndex = i;
                            for (int j = i + 1; j < 6; j++) {
                                if(neighbors[i] == neighbors[j]){
                                    if((i == bestIndex || j == bestIndex) && neighbors[bestIndex] != 0) {
                                        nextColors[x][y][z] = (byte) neighbors[bestIndex];
                                        nextSlopes[x][y][z] = (byte) slope;
                                        continue PER_CELL;
                                    }
                                } else if(neighbors[bestIndex] < neighbors[i]) {
                                    bestIndex = i;
                                }
                            }
                        }
                        nextColors[x][y][z] = (byte) neighbors[bestIndex];
                        nextSlopes[x][y][z] = (byte) slope;
                    }
                    else
                    {
                        nextColors[x][y][z] = voxels[x][y][z];
                        nextSlopes[x][y][z] = -1;
                    }
                }
            }
        }

        for (int x = 0; x <= limitX; x++) {
            for (int y = 0; y <= limitY; y++) {
                PER_CELL:
                for (int z = 0; z <= limitZ; z++) {
                    if(nextColors[x][y][z] == 0)
                    {
                        int slope = 0;
                        if((neighbors[0] = x == 0 ? 0 : (nextColors[x-1][y][z] & 255)) != 0 && (nextSlopes[x-1][y][z] & 0xAA) != 0xAA) slope      |= (nextSlopes[x-1][y][z] & 0xAA) >>> 1;
                        if((neighbors[1] = y == 0 ? 0 : (nextColors[x][y-1][z] & 255)) != 0 && (nextSlopes[x][y-1][z] & 0xCC) != 0xCC) slope      |= (nextSlopes[x][y-1][z] & 0xCC) >>> 2;
                        if((neighbors[2] = z == 0 ? 0 : (nextColors[x][y][z-1] & 255)) != 0 && (nextSlopes[x][y][z-1] & 0xF0) != 0xF0) slope      |= (nextSlopes[x][y][z-1] & 0xF0) >>> 4;
                        if((neighbors[3] = x == limitX ? 0 : (nextColors[x+1][y][z] & 255)) != 0 && (nextSlopes[x+1][y][z] & 0x55) != 0x55) slope |= (nextSlopes[x+1][y][z] & 0x55) << 1;
                        if((neighbors[4] = y == limitY ? 0 : (nextColors[x][y+1][z] & 255)) != 0 && (nextSlopes[x][y+1][z] & 0x33) != 0x33) slope |= (nextSlopes[x][y+1][z] & 0x33) << 2;
                        if((neighbors[5] = z == limitZ ? 0 : (nextColors[x][y][z+1] & 255)) != 0 && (nextSlopes[x][y][z+1] & 0x0F) != 0x0F) slope |= (nextSlopes[x][y][z+1] & 0x0F) << 4;
                        if(Integer.bitCount(slope) < 4) // surrounded by empty or only one partial face
                        {
                            writeSlope(result, x, y, z, -1, (byte) 0);
                            continue;
                        }
                        int bestIndex = -1;
                        for (int i = 0; i < 6; i++) {
                            if(neighbors[i] == 0) continue;
                            if(bestIndex == -1) bestIndex = i;
                            for (int j = i + 1; j < 6; j++) {
                                if(neighbors[i] == neighbors[j]){
                                    if((i == bestIndex || j == bestIndex) && neighbors[bestIndex] != 0) {
                                        writeSlope(result, x, y, z, slope, (byte) neighbors[bestIndex]);
                                        continue PER_CELL;
                                    }
                                } else if(neighbors[bestIndex] < neighbors[i]) {
                                    bestIndex = i;
                                }
                            }
                        }
                        writeSlope(result, x, y, z, slope, (byte) neighbors[bestIndex]);
                    }
                    else
                    {
                        writeSlope(result, x, y, z, nextSlopes[x][y][z], nextColors[x][y][z]);
                    }
                }
            }
        }
        return result;
    }

    public static int firstTight(byte[][][] voxels)
    {
        final int xs = voxels.length, ys = voxels[0].length, zs = voxels[0][0].length;
        for (int x = 0; x < xs; x++) {
            for (int y = 0; y < ys; y++) {
                for (int z = 0; z < zs; z++) {
                    if (voxels[x][y][z] != 0)
                        return zs * (x * ys + y) + z;
                }
            }
        }
        return -1;
    }

    public static void findConnectors(byte[][][] voxels, int[] connectors)
    {
        Arrays.fill(connectors, -1);
        int curr;
        final int xs = voxels.length, ys = voxels[0].length, zs = voxels[0][0].length;
        for (int x = 0; x < xs; x++) {
            for (int y = 0; y < ys; y++) {
                for (int z = 0; z < zs; z++) {
                    curr = voxels[x][y][z] & 255;
                    if(curr >= 8 && curr < 16)
                        connectors[curr - 8] = zs * (x * ys + y) + z;
                    else if(curr >= 136 && curr < 144)
                        connectors[curr - 128] = zs * (x * ys + y) + z;
                }
            }
        }
    }
    public static int flood(byte[][][] base, byte[][][] bounds)
    {
        final int xs = base.length, ys = base[0].length, zs = base[0][0].length;
        int size = count(base), totalSize = 0;
        /*
        for (int x = 0; x < xs; x++) {
            for (int y = 0; y < ys; y++) {
                for (int z = 0; z < zs; z++) {
                    if(base[x][y][z] != 0 && bounds[x][y][z] != 0)
                        size++;
                }
            }
        }
        */

        byte[][][] nx = deepCopy(base);
        byte t;
        do {
            totalSize += size;
            size = 0;
            for (int x = 0; x < xs; x++) {
                for (int y = 0; y < ys; y++) {
                    for (int z = 0; z < zs; z++) {
                        if (nx[x][y][z] != 0 && (t = bounds[x][y][z]) != 0) {
                            nx[x][y][z] = t;
                            //++size;
                            if (x > 0 && nx[x - 1][y][z] == 0 && (t = bounds[x - 1][y][z]) != 0) {
                                nx[x - 1][y][z] = t;
                                ++size;
                            }
                            if (x < xs - 1 && nx[x + 1][y][z] == 0 && (t = bounds[x + 1][y][z]) != 0) {
                                nx[x + 1][y][z] = t;
                                ++size;
                            }
                            if (y > 0 && nx[x][y - 1][z] == 0 && (t = bounds[x][y - 1][z]) != 0) {
                                nx[x][y - 1][z] = t;
                                ++size;
                            }
                            if (y < ys - 1 && nx[x][y + 1][z] == 0 && (t = bounds[x][y + 1][z]) != 0) {
                                nx[x][y + 1][z] = t;
                                ++size;
                            }
                            if (z > 0 && nx[x][y][z - 1] == 0 && (t = bounds[x][y][z - 1]) != 0) {
                                nx[x][y][z - 1] = t;
                                ++size;
                            }
                            if (z < zs - 1 && nx[x][y][z + 1] == 0 && (t = bounds[x][y][z + 1]) != 0) {
                                nx[x][y][z + 1] = t;
                                ++size;
                            }
                        }
                    }
                }
            }
        } while (size != 0);
        deepCopyInto(nx, base);
        return totalSize + size;
    }

    public static byte[][][] largestPart(byte[][][] voxels)
    {
        final int xs = voxels.length, ys = voxels[0].length, zs = voxels[0][0].length;
        int fst = firstTight(voxels), bestSize = 0, currentSize, x, y, z;

        byte[][][] remaining = deepCopy(voxels), filled = new byte[xs][ys][zs],
                choice = new byte[xs][ys][zs];
        while (fst >= 0) {
            fill(filled, 0);
            x = fst / (ys * zs);
            y = (fst / zs) % ys;
            z = fst % zs;
            filled[x][y][z] = voxels[x][y][z];
            currentSize = flood(filled, remaining);
            if(currentSize > bestSize)
            {
                bestSize = currentSize;
                deepCopyInto(filled, choice);
            }

            for (x = 0; x < xs; x++) {
                for (y = 0; y < ys; y++) {
                    for (z = 0; z < zs; z++) {
                        if(filled[x][y][z] != 0)
                            remaining[x][y][z] = 0;
                    }
                }
            }
            fst = firstTight(remaining);
        }
        return choice;
    }

    public static byte[][][] translateCopy(byte[][][] voxels, int xMove, int yMove, int zMove)
    {
        int xs, ys, zs;
        byte[][][] next = new byte[xs = voxels.length][ys = voxels[0].length][zs = voxels[0][0].length];
        final int xLimit = xs - Math.abs(xMove), xStart = Math.max(0, -xMove);
        final int yLimit = ys - Math.abs(yMove), yStart = Math.max(0, -yMove);
        final int zLimit = zs - Math.abs(zMove), zStart = Math.max(0, -zMove);
        if(zLimit <= 0)
            return next;
        for (int x = xStart, xx = 0; x < xs && xx < xLimit && xx < xs; x++, xx++) {
            for (int y = yStart, yy = 0; y < ys && yy < yLimit && yy < ys; y++, yy++) {
                System.arraycopy(voxels[x][y], 0, next[xx][yy], zStart, zLimit);
            }
        }
        return next;

    }

    public static void translateCopyInto(byte[][][] voxels, byte[][][] into, int xMove, int yMove, int zMove) {
        int xs, ys, zs;
        xs = into.length;
        ys = into[0].length;
        zs = into[0][0].length;
        final int xLimit = voxels.length, xStart = Math.max(0, xMove);
        final int yLimit = voxels[0].length, yStart = Math.max(0, yMove);
        final int zLimit = voxels[0][0].length, zStart = Math.max(0, zMove);
        for (int x = xStart, xx = 0; x < xs && xx < xLimit && xx < xs; x++, xx++) {
            for (int y = yStart, yy = 0; y < ys && yy < yLimit && yy < ys; y++, yy++) {
                for (int z = zStart, zz = 0; z < zs && zz < zLimit && zz < zs; z++, zz++) {
                    if (into[x][y][z] == 0 && voxels[xx][yy][zz] != 0)
                        into[x][y][z] = voxels[xx][yy][zz];
                }
            }
        }
    }
    
    private static int isSurface(byte[][][] voxels, int x, int y, int z) {
        if(x < 0 || y < 0 || z < 0 || 
                x >= voxels.length || y >= voxels[x].length || z >= voxels[x][y].length || 
                voxels[x][y][z] == 0)
            return 0;
        if(x <= 0 || voxels[x-1][y][z] == 0) return 1;
        if(y <= 0 || voxels[x][y-1][z] == 0) return 2;
        if(z <= 0 || voxels[x][y][z-1] == 0) return 3;
        if(x >= voxels.length - 1 || voxels[x+1][y][z] == 0) return 4;
        if(y >= voxels[x].length - 1 || voxels[x][y+1][z] == 0) return 5;
        if(z >= voxels[x][y].length - 1 || voxels[x][y][z+1] == 0) return 6;
        return -1;
    }
    
    public static void soakInPlace(byte[][][] voxels)
    {
        final int xs = voxels.length, ys = voxels[0].length, zs = voxels[0][0].length;
        byte b;
        for (int x = 0; x < xs; x++) {
            for (int y = 0; y < ys; y++) {
                for (int z = 0; z < zs; z++) {
                    if(isSurface(voxels, x, y, z) > 0){
                        b = voxels[x][y][z];
                        if(isSurface(voxels, x, y, z-1) == -1) voxels[x][y][z-1] = b;
                        if(isSurface(voxels, x-1, y, z) == -1) voxels[x-1][y][z] = b;
                        if(isSurface(voxels, x, y-1, z) == -1) voxels[x][y-1][z] = b;
                        if(isSurface(voxels, x+1, y, z) == -1) voxels[x+1][y][z] = b;
                        if(isSurface(voxels, x, y+1, z) == -1) voxels[x][y+1][z] = b;
                        if(isSurface(voxels, x, y, z+1) == -1) voxels[x][y][z+1] = b;
                    }
                }
            }
        }
    }
}
