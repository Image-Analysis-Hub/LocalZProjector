package fr.pasteur.iah.localzprojector.process;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.StorelessUnivariateStatistic;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Max;

public class ExtractSurfaceParameters
{

	public enum ProjectionMethod
	{
		MIP( "Maximum intensity Projection" ),
		MEAN( "Mean intensity projection" );

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

		public StorelessUnivariateStatistic projector()
		{
			switch ( this )
			{
			case MEAN:
				return new Mean();
			case MIP:
				return new Max();
			default:
				throw new IllegalArgumentException( "Unknown projection method: " + this + "." );

			}
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

	public int offset( final int channel )
	{
		final Integer val = offsets.get( Integer.valueOf( channel ) );
		if ( null == val )
			return 0;
		else
			return val.intValue();
	}

	public int deltaZ( final int channel )
	{
		final Integer val = deltaZs.get( Integer.valueOf( channel ) );
		if ( null == val )
			return 0;
		else
			return val.intValue();
	}

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

		public Builder zOffset( final int channel, final int offset )
		{
			offsets.put( Integer.valueOf( channel ), Integer.valueOf( offset ) );
			return this;
		}

		public Builder deltaZ( final int channel, final int deltaZ )
		{
			deltaZs.put( Integer.valueOf( channel ), Integer.valueOf( deltaZ ) );
			return this;
		}

		public Builder projectionMethod( final int channel, final ProjectionMethod projectionMethod )
		{
			projectionMethods.put( Integer.valueOf( channel ), projectionMethod );
			return this;
		}

		public ExtractSurfaceParameters get()
		{
			return new ExtractSurfaceParameters( offsets, deltaZs, projectionMethods );
		}
	}

	public static Builder create()
	{
		return new Builder();
	}
}
