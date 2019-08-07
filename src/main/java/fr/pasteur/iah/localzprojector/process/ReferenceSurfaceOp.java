package fr.pasteur.iah.localzprojector.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;

import fr.pasteur.iah.localzprojector.binning.BinningOp;
import fr.pasteur.iah.localzprojector.binning.UnBinningOp;
import net.imagej.ops.OpService;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;
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
		final RandomAccess< UnsignedShortType > ra = output.randomAccess( output );

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
				processGrid( binned, 2 * neighborhoodHalfSize, ( n, v ) -> ops.stats().mean( v, n ), filtered );
				break;
			case SPARSE_MAX_OF_STD:
				processGrid( binned, 2 * neighborhoodHalfSize, ( n, v ) -> ops.stats().stdDev( v, n ), filtered );
				break;
			default:
				throw new IllegalArgumentException( "Unkown filtering method: " + params.method + "." );
			}

			// Same iteration order.
			final Cursor< T > filteredCursor = filtered.cursor();
			final Cursor< T > maxValCursor = maxValueImg.cursor();
			while ( filteredCursor.hasNext() )
			{
				filteredCursor.fwd();
				final double filteredValue = filteredCursor.get().getRealDouble();
				maxValCursor.fwd();
				final double maxValue = maxValCursor.get().getRealDouble();

				if ( filteredValue > maxValue )
				{
					maxValCursor.get().setReal( filteredValue );
					ra.setPosition( maxValCursor );
					ra.get().set( z );
				}
			}
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

	/**
	 * Computes the value of a filter on a source image, but only on a sparse
	 * grid, then do N-linear interpolation in between grid points.
	 * 
	 * @param source
	 *            the input image.
	 * @param gridSize
	 *            the step of the grid.
	 * @param fun
	 *            the filter operation. Should accept and iterable and put the
	 *            results in the {@link DoubleType} argument.
	 * @param output
	 *            the output into which to write the upscaled results. Must be
	 *            of same size than the source.
	 */
	private void processGrid( final RandomAccessibleInterval< T > source, final int gridSize, final BiConsumer< Iterable< T >, DoubleType > func, final Img< T > output )
	{

		// Generate blocks.
		final int[] blockSize = new int[ source.numDimensions() ];
		Arrays.fill( blockSize, gridSize );
		final long[] dimensions = new long[ source.numDimensions() ];
		source.dimensions( dimensions );
		final List< Pair< Interval, long[] > > blocks = Grids.collectAllContainedIntervalsWithGridPositions( dimensions, blockSize );

		// Create tmp target.
		final long[] downscaledDims = new long[ source.numDimensions() ];
		for ( int d = 0; d < downscaledDims.length; d++ )
			downscaledDims[ d ] = 1 + source.dimension( d ) / gridSize;
		final Img< T > target = ops().create().img( FinalDimensions.wrap( downscaledDims ), Util.getTypeFromInterval( source ) );

		// Generate workers, 1 per thread.
		final int nThreads = Runtime.getRuntime().availableProcessors();
		final ConcurrentLinkedQueue< Pair< Interval, long[] > > todos = new ConcurrentLinkedQueue<>( blocks );
		final List< Runnable > runnables = new ArrayList<>( nThreads );
		for ( int i = 0; i < nThreads; i++ )
		{
			final RandomAccess< T > ra = target.randomAccess( target );
			final DoubleType val = new DoubleType( 0. );
			runnables.add( new Runnable()
			{

				@Override
				public void run()
				{
					do
					{
						final Pair< Interval, long[] > pair = todos.poll();
						if ( null == pair )
							return;

						final Interval interval = pair.getA();
						func.accept( Views.interval( source, interval ), val );

						ra.setPosition( pair.getB() );
						ra.get().setReal( val.getRealDouble() );
					}
					while ( !todos.isEmpty() );
				}
			} );
		}

		// Put the workers to work.
		final ExecutorService es = threadService.getExecutorService();
		final List< Future< ? > > futures = runnables.stream()
				.map( r -> es.submit( r ) )
				.collect( Collectors.toList() );
		try
		{
			for ( final Future< ? > future : futures )
				future.get();
		}
		catch ( final InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
		}

		// Upscale.
		final double[] scaleFactors = new double[ target.numDimensions() ];
		for ( int d = 0; d < scaleFactors.length; d++ )
			scaleFactors[ d ] = gridSize;
		final RandomAccessibleInterval< T > upscaled = ops().transform().scaleView( target, scaleFactors, new NLinearInterpolatorFactory<>() );

		// Copy view to storage.
		final Cursor< T > cursor = output.localizingCursor();
		final RandomAccess< T > raView = Views.extendBorder( upscaled ).randomAccess( output );
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			raView.setPosition( cursor );
			cursor.get().set( raView.get() );
		}
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
