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
	    if(y == 0 && x >= 0) return 0;
	    final float ax = Math.abs(x), ay = Math.abs(y);
	    final float a = ax / ay, s = a * a;
	    float r = (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a) * 0.15915494309189535f;
	    if(ax < ay) r = 0.25f - r;
	    return (x < 0) ? (y < 0) ? 0.5f + r : 0.5f - r
			   : (y < 0) ? 1.0f - r : r;
	}

}
