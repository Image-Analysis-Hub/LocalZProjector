/*-
 * #%L
 * Image Analysis Hub support for Life Scientists.
 * %%
 * Copyright (C) 2019 - 2021 IAH developers.
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the IAH / C2RT / Institut Pasteur nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package fr.pasteur.iah.localzprojector.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ExtractSurfaceParameters
{

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

	public static final ExtractSurfaceParameters df;
	static
	{
		df = ExtractSurfaceParameters.create()
				.deltaZ( 0, 2 )
				.deltaZ( 1, 2 )
				.deltaZ( 2, 2 )
				.deltaZ( 3, 2 )
				.deltaZ( 4, 2 )
				.deltaZ( 5, 2 )
				.deltaZ( 6, 2 )
				.deltaZ( 7, 2 )
				.deltaZ( 8, 2 )
				.deltaZ( 9, 2 )
				.zOffset( 0, 0 )
				.zOffset( 1, 0 )
				.zOffset( 2, 0 )
				.zOffset( 3, 0 )
				.zOffset( 4, 0 )
				.zOffset( 5, 0 )
				.zOffset( 6, 0 )
				.zOffset( 7, 0 )
				.zOffset( 8, 0 )
				.zOffset( 9, 0 )
				.projectionMethod( 0, ProjectionMethod.MIP )
				.projectionMethod( 1, ProjectionMethod.MIP )
				.projectionMethod( 2, ProjectionMethod.MIP )
				.projectionMethod( 3, ProjectionMethod.MIP )
				.projectionMethod( 4, ProjectionMethod.MIP )
				.projectionMethod( 5, ProjectionMethod.MIP )
				.projectionMethod( 6, ProjectionMethod.MIP )
				.projectionMethod( 7, ProjectionMethod.MIP )
				.projectionMethod( 8, ProjectionMethod.MIP )
				.projectionMethod( 9, ProjectionMethod.MIP )
				.get();
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
			deltaZs.put( Integer.valueOf( channel ), Integer.valueOf( Math.abs( deltaZ ) ) );
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
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final String str = gson.toJson( parameters );

		try (FileWriter writer = new FileWriter( file ))
		{
			writer.append( str );
		}
	}

	public static ExtractSurfaceParameters deserialize( final File file ) throws FileNotFoundException, IOException, ClassNotFoundException
	{
		try (FileReader reader = new FileReader( file ))
		{
			final String str = Files.lines( Paths.get( file.getAbsolutePath() ) )
					.collect( Collectors.joining( System.lineSeparator() ) );

			final Gson gson = new GsonBuilder().setPrettyPrinting().create();
			final ExtractSurfaceParameters params = ( str == null || str.isEmpty() )
					? df
					: gson.fromJson( str, ExtractSurfaceParameters.class );

			return params;
		}
	}
}
