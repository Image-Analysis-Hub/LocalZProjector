package fr.pasteur.iah.localzprojector.process;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.display.DisplayService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.VersionUtils;

import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters.ProjectionMethod;
import fr.pasteur.iah.localzprojector.util.ImgPlusUtil;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imagej.autoscale.AutoscaleService;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.ops.OpEnvironment;
import net.imagej.ops.special.computer.Computers;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

@Plugin( type = LocalZProjectionOp.class )
public class LocalZProjectionOp< T extends RealType< T > & NativeType< T > > extends AbstractUnaryFunctionOp< Dataset, Dataset > implements Cancelable
{

	@Parameter( type = ItemIO.INPUT )
	protected ReferenceSurfaceParameters referenceSurfaceParams;

	@Parameter( type = ItemIO.INPUT )
	protected ExtractSurfaceParameters extractSurfaceParams;

	@Parameter( type = ItemIO.INPUT, required = false )
	protected boolean showReferenceSurface = false;

	@Parameter( type = ItemIO.INPUT, required = false )
	protected boolean showOutputDuringCalculation = false;

	@Parameter( type = ItemIO.INPUT, required = false )
	protected boolean saveAtEachTimePoint = false;

	@Parameter( type = ItemIO.INPUT, required = false )
	protected String saveFolder = System.getProperty( "user.home" );

	@Parameter
	protected DisplayService displayService;

	@Parameter
	protected StatusService status;

	@Parameter
	protected DatasetIOService ioService;

	@Parameter
	private AutoscaleService autoscaleService;

	protected String cancelReason;

	protected Cancelable cancelable;

	/**
	 * Stores the reference surface.
	 */
	private DefaultDataset referenceSurfaces;

	@SuppressWarnings( "unchecked" )
	@Override
	public Dataset calculate( final Dataset input )
	{
		cancelReason = null;

		/*
		 * Do we have a Z axis?
		 */

		final int zAxis = input.dimensionIndex( Axes.Z );
		if ( zAxis < 0 )
			return input.duplicate();

		/*
		 * Do we have to project or collect a thick slice?
		 */

		boolean doCollect = false;
		int deltaZCollect = 0;
		final long nChannels = input.dimension( Axes.CHANNEL );
		for ( int c = 0; c < nChannels; c++ )
		{
			if ( extractSurfaceParams.projectionMethod( c ).equals( ProjectionMethod.COLLECT ) )
			{
				doCollect = true;
				deltaZCollect = Math.max( deltaZCollect, Math.abs( extractSurfaceParams.deltaZ( c ) ) );
			}
		}

		final String outputName = doCollect
				? "Local Volume of " + input.getName()
				: "Local Z Projection of " + input.getName();

		/*
		 * Create output.
		 */

		final CalibratedAxis[] outputAxes = doCollect
				? new CalibratedAxis[ input.numDimensions() ]
				: new CalibratedAxis[ input.numDimensions() - 1 ];
		final long[] outputDims = doCollect
				? new long[ input.numDimensions() ]
				: new long[ input.numDimensions() - 1 ];

		int id = 0;
		for ( int d = 0; d < input.numDimensions(); d++ )
		{
			if ( d == zAxis )
			{
				if ( doCollect )
				{
					outputDims[ id ] = 1 + 2 * deltaZCollect;
					outputAxes[ id++ ] = input.axis( d );
				}
				else
					continue;
			}
			else
			{
				outputDims[ id ] = input.dimension( d );
				outputAxes[ id++ ] = input.axis( d );
			}
		}
		final Img< T > outputImg = ops().create().img( FinalDimensions.wrap( outputDims ), ( T ) input.firstElement() );
		final ImgPlus< T > outImgPlus = new ImgPlus<>( outputImg, outputName, outputAxes );
		final DefaultDataset output = new DefaultDataset( ops().context(), outImgPlus );

		/*
		 * Stores reference surface in a dataset.
		 */

		// We want single channel & single Z.
		final int cAxis = input.dimensionIndex( Axes.CHANNEL );
		final long[] refSurfaceDims = cAxis < 0
				? new long[ input.numDimensions() - 1 ]
				: new long[ input.numDimensions() - 2 ];
		final CalibratedAxis[] refSurfaceAxes = cAxis < 0
				? new CalibratedAxis[ input.numDimensions() - 1 ]
				: new CalibratedAxis[ input.numDimensions() - 2 ];
		int id2 = 0;
		for ( int d = 0; d < input.numDimensions(); d++ )
		{
			if ( d == cAxis || d == zAxis )
				continue;
			refSurfaceDims[ id2 ] = input.dimension( d );
			refSurfaceAxes[ id2++ ] = input.axis( d );
		}
		final Img< UnsignedShortType > refSurfaceImg = ops().create().img( FinalDimensions.wrap( refSurfaceDims ), new UnsignedShortType() );
		final ImgPlus< UnsignedShortType > refSurfaceImgPlus = new ImgPlus<>( refSurfaceImg, "Reference surface of " + input.getName(), refSurfaceAxes );
		referenceSurfaces = new DefaultDataset( ops().context(), refSurfaceImgPlus );

		/*
		 * Show output?
		 */

		final ImageDisplay referenceSurfaceDisplay;
		final ImageDisplay projectionDisplay;
		if ( showOutputDuringCalculation )
		{
			projectionDisplay = ( ImageDisplay ) displayService.createDisplay( output );

			// Try to see if we can force display as composite.
			output.setCompositeChannelCount( ( int ) output.dimension( Axes.CHANNEL ) );

			// Autoscale based on source display.
			final DatasetView dataViewProjection = ( DatasetView ) projectionDisplay.get( 0 );
			for ( int c = 0; c < output.dimension( Axes.CHANNEL ); c++ )
			{
				final double min = input.getChannelMinimum( c );
				final double max = input.getChannelMaximum( c );
				final double range = max - min;
				final double alpha = 0.; // display saturation.
				output.setChannelMinimum( c, min + alpha * range );
				output.setChannelMaximum( c, max - alpha * range );
				dataViewProjection.setChannelRange( c, output.getChannelMinimum( c ), output.getChannelMaximum( c ) );
			}
			projectionDisplay.update();

			if ( showReferenceSurface )
			{
				referenceSurfaceDisplay = ( ImageDisplay ) displayService.createDisplay( referenceSurfaces );
				final DatasetView dataViewReference = ( DatasetView ) referenceSurfaceDisplay.get( 0 );
				dataViewReference.setChannelRanges( 0, input.dimension( Axes.Z ) );
			}
			else
			{
				referenceSurfaceDisplay = null;
			}
		}
		else
		{
			projectionDisplay = null;
			referenceSurfaceDisplay = null;
		}

		/*
		 * Process time-point by time-point.
		 */

		// Create reference surface op.
		@SuppressWarnings( "rawtypes" )
		final ReferenceSurfaceOp< T > referenceSurfaceOp = ( ReferenceSurfaceOp ) Functions.unary(
				ops(),
				ReferenceSurfaceOp.class,
				Img.class,
				ImgPlus.class,
				referenceSurfaceParams );

		// Create projection op.
		final ProjectorOp< T > projectorOp;
		if ( doCollect )
		{
			@SuppressWarnings( "rawtypes" )
			final CollectVolumeOp< T > lop = ( CollectVolumeOp ) Computers.binary( ops(),
					CollectVolumeOp.class,
					RandomAccessibleInterval.class,
					ImgPlus.class,
					RandomAccessibleInterval.class,
					extractSurfaceParams,
					deltaZCollect );
			projectorOp = lop;
		}
		else
		{
			@SuppressWarnings( "rawtypes" )
			final ProjectorOp< T > lop = ( ExtractSurfaceOnePassOp ) Computers.binary( ops(),
					ExtractSurfaceOnePassOp.class,
					RandomAccessibleInterval.class,
					ImgPlus.class,
					RandomAccessibleInterval.class,
					extractSurfaceParams );
			projectorOp = lop;
		}

		final long nFrames = input.getFrames();
		for ( long t = 0; t < nFrames; t++ )
		{

			status.showStatus( "Processing time-point " + t );
			final ImgPlus< T > tp = getSourceTimePoint( ( ImgPlus< T > ) input.getImgPlus(), t, ops() );

			/*
			 * Get reference surface.
			 */

			if ( isCanceled() )
				return output;
			cancelable = referenceSurfaceOp;

			final ImgPlus< T > channel = ImgPlusUtil.hypersliceChannel( tp, referenceSurfaceParams.targetChannel );
			final Img< UnsignedShortType > referenceSurface = referenceSurfaceOp.calculate( channel );
			copyOnReferenceSurfaceOutput( referenceSurface, ( ImgPlus< UnsignedShortType > ) referenceSurfaces.getImgPlus(), t );

			/*
			 * Show reference surface?
			 */

			if ( showOutputDuringCalculation && showReferenceSurface )
			{
				final int timeAxisIndex = referenceSurfaceDisplay.dimensionIndex( Axes.TIME );
				if ( timeAxisIndex >= 0 )
					referenceSurfaceDisplay.setPosition( t, timeAxisIndex );
				referenceSurfaceDisplay.update();
			}

			/*
			 * Extract surface.
			 */

			if ( isCanceled() )
				return output;
			cancelable = projectorOp;

			final ImgPlus< T > outputSlice = ImgPlusUtil.hypersliceTimePoint( outImgPlus, t );
			projectorOp.compute( tp, referenceSurface, outputSlice );

			if ( showOutputDuringCalculation )
			{
				output.update();
				final int timeAxisIndex = projectionDisplay.dimensionIndex( Axes.TIME );
				if ( timeAxisIndex >= 0 )
					projectionDisplay.setPosition( t, timeAxisIndex );
				projectionDisplay.update();
			}

			/*
			 * Save at each time-point.
			 */

			if ( saveAtEachTimePoint )
			{
				final String str = input.getName() == null
						? "LocalZProjectorOutput"
						: input.getName();
				final int dotIndex = str.lastIndexOf( '.' );
				final String inputName = dotIndex > 0
						? str.substring( 0, dotIndex )
						: str;
				final int ndigits = Long.toString( nFrames ).length();
				// Save reference surface if asked.
				if ( showReferenceSurface )
				{
					final String refTpName = String.format( "%s_RefSurface_%0" + ndigits + "d", inputName, t );
					
					final CalibratedAxis[] axesRefSurface = new CalibratedAxis[ 2 ];
					axesRefSurface[ 0 ] = input.getImgPlus().axis( input.getImgPlus().dimensionIndex( Axes.X ) );
					axesRefSurface[ 1 ] = input.getImgPlus().axis( input.getImgPlus().dimensionIndex( Axes.Y ) );

					final ImgPlus< UnsignedShortType > imgPlusSingleTP = new ImgPlus<>( referenceSurface, refTpName, axesRefSurface );
					final Dataset refSurfaceDataset = new DefaultDataset( ioService.context(), imgPlusSingleTP );

					final Path destination = Paths.get( saveFolder, refTpName + ".tif" );
					try
					{
						ioService.save( refSurfaceDataset, destination.toString() );
					}
					catch ( final IOException e )
					{
						e.printStackTrace();
					}
				}

				final String outputTpName = String.format( "%s_LocalProjection_%0" + ndigits + "d", inputName, t );
				final CalibratedAxis[] outputTpAxes = ImgPlusUtil.hypersliceAxes( outImgPlus, Axes.TIME );
				final ImgPlus< T > imgPlusOutputSingleTP = new ImgPlus< T >( ImgPlusUtil.wrapToImgPlus( outputSlice ), outputTpName, outputTpAxes );
				final Dataset outputTpDataset = new DefaultDataset( ioService.context(), imgPlusOutputSingleTP );

				final Path destination = Paths.get( saveFolder, outputTpName + ".tif" );
				try
				{
					ioService.save( outputTpDataset, destination.toString() );
				}
				catch ( final IOException e )
				{
					e.printStackTrace();
				}

			}

			status.showProgress( ( int ) t + 1, ( int ) nFrames );
		}

		return output;
	}

	/**
	 * Return the dataset containing the reference surface calculated during the
	 * last projection (after {@link #calculate()}).
	 * 
	 * @return the reference surface as dataset.
	 */
	public DefaultDataset getReferenceSurface()
	{
		return referenceSurfaces;
	}

	private void copyOnReferenceSurfaceOutput( final Img< UnsignedShortType > tp, final ImgPlus< UnsignedShortType > output, final long t )
	{
		final int timeAxis = output.dimensionIndex( Axes.TIME );
		if ( timeAxis < 0 )
		{
			ops().copy().rai( output, tp );
		}
		else
		{
			final IntervalView< UnsignedShortType > slice = Views.hyperSlice( output, timeAxis, t );
			ops().copy().rai( slice, tp );
		}
	}

	public static final < T extends RealType< T > & NativeType< T > > ImgPlus< T > getSourceTimePoint( final ImgPlus< T > img, final long t, final OpEnvironment ops )
	{
		final int timeAxis = img.dimensionIndex( Axes.TIME );
		if ( timeAxis < 0 )
			return img;

		final IntervalView< T > tp = Views.hyperSlice( img, timeAxis, t );
		final Img< T > copy = ImgView.wrap( tp, Util.getArrayOrCellImgFactory( tp, img.firstElement() ) );

		// Put back axes.
		final CalibratedAxis[] axes = new CalibratedAxis[ img.numDimensions() - 1 ];
		int id = 0;
		for ( int d = 0; d < img.numDimensions(); d++ )
		{
			if ( d == timeAxis )
				continue;
			axes[ id++ ] = img.axis( d );
		}
		final ImgPlus< T > imgPlus = new ImgPlus<>( copy, "Time-point " + t + "  of " + img.getName(), axes );
		return imgPlus;
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
		if ( null != cancelable )
			cancelable.cancel( reason );
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}

	/**
	 * Returns the current version of LocalZProjector as a string.
	 * 
	 * @return the current version of LocalZProjector.
	 */
	public String getVersion()
	{
		return VersionUtils.getVersion( LocalZProjectionOp.class );
	}
}
