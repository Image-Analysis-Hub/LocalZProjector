package fr.pasteur.iah.localzprojector.process;

import org.scijava.listeners.Listeners;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters.ProjectionMethod;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.ops.OpService;
import net.imagej.ops.create.img.Imgs;
import net.imagej.ops.special.computer.AbstractBinaryComputerOp;
import net.imglib2.Cursor;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * This {@link ProjectorOp} works z-slice-by-slice.
 * <p>
 * This is important for large images, that are loaded from Fiji using the
 * "virtual stack". For these stacks, they are loaded in memory only on request,
 * one Z-slice at a time. We want to avoid triggering a loading event for a
 * whole plane for every pixel. In summary: we want to avoid moving in Z as
 * little as possible.
 * 
 * @author Jean-Yves Tinevez
 *
 * @param <T>
 *            the type of pixels in the image to project.
 */
@Plugin( type = ExtractSurfaceOnePassOp.class )
public class ExtractSurfaceOnePassOp< T extends RealType< T > & NativeType< T > >
		extends AbstractBinaryComputerOp< ImgPlus< T >, RandomAccessibleInterval< UnsignedShortType >, RandomAccessibleInterval< T > >
		implements ProjectorOp< T >
{

	@Parameter
	private ExtractSurfaceParameters params;

	@Parameter
	private OpService ops;

	private String cancelReason;

	private Listeners.List< SliceProcessListener > listeners = new Listeners.List<>();

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
				if ( isCanceled() )
					break;

				final IntervalView< T > channel = Views.hyperSlice( img, channelAxis, c );
				final IntervalView< T > target = Views.hyperSlice( output, channelAxis, c );
				processChannel( channel, c, referenceSurface, target );
			}
		}
	}

	private void processChannel(
			final RandomAccessibleInterval< T > channel,
			final int c,
			final RandomAccessibleInterval< UnsignedShortType > referenceSurface,
			final RandomAccessibleInterval< T > target )
	{
		final int offset = params.offset( c );
		final int deltaZ = params.deltaZ( c );
		final ProjectionMethod projectionMethod = params.projectionMethod( c );

		final Accumulator< T > accumulator;
		switch ( projectionMethod )
		{
		case COLLECT:
			throw new IllegalArgumentException( "Cannot project with the " + projectionMethod + " method." );
		case MEAN:
			accumulator = new MeanAccumulator< T >( target );
			break;
		case MIP:
			accumulator = new MIPAccumulator< T >( target );
			break;
		default:
			throw new IllegalArgumentException( "Unknown projection method: " + this + "." );
		}

		final Pair< UnsignedShortType, UnsignedShortType > minMax = ops.stats().minMax( Views.iterable( referenceSurface ) );
		final long minBound = minMax.getA().getIntegerLong() + offset - deltaZ;
		final long maxBound = minMax.getB().getIntegerLong() + offset + deltaZ;
		final long minZ = Math.max( channel.min( 2 ), minBound );
		final long maxZ = Math.min( channel.max( 2 ), maxBound );

		final RandomAccess< UnsignedShortType > raReference = referenceSurface.randomAccess( referenceSurface ); // 2D
		for ( long z = minZ; z <= maxZ; z++ )
		{
			final long localZ = z;
			if ( isCanceled() )
				break;

			final Cursor< T > cursor = Views.hyperSlice( channel, 2, localZ ).localizingCursor();
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				raReference.setPosition( cursor );

				final int surfaceZ = raReference.get().get();
				if ( localZ >= surfaceZ + offset - deltaZ && localZ <= surfaceZ + offset + deltaZ )
					accumulator.accumulate( cursor.get(), cursor );
			}
			listeners.list.forEach( l -> l.sliceProcessed( localZ ) );
		}
	}

	public Listeners.List< SliceProcessListener > getListeners()
	{
		return listeners;
	}

	/**
	 * Interface for projection accumulator.
	 */
	private static interface Accumulator< T extends RealType< T > & NativeType< T > >
	{
		public void accumulate( T val, final Localizable pos );
	}

	private static final class MeanAccumulator< T extends RealType< T > & NativeType< T > > implements Accumulator< T >
	{

		private final RandomAccess< IntType > nRa;

		private final RandomAccess< T > ra;

		public MeanAccumulator( final RandomAccessibleInterval< T > target )
		{
			this.ra = target.randomAccess( target );
			this.nRa = Imgs.create( Util.getArrayOrCellImgFactory( target, new IntType() ), target, new IntType() ).randomAccess( target );
		}

		@Override
		public void accumulate( final T val, final Localizable pos )
		{
			nRa.setPosition( pos );
			final int n = nRa.get().get() + 1;

			ra.setPosition( pos );
			final double av = ra.get().getRealDouble();

			// Running average trying to limit integer precision impact and overflow.
			final double nAv = ( n - 1. ) / n * av + val.getRealDouble() / n;
			ra.get().setReal( nAv );

			nRa.get().inc();
		}
	}

	private static final class MIPAccumulator< T extends RealType< T > & NativeType< T > > implements Accumulator< T >
	{

		private final RandomAccess< T > ra;

		/**
		 * We work directly on the target.
		 * 
		 * @param target
		 *            the image in which to write the MIP.
		 */
		public MIPAccumulator( final RandomAccessibleInterval< T > target )
		{
			this.ra = target.randomAccess( target );
		}

		@Override
		public void accumulate( final T val, final Localizable pos )
		{
			ra.setPosition( pos );
			if ( ra.get().compareTo( val ) < 0 )
				ra.get().set( val );
		}

	}

	public interface SliceProcessListener
	{
		public void sliceProcessed( long z );
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
