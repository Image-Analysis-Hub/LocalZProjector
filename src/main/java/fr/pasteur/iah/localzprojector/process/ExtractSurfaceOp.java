package fr.pasteur.iah.localzprojector.process;

import org.apache.commons.math3.stat.descriptive.StorelessUnivariateStatistic;
import org.scijava.Cancelable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.ops.OpService;
import net.imagej.ops.special.computer.AbstractBinaryComputerOp;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

@Plugin( type = ExtractSurfaceOp.class )
public class ExtractSurfaceOp< T extends RealType< T > & NativeType< T > > extends AbstractBinaryComputerOp< ImgPlus< T >, RandomAccessibleInterval< UnsignedShortType >, RandomAccessibleInterval< T > > implements Cancelable
{

	@Parameter
	private ExtractSurfaceParameters params;

	@Parameter
	private OpService ops;

	private String cancelReason;


	@Override
	public void compute( final ImgPlus< T > source, final RandomAccessibleInterval< UnsignedShortType > referenceSurface, final RandomAccessibleInterval< T > output )
	{
		// Prepare.
		cancelReason = null;

		final Img< T > img = source.getImg();

		final int channelAxis = source.dimensionIndex( Axes.CHANNEL );
		if ( channelAxis < 0 )
		{
			// Check input.
			if ( source.numDimensions() != 3 )
				throw new IllegalArgumentException( "Expected single-channel source to be 3D, but was " + source.numDimensions() + "D." );

			// Process.
			processChannel( img, 0, referenceSurface, output );
		}
		else
		{

			// Check input.
			if ( source.numDimensions() != 4 )
				throw new IllegalArgumentException( "Expected multi-channel source to be 4D (3D+C), but was " + source.numDimensions() + "D." );
			final long nChannels = img.dimension( channelAxis );

			// Process.
			for ( int c = 0; c < nChannels; c++ )
			{
				final IntervalView< T > channel = Views.hyperSlice( img, channelAxis, c );
				final IntervalView< T > target = Views.hyperSlice( output, channelAxis, c );
				processChannel( channel, c, referenceSurface, target );
			}
		}
	}

	private void processChannel( final RandomAccessibleInterval< T > channel, final int c, final RandomAccessibleInterval< UnsignedShortType > referenceSurface, final RandomAccessibleInterval< T > target )
	{
		final int offset = params.offset( c );
		final int deltaZ = params.deltaZ( c );
		final StorelessUnivariateStatistic projector = params.projectionMethod( c ).projector();
		final Cursor< T > cursor = Views.iterable( target ).localizingCursor(); // 2D
		final RandomAccess< UnsignedShortType > raReference = referenceSurface.randomAccess( referenceSurface ); // 2D
		final RandomAccess< T > ra = channel.randomAccess( channel ); // 3D

		while ( cursor.hasNext() )
		{
			cursor.fwd();
			raReference.setPosition( cursor );
			ra.setPosition( cursor.getLongPosition( 0 ), 0 );
			ra.setPosition( cursor.getLongPosition( 1 ), 1 );

			final long zmin = Math.max( 0,
					Math.min( channel.dimension( 2 ) - 1,
							raReference.get().get() + offset - deltaZ ) );
			final long zmax = Math.max( 0,
					Math.min( channel.dimension( 2 ) - 1,
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
