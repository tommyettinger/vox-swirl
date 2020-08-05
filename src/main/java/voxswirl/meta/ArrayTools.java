package voxswirl.meta;

import java.util.Arrays;

/**
 * Created by Tommy Ettinger on 8/4/2020.
 */
public class ArrayTools {
	/**
	 * Creates a 2D array of the given width and height, filled with entirely with the value contents.
	 * You may want to use {@link #fill(int[][], int)} to modify an existing 2D array instead.
	 * @param contents the value to fill the array with
	 * @param width    the desired width
	 * @param height   the desired height
	 * @return a freshly allocated 2D array of the requested dimensions, filled entirely with contents
	 */
	public static int[][] fill(int contents, int width, int height) {
		int[][] next = new int[width][height];
		for (int x = 0; x < width; x++) {
			Arrays.fill(next[x], contents);
		}
		return next;
	}
	/**
	 * Fills {@code array2d} with {@code value}.
	 * Not to be confused with {@link #fill(int, int, int)}, which makes a new 2D array.
	 * @param array2d a 2D array that will be modified in-place
	 * @param value the value to fill all of array2D with
	 */
	public static void fill(int[][] array2d, int value) {
		final int width = array2d.length;
		for (int i = 0; i < width; i++) {
			Arrays.fill(array2d[i], value);
		}
	}
}
