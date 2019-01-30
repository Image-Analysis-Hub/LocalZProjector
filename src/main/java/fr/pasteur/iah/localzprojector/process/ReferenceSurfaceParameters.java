package fr.pasteur.iah.localzprojector.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ReferenceSurfaceParameters implements Serializable
{

	private static final long serialVersionUID = -7529278459519251051L;

	public final Method method;

	public final int halfWindowSize;

	public final int zMin;

	public final int zMax;

	public final double sigma;

	public final int medianHalfSize;

	public final int targetChannel;

	public final int binning;

	private ReferenceSurfaceParameters( final int targetChannel, final Method method, final int halfWindowSize, final int zMin, final int zMax, final double sigma, final int medianSize, final int binning )
	{
		this.targetChannel = targetChannel;
		this.method = method;
		this.halfWindowSize = halfWindowSize;
		this.zMin = zMin;
		this.zMax = zMax;
		this.sigma = sigma;
		this.medianHalfSize = medianSize;
		this.binning = binning;
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

		private int binning = 1;

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

		public Builder binning( final int binning )
		{
			this.binning = binning;
			return this;
		}

		public ReferenceSurfaceParameters get()
		{
			boolean ok = true;
			final StringBuilder message = new StringBuilder();
			if ( binning < 1 )
			{
				message.append( "\nBinning cannot be lower than 1. Was " + binning + "." );
				ok = false;
			}

			if ( !ok )
				throw new IllegalArgumentException( "Error building ReferenceSurfaceParameters:" + message.toString() );

			return new ReferenceSurfaceParameters(
					targetChannel,
					method,
					halfWindowSize,
					Math.min( zMin, zMax ),
					Math.max( zMin, zMax ),
					sigma,
					medianHalfSize,
					binning );
		}
	}

	public static void serialize( final ReferenceSurfaceParameters parameters, final File file ) throws FileNotFoundException, IOException
	{
		try (FileOutputStream stream = new FileOutputStream( file );
				ObjectOutputStream out = new ObjectOutputStream( stream ))
		{
			out.writeObject( parameters );
		}
	}

	public static ReferenceSurfaceParameters deserialize( final File file ) throws FileNotFoundException, IOException, ClassNotFoundException
	{
		try (FileInputStream stream = new FileInputStream( file );
				ObjectInputStream in = new ObjectInputStream( stream ))
		{
			final Object readObject = in.readObject();
			return ( ReferenceSurfaceParameters ) readObject;
		}
	}
}
