package voxswirl;

import org.junit.Assert;
import org.junit.Test;
import voxswirl.meta.TrigTools;

/**
 * Created by Tommy Ettinger on 8/11/2020.
 */
public class TrigTests {
	public static long randomize(long state)
	{
		return (state = ((state = (state ^ (state << 41 | state >>> 23) ^ (state << 17 | state >>> 47) ^ 0xD1B54A32D192ED03L) * 0xAEF17502108EF2D9L) ^ state >>> 43 ^ state >>> 31 ^ state >>> 23) * 0xDB4F0B9175AE2165L) ^ state >>> 28;
	}
	public static float atan2_alt(final float y, final float x)
	{
		if(y == 0 && x >= 0) return 0;
		final float ax = Math.abs(x), ay = Math.abs(y);
		final float a = ax / ay, s = a * a;
		float r = (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a) * 0.15915494309189535f;
		if(ax < ay) r = 0.25f - r;
		return (x < 0) ? (y < 0) ? 0.5f + r : 0.5f - r
				: (y < 0) ? 1.0f - r : r;
	}

	@Test(expected = AssertionError.class)
	public void testAtan2(){
		float y = 0, x = 0;
		long r;
		for (int i = 0; i < 1000; i++) {
			Assert.assertEquals("Failed on iteration " + i + " with x=" + x + ", y=" + y,         TrigTools.atan2_(y, x), atan2_alt(y, x), 0.0001f);
			Assert.assertEquals("Failed on reverse iteration " + i + " with x=" + y + ", y=" + x, TrigTools.atan2_(x, y), atan2_alt(x, y), 0.0001f);
			x = ((r = randomize(i)) >>> 50) * 0x1p-10f;
			y = (randomize(r) >>> 50) * 0x1p-10f;
		}
	}

	@Test
	public void testAtan2Correctness(){
		float y = 0, x = 0;
		long r;
		for (int i = 0; i < 1000; i++) {
			Assert.assertEquals("Failed on iteration " + i + " with x=" + x + ", y=" + y,         (Math.atan2(y, x) * 0.5 / Math.PI + 1.0) % 1.0, TrigTools.atan2_(y, x), 0.001);
			Assert.assertEquals("Failed on reverse iteration " + i + " with x=" + y + ", y=" + x, (Math.atan2(x, y) * 0.5 / Math.PI + 1.0) % 1.0, TrigTools.atan2_(x, y), 0.001);
			x = ((r = randomize(i)) >>> 50) * 0x1p-10f;
			y = (randomize(r) >>> 50) * 0x1p-10f;
		}
	}

	@Test(expected = AssertionError.class)
	public void testAtan2AltCorrectness(){
		float y = 0, x = 0;
		long r;
		for (int i = 0; i < 1000; i++) {
			Assert.assertEquals("Failed on iteration " + i + " with x=" + x + ", y=" + y,         (Math.atan2(y, x) * 0.5 / Math.PI + 1.0) % 1.0, atan2_alt(y, x), 0.001);
			Assert.assertEquals("Failed on reverse iteration " + i + " with x=" + y + ", y=" + x, (Math.atan2(x, y) * 0.5 / Math.PI + 1.0) % 1.0, atan2_alt(x, y), 0.001);
			x = ((r = randomize(i + 11)) >>> 50) * 0x1p-10f;
			y = (randomize(r) >>> 50) * 0x1p-10f;
		}
	}
}
