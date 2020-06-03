package fr.pasteur.iah.localzprojector.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ExtractSurfaceParameters implements Serializable
{

	private static final long serialVersionUID = -63871622455310296L;

	/**
	 * Method to use to generate a single pixel value from the z-neighborhood
	 * around the reference surface.
	 */
	public enum ProjectionMethod
	{
		/**
		 * Take the maximum intensity along Z.
		 */
		MIP( "MIP" ),
		/**
		 * Take the mean intensity along Z.
		 */
		MEAN( "Mean" ),
		/**
		 * Do not project but instead collect all the specified slices around
		 * the reference surface.
		 */
		COLLECT( "Collect" );

		private final String str;

		private ProjectionMethod( final String str )
		{
			this.str = str;
		}

		@Override
		public String toString()
		{
			return str;
		}
	}

	private final Map< Integer, Integer > offsets;

	private final Map< Integer, Integer > deltaZs;

	private final Map< Integer, ProjectionMethod > projectionMethods;

	private ExtractSurfaceParameters( final Map< Integer, Integer > offsets, final Map< Integer, Integer > deltaZs, final Map< Integer, ProjectionMethod > projectionMethods )
	{
		this.offsets = offsets;
		this.deltaZs = deltaZs;
		this.projectionMethods = projectionMethods;
	}

	/**
	 * Returns the offset to use for the specified channel.
	 * <p>
	 * The intensity will be collected around the reference plane, plus the
	 * offset specified here, in pixel units along Z. If the reference plane is
	 * at Z = 15 and the offset is equal to -4, then the intensity will be
	 * collected around Z = 11.
	 * 
	 * @param channel
	 *            the channel for which this parameter applies.
	 * @return the offset.
	 */
	public int offset( final int channel )
	{
		final Integer val = offsets.get( Integer.valueOf( channel ) );
		if ( null == val )
			return 0;
		else
			return val.intValue();
	}

	/**
	 * Returns the half-range of Z of intensity collection, to use for the
	 * specified channel.
	 * <p>
	 * The intensity will be collected around the reference plane (plus the
	 * offset) in a range from <code>ref+offset-deltaZ</code> to
	 * <code>ref+offset+deltaZ</code>, in pixel units along Z.
	 * 
	 * @param channel
	 *            the channel for which this parameter applies.
	 * @return the half-range.
	 */
	public int deltaZ( final int channel )
	{
		final Integer val = deltaZs.get( Integer.valueOf( channel ) );
		if ( null == val )
			return 0;
		else
			return val.intValue();
	}

	/**
	 * Returns the projection method to use for the specified channel.
	 * 
	 * @param channel
	 *            the channel for which this parameter applies.
	 * @return the projection method.
	 */
	public ProjectionMethod projectionMethod( final int channel )
	{
		final ProjectionMethod val = projectionMethods.get( Integer.valueOf( channel ) );
		if ( null == val )
			return ProjectionMethod.MIP;
		else
			return val;
	}

	public static class Builder
	{

		private final Map< Integer, Integer > offsets;

		private final Map< Integer, Integer > deltaZs;

		private final Map< Integer, ProjectionMethod > projectionMethods;

		private Builder()
		{
			this.offsets = new HashMap<>();
			this.deltaZs = new HashMap<>();
			this.projectionMethods = new HashMap<>();
		}

		/**
		 * Sets the offset to use for the specified channel.
		 * <p>
		 * The intensity will be collected around the reference plane, plus the
		 * offset specified here, in pixel units along Z. If the reference plane
		 * is at Z = 15 and the offset is equal to -4, then the intensity will
		 * be collected around Z = 11.
		 * 
		 * @param channel
		 *            the channel for which this parameter applies.
		 * @param offset
		 *            the offset to set for this channel, in pixel units.
		 * @return this builder.
		 */
		public Builder zOffset( final int channel, final int offset )
		{
			offsets.put( Integer.valueOf( channel ), Integer.valueOf( offset ) );
			return this;
		}

		/**
		 * Sets the half-range of Z of intensity collection, to use for the
		 * specified channel.
		 * <p>
		 * The intensity will be collected around the reference plane (plus the
		 * offset) in a range from <code>ref+offset-deltaZ</code> to
		 * <code>ref+offset+deltaZ</code>, in pixel units along Z.
		 * 
		 * @param channel
		 *            the channel for which this parameter applies.
		 * @param deltaZ
		 *            the half-range of Z, in pixel units.
		 * @return this builder.
		 */
		public Builder deltaZ( final int channel, final int deltaZ )
		{
			deltaZs.put( Integer.valueOf( channel ), Integer.valueOf( deltaZ ) );
			return this;
		}

		/**
		 * Sets the projection method to use for the specified channel.
		 * 
		 * @param channel
		 *            the channel for which this parameter applies.
		 * @param projectionMethod
		 *            the projection method.
		 * @return this builder.
		 */
		public Builder projectionMethod( final int channel, final ProjectionMethod projectionMethod )
		{
			projectionMethods.put( Integer.valueOf( channel ), projectionMethod );
			return this;
		}

		/**
		 * Creates the parameter object.
		 * 
		 * @return a new {@link ExtractSurfaceParameters} instance.
		 */
		public ExtractSurfaceParameters get()
		{
			return new ExtractSurfaceParameters( offsets, deltaZs, projectionMethods );
		}
	}

	public static Builder create()
	{
		return new Builder();
	}

	public static void serialize( final ExtractSurfaceParameters parameters, final File file ) throws FileNotFoundException, IOException
	{
		try (FileOutputStream stream = new FileOutputStream( file );
				ObjectOutputStream out = new ObjectOutputStream( stream ))
		{
			out.writeObject( parameters );
		}
	}

	public static ExtractSurfaceParameters deserialize( final File file ) throws FileNotFoundException, IOException, ClassNotFoundException
	{
		try (FileInputStream stream = new FileInputStream( file );
				ObjectInputStream in = new ObjectInputStream( stream ))
		{
			final Object readObject = in.readObject();
			return ( ExtractSurfaceParameters ) readObject;
		}
	}
}
