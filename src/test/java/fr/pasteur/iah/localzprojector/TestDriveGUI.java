package fr.pasteur.iah.localzprojector;

import java.io.IOException;
import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fr.pasteur.iah.localzprojector.gui.GuiController;
import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters.ProjectionMethod;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters.Method;
import net.imagej.ImageJ;

public class TestDriveGUI
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException
	{
		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final ImageJ ij = new ImageJ();
		ij.launch( args );

		final String imageFile = "samples/Composite.tif";
		final Object open = ij.io().open( imageFile );
		ij.ui().show( open );

		final GuiController controller = new GuiController( ij.context() );

		final int channel = 1;
		final ReferenceSurfaceParameters referenceSurfaceParameters = ReferenceSurfaceParameters.create()
				.method( Method.MAX_OF_STD )
				.zMin( 0 )
				.zMax( 100000 )
				.halfWindowSize( 10 )
				.gaussianPreFilter( 1. )
				.medianPostFilterHalfSize( 20 )
				.targetChannel( channel )
				.get();
		final ExtractSurfaceParameters extractSurfaceParameters = ExtractSurfaceParameters.create()
				.zOffset( 0, +8 )
				.zOffset( 1, 0 )
				.deltaZ( 0, 3 )
				.deltaZ( 1, 3 )
				.projectionMethod( 0, ProjectionMethod.MIP )
				.projectionMethod( 1, ProjectionMethod.MIP )
				.get();

		controller.setReferenceSurfaceParameters( referenceSurfaceParameters );
		controller.setExtractSurfaceParameters( extractSurfaceParameters );
	}
}
