package fr.pasteur.iah.localzprojector;

import java.io.IOException;
import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fr.pasteur.iah.localzprojector.process.LocalProjectionOp;
import fr.pasteur.iah.localzprojector.process.LocalProjectionParameters;
import fr.pasteur.iah.localzprojector.process.LocalProjectionParameters.ProjectionMethod;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceOp;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters.Method;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.ops.special.function.Functions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;

public class TestDrive
{

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException
	{
		final String imageFile = "samples/Composite.tif";
		final int channel = 1;
		final ReferenceSurfaceParameters params = ReferenceSurfaceParameters.create()
				.method( Method.MAX_OF_STD )
				.zMin( 0 )
				.zMax( 100000 )
				.halfWindowSize( 10 )
				.gaussianPreFilter( 1. )
				.medianPostFilterHalfSize( 20 )
				.get();

		final LocalProjectionParameters params2 = LocalProjectionParameters.create()
				.zOffset( 0, +8 )
				.zOffset( 1, 0 )
				.deltaZ( 0, 3 )
				.deltaZ( 1, 3 )
				.projectionMethod( 0, ProjectionMethod.MIP )
				.projectionMethod( 1, ProjectionMethod.MIP )
				.get();

		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final ImageJ ij = new ImageJ();
		ij.launch( args );

		final Object open = ij.io().open( imageFile );
		ij.ui().show( open );

		final Dataset dataset = ij.dataset().getDatasets().get( 0 );
		@SuppressWarnings( "unchecked" )
		final Img< T > img = ( Img< T > ) dataset.getImgPlus().getImg();

		// Extract reference.

		final int channelAxis = dataset.dimensionIndex( Axes.CHANNEL );
		final RandomAccessibleInterval< T > source;
		if ( channelAxis >= 0 )
			source = Views.hyperSlice( img, channelAxis, channel );
		else
			source = img;

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final ReferenceSurfaceOp< T > op = ( ReferenceSurfaceOp ) Functions.unary( ij.op(), ReferenceSurfaceOp.class, Img.class, source, params );
		final Img< IntType > referenceSurface = op.calculate( source );

		ij.ui().show( referenceSurface );

		// Extract surface.

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		final LocalProjectionOp< T > op2 = ( LocalProjectionOp ) Functions.binary( ij.op(), LocalProjectionOp.class, Dataset.class, dataset, referenceSurface, params2 );
		final Dataset surface = op2.calculate( dataset, referenceSurface );

		ij.ui().show( surface );
	}
}
