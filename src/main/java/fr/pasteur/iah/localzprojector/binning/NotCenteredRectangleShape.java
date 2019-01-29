package fr.pasteur.iah.localzprojector.binning;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleNeighborhood;
import net.imglib2.algorithm.neighborhood.RectangleNeighborhoodFactory;
import net.imglib2.algorithm.neighborhood.RectangleNeighborhoodUnsafe;
import net.imglib2.algorithm.neighborhood.RectangleShape.NeighborhoodsAccessible;
import net.imglib2.algorithm.neighborhood.RectangleShape.NeighborhoodsIterableInterval;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.util.Util;

public class NotCenteredRectangleShape implements Shape {

	private final long[] extent;

	/**
	 * Will iterate over <code>0</code> to <code>extend[d]</code> for all
	 * dimensions.
	 *
	 * @param extent
	 *                   the extend of the shape.
	 */
	public NotCenteredRectangleShape(final long[] extent) {
		this.extent = extent;
	}

	public NotCenteredRectangleShape(final int[] extent) {
		this.extent = new long[extent.length];
		for (int d = 0; d < extent.length; d++)
			this.extent[d] = extent[d];
	}

	@Override
	public <T> IterableInterval<Neighborhood<T>> neighborhoods(final RandomAccessibleInterval<T> source) {
		final RectangleNeighborhoodFactory<T> f = RectangleNeighborhoodUnsafe.<T>factory();
		return new NeighborhoodsIterableInterval<>(source, createSpan(), f);
	}

	@Override
	public <T> RandomAccessible<Neighborhood<T>> neighborhoodsRandomAccessible(final RandomAccessible<T> source) {
		final RectangleNeighborhoodFactory<T> f = RectangleNeighborhoodUnsafe.<T>factory();
		return new NeighborhoodsAccessible<>(source, createSpan(), f);
	}

	@Override
	public <T> IterableInterval<Neighborhood<T>> neighborhoodsSafe(final RandomAccessibleInterval<T> source) {
		final RectangleNeighborhoodFactory<T> f = RectangleNeighborhood.<T>factory();
		return new NeighborhoodsIterableInterval<>(source, createSpan(), f);
	}

	@Override
	public <T> RandomAccessible<Neighborhood<T>> neighborhoodsRandomAccessibleSafe(final RandomAccessible<T> source) {
		final RectangleNeighborhoodFactory<T> f = RectangleNeighborhood.<T>factory();
		return new NeighborhoodsAccessible<>(source, createSpan(), f);
	}

	private Interval createSpan() {
		final long[] max = new long[extent.length];
		for (int d = 0; d < max.length; d++)
			max[d] = extent[d] - 1;
		return new FinalInterval(Util.getArrayFromValue(0l, extent.length), max);
	}
}