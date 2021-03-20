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
package fr.pasteur.iah.localzprojector.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imglib2.Cursor;
import net.imglib2.FinalDimensions;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.img.Img;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * Computes the value of a filter on a source image, but only on a sparse grid,
 * then do N-linear interpolation in between grid points.
 * 
 * @author Jean-Yves Tinevez
 */
@Plugin( type = GridProcessingOp.class )
public class GridProcessingOp< T extends NativeType< T > & RealType< T > > extends AbstractUnaryComputerOp< RandomAccessibleInterval< T >, IterableInterval< T > >
		implements UnaryComputerOp< RandomAccessibleInterval< T >, IterableInterval< T > >
{

	@Parameter
	private ThreadService threadService;

	/**
	 * The step of the grid.
	 */
	@Parameter
	private int gridSize;

	/**
	 * the filter operation. Should accept an iterable and put the results in
	 * the {@link DoubleType} argument.
	 */
	@Parameter
	private BiConsumer< Iterable< T >, DoubleType > func;

	/**
	 * Computes the value of a filter on a source image, but only on a sparse
	 * grid, then do N-linear interpolation in between grid points.
	 * 
	 * @param source
	 *            the input image.
	 * @param output
	 *            the output into which to write the upscaled results. Must be
	 *            of same size than the source.
	 */
	@Override
	public void compute( final RandomAccessibleInterval< T > source, final IterableInterval< T > output )
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
}
