package fr.pasteur.iah.localzprojector.process;

public class ReferenceSurfaceParameters
{

	public final Method method;

	public final int halfWindowSize;

	public final int zMin;

	public final int zMax;

	public final double sigma;

	public final int medianHalfSize;

	public final int targetChannel;

	private ReferenceSurfaceParameters( final int targetChannel, final Method method, final int halfWindowSize, final int zMin, final int zMax, final double sigma, final int medianSize )
	{
		this.targetChannel = targetChannel;
		this.method = method;
		this.halfWindowSize = halfWindowSize;
		this.zMin = zMin;
		this.zMax = zMax;
		this.sigma = sigma;
		this.medianHalfSize = medianSize;
	}

	public enum Method
	{

		MAX_OF_MEAN( "Max of mean" ),
		MAX_OF_STD( "Max of std" );

		private final String str;

		private Method( final String str )
		{
			this.str = str;
		}

		@Override
		public String toString()
		{
			return str;
		}
	}

	public static final Builder create()
	{
		return new Builder();
	}

	public static class Builder
	{

		private Builder()
		{}

		private Method method = Method.MAX_OF_MEAN;

		private int zMin = -1;

		private int zMax = -1;

		private int halfWindowSize = 10;

		private double sigma = -1.;

		private int medianHalfSize = -1;

		private int targetChannel = 0;

		public Builder method( final Method method )
		{
			this.method = method;
			return this;
		}

		public Builder zMin( final int zMin )
		{
			this.zMin = zMin;
			return this;
		}

		public Builder zMax( final int zMax )
		{
			this.zMax = zMax;
			return this;
		}

		public Builder halfWindowSize( final int halfWindowSize )
		{
			this.halfWindowSize = halfWindowSize;
			return this;
		}

		public Builder gaussianPreFilter( final double sigma )
		{
			this.sigma = sigma;
			return this;
		}

		public Builder medianPostFilterHalfSize( final int halfSize )
		{
			this.medianHalfSize = halfSize;
			return this;
		}

		public Builder targetChannel( final int targetChannel )
		{
			this.targetChannel = targetChannel;
			return this;
		}

		public ReferenceSurfaceParameters get()
		{
			return new ReferenceSurfaceParameters(
					targetChannel,
					method,
					halfWindowSize,
					Math.min( zMin, zMax ),
					Math.max( zMin, zMax ),
					sigma,
					medianHalfSize );
		}
	}
}
