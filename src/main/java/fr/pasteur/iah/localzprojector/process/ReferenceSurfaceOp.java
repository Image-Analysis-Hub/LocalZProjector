package fr.pasteur.iah.localzprojector.process;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.OpService;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

@Plugin( type = ReferenceSurfaceOp.class )
public class ReferenceSurfaceOp< T extends RealType< T > & NativeType< T > > extends AbstractUnaryFunctionOp< RandomAccessibleInterval< T >, Img< IntType > > implements Cancelable
{

	@Parameter( type = ItemIO.INPUT )
	private ReferenceSurfaceParameters params;

	@Parameter
	private OpService ops;

	private String cancelReason;

	@Override
	public Img< IntType > calculate( final RandomAccessibleInterval< T > source )
	{
		// Prepare.
		cancelReason = null;

		// Check input.
		if ( source.numDimensions() != 3 )
			throw new IllegalArgumentException( "Expected source to be 3D, but was " + source.numDimensions() + "D." );

		// Create output. Store the Z value for max.
		final Dimensions targetSize = new FinalDimensions( source.dimension( 0 ), source.dimension( 1 ) );
		final ImgFactory< IntType > intFactory = Util.getArrayOrCellImgFactory( targetSize, new IntType() );
		final Img< IntType > output = intFactory.create( targetSize );
		final RandomAccess< IntType > ra = output.randomAccess( output );

		// Temp storage for max value.
		final ImgFactory< T > factory = intFactory.imgFactory( Util.getTypeFromInterval( source ) );
		final Img< T > maxValueImg = factory.create( targetSize );
		for ( final T p : maxValueImg )
			p.setReal( Double.NEGATIVE_INFINITY );

		// Temp storage for filtered slice.
		final Img< T > filtered = factory.create( targetSize );

		// Neighborhood for filtering.
		final Shape shape = new RectangleShape( params.halfWindowSize, false );

		// Iterate over Z.
		for ( int z = Math.max( 0, params.zMin ); z <= Math.min( source.dimension( 2 ) - 1, params.zMax ); z++ )
		{
			if ( isCanceled() )
				return output;

			final IntervalView< T > slice = Views.hyperSlice( source, 2, z );
			if ( params.sigma > 0 )
				ops.filter().gauss( slice, slice, params.sigma );

			switch ( params.method )
			{
			case MAX_OF_MEAN:
				ops.filter().mean( filtered, slice, shape );
				break;
			case MAX_OF_STD:
				ops.filter().variance( filtered, slice, shape );
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
			return output;

		if ( params.medianHalfSize > 0 )
		{
			final Shape medianFilterShape = new RectangleShape( params.medianHalfSize, false );
			final Img< IntType > output2 = ops.create().img( output );
			ops.filter().median( output2, output, medianFilterShape );
			return output2;
		}

		return output;
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
