package fr.pasteur.iah.localzprojector.gui;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.scijava.Cancelable;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.ui.UIService;

import fr.pasteur.iah.localzprojector.process.LocalProjectionOp;
import fr.pasteur.iah.localzprojector.process.LocalProjectionParameters;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceOp;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.util.EverythingDisablerAndReenabler;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.display.ImageDisplayService;
import net.imagej.ops.OpService;
import net.imagej.ops.special.function.Functions;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
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

	private < T extends RealType< T > & NativeType< T > > void runLocalProjection( final boolean showReferencePlane )
	{
		/*
		 * Prepare.
		 */

		final Dataset dataset = guiPanel.getSelectedDataset();
		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > source = ( ImgPlus< T > ) dataset.getImgPlus();

		/*
		 * Prepare output.
		 */

		int nDims = 2;
		final int channelAxis = source.dimensionIndex( Axes.CHANNEL );
		if ( channelAxis >= 0 )
			nDims++;
		final int timeAxis = source.dimensionIndex( Axes.TIME );
		if ( timeAxis >= 0 )
			nDims++;

		final long[] dims = new long[ nDims ];
		int currentDim = 0;
		dims[ currentDim++ ] = source.dimension( 0 );
		dims[ currentDim++ ] = source.dimension( 1 );
		if ( channelAxis >= 0 )
			dims[ currentDim++ ] = source.dimension( channelAxis );
		if ( timeAxis >= 0 )
			dims[ currentDim++ ] = source.dimension( timeAxis );
		final Dimensions dimensions = new FinalDimensions( dims );
		final Img< T > output = ops.create().img( dimensions, Util.getTypeFromInterval( source ) );

		/*
		 * Loop over time.
		 */

		final long nTimepoints = dataset.getFrames();
		for ( long t = 0; t < nTimepoints; t++ )
		{
			previewLocalProjection( t, showReferencePlane );
		}

	}

	private < T extends RealType< T > & NativeType< T > > void previewLocalProjection( final long t, final boolean showReferencePlane )
	{
		final Dataset dataset = guiPanel.getSelectedDataset();
		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > source = ( ImgPlus< T > ) dataset.getImgPlus();
		final ImgPlus< T > timepoint = sliceTimepoint( source, t );

		/*
		 * Extract reference.
		 */

		final ReferenceSurfaceParameters referenceSurfaceParams = guiPanel.getReferenceSurfaceParameters();

		final int channelAxis = timepoint.dimensionIndex( Axes.CHANNEL );
		final RandomAccessibleInterval< T > currentChannel;
		if ( channelAxis >= 0 )
			currentChannel = Views.hyperSlice( source, channelAxis, referenceSurfaceParams.targetChannel );
		else
			currentChannel = source;

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final ReferenceSurfaceOp< T > op = ( ReferenceSurfaceOp ) Functions.unary( ops, ReferenceSurfaceOp.class, Img.class, currentChannel, referenceSurfaceParams );
		cancelable = op;
		final Img< IntType > referenceSurface = op.calculate( currentChannel );

		if ( showReferencePlane )
		{
			final UIService uiService = context.getService( UIService.class );
			uiService.show( referenceSurface );

			// HOW TO STORE THIS FOR MULTI T?
		}

		/*
		 * Run local projection
		 */

		final LocalProjectionParameters localProjectionParameters = guiPanel.getLocalProjectionParameters();

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		final LocalProjectionOp< T > op2 = ( LocalProjectionOp ) Functions.binary( ops, LocalProjectionOp.class, Dataset.class, dataset, referenceSurface, localProjectionParameters );
		cancelable = op2;
		final Dataset surface = op2.calculate( dataset, referenceSurface );

	}

	/**
	 * Returns the single specified time-point in the source {@link ImgPlus} as
	 * a copy, of the source itself if there is not time axis.
	 * 
	 * @param source
	 *            the source to reslice.
	 * @param t
	 *            the time-point to extract.
	 * @return an {@link ImgPlus}.
	 */
	private < T extends RealType< T > & NativeType< T > > ImgPlus< T > sliceTimepoint( final ImgPlus< T > source, final long t )
	{
		final int timeIndex = source.dimensionIndex( Axes.TIME );
		if ( timeIndex < 0 )
			return source;

		final IntervalView< T > hyperSlice = Views.hyperSlice( source, timeIndex, t );
		final Img< T > img = ops.create().img( hyperSlice, source.firstElement() );

		final CalibratedAxis[] axes = new CalibratedAxis[ source.numDimensions() - 1 ];
		int od = 0;
		for ( int d = 0; d < source.numDimensions(); d++ )
		{
			if ( d == timeIndex )
				continue;
			axes[ od++ ] = source.axis( d );
		}
		return new ImgPlus<>( img, source.getName() + "_T=" + t, axes );
	}

	private < T extends RealType< T > & NativeType< T > > void previewLocalProjection( final boolean showReferencePlane )
	{
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( guiPanel, new Class[] { JLabel.class } );
		disabler.disable();
		new Thread( () -> {
			try
			{
				final Dataset dataset = guiPanel.getSelectedDataset();
				if ( null == dataset )
				{
					logService.warn( "Please select an image before running Local Z Projector." );
					return;
				}

				/*
				 * Extract reference.
				 */

				final ReferenceSurfaceParameters referenceSurfaceParams = guiPanel.getReferenceSurfaceParameters();

				final int channelAxis = dataset.dimensionIndex( Axes.CHANNEL );
				@SuppressWarnings( "unchecked" )
				final Img< T > img = ( Img< T > ) dataset.getImgPlus().getImg();
				final RandomAccessibleInterval< T > source;
				if ( channelAxis >= 0 )
					source = Views.hyperSlice( img, channelAxis, referenceSurfaceParams.targetChannel );
				else
					source = img;

				@SuppressWarnings( { "rawtypes", "unchecked" } )
				final ReferenceSurfaceOp< T > op = ( ReferenceSurfaceOp ) Functions.unary( ops, ReferenceSurfaceOp.class, Img.class, source, referenceSurfaceParams );
				cancelable = op;
				final Img< IntType > referenceSurface = op.calculate( source );

				if ( showReferencePlane )
				{
					final UIService uiService = context.getService( UIService.class );
					uiService.show( referenceSurface );
				}

				/*
				 * Run local projection
				 */

				final LocalProjectionParameters localProjectionParameters = guiPanel.getLocalProjectionParameters();

				@SuppressWarnings( { "unchecked", "rawtypes" } )
				final LocalProjectionOp< T > op2 = ( LocalProjectionOp ) Functions.binary( ops, LocalProjectionOp.class, Dataset.class, dataset, referenceSurface, localProjectionParameters );
				cancelable = op2;
				final Dataset surface = op2.calculate( dataset, referenceSurface );

				final UIService uiService = context.getService( UIService.class );
				uiService.show( surface );
			}
			finally
			{
				cancelable = null;
				disabler.reenable();
			}
		},
				"Local Z Projector Preview Local Projection Thread" ).start();

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
				final Img< IntType > referenceSurface = op.calculate( source );

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

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException
	{
		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final ImageJ ij = new ImageJ();
		ij.launch( args );

		final String imageFile = "samples/Composite.tif";
		final Object open = ij.io().open( imageFile );
		ij.ui().show( open );

		new GuiController( ij.context() );
	}
}
