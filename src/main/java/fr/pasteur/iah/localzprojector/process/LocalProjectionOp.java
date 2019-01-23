package fr.pasteur.iah.localzprojector.process;

import org.apache.commons.math3.stat.descriptive.StorelessUnivariateStatistic;
import org.scijava.Cancelable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.ops.OpService;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

@Plugin( type = LocalProjectionOp.class )
public class LocalProjectionOp< T extends RealType< T > & NativeType< T > > extends AbstractBinaryFunctionOp< Dataset, RandomAccessibleInterval< IntType >, Dataset > implements Cancelable
{

	@Parameter
	private LocalProjectionParameters params;

	@Parameter
	private OpService ops;

	private String cancelReason;

	@Override
	public Dataset calculate( final Dataset source, final RandomAccessibleInterval< IntType > referenceSurface )
	{
		// Prepare.
		cancelReason = null;

		// Create output.
		@SuppressWarnings( "unchecked" )
		final Img< T > img = ( Img< T > ) source.getImgPlus().getImg();


		final int channelAxis = source.dimensionIndex( Axes.CHANNEL );
		if ( channelAxis < 0 )
		{
			// Check input.
			if ( source.numDimensions() != 3 )
				throw new IllegalArgumentException( "Expected single-channel source to be 3D, but was " + source.numDimensions() + "D." );

			// Create output.
			final Img< T > output = ops.create().img( referenceSurface, Util.getTypeFromInterval( img ) );

			// Process.
			processChannel( img, 0, referenceSurface, output );

			// Make a new dataset with the output.
			final ImgPlus< T > imgPlus = new ImgPlus<>( output, "Local projection of " + source.getName(),
					source.axis( 0 ),
					source.axis( 1 ) );
			return new DefaultDataset( ops.context(), imgPlus );
		}
		else
		{

			// Check input.
			if ( source.numDimensions() != 4 )
				throw new IllegalArgumentException( "Expected multi-channel source to be 4D (3D+C), but was " + source.numDimensions() + "D." );

			// Create output.
			final long nChannels = img.dimension( channelAxis );
			final long[] dims = new long[ 3 ];
			dims[ 0 ] = source.dimension( 0 );
			dims[ 1 ] = source.dimension( 1 );
			dims[ 2 ] = nChannels;
			final Dimensions dimensions = new FinalDimensions( dims );
			final Img< T > output = ops.create().img( dimensions, Util.getTypeFromInterval( img ) );

			// Process.
			for ( int c = 0; c < nChannels; c++ )
			{
				final IntervalView< T > channel = Views.hyperSlice( img, channelAxis, c );
				final IntervalView< T > target = Views.hyperSlice( output, channelAxis, c );
				processChannel( channel, c, referenceSurface, target );
			}

			// Make a new dataset with the output.
			final ImgPlus< T > imgPlus = new ImgPlus<>( output, "Local projection of " + source.getName(),
					source.axis( 0 ),
					source.axis( 0 ),
					source.axis( channelAxis ) );
			return new DefaultDataset( ops.context(), imgPlus );
		}
	}

	private void processChannel( final RandomAccessibleInterval< T > channel, final int c, final RandomAccessibleInterval< IntType > referenceSurface, final IterableInterval< T > target )
	{
		final int offset = params.offset( c );
		final int deltaZ = params.deltaZ( c );
		final StorelessUnivariateStatistic projector = params.projectionMethod( c ).projector();
		final Cursor< T > cursor = target.localizingCursor(); // 2D
		final RandomAccess< IntType > raReference = referenceSurface.randomAccess( referenceSurface ); // 2D
		final RandomAccess< T > ra = channel.randomAccess( channel ); // 3D

		while ( cursor.hasNext() )
		{
			cursor.fwd();
			raReference.setPosition( cursor );
			ra.setPosition( cursor.getLongPosition( 0 ), 0 );
			ra.setPosition( cursor.getLongPosition( 1 ), 1 );

			final long zmin = Math.max( 0,
					Math.min( channel.dimension( 2 ),
							raReference.get().get() + offset - deltaZ ) );
			final long zmax = Math.max( 0,
					Math.min( channel.dimension( 2 ),
							raReference.get().get() + offset + deltaZ ) );


			projector.clear();
			for ( long z = zmin; z <= zmax; z++ )
			{
				ra.setPosition( z, 2 );
				projector.increment( ra.get().getRealDouble() );

			}
			cursor.get().setReal( projector.getResult() );
		}
	}

	@Override
	public boolean isCanceled()
	{
		return cancelReason != null;
	}

	@Override
	public void cancel( final String reason )
	{
		this.cancelReason = reason;
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}
}
