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
package fr.pasteur.iah.localzprojector.gui;

import java.io.File;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.scijava.Cancelable;
import org.scijava.Context;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.prefs.PrefService;
import org.scijava.ui.UIService;

import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.LocalZProjectionOp;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceOp;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.util.EverythingDisablerAndReenabler;
import fr.pasteur.iah.localzprojector.util.GuiUtil;
import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.legacy.LegacyService;
import net.imagej.legacy.display.ImagePlusDisplay;
import net.imagej.ops.OpService;
import net.imagej.ops.special.function.Functions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

public class GuiController
{

	private static final ImageIcon ICON;

	static
	{
		final ImageIcon imageIcon = new ImageIcon( ReferenceSurfacePanel.class.getResource( "LocalZProjectorLogo-512.png" ) );
		ICON = GuiUtil.scaleImage( imageIcon, 32, 32 );
	}

	private final Context context;

	private final GuiPanel guiPanel;

	private Cancelable cancelable;

	private final OpService ops;

	private final LogService logService;

	public GuiController( final Context context )
	{
		this.context = context;
		this.ops = context.getService( OpService.class );
		this.logService = context.getService( LogService.class );

		final ImageDisplayService imageDisplayService = context.getService( ImageDisplayService.class );
		this.guiPanel = new GuiPanel(
				imageDisplayService.getActiveDataset(),
				() -> previewReferencePlane(),
				( b ) -> previewLocalProjection( b ),
				() -> runLocalProjection(),
				() -> stop(),
				context.getService( PrefService.class ) );

		final JFrame frame = new JFrame();
		frame.setTitle( "Local Z Projector" );
		frame.setIconImage( ICON.getImage() );
		frame.setLocationByPlatform( true );
		frame.setLocationRelativeTo( null );
		frame.getContentPane().add( guiPanel );
		frame.pack();
		frame.setVisible( true );
	}

	private void stop()
	{
		if ( null != cancelable )
			cancelable.cancel( "User pressed the stop button." );
	}

	private < T extends RealType< T > & NativeType< T > > void run( final Dataset input, final boolean showReferencePlane, final boolean saveEachTimePoint, final String savePath )
	{
		if ( null == input )
		{
			logService.warn( "Please select an image before running Local Z Projector." );
			return;
		}

		if ( saveEachTimePoint && !new File( savePath ).canWrite() )
		{
			logService.error( "Cannot write into target folder: " + savePath + "." );
			return;
		}

		/*
		 * Prepare.
		 */

		final ExtractSurfaceParameters extractSurfaceParameters = guiPanel.getExtractSurfaceParameters();
		final ReferenceSurfaceParameters referenceSurfaceParameters = guiPanel.getReferenceSurfaceParameters();
		final boolean showOutputDuringCalculation = true;

		/*
		 * Run
		 */

		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( guiPanel, new Class[] { JLabel.class } );
		disabler.disable();
		guiPanel.runPanel.btnStop.setEnabled( true );
		new Thread( () -> {
			try
			{
				@SuppressWarnings( { "rawtypes", "unchecked" } )
				final LocalZProjectionOp< T > localZProjectionOp = ( LocalZProjectionOp ) Functions.unary(
						ops,
						LocalZProjectionOp.class,
						Dataset.class,
						Dataset.class,
						referenceSurfaceParameters,
						extractSurfaceParameters,
						showReferencePlane,
						showOutputDuringCalculation,
						saveEachTimePoint,
						savePath );
				cancelable = localZProjectionOp;
				localZProjectionOp.calculate( input );
			}
			finally
			{
				cancelable = null;
				disabler.reenable();
			}
		}, "Local Z Projector Run Local Projection Thread" ).start();
	}

	private < T extends RealType< T > & NativeType< T > > void runLocalProjection()
	{
		final boolean showReferenceSurfaceMovie = guiPanel.runPanel.showReferenceSurfaceMovie();
		final boolean saveEachTimepoint = guiPanel.runPanel.saveEachTimepoint();
		final String savePath = guiPanel.runPanel.getSavePath(); 
		final Dataset input = guiPanel.getSelectedDataset();
		run( input, showReferenceSurfaceMovie, saveEachTimepoint, savePath );
	}

