package fr.pasteur.iah.localzprojector.binning;

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

public class Binner<T extends RealType<T>> {

	private final int[] binfactors;

	public Binner(final int[] binfactors) {
		this.binfactors = binfactors;
	}

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

		while (cursor.hasNext()) {
			cursor.next();
			cursor.localize(currPos);

			// Transform coordinates.
			for (int d = 0; d < numDimensions; d++)
				currPos[d] *= binfactors[d];

			// Iterate and sum.
			ra.setPosition(currPos);

			double sum = 0.;
			for (final T t : ra.get()) {
				sum += t.getRealDouble();
			}

			// Set.
			cursor.get().setReal(sum);
		}
		return binned;
	}
}
