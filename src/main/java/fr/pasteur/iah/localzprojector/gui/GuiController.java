package fr.pasteur.iah.localzprojector.gui;

import java.util.List;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.scijava.Cancelable;
import org.scijava.Context;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.ui.UIService;

import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.LocalZProjectionOp;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceOp;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.util.EverythingDisablerAndReenabler;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
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
		final Supplier< Dataset > datasetSupplier = () -> imageDisplayService.getActiveDataset();

		this.guiPanel = new GuiPanel(
				datasetSupplier,
				() -> previewReferencePlane(),
				( b ) -> previewLocalProjection( b ),
				( b ) -> runLocalProjection( b ),
				() -> stop() );

		final JFrame frame = new JFrame();
		frame.getContentPane().add( guiPanel );
		frame.pack();
		frame.setVisible( true );
	}

	private void stop()
	{
		if ( null != cancelable )
			cancelable.cancel( "User pressed the stop button." );
	}

	private < T extends RealType< T > & NativeType< T > > void run( final Dataset input, final boolean showReferencePlane )
	{
		if ( null == input )
		{
			logService.warn( "Please select an image before running Local Z Projector." );
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
						showOutputDuringCalculation);
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

	private < T extends RealType< T > & NativeType< T > > void runLocalProjection( final boolean showReferencePlane )
	{
		final Dataset input = guiPanel.getSelectedDataset();
		run( input, showReferencePlane );
	}

	private < T extends RealType< T > & NativeType< T > > void previewLocalProjection( final boolean showReferencePlane )
	{
		final Dataset dataset = guiPanel.getSelectedDataset();
		if ( null == dataset )
		{
			logService.warn( "Please select an image before running Local Z Projector." );
			return;
		}

		final DisplayService displayService = ops.context().getService( DisplayService.class );
		final List< Display< ? > > displays = displayService.getDisplays( dataset );
		int currentT = 0;
		for ( final Display< ? > display : displays )
		{
			if ( display instanceof ImageDisplay )
			{
				currentT = ( ( ImageDisplay ) display ).getIntPosition( Axes.TIME );
				break;
			}
			if ( display instanceof ImagePlusDisplay )
			{
				currentT = ( ( ImagePlusDisplay ) display ).get( 0 ).getT();
				break;
			}
		}

		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > source = ( ImgPlus< T > ) dataset.getImgPlus();
		final ImgPlus< T > tp = LocalZProjectionOp.getSourceTimePoint( source, currentT, ops );
		final DefaultDataset timepoint = new DefaultDataset( context, tp );
		run( timepoint, showReferencePlane );
	}

	private < T extends RealType< T > & NativeType< T > > void previewReferencePlane()
	{
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( guiPanel, new Class[] { JLabel.class } );
		disabler.disable();
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

				final ReferenceSurfaceParameters params = guiPanel.getReferenceSurfaceParameters();

				final int channelAxis = dataset.dimensionIndex( Axes.CHANNEL );
				@SuppressWarnings( "unchecked" )
				final Img< T > img = ( Img< T > ) dataset.getImgPlus().getImg();
				final RandomAccessibleInterval< T > source;
				if ( channelAxis >= 0 )
					source = Views.hyperSlice( img, channelAxis, params.targetChannel );
				else
					source = img;

				@SuppressWarnings( { "rawtypes", "unchecked" } )
				final ReferenceSurfaceOp< T > op = ( ReferenceSurfaceOp ) Functions.unary( ops, ReferenceSurfaceOp.class, Img.class, source, params );
				cancelable = op;
				final Img< UnsignedShortType > referenceSurface = op.calculate( source );

				final UIService uiService = context.getService( UIService.class );
				uiService.show( referenceSurface );
			}
			finally
			{
				cancelable = null;
				disabler.reenable();
			}
		},
				"Local Z Projector Preview Reference Plane Thread" ).start();
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