	private < T extends RealType< T > & NativeType< T > > void previewLocalProjection( final boolean showReferencePlane )
	{
		final Dataset dataset = guiPanel.getSelectedDataset();
		if ( null == dataset )
		{
			logService.warn( "Please select an image before running Local Z Projector." );
			return;
		}

		final int currentT = getCurrentTimePoint( dataset );
		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > source = ( ImgPlus< T > ) dataset.getImgPlus();
		final ImgPlus< T > tp = LocalZProjectionOp.getSourceTimePoint( source, currentT, ops );
		final DefaultDataset timepoint = new DefaultDataset( context, tp );
		run( timepoint, showReferencePlane, false, "" );
	}

	private < T extends RealType< T > & NativeType< T > > void previewReferencePlane()
	{
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( guiPanel, new Class[] { JLabel.class } );
		disabler.disable();
		guiPanel.runPanel.btnStop.setEnabled( true );

		new Thread( () -> {
			try
			{
				final LogService logService = context.getService( LogService.class );

				final Dataset dataset = guiPanel.getSelectedDataset();
				if ( null == dataset )
				{
					logService.warn( "Please select an image before running Local Z Projector." );
					return;
				}

				final int currentT = getCurrentTimePoint( dataset );
				@SuppressWarnings( "unchecked" )
				final ImgPlus< T > source = ( ImgPlus< T > ) dataset.getImgPlus();
				final ImgPlus< T > tp = LocalZProjectionOp.getSourceTimePoint( source, currentT, ops );

				final ReferenceSurfaceParameters params = guiPanel.getReferenceSurfaceParameters();

				final int channelAxis = tp.dimensionIndex( Axes.CHANNEL );

				final RandomAccessibleInterval< T > tpc;
				if ( channelAxis >= 0 )
					tpc = Views.hyperSlice( tp, channelAxis, params.targetChannel );
				else
					tpc = tp;

				@SuppressWarnings( { "rawtypes", "unchecked" } )
				final ReferenceSurfaceOp< T > op = ( ReferenceSurfaceOp ) Functions.unary( ops, ReferenceSurfaceOp.class, Img.class, tpc, params );
				cancelable = op;
				final Img< UnsignedShortType > referenceSurface = op.calculate( tpc );

				final UIService uiService = context.getService( UIService.class );
				uiService.show( "Reference surface preview tp " + currentT + "  of " + dataset.getName(), referenceSurface );
			}
			finally
			{
				cancelable = null;
				disabler.reenable();
			}
		},
				"Local Z Projector Preview Reference Plane Thread" ).start();
	}

	private int getCurrentTimePoint( final Dataset dataset )
	{
		final DisplayService displayService = ops.context().getService( DisplayService.class );
		final List< Display< ? > > displays = displayService.getDisplays( dataset );
		for ( final Display< ? > display : displays )
		{
			if ( display instanceof ImageDisplay )
			{
				final ImageDisplay imaegDisplay = ( ImageDisplay ) display;

				// Top priority: can we find an ImagePlus for this dataset?
				final LegacyService legacyService = ops.context().getService( LegacyService.class );
				if ( null != legacyService )
				{
					final ImagePlus imp = legacyService.getImageMap().lookupImagePlus( imaegDisplay );
					return imp.getT();
				}

				return ( ( ImageDisplay ) display ).getActiveView().getIntPosition( Axes.TIME );
			}
			
			if ( display instanceof ImagePlusDisplay )
				return ( ( ImagePlusDisplay ) display ).get( 0 ).getT();
		}
		return 0;
	}

	public void setReferenceSurfaceParameters( final ReferenceSurfaceParameters referenceSurfaceParameters )
	{
		final ReferenceSurfacePanel referenceSurfacePanel = guiPanel.referenceSurfacePanel;
		if ( null == referenceSurfacePanel )
			return;

		referenceSurfacePanel.setParameters( referenceSurfaceParameters );
	}

	public void setExtractSurfaceParameters( final ExtractSurfaceParameters extractSurfaceParameters )
	{
		final ExtractSurfacePanel extractSurfacePanel = guiPanel.extractSurfacePanel;
		if ( null == extractSurfacePanel )
			return;

		extractSurfacePanel.setParameters( extractSurfaceParameters );
	}

	public void setShowReferenceSurfaceMovie( final boolean showReferenceSurfaceMovie )
	{
		guiPanel.runPanel.setShowReferenceSurfaceMovie( showReferenceSurfaceMovie );
	}
}
