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
package fr.pasteur.iah.localzprojector;

import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import bdv.BigDataViewer;
import bdv.export.ProgressWriter;
import bdv.export.ProgressWriterConsole;
import bdv.viewer.ViewerOptions;
import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters.ProjectionMethod;
import fr.pasteur.iah.localzprojector.process.LocalZProjectionOp;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters.Method;
import fr.pasteur.iah.localzprojector.util.ImgPlusUtil;
import mpicbg.spim.data.SpimDataException;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.DefaultLinearAxis;
import net.imagej.ops.special.function.Functions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class TestBigImageBDV
{

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, SpimDataException
	{
		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final String xmlFilename = "samples/A.xml";

		final ImageJ ij = new ImageJ();
		ij.launch( args );

		final ProgressWriter progressWriter = new ProgressWriterConsole();
		final ViewerOptions options = ViewerOptions.options();
		final BigDataViewer bdv = BigDataViewer.open( xmlFilename, "Large imges", progressWriter, options );
		
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< T > source = ( RandomAccessibleInterval< T > ) bdv.getViewer().state()
				.getSources().get( 0 ).getSpimSource().getSource( 0, 0 );

		final ImgPlus< T > imgPlus = ImgPlusUtil.wrapToImgPlus( source );
		imgPlus.setAxis( new DefaultLinearAxis( Axes.X, 0.1625 ), 0 );
		imgPlus.setAxis( new DefaultLinearAxis( Axes.Y, 0.1625 ), 1 );
		imgPlus.setAxis( new DefaultLinearAxis( Axes.Z, 0.1625 ), 2 );

		final DefaultDataset dataset = new DefaultDataset( ij.context(), imgPlus );
		
		final int channel = 0;
		final ReferenceSurfaceParameters referenceSurfaceParams = ReferenceSurfaceParameters.create()
				.method( Method.MAX_OF_MEAN )
				.zMin( 0 )
				.zMax( 100000 )
				.filterWindowSize( 40 )
				.binning( 4 )
				.gaussianPreFilter( 0.5 )
				.targetChannel( channel )
				.medianPostFilterHalfSize( 100 / 4 )
				.get();
		final ExtractSurfaceParameters extractSurfaceParameters = ExtractSurfaceParameters.create()
				.zOffset( 0, 0 )
				.deltaZ( 0, 1 )
				.projectionMethod( 0, ProjectionMethod.MIP )
				.get();
		final boolean showReferencePlane = true;
		final boolean showOutputDuringCalculation = true;

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final LocalZProjectionOp< T > localZProjectionOp = ( LocalZProjectionOp ) Functions.unary(
				ij.op(),
				LocalZProjectionOp.class,
				Dataset.class,
				Dataset.class,
				referenceSurfaceParams,
				extractSurfaceParameters,
				showReferencePlane,
				showOutputDuringCalculation );

		final long start = System.currentTimeMillis();
		localZProjectionOp.calculate( dataset );
		final long end = System.currentTimeMillis();
		System.out.println( String.format( "Projection time: %.2f s.", ( end - start ) / 1000. ) );
	}
}
