package fr.pasteur.iah.localzprojector.binning;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.thread.chunker.ChunkerOp;
import net.imagej.ops.thread.chunker.CursorBasedChunk;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

/**
 * Inverse the binning op: rescale the image by integer factors, and simply copy
 * values on new pixels.
 */
@Plugin( type = UnBinningOp.class )
public class UnBinningOp< T extends RealType< T > & NativeType< T > > extends AbstractUnaryFunctionOp< RandomAccessibleInterval< T >, Img< T > >
{

	@Parameter( type = ItemIO.INPUT )
	private int[] binfactors;

	@Parameter( type = ItemIO.INPUT, required = false )
	private Dimensions desiredDimensions;

	@Override
	public Img< T > calculate( final RandomAccessibleInterval< T > input )
	{
		final int numDimensions = input.numDimensions();
		if ( numDimensions != binfactors.length )
			throw new IllegalArgumentException( "Bin n-dimensions and input n-dimensions must be equal. Bins have "
					+ binfactors.length + " dimensions and input has " + numDimensions + " dimensions." );

		if (null == desiredDimensions)
		{
			final long[] dims = new long[ numDimensions ];
			for ( int d = 0; d < numDimensions; d++ )
				dims[ d ] = input.dimension( d ) * binfactors[ d ];
			desiredDimensions = FinalDimensions.wrap( dims );
		}
		final Img< T > rescaled = ops().create().img( desiredDimensions, Util.getTypeFromInterval( input ) );
		
		final ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > extended = Views.extendBorder( input );

		// Multithread.
		ops().run( ChunkerOp.class, new CursorBasedChunk()
		{
			@Override
			public void execute( final long startIndex, final long stepSize, final long numSteps )
			{
				final RandomAccess< T > ra = extended.randomAccess();
				final long[] currPos = new long[ input.numDimensions() ];
				final Cursor< T > cursor = rescaled.localizingCursor();
				cursor.jumpFwd( startIndex );
				for ( int i = 0; i < numSteps; ++i )
				{
					cursor.next();
					cursor.localize( currPos );

					// 'Transform' coordinates.
					for ( int d = 0; d < numDimensions; d++ )
						currPos[ d ] /= binfactors[ d ];

					// Iterate and compute.
					ra.setPosition( currPos );
					cursor.get().set( ra.get() );
				}
			}
		}, rescaled.size() );

		return rescaled;
	}
}
