package fr.pasteur.iah.localzprojector;

import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters.ProjectionMethod;
import fr.pasteur.iah.localzprojector.process.LocalZProjectionOp;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters.Method;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.special.function.Functions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class TestBigImage
{

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final String imageFile = "samples/A.tif";

		final ImageJ ij = new ImageJ();
		ij.launch( args );

		final ImagePlus imp = IJ.openVirtual( imageFile );
		imp.show();

		final int channel = 0;
		final int binning = 16;
		final ReferenceSurfaceParameters referenceSurfaceParams = ReferenceSurfaceParameters.create()
				.method( Method.MAX_OF_MEAN )
				.zMin( 0 )
				.zMax( 100000 )
				.filterWindowSize( 40 )
				.binning( binning )
				.gaussianPreFilter( 0. )
				.targetChannel( channel )
				.medianPostFilterHalfSize( 100 / binning )
				.get();
		final ExtractSurfaceParameters extractSurfaceParameters = ExtractSurfaceParameters.create()
				.zOffset( 0, 0 )
				.deltaZ( 0, 1 )
				.projectionMethod( 0, ProjectionMethod.MIP )
				.get();
		final boolean showReferencePlane = true;
		final boolean showOutputDuringCalculation = true;

		final Dataset dataset = ij.imageDisplay().getActiveDataset();

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
