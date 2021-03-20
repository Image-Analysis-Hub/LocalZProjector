package fr.pasteur.iah.localzprojector.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ReferenceSurfaceParameters
{

	public final Method method;

	public final int filterWindowSize;

	public final int zMin;

	public final int zMax;

	public final double sigma;

	public final int medianHalfSize;

	public final int targetChannel;

	public final int binning;

	private ReferenceSurfaceParameters( final int targetChannel, final Method method, final int filterWindowSize, final int zMin, final int zMax, final double sigma, final int medianSize, final int binning )
	{
		this.targetChannel = targetChannel;
		this.method = method;
		this.filterWindowSize = filterWindowSize;
		this.zMin = zMin;
		this.zMax = zMax;
		this.sigma = sigma;
		this.medianHalfSize = medianSize;
		this.binning = binning;
	}

	public enum Method
	{

		MAX_OF_MEAN( "Max of mean" ),
		MAX_OF_STD( "Max of std" ),
		SPARSE_MAX_OF_MEAN( "Mean max on grid" ),
		SPARSE_MAX_OF_STD( "Std max on grid" );

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

	public static final ReferenceSurfaceParameters df;
	static 
	{
		df = ReferenceSurfaceParameters.create()
				.binning( 6 )
				.filterWindowSize( 6 )
				.gaussianPreFilter( 0.7 )
				.medianPostFilterHalfSize( 4 )
				.method( Method.MAX_OF_STD )
				.get();
	}

	public static class Builder
	{

		private Builder()
		{}

		private Method method = Method.MAX_OF_MEAN;

		private int zMin = -1;

		private int zMax = -1;

		private int filterWindowSize = 10;

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

		/**
		 * The window size over which to filter, without taking into account the
		 * binning value.
		 * 
		 * @param filterWindowSize
		 *            the filter window size.
		 * @return this builder.
		 */
		public Builder filterWindowSize( final int filterWindowSize )
		{
			this.filterWindowSize = filterWindowSize;
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
					filterWindowSize,
					Math.min( zMin, zMax ),
					Math.max( zMin, zMax ),
					sigma,
					medianHalfSize,
					binning );
		}
	}

	public static void serialize( final ReferenceSurfaceParameters parameters, final File file ) throws FileNotFoundException, IOException
	{
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final String str = gson.toJson( parameters );

		try (FileWriter writer = new FileWriter( file ))
		{
			writer.append( str );
		}
	}

	public static ReferenceSurfaceParameters deserialize( final File file ) throws FileNotFoundException, IOException, ClassNotFoundException
	{
		try (FileReader reader = new FileReader( file ))
		{
			final String str = Files.lines( Paths.get( file.getAbsolutePath() ) )
					.collect( Collectors.joining( System.lineSeparator() ) );

			final Gson gson = new GsonBuilder().setPrettyPrinting().create();
			final ReferenceSurfaceParameters params = ( str == null || str.isEmpty() )
					? df
					: gson.fromJson( str, ReferenceSurfaceParameters.class );

			return params;
		}
	}
}
