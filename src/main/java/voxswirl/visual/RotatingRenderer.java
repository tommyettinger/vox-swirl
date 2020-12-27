package voxswirl.visual;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import voxswirl.physical.VoxMaterial;

import static voxswirl.meta.ArrayTools.fill;
import static com.github.tommyettinger.colorful.TrigTools.cos_;
import static com.github.tommyettinger.colorful.TrigTools.sin_;

/**
 * Renders {@code byte[][][]} voxel models to {@link Pixmap}s with arbitrary yaw, pitch, and roll rotation.
 */
public class RotatingRenderer extends SplatRenderer {
    public RotatingRenderer(final int size) {
        this.size = size;
        final int w = size * 6 + 4, h = size * 6 + 4;
        pixmap = new Pixmap(w>>>1, h>>>1, Pixmap.Format.RGBA8888);
        render =   new int[w][h];
        outlines = new int[w][h];
        depths =   new int[w][h];
        materials = new VoxMaterial[w][h];
        voxels = fill(-1, w, h);
        shadeX = fill(-1f, size * 4, size * 4);
        shadeZ = fill(-1f, size * 4, size * 4);
        colorI = fill(-1f, w, h);
        colorP = fill(-1f, w, h);
        colorT = fill(-1f, w, h);
        remade = new byte[size << 1][size << 1][size << 1];
    }

    // To move one x+ in voxels is x + 2, y - 1 in pixels.
    // To move one x- in voxels is x - 2, y + 1 in pixels.
    // To move one y+ in voxels is x - 2, y - 1 in pixels.
    // To move one y- in voxels is x + 2, y + 1 in pixels.
    // To move one z+ in voxels is y + 3 in pixels.
    // To move one z- in voxels is y - 3 in pixels.

    public Pixmap drawSplats(byte[][][] colors, float yaw, float pitch, float roll, IntMap<VoxMaterial> materialMap) {
        this.materialMap = materialMap;
        seed += TimeUtils.millis() * 0x632BE59BD9B4E019L;        
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
                        splat(  ox * x_x + oy * y_x + oz * z_x + size,
                                ox * x_y + oy * y_y + oz * z_y + size,
                                ox * x_z + oy * y_z + oz * z_z + hs, x, y, z, v);
                    }
                }
            }
        }
        return blit(yaw, pitch, roll);
    }

}
