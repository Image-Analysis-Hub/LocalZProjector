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
import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.Computers;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.special.function.Functions;
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

	@Override
	public Img<T> calculate(final RandomAccessibleInterval<T> input) {

		final int numDimensions = input.numDimensions();
		if (numDimensions != binfactors.length)
			throw new IllegalArgumentException("Bin n-dimensions and input n-dimensions must be equal. Bins have "
					+ binfactors.length + " dimensions and input has " + numDimensions + " dimensions.");

		// Prepare output.
		final long[] imgSize = new long[numDimensions];
		input.dimensions(imgSize);
		final long[] newSize = new long[numDimensions];
		for (int d = 0; d < input.numDimensions(); ++d)
			newSize[d] = input.dimension(d) / binfactors[d];

		final ImgFactory<T> factory = Util.getSuitableImgFactory(FinalDimensions.wrap(newSize),
				Util.getTypeFromInterval(input));
		final Img<T> binned = factory.create(FinalDimensions.wrap(newSize));
		final Cursor<T> cursor = binned.localizingCursor();
		final long[] currPos = new long[input.numDimensions()];

		final NotCenteredRectangleShape shape = new NotCenteredRectangleShape( binfactors );
		final RandomAccessible< Neighborhood< T > > ran = shape.neighborhoodsRandomAccessible( Views.extendMirrorSingle( input ) );
		final RandomAccess<Neighborhood<T>> ra = ran.randomAccess(input);


		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final UnaryComputerOp< Iterable< T >, T > op = ( UnaryComputerOp ) Computers.unary( ops(), Ops.Stats.Mean.class, binned.firstElement().getClass(), Iterable.class );

		while (cursor.hasNext()) {
			cursor.next();
			cursor.localize(currPos);

			// Transform coordinates.
			for (int d = 0; d < numDimensions; d++)
				currPos[d] *= binfactors[d];

			// Iterate and sum.
			ra.setPosition(currPos);

			// Iterate and compute.
			op.compute( ra.get(), cursor.get() );
		}
		return binned;
	}

	@SuppressWarnings( "unchecked" )
	public static <T extends RealType< T >> void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException
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
		final BinningOp< T > binner = (BinningOp) Functions.unary(
				ij.op(),
				BinningOp.class,
				Img.class,
				RandomAccessibleInterval.class,
				binFactors  );

		final Img< T > out = binner.calculate( ( RandomAccessibleInterval< T > ) dataset.getImgPlus() );
		ij.ui().show( out );
	}
}
