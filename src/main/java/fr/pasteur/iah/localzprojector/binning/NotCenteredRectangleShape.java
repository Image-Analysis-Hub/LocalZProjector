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
