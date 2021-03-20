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
package fr.pasteur.iah.localzprojector.binning;

import java.io.IOException;
import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.Op;
import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.Computers;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.thread.chunker.ChunkerOp;
import net.imagej.ops.thread.chunker.CursorBasedChunk;
import net.imglib2.Cursor;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

@Plugin( type = BinningOp.class )
public class BinningOp< T extends RealType< T > > extends AbstractUnaryFunctionOp< RandomAccessibleInterval< T >, Img< T > >
{

	@Parameter( type = ItemIO.INPUT )
	private int[] binfactors;

	@Parameter( type = ItemIO.INPUT, required = false )
	private Class< ? extends Op > ocClass = Ops.Stats.Mean.class;

	@Override
	public Img< T > calculate( final RandomAccessibleInterval< T > input )
	{

		final int numDimensions = input.numDimensions();
		if ( numDimensions != binfactors.length )
			throw new IllegalArgumentException( "Bin n-dimensions and input n-dimensions must be equal. Bins have "
					+ binfactors.length + " dimensions and input has " + numDimensions + " dimensions." );

		// Prepare output.
		final long[] imgSize = new long[ numDimensions ];
		input.dimensions( imgSize );
		final long[] newSize = new long[ numDimensions ];
		for ( int d = 0; d < input.numDimensions(); ++d )
			newSize[ d ] = input.dimension( d ) / binfactors[ d ];

		final ImgFactory< T > factory = Util.getSuitableImgFactory( FinalDimensions.wrap( newSize ),
				Util.getTypeFromInterval( input ) );
		final Img< T > binned = factory.create( FinalDimensions.wrap( newSize ) );

		final NotCenteredRectangleShape shape = new NotCenteredRectangleShape( binfactors );
		final RandomAccessible< Neighborhood< T > > ran = shape.neighborhoodsRandomAccessible( Views.extendMirrorSingle( input ) );

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final UnaryComputerOp< Iterable< T >, T > op = ( UnaryComputerOp ) Computers.unary( ops(), ocClass, binned.firstElement().getClass(), Iterable.class );

		// Multithread.
		ops().run( ChunkerOp.class, new CursorBasedChunk()
		{
			@Override
			public void execute( final long startIndex, final long stepSize, final long numSteps )
			{
				final RandomAccess< Neighborhood< T > > ra = ran.randomAccess( input );
				final long[] currPos = new long[ input.numDimensions() ];
				final Cursor< T > cursor = binned.localizingCursor();
				cursor.jumpFwd( startIndex );
				for ( int i = 0; i < numSteps; ++i )
				{
					cursor.next();
					cursor.localize( currPos );

					// 'Transform' coordinates.
					for ( int d = 0; d < numDimensions; d++ )
						currPos[ d ] *= binfactors[ d ];

					// Iterate and compute.
					ra.setPosition( currPos );
					op.compute( ra.get(), cursor.get() );
				}
			}
		}, binned.size() );

		return binned;
	}

	@SuppressWarnings( "unchecked" )
	public static < T extends RealType< T > > void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException
	{
		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		final ImageJ ij = new ImageJ();
		ij.launch( args );

		final String imageFile = "samples/SingleSlice.tif";
		final Object open = ij.io().open( imageFile );
		ij.ui().show( open );

		final int[] binFactors = new int[] { 4, 4 };
		final Dataset dataset = ij.dataset().getDatasets().get( 0 );

		@SuppressWarnings( "rawtypes" )
		final BinningOp< T > binnerAvg = ( BinningOp ) Functions.unary(
				ij.op(),
				BinningOp.class,
				Img.class,
				RandomAccessibleInterval.class,
				binFactors );

		final Img< T > out = binnerAvg.calculate( ( RandomAccessibleInterval< T > ) dataset.getImgPlus() );
		ij.ui().show( "Binned with mean", out );

		@SuppressWarnings( "rawtypes" )
		final BinningOp< T > binnerMax = ( BinningOp ) Functions.unary(
				ij.op(),
				BinningOp.class,
				Img.class,
				RandomAccessibleInterval.class,
				binFactors,
				Ops.Stats.Max.class );

		final Img< T > out2 = binnerMax.calculate( ( RandomAccessibleInterval< T > ) dataset.getImgPlus() );
		ij.ui().show( "Binned with max", out2 );

		@SuppressWarnings( "rawtypes" )
		final BinningOp< T > binnerMin = ( BinningOp ) Functions.unary(
				ij.op(),
				BinningOp.class,
				Img.class,
				RandomAccessibleInterval.class,
				binFactors,
				Ops.Stats.Min.class );

		final Img< T > out3 = binnerMin.calculate( ( RandomAccessibleInterval< T > ) dataset.getImgPlus() );
		ij.ui().show( "Binned with min", out3 );
	}
}
