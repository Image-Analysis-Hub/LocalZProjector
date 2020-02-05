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
import net.imagej.ops.special.computer.Computers;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.FinalDimensions;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * A one-pass version of the Local Z Projection tool.
 * <p>
 * This version is well suited to generating the projection of large 3D
 * datasets, that are larger than available RAM. In this case, the input dataset
 * has to be opened as a virtual-stack in Fiji. The virtual stack strategy reads
 * one Z-plane from the disk at a time, which will become the performance
 * bottleneck if we have to access various Z-position during processing. By
 * doing one-pass, we ensure that we load each Z-plane at most once. The
 * projection is done on the fly. The price to pay for this is the limited
 * (compared to the classical LocalZProjectorOp) configuration capability of the
 * projection. The projection must be the done with MIP, and deltaZ is 0: the
 * local projection only includes 1 plane. Also, we cannot do height-map image
 * post-processing (median filter).
 * 
 * @author Jean-Yves Tinevez
 *
 * @param <T>
 *            the type of the input image.
 */
@Plugin( type = LocalZProjectionOnePassOp.class )
public class LocalZProjectionOnePassOp< T extends RealType< T > & NativeType< T > > extends AbstractUnaryFunctionOp< Dataset, Dataset > implements Cancelable
{

	@Parameter( type = ItemIO.INPUT )
	protected ReferenceSurfaceParameters referenceSurfaceParams;

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

		final String outputName = "Local Z Projection of " + input.getName();

		/*
		 * Create output.
		 */

		// We don't want to deal with channels.
		final int cAxis = input.dimensionIndex( Axes.CHANNEL );
		final int nDims = ( cAxis < 0 ) ? input.numDimensions() - 1 : input.numDimensions() - 2;

		final CalibratedAxis[] outputAxes = new CalibratedAxis[ nDims ];
		final long[] outputDims = new long[ nDims ];

		int id = 0;
		for ( int d = 0; d < input.numDimensions(); d++ )
		{
			if ( d == zAxis || d == cAxis )
			{
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
		final long[] refSurfaceDims = new long[ nDims ];
		final CalibratedAxis[] refSurfaceAxes = new CalibratedAxis[ nDims ];
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
		final FilterBasedOnePassProjectorOp< T > projectionOp = ( FilterBasedOnePassProjectorOp ) Computers.unary(
				ops(),
				FilterBasedOnePassProjectorOp.class,
				ImgPlus.class,
				ImgPlus.class,
				referenceSurfaceParams );

		final long nFrames = input.getFrames();
		for ( long t = 0; t < nFrames; t++ )
		{

			status.showStatus( "Processing time-point " + t );

			// Get a VIEW of the time-point, make no copy.
			final ImgPlus< T > timepoint = ImgPlusUtil.hypersliceTimePoint( ( ImgPlus< T > ) input.getImgPlus(), t );
			// View of the target channel.
			final ImgPlus< T > channel = ImgPlusUtil.hypersliceChannel( timepoint, referenceSurfaceParams.targetChannel );

			// View of the time-point in the output dataset.
			final ImgPlus< T > outputSlice = ImgPlusUtil.hypersliceTimePoint( outImgPlus, t );

			/*
			 * Get reference surface & project all at once.
			 */

			if ( isCanceled() )
				return output;
			cancelable = projectionOp;

			// Compute projection.
			projectionOp.compute( channel, outputSlice );

			// Receive height-map.
			final Img< UnsignedShortType > heightMap = projectionOp.getHeightMap();
			copyOnReferenceSurfaceOutput( heightMap, ( ImgPlus< UnsignedShortType > ) referenceSurfaces.getImgPlus(), t );

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

					final ImgPlus< UnsignedShortType > imgPlusSingleTP = new ImgPlus<>( heightMap, refTpName, axesRefSurface );
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
				final CalibratedAxis[] outputTpAxes = new CalibratedAxis[ outputSlice.numDimensions() ];
				// Remove T
				int id3 = 0;
				for ( int d = 0; d < outImgPlus.numDimensions(); d++ )
				{
					if ( d == outImgPlus.dimensionIndex( Axes.TIME ) )
						continue;
					outputTpAxes[ id3++ ] = outImgPlus.axis( d );
				}

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

			status.showProgress( ( int ) t, ( int ) nFrames );
		}

		return output;
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
