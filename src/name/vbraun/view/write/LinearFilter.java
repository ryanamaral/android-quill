package name.vbraun.view.write;

import android.util.Log;
import junit.framework.Assert;

public class LinearFilter {
	private final static String TAG = "LinearFilter";

	/*
	 * Linear Filter (convolution of input signal with a kernel)
	 * 
	 * Let s = sigma be the width parameter (standard deviation). The continuum
	 * Gaussian kernel is
	 * 
	 * 1/sqrt(2*pi*s) * exp((-x^2)/(2*s))
	 * 
	 * It is the solution to the heat equation, so it smoothens in the same way
	 * as heat spreads out in a homogeneous medium. The discrete Gaussian kernel
	 * is the analogous solution for the discrete heat equation, and has Bessel
	 * functions instead of exponentials
	 * 
	 * exp(-s) * bessel_I(x, s)
	 * 
	 * For lange s, the discrete Gaussian kernel approximates the continuum
	 * version. Either version has non-zero weights for all x, so it must be
	 * truncated somehow. In Quill, the parameter s is chosen such that 99% of
	 * the filter weight are within the range. This is for s=0.7673452983475789
	 * and s=4.343082292959792 for the filters with 5 and 11 support points,
	 * respectively.
	 * 
	 * The Savitzky-Golay kernel is completely different. There, the values are
	 * approximated by a polynomial. It is always of finite length (the degree
	 * of the approximating polynomial)
	 * 
	 * References:
	 * 
	 * - http://en.wikipedia.org/wiki/Gaussian_filter
	 * 
     * - Tony Lindeberg: Scale-Space for Discrete Signals
     *   IEEE Transactions of Pattern Analysis and Machine Intelligence, 12(3), 234--254, 1990.
	 *
	 * - http://en.wikipedia.org/wiki/Savitzky–Golay_smoothing_filter
	 * 
	 */
	
	public static final LinearFilter[] Kernel = { 
		new LinearFilter(Filter.KERNEL_NONE, 
				new float[] { 1f }),
		new LinearFilter(Filter.KERNEL_GAUSSIAN_5, 
				new float[] { 0.535139208712f, 
							  0.191553166229f, 
							  0.0358772294153f }),
		new LinearFilter(Filter.KERNEL_GAUSSIAN_11, 
				new float[] { 0.197952965559f, 
				              0.173313792797f, 
				              0.118141540812f, 
				              0.0645048567988f, 
				              0.0290276086587f, 
				              0.0110357181537f }),
		new LinearFilter(Filter.KERNEL_SAVITZKY_GOLAY_5, 
				new float[] { 17f, 12f, -3f }),
		new LinearFilter(Filter.KERNEL_SAVITZKY_GOLAY_11, 
				new float[] { 89f, 84f, 69f, 44f, 9f, -36f }), 
	};

	public enum Filter {
		KERNEL_NONE, KERNEL_GAUSSIAN_5, KERNEL_GAUSSIAN_11, KERNEL_SAVITZKY_GOLAY_5, KERNEL_SAVITZKY_GOLAY_11
	}

	public static LinearFilter get(Filter id) {
		return Kernel[id.ordinal()];
	}
	
	/**
	 * The corresponding enum Filter for this kernel.
	 */
	protected final Filter kernel;

	/**
	 * The normalized weights. Only half of the weights are stored (the weights
	 * with non-negative indices).
	 */
	protected final float[] weight;

	/**
	 * The half length (the number of weights with non-negative index).
	 */
	protected final int length;

	private LinearFilter(Filter kernel, final float[] weight) {
		this.kernel = kernel;
		this.length = weight.length;
		this.weight = weight;
		Assert.assertTrue(weight.length == length);
		float sum = weight[0];
		for (int i = 1; i < length; i++)
			sum += 2 * weight[i];
		float normalization = 1f / sum;
		for (int i = 0; i < length; i++)
			this.weight[i] *= normalization;
	}
	
	/**
	 * Apply the filter to an array
	 * @param x the array of floats to operate on. Will be mutated.
	 */
	public void apply(float[] x) {
		if (kernel == Filter.KERNEL_NONE)
			return;
		final int N = x.length;
		float[] raw_x = x.clone();
		int i;
		float sum, norm;

		// split [0 .. N-1] into [ 0, .., i_pre-1] + [i_pre, ..., i_post-1], [i_post, ..., N-1 ] 
		// such that i_pre = length-1 and i_post = N-length if there is enough space
		int i_pre  = Math.min(length-1, N/2);
		int i_post = Math.max(N-length, N/2);
		
		for (i=0; i<i_pre; i++) {
			norm = weight[0];
			sum = raw_x[i] * weight[0];
			for (int n=1; n<=i; n++) {
				sum += (raw_x[i-n] + raw_x[i+n]) * weight[n];
				norm += 2 * weight[n];
			}
			//x[i] = sum / norm;
		}
		
		for (i=i_pre; i<i_post; i++) {
			sum = raw_x[i] * weight[0];
			for (int n=1; n<length; n++)
				sum += (raw_x[i-n] + raw_x[i+n]) * weight[n];
			x[i] = sum;
		}
		
		for (i=i_post; i<N; i++) {
			norm = weight[0];
			sum = raw_x[i] * weight[0];
			for (int n=1; n<N-i; n++) {
				sum += (raw_x[i-n] + raw_x[i+n]) * weight[n];
				norm += 2 * weight[n];
			}
			x[i] = sum / norm;
		}
	}
}



