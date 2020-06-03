package fr.pasteur.iah.localzprojector.process.offline;

import java.util.function.BiConsumer;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import fr.pasteur.iah.localzprojector.binning.BinningOp;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.util.GridProcessingOp;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imagej.ops.special.computer.Computers;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.thread.chunker.ChunkerOp;
import net.imagej.ops.thread.chunker.CursorBasedChunk;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

@Plugin( type = FilterBasedOnePassProjectorOp.class )
public class FilterBasedOnePassProjectorOp< T extends RealType< T > & NativeType< T > >
		extends AbstractUnaryComputerOp< ImgPlus< T >, ImgPlus< T > >
		implements OnePassProjectorOp< T >
{

	@Parameter( type = ItemIO.INPUT )
	private ReferenceSurfaceParameters params;

	@Parameter
	private OpService ops;

	@Parameter
	private ThreadService threadService;

	private String cancelReason;

	private Img< UnsignedShortType > heightMap;

	@Override
	public void compute( final ImgPlus< T > input, final ImgPlus< T > output )
	{
		// Prepare.
		cancelReason = null;

		// Check input.
		if ( input.numDimensions() != 3 )
			throw new IllegalArgumentException( "Expected input to be 3D, but was " + input.numDimensions() + "D." );

		// Check output.
		if ( output.numDimensions() != 2 )
			throw new IllegalArgumentException( "Expected ouput to be 2D, but was " + output.numDimensions() + "D." );

		// Compute binned size.
		final Dimensions origSize = new FinalDimensions( input.dimension( 0 ), input.dimension( 1 ) );
		final Dimensions binnedSize = new FinalDimensions( input.dimension( 0 ) / params.binning, input.dimension( 1 ) / params.binning );

		// Create height-map output.
		final ImgFactory< UnsignedShortType > intFactory = Util.getArrayOrCellImgFactory( binnedSize, new UnsignedShortType() );
		heightMap = intFactory.create( origSize );

		// Temp storage for max value.
		final ImgFactory< T > factory = intFactory.imgFactory( Util.getTypeFromInterval( input ) );
		final Img< T > maxValueImg = factory.create( binnedSize );
		for ( final T p : maxValueImg )
			p.setReal( Double.NEGATIVE_INFINITY );

		// Temp storage for filtered slice.
		final Img< T > filtered = factory.create( binnedSize );

		// Neighborhood size for filtering on the binned image.
		final int neighborhoodHalfSize = ( int ) Math.ceil( ( double ) params.filterWindowSize / params.binning / 2. );
		final Shape shape = new RectangleShape( neighborhoodHalfSize, false );

		// Binning op.
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final BinningOp< T > binner = ( BinningOp ) Functions.unary(
				ops(),
				BinningOp.class,
				Img.class,
				RandomAccessibleInterval.class,
				new int[] { params.binning, params.binning } );

		// Grid processing op - in case we need it.
		final BiConsumer< Iterable< T >, DoubleType > func;
		switch ( params.method )
		{
		case MAX_OF_MEAN:
			func = ( n, v ) -> ops.stats().mean( v, n );
			break;
		case MAX_OF_STD:
		default:
			func = ( n, v ) -> ops.stats().stdDev( v, n );
			break;
		}
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final GridProcessingOp< T > gridProcessingOp = ( GridProcessingOp ) Computers.unary(
				ops, GridProcessingOp.class,
				IterableInterval.class,
				RandomAccessibleInterval.class,
				2 * neighborhoodHalfSize,
				func );

		// Iterate over Z.
		for ( int z = Math.max( 0, params.zMin ); z <= Math.min( input.dimension( 2 ) - 1, params.zMax ); z++ )
		{
			if ( isCanceled() )
				break;

			final IntervalView< T > sliceInput = Views.hyperSlice( input, 2, z );

			// Binning.
			final RandomAccessibleInterval< T > binned;
			if ( params.binning > 1 )
				binned = binner.calculate( sliceInput );
			else
			{
				if ( params.sigma > 0. )
					// Duplicate so that we don't smooth the source.
					binned = ops().copy().rai( sliceInput );
				else
					binned = sliceInput;
			}

			// Gaussian filtering.
			if ( params.sigma > 0. )
				ops.filter().gauss( binned, binned, params.sigma );

			// Surface filtering method.
			switch ( params.method )
			{
			case MAX_OF_MEAN:
				ops.filter().mean( filtered, binned, shape );
				break;
			case MAX_OF_STD:
				ops.filter().variance( filtered, binned, shape );
				break;
			case SPARSE_MAX_OF_MEAN:
			case SPARSE_MAX_OF_STD:
				gridProcessingOp.compute( binned, filtered );
				break;
			default:
				throw new IllegalArgumentException( "Unkown filtering method: " + params.method + "." );
			}

			/*
			 * Now look if we have a stronger filter response than the one from
			 * previous Z-slices. If yet, update the max-value storage, the
			 * height-map and the projection.
			 */

			// Multithread.
			final int localZ = z;
			ops().run( ChunkerOp.class, new CursorBasedChunk()
			{
				@Override
				public void execute( final long startIndex, final long stepSize, final long numSteps )
				{
					final Cursor< T > filteredCursor = filtered.localizingCursor();
					final RandomAccess< T > raMaxValue = maxValueImg.randomAccess( maxValueImg );
					final RandomAccess< T > raInput = sliceInput.randomAccess( sliceInput );
					final RandomAccess< T > raOutput = output.randomAccess( output );
					final RandomAccess< UnsignedShortType > raHeightMap = heightMap.randomAccess( heightMap );

					final long[] patchStart = new long[ input.numDimensions() ];
					final long[] patchEnd = new long[ input.numDimensions() ];

					filteredCursor.jumpFwd( startIndex );
					for ( int i = 0; i < numSteps; ++i )
					{
						filteredCursor.next();
						raMaxValue.setPosition( filteredCursor );
						final double filteredValue = filteredCursor.get().getRealDouble();
						final double maxValue = raMaxValue.get().getRealDouble();

						if ( filteredValue > maxValue )
						{
							// Update max-value storage.
							raMaxValue.get().setReal( filteredValue );

							/*
							 * Update projection & height-map. Now this is more
							 * complicated: We operate on a binned image, but we
							 * must copy the non-binned pixels to the output. So
							 * we must copy a small patch, the size of a bin
							 * square to the output.
							 */

							if ( params.binning > 1 )
							{
								// 'Transform' coordinates.
								filteredCursor.localize( patchStart );
								for ( int d = 0; d < 2; d++ )
								{
									patchStart[ d ] *= params.binning;
									patchEnd[ d ] = patchStart[ d ] + params.binning - 1;
								}
								final FinalInterval patch = new FinalInterval( patchStart, patchEnd );

								// Height-map.

								Views.interval( heightMap, patch ).forEach( h -> h.set( localZ ) );
								final Cursor< T > outputCursor = Views.interval( output, patch ).localizingCursor();
								while ( outputCursor.hasNext() )
								{
									outputCursor.fwd();
									raInput.setPosition( outputCursor );
									outputCursor.get().set( raInput.get() );
								}
							}
							else
							{
								raHeightMap.setPosition( filteredCursor );
								raHeightMap.get().set( localZ );

								raOutput.setPosition( filteredCursor );
								raInput.setPosition( filteredCursor );
								raOutput.get().set( raInput.get() );
							}
						}
					}
				}
			}, filtered.size() );
		}
	}

	@Override
	public Img< UnsignedShortType > getHeightMap()
	{
		return heightMap;
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
