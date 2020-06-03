package fr.pasteur.iah.localzprojector;

import java.io.IOException;

import org.scijava.io.location.FileLocation;

import net.imagej.Dataset;
import net.imagej.ImageJ;

public class TestLoadBigImage
{

	public static void main( final String[] args ) throws IOException
	{
		final ImageJ ij = new ImageJ();
		ij.launch( args );

		final Dataset dataset = ij.scifio().datasetIO().open( new FileLocation( "/Users/tinevez/Desktop/A.tif" ) );
		ij.ui().show( dataset );
	}

}
