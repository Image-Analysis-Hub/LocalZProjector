package fr.pasteur.iah.localzprojector;

import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceOp;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters.Method;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.ops.special.function.Functions;
import net.imglib2.img.Img;
import net.imglib2.img.VirtualStackAdapter;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class TestReferenceSurfaceBigImage
{

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final String imageFile = "/Users/tinevez/Desktop/A.tif";

		final ImageJ ij = new ImageJ();
		ij.launch( args );

		final long start1 = System.currentTimeMillis();

		final ImagePlus imp = IJ.openVirtual( imageFile );
		imp.show();

		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > img = ( ImgPlus< T > ) VirtualStackAdapter.wrap( imp );
		final long end1 = System.currentTimeMillis();

		System.out.println( String.format( "Opening image time: %.2f s.", ( end1 - start1 ) / 1000. ) );

		final long start2 = System.currentTimeMillis();

		final int channel = 1;
		final ReferenceSurfaceParameters referenceSurfaceParams = ReferenceSurfaceParameters.create()
				.method( Method.MAX_OF_MEAN )
				.zMin( 0 )
				.zMax( 100000 )
				.filterWindowSize( 40 )
				.binning( 4 )
				.gaussianPreFilter( 0.5 )
				.targetChannel( channel )
				.medianPostFilterHalfSize( 100 / 4 )
				.get();

		// Create reference surface op.
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final ReferenceSurfaceOp< T > referenceSurfaceOp = ( ReferenceSurfaceOp ) Functions.unary(
				ij.op(),
				ReferenceSurfaceOp.class,
				Img.class,
				ImgPlus.class,
				referenceSurfaceParams );
		final Img< UnsignedShortType > referenceSurface = referenceSurfaceOp.calculate( img );

		final long end2 = System.currentTimeMillis();

		System.out.println( "Done" );
		System.out.println( String.format( "Processing time: %.2f s.", ( end2 - start2 ) / 1000. ) );

		ij.ui().show( referenceSurface );
	}

}
