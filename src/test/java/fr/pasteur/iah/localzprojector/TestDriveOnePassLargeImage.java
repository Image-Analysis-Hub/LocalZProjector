package fr.pasteur.iah.localzprojector;

import java.io.IOException;
import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters.Method;
import fr.pasteur.iah.localzprojector.process.offline.LocalZProjectionOnePassOp;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.ops.special.function.Functions;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.img.VirtualStackAdapter;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class TestDriveOnePassLargeImage
{
	static
	{
		LegacyInjector.preinit();
	}

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException
	{
		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

//		final String imageFile = "/Volumes/@HAI/TeamProjects/DProj/Data/JGros/2mm-slice-1channel.tif rotated.tif rotated.tif";
		final String imageFile = "/Users/tinevez/Desktop/A.tif";

		final ImageJ ij = new ImageJ();
		ij.launch( args );

		final long start1 = System.currentTimeMillis();

		final ImagePlus imp = IJ.openVirtual( imageFile );
		imp.show();

		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > img = ( ImgPlus< T > ) VirtualStackAdapter.wrap( imp );

		final DefaultDataset dataset = new DefaultDataset( ij.context(), img );

		final long end1 = System.currentTimeMillis();
		
		System.out.println( String.format( "Opening image time: %.2f s.", ( end1 - start1 ) / 1000. ) );

		final long start2 = System.currentTimeMillis();

		final boolean showReferencePlane = true;
		final boolean showOutputDuringCalculation = true;
		final int channel = 1;
		final ReferenceSurfaceParameters referenceSurfaceParameters = ReferenceSurfaceParameters.create()
				.method( Method.MAX_OF_MEAN )
				.zMin( 0 )
				.zMax( 100000 )
				.filterWindowSize( 40 )
				.binning( 4 )
				.gaussianPreFilter( 0.5 )
				.targetChannel( channel )
				.medianPostFilterHalfSize( 100 / 4 )
				.get();
		
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

		final long end2 = System.currentTimeMillis();

		System.out.println( "Done" );
		System.out.println( String.format( "Processing time: %.2f s.", ( end2 - start2 ) / 1000. ) );

	}
}
