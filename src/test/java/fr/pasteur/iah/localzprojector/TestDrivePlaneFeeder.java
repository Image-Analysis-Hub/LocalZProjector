package fr.pasteur.iah.localzprojector;

import java.io.IOException;
import java.util.List;

import javax.swing.UnsupportedLookAndFeelException;

import org.scijava.io.location.FileLocation;

import io.scif.FormatException;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;
import io.scif.config.SCIFIOConfig;
import io.scif.config.SCIFIOConfig.ImgMode;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;
import net.imagej.ImageJ;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class TestDrivePlaneFeeder
{
	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException, FormatException
	{
		final ImageJ ij = new ImageJ();
		ij.launch( args );
		
		final FileLocation imageFile = new FileLocation( "/Users/tinevez/Desktop/A.ics" );
//		final String imageFile = "/Users/tinevez/Desktop/A.ics";

		final SCIFIOConfig config = new SCIFIOConfig();
		config.imgOpenerSetImgModes( ImgMode.PLANAR );

		final SCIFIO scifio = new SCIFIO();
		final Reader reader = scifio.initializer().initializeReader( imageFile );

		for ( int i = 0; i < 30; i++ )
		{
			final Plane plane = reader.openPlane( 0, i );
			System.out.println( "plane " + i + ": " + plane + ", length: " +
					plane.getBytes().length );
			// ALL GOOD
		}

		final List< SCIFIOImgPlus< ? > > imgs = new ImgOpener( ij.context() ).openImgs( reader, config );
		@SuppressWarnings( "unchecked" )
		final SCIFIOImgPlus< T > img = ( SCIFIOImgPlus< T > ) imgs.get( 0 );

		final int numDimensions = img.numDimensions();
		final CalibratedAxis[] axes = new CalibratedAxis[ numDimensions ];
		img.axes( axes );
		for ( int d = 0; d < axes.length; d++ )
			System.out.println( axes[ d ].type() + " -> " + img.dimension( d ) );
		
		ij.ui().show( img ); // BREAKS HERE
	}

}
