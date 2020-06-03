package fr.pasteur.iah.localzprojector.process;

import java.util.function.BiConsumer;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;

import fr.pasteur.iah.localzprojector.binning.BinningOp;
import fr.pasteur.iah.localzprojector.binning.UnBinningOp;
import fr.pasteur.iah.localzprojector.util.GridProcessingOp;
import net.imagej.ops.OpService;
import net.imagej.ops.special.computer.Computers;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.thread.chunker.ChunkerOp;
import net.imagej.ops.thread.chunker.CursorBasedChunk;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
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

@Plugin( type = ReferenceSurfaceOp.class )
public class ReferenceSurfaceOp< T extends RealType< T > & NativeType< T > > extends AbstractUnaryFunctionOp< RandomAccessibleInterval< T >, Img< UnsignedShortType > > implements Cancelable
{

	@Parameter( type = ItemIO.INPUT )
	private ReferenceSurfaceParameters params;

	@Parameter
	private OpService ops;

	@Parameter
	private ThreadService threadService;

	@Parameter
	private UIService ui;// DEBUG

	private String cancelReason;

	@Override
	public Img< UnsignedShortType > calculate( final RandomAccessibleInterval< T > source )
	{
		// Prepare.
		cancelReason = null;

		// Check input.
		if ( source.numDimensions() != 3 )
			throw new IllegalArgumentException( "Expected source to be 3D, but was " + source.numDimensions() + "D." );

		// Compute binned size.
		final Dimensions origSize = new FinalDimensions( source.dimension( 0 ), source.dimension( 1 ) );
		final Dimensions binnedSize = new FinalDimensions( source.dimension( 0 ) / params.binning, source.dimension( 1 ) / params.binning );

		// Create output. Store the Z value for max.
		final ImgFactory< UnsignedShortType > intFactory = Util.getArrayOrCellImgFactory( binnedSize, new UnsignedShortType() );
		final Img< UnsignedShortType > output = intFactory.create( binnedSize );

		// Temp storage for max value.
		final ImgFactory< T > factory = intFactory.imgFactory( Util.getTypeFromInterval( source ) );
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
		for ( int z = Math.max( 0, params.zMin ); z <= Math.min( source.dimension( 2 ) - 1, params.zMax ); z++ )
		{
			if ( isCanceled() )
				break;

			final IntervalView< T > slice = Views.hyperSlice( source, 2, z );

			// Binning.
			final RandomAccessibleInterval< T > binned;
			if ( params.binning > 1 )
				binned = binner.calculate( slice );
			else
			{
				if ( params.sigma > 0. )
					// Duplicate so that we don't smooth the source.
					binned = ops().copy().rai( slice );
				else
					binned = slice;
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

			// Update reference map, multithreaded.
			final int localZ = z;
			ops().run( ChunkerOp.class, new CursorBasedChunk()
			{

				@Override
				public void execute( final long startIndex, final long stepSize, final long numSteps )
				{
					final Cursor< T > filteredCursor = filtered.localizingCursor();
					final RandomAccess< T > raMaxValue = maxValueImg.randomAccess( maxValueImg );
					final RandomAccess< UnsignedShortType > ra = output.randomAccess( output );

					filteredCursor.jumpFwd( startIndex );
					for ( int i = 0; i < numSteps; ++i )
					{
						filteredCursor.next();
						raMaxValue.setPosition( filteredCursor );
						final double filteredValue = filteredCursor.get().getRealDouble();
						final double maxValue = raMaxValue.get().getRealDouble();

						if ( filteredValue > maxValue )
						{
							raMaxValue.get().setReal( filteredValue );
							ra.get().set( localZ );
						}
					}
				}
			}, filtered.size() );
		}

		if ( isCanceled() )
			return rescale( output, params.binning, origSize );

		// Median filter.
		final Img< UnsignedShortType > output2;
		if ( params.medianHalfSize > 0 )
		{
			final Shape medianFilterShape = new RectangleShape( params.medianHalfSize, false );
			output2 = ops.create().img( output );
			ops.filter().median( output2, output, medianFilterShape );
		}
		else
		{
			output2 = output;
		}

		// Rescale binned image back to full size.
		final Img< UnsignedShortType > rescaled = rescale( output2, params.binning, origSize );
		return rescaled;
	}

	private Img< UnsignedShortType > rescale( final Img< UnsignedShortType > binned, final int binning, final Dimensions origSize )
	{
		if ( binning == 1 )
			return binned;

		final int numDimensions = binned.numDimensions();
		final int[] binFactors = Util.getArrayFromValue( params.binning, numDimensions );

		// UnBinning op.
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final UnBinningOp< UnsignedShortType > unbinner = ( UnBinningOp ) Functions.unary(
				ops(),
				UnBinningOp.class,
				Img.class,
				RandomAccessibleInterval.class,
				binFactors,
				origSize );
		return unbinner.calculate( binned );

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
