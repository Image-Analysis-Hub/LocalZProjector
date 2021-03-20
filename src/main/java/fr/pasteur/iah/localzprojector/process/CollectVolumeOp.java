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
package fr.pasteur.iah.localzprojector.process;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.ops.OpService;
import net.imagej.ops.special.computer.AbstractBinaryComputerOp;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

@Plugin( type = CollectVolumeOp.class )
public class CollectVolumeOp< T extends RealType< T > & NativeType< T > >
		extends AbstractBinaryComputerOp< ImgPlus< T >, RandomAccessibleInterval< UnsignedShortType >, RandomAccessibleInterval< T > >
		implements ProjectorOp< T >
{

	@Parameter
	private ExtractSurfaceParameters params;

	@Parameter
	private int deltaZ = 0;

	@Parameter
	private OpService ops;

	private String cancelReason;

	@Override
	public void compute( final ImgPlus< T > source, final RandomAccessibleInterval< UnsignedShortType > referenceSurface, final RandomAccessibleInterval< T > output )
	{
		// Prepare.
		cancelReason = null;

		final Img< T > img = source.getImg();

		final int channelAxis = source.dimensionIndex( Axes.CHANNEL );
		if ( channelAxis < 0 )
		{
			// Check input.
			if ( source.numDimensions() != 3 )
				throw new IllegalArgumentException( "Expected single-channel source to be 3D, but was " + source.numDimensions() + "D." );

			// Process.
			processChannel( img, 0, referenceSurface, output );
		}
		else
		{

			// Check input.
			if ( source.numDimensions() != 4 )
				throw new IllegalArgumentException( "Expected multi-channel source to be 4D (3D+C), but was " + source.numDimensions() + "D." );
			final long nChannels = img.dimension( channelAxis );

			// Process.
			for ( int c = 0; c < nChannels; c++ )
			{
				final IntervalView< T > channel = Views.hyperSlice( img, channelAxis, c );
				final IntervalView< T > target = Views.hyperSlice( output, channelAxis, c );
				processChannel( channel, c, referenceSurface, target );
				if ( isCanceled() )
					break;
			}
		}
	}

	private void processChannel( final RandomAccessibleInterval< T > channel, final int c, final RandomAccessibleInterval< UnsignedShortType > referenceSurface, final RandomAccessibleInterval< T > target )
	{

		final Cursor< T > cursor = Views.iterable( target ).localizingCursor(); // 3D
		final RandomAccess< UnsignedShortType > raReference = referenceSurface.randomAccess( referenceSurface ); // 2D
		final RandomAccess< T > raInput = Views.extendZero( channel ).randomAccess(); // 3D

		// Iterate over output.
		while ( cursor.hasNext() )
		{
			cursor.fwd();

			// Read offset set by the reference surface.
			raReference.setPosition( cursor.getLongPosition( 0 ), 0 );
			raReference.setPosition( cursor.getLongPosition( 1 ), 1 );
			final int referenceOffset = raReference.get().get();

			// Position the input cursor at the right z.
			final long zTarget = cursor.getLongPosition( 2 );
			raInput.setPosition( cursor.getLongPosition( 0 ), 0 );
			raInput.setPosition( cursor.getLongPosition( 1 ), 1 );
			raInput.setPosition( referenceOffset - deltaZ + zTarget + params.offset( c ), 2 );

			// Copy values.
			cursor.get().set( raInput.get() );
		}
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
