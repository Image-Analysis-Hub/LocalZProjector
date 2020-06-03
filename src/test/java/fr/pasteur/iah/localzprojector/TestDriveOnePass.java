package fr.pasteur.iah.localzprojector;

import java.io.IOException;
import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters.Method;
import fr.pasteur.iah.localzprojector.process.offline.LocalZProjectionOnePassOp;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.special.function.Functions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class TestDriveOnePass
{

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException
	{
		final String imageFile = "samples/Composite.tif";
		final boolean showReferencePlane = true;
		final boolean showOutputDuringCalculation = true;
		final int channel = 1;
		final ReferenceSurfaceParameters referenceSurfaceParameters = ReferenceSurfaceParameters.create()
				.method( Method.SPARSE_MAX_OF_STD )
				.zMin( 0 )
				.zMax( 100000 )
				.filterWindowSize( 40 )
				.binning( 4 )
				.gaussianPreFilter( 0. )
				.targetChannel( channel )
				.get();

		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final ImageJ ij = new ImageJ();
		ij.launch( args );

		final Object open = ij.io().open( imageFile );
		ij.ui().show( open );

		final Dataset dataset = ij.dataset().getDatasets().get( 0 );

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final LocalZProjectionOnePassOp< T > localZProjectionOnePassOp = ( LocalZProjectionOnePassOp ) Functions.unary(
				ij.op(),
				LocalZProjectionOnePassOp.class,
				Dataset.class,
				Dataset.class,
				referenceSurfaceParameters,
				showReferencePlane,
				showOutputDuringCalculation );
		localZProjectionOnePassOp.calculate( dataset );

		System.out.println( "Done" );
	}
}
