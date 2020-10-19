package voxswirl.meta;

import com.badlogic.gdx.math.MathUtils;

/**
 * Created by Tommy Ettinger on 8/4/2020.
 */
public class TrigTools {
	public static float sin_(float turns){
		return MathUtils.sinDeg(turns * 360);
	}
	public static float cos_(float turns){
		return MathUtils.cosDeg(turns * 360);
	}
	public static float atan2_(final float y, final float x)
	{
		if(y == 0.0 && x >= 0.0) return 0f;
		final float ax = Math.abs(x), ay = Math.abs(y);
		if(ax < ay)
		{
			final float a = ax / ay, s = a * a,
					r = 0.25f - (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a) * 0.15915494309189535f;
			return (x < 0.0f) ? (y < 0.0f) ? 0.5f + r : 0.5f - r : (y < 0.0f) ? 1f - r : r;
		}
		else {
			final float a = ay / ax, s = a * a,
					r = (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a) * 0.15915494309189535f;
			return (x < 0.0f) ? (y < 0.0f) ? 0.5f + r : 0.5f - r : (y < 0.0f) ? 1f - r : r;
		}
	}
	public static float swayRandomized(final int seed, float value)
	{
		final int floor = value >= 0f ? (int) value : (int) value - 1;
		int z = seed + floor;
		final float start = (((z = (z ^ 0xD1B54A35) * 0x1D2BC3)) * ((z ^ z >>> 15) | 0xFFE00001) ^ z ^ z << 11) * 0x0.ffffffp-31f,
				end = (((z = (seed + floor + 1 ^ 0xD1B54A35) * 0x1D2BC3)) * ((z ^ z >>> 15) | 0xFFE00001) ^ z ^ z << 11) * 0x0.ffffffp-31f;
		value -= floor;
		value *= value * (3 - 2 * value);
		return (1 - value) * start + value * end;
	}

}
