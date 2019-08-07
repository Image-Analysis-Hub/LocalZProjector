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

import fr.pasteur.iah.localzprojector.util.AppUtil;
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
import net.imglib2.img.ImgFactory;
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
	private ReferenceSurfaceParameters referenceSurfaceParams;

	@Parameter( type = ItemIO.INPUT )
	private ExtractSurfaceParameters extractSurfaceParams;

	@Parameter( type = ItemIO.INPUT, required = false )
	private boolean showReferenceSurface = false;

	@Parameter( type = ItemIO.INPUT, required = false )
	private boolean showOutputDuringCalculation = false;

	@Parameter( type = ItemIO.INPUT, required = false )
	private boolean saveAtEachTimePoint = false;

	@Parameter( type = ItemIO.INPUT, required = false )
	private String saveFolder = System.getProperty( "user.home" );

	/**
	 * Specifies whether the source image is opened as a virtual stack.
	 * <p>
	 * For large movies it is a good idea to copy a full 3D+C stack on a new
	 * Img. Why? Because when movies are very big they will be opened as a
	 * virtual stack in ImageJ. Virtual stacks are cool, but a disk access
	 * happens every-time we change the Z position. And when we do the extract
	 * of the surface, we iterate along Z for each XY position. This copy here
	 * works like a simple cache for 3D.
	 * <p>
	 * But it takes time, which might slow down process when we have only one
	 * time-point, but a large 3D image that fits into memory. So we let the
	 * user specifies whether the source movie is virtual (do a copy) or not (no
	 * copy needed, the default).
	 */
	@Parameter( type = ItemIO.INPUT, required = false )
	private boolean isVirtual = false;

	@Parameter
	private DisplayService displayService;

	@Parameter
	private StatusService status;

	@Parameter
	private DatasetIOService ioService;

	@Parameter
	private AutoscaleService autoscaleService;

	private String cancelReason;

	private Cancelable cancelable;

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
		 * Create output.
		 */

		final CalibratedAxis[] axes = new CalibratedAxis[ input.numDimensions() - 1 ];
		int id = 0;
		final long[] dims = new long[ input.numDimensions() - 1 ];
		for ( int d = 0; d < input.numDimensions(); d++ )
		{
			if ( d == zAxis )
				continue;
			dims[ id ] = input.dimension( d );
			axes[ id++ ] = input.axis( d );
		}
		final Img< T > outImg = ops().create().img( FinalDimensions.wrap( dims ), ( T ) input.firstElement() );
		final ImgPlus< T > outImgPlus = new ImgPlus<>( outImg, "Local Z Projection of " + input.getName(), axes );
		final DefaultDataset output = new DefaultDataset( ops().context(), outImgPlus );

		/*
		 * Show reference surfaces.
		 */

		final DefaultDataset referenceSurfaces;
		if ( showReferenceSurface )
		{
			// We want single channel.
			final int cAxis = output.dimensionIndex( Axes.CHANNEL );
			final ImgPlus< UnsignedShortType > imgPlus;
			if ( cAxis < 0 )
			{
				final Img< UnsignedShortType > rsImg = ops().create().img( output, new UnsignedShortType() );
				imgPlus = new ImgPlus<>( rsImg, "Reference planes of " + input.getName(), axes );
			}
			else
			{
				final CalibratedAxis[] axesRefSurface = new CalibratedAxis[ output.numDimensions() - 1 ];
				int id2 = 0;
				final long[] dimsRefSurface = new long[ output.numDimensions() - 1 ];
				for ( int d = 0; d < output.numDimensions(); d++ )
				{
					if ( d == cAxis )
						continue;
					dimsRefSurface[ id2 ] = output.dimension( d );
					axesRefSurface[ id2++ ] = output.axis( d );
				}

				final Img< UnsignedShortType > rsImg = ops().create().img( FinalDimensions.wrap( dimsRefSurface ), new UnsignedShortType() );
				imgPlus = new ImgPlus<>( rsImg, "Reference planes of " + input.getName(), axesRefSurface );

			}
			referenceSurfaces = new DefaultDataset( ops().context(), imgPlus );
		}
		else
		{
			referenceSurfaces = null;
		}

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

		@SuppressWarnings( "rawtypes" )
		final ReferenceSurfaceOp< T > referenceSurfaceOp = ( ReferenceSurfaceOp ) Functions.unary(
				ops(),
				ReferenceSurfaceOp.class,
				Img.class,
				ImgPlus.class,
				referenceSurfaceParams );

		@SuppressWarnings( "rawtypes" )
		final ExtractSurfaceOp< T > extractSurfaceOp = ( ExtractSurfaceOp ) Computers.binary( ops(),
				ExtractSurfaceOp.class,
				RandomAccessibleInterval.class,
				ImgPlus.class,
				RandomAccessibleInterval.class,
				extractSurfaceParams );

		final long nFrames = input.getFrames();
		for ( long t = 0; t < nFrames; t++ )
		{

			status.showStatus( "Processing time-point " + t );
			final ImgPlus< T > tp = getSourceTimePoint( ( ImgPlus< T > ) input.getImgPlus(), t, ops(), isVirtual );

			/*
			 * Get reference surface.
			 */

			if ( isCanceled() )
				return output;
			cancelable = referenceSurfaceOp;

			final RandomAccessibleInterval< T > channel = getChannel( tp, referenceSurfaceParams.targetChannel );
			final Img< UnsignedShortType > referenceSurface = referenceSurfaceOp.calculate( channel );

			/*
			 * Show reference surface?
			 */

			if ( showOutputDuringCalculation && showReferenceSurface )
			{
				copyOnReferenceSurfaceOutput( referenceSurface, ( ImgPlus< UnsignedShortType > ) referenceSurfaces.getImgPlus(), t );

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
			cancelable = extractSurfaceOp;

			final RandomAccessibleInterval< T > outputSlice = getOutputTimePoint( outImgPlus, t );
			extractSurfaceOp.compute( tp, referenceSurface, outputSlice );

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

					final ImgPlus< UnsignedShortType > imgPlus = new ImgPlus<>( referenceSurface, refTpName, axesRefSurface );
					final Dataset refSurfaceDataset = new DefaultDataset( ioService.context(), imgPlus );

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
				int id2 = 0;
				for ( int d = 0; d < outImgPlus.numDimensions(); d++ )
				{
					if ( d == outImgPlus.dimensionIndex( Axes.TIME ) )
						continue;
					outputTpAxes[ id2++ ] = outImgPlus.axis( d );
				}

				final ImgPlus< T > imgPlus = new ImgPlus< T >( wrapToImgPlus( outputSlice ), outputTpName, outputTpAxes );
				final Dataset outputTpDataset = new DefaultDataset( ioService.context(), imgPlus );

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

	private RandomAccessibleInterval< T > getChannel( final ImgPlus< T > img, final long c )
	{
		final int channelAxis = img.dimensionIndex( Axes.CHANNEL );
		final RandomAccessibleInterval< T > rai;
		if ( channelAxis >= 0 )
			rai = Views.hyperSlice( img, channelAxis, c );
		else
			rai = img;
		return rai;
	}

	private RandomAccessibleInterval< T > getOutputTimePoint( final ImgPlus< T > output, final long t )
	{
		final int timeAxis = output.dimensionIndex( Axes.TIME );
		if ( timeAxis < 0 )
			return output;

		return Views.hyperSlice( output, timeAxis, t );
	}

	public static final < T extends RealType< T > & NativeType< T > > ImgPlus< T > getSourceTimePoint( final ImgPlus< T > img, final long t, final OpEnvironment ops, final boolean isVirtual )
	{
		final int timeAxis = img.dimensionIndex( Axes.TIME );
		if ( timeAxis < 0 )
			return img;

		final IntervalView< T > tp = Views.hyperSlice( img, timeAxis, t );
		final Img< T > copy;
		if ( isVirtual )
		{
			/*
			 * For large images it is a good idea to copy a full 3D+C stack on a
			 * new Img.
			 *
			 * Why? Because when images are very big they will be opened as a
			 * virtual stack in ImageJ. Virtual stacks are cool, but a disk
			 * access happens every-time we change the Z position. And when we
			 * do the extract of the surface, we iterate along Z for each XY
			 * position. This copy here works like a simple cache for 3D.
			 */
			copy = ops.create().img( tp, img.firstElement() );
			ops.copy().rai( copy, tp );
		}
		else
		{
			/*
			 * The line below is a method that does not do any duplication.
			 */
			copy = ImgView.wrap( tp, Util.getArrayOrCellImgFactory( tp, img.firstElement() ) );
		}

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

	private ImgPlus< T > wrapToImgPlus(
			final RandomAccessibleInterval< T > rai )
	{
		if ( rai instanceof ImgPlus )
			return ( ImgPlus< T > ) rai;
		return new ImgPlus<>( wrapToImg( rai ) );
	}

	private Img< T > wrapToImg(
			final RandomAccessibleInterval< T > rai )
	{
		if ( rai instanceof Img )
			return ( Img< T > ) rai;
		return ImgView.wrap( rai, imgFactory( rai ) );
	}

	private ImgFactory< T > imgFactory(
			final RandomAccessibleInterval< T > rai )
	{
		final T type = Util.getTypeFromInterval( rai );
		return Util.getSuitableImgFactory( rai, type );
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
		return AppUtil.getVersion();
	}
}
