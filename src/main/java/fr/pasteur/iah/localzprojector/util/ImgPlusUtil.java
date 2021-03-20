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

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.space.CalibratedSpace;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.type.NativeType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class ImgPlusUtil
{

	/**
	 * Return the array of calibrated axes taken from the source image, slicing
	 * for the dimension specified by <code>axis</code>. If the axis cannot be
	 * found, all the axes of the source image are returned. The axes are
	 * returned as an array, in the same order that of the source image, to be
	 * used with the
	 * {@link Views#hyperSlice(net.imglib2.RandomAccessible, int, long)} method.
	 * 
	 * @param img
	 *            the source image.
	 * @param axis
	 *            the axis to look for.
	 * @return a new array of axes.
	 */
	public static CalibratedAxis[] hypersliceAxes( final CalibratedSpace< CalibratedAxis > img, final AxisType axis )
	{
		final int dimensionIndex = img.dimensionIndex( axis );
		final int nAxis = ( dimensionIndex < 0 )
				? img.numDimensions()
				: img.numDimensions() - 1;

		final CalibratedAxis[] axes = new CalibratedAxis[ nAxis ];
		int id = 0;
		for ( int d = 0; d < img.numDimensions(); d++ )
		{
			if ( d == dimensionIndex )
				continue;
			axes[ id++ ] = img.axis( d );
		}
		return axes;
	}

	public static < T extends NativeType< T > > ImgPlus< T > hyperslice( final ImgPlus< T > img, final AxisType axis, final long pos )
	{
		final int dimensionIndex = img.dimensionIndex( axis );
		if ( dimensionIndex < 0 )
			return img;

		final CalibratedAxis[] axes = hypersliceAxes( img, axis );

		final RandomAccessibleInterval< T > rai = Views.hyperSlice( img, dimensionIndex, pos );
		final Img< T > wrapped = ImgView.wrap( rai, Util.getArrayOrCellImgFactory( rai, img.firstElement() ) );

		return new ImgPlus<>( wrapped, img.getName() + "_" + axis.getLabel() + "-" + pos, axes );
	}

	public static < T extends NativeType< T > > ImgPlus< T > hypersliceChannel( final ImgPlus< T > img, final long c )
	{
		return hyperslice( img, Axes.CHANNEL, c );
	}

	public static < T extends NativeType< T > > ImgPlus< T > hypersliceTimePoint( final ImgPlus< T > img, final long t )
	{
		return hyperslice( img, Axes.TIME, t );
	}

	public static < T extends NativeType< T > > ImgPlus< T > wrapToImgPlus(
			final RandomAccessibleInterval< T > rai )
	{
		if ( rai instanceof ImgPlus )
			return ( ImgPlus< T > ) rai;
		return new ImgPlus<>( wrapToImg( rai ) );
	}

	public static final < T extends NativeType< T > > Img< T > wrapToImg(
			final RandomAccessibleInterval< T > rai )
	{
		if ( rai instanceof Img )
			return ( Img< T > ) rai;
		return ImgView.wrap( rai, imgFactory( rai ) );
	}

	public static < T extends NativeType< T > > ImgFactory< T > imgFactory(
			final RandomAccessibleInterval< T > rai )
	{
		final T type = Util.getTypeFromInterval( rai );
		return Util.getSuitableImgFactory( rai, type );
	}

	private ImgPlusUtil()
	{}
}
