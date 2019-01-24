package fr.pasteur.iah.localzprojector.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.imagej.Dataset;
import net.imagej.ImageJ;

public class GuiPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private ReferenceSurfacePanel referenceSurfacePanel;

	private LocalProjectionPanel localProjectionPanel;

	private final TargetImagePanel targetImagePanel;

	public GuiPanel( final Supplier< Dataset > datasetSupplier )
	{
		setPreferredSize( new Dimension( 400, 1000 ) );

		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[] { 1. };
		gridBagLayout.rowWeights = new double[] { 0., 0., 0., 0., 0., 0., 0., 1. };
		gridBagLayout.rowHeights = new int[] { 0, 20, 0, 20, 0, 20 };
		setLayout( gridBagLayout );

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets( 5, 10, 5, 10 );
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;

		/*
		 * Title. y = 0.
		 */

		final JLabel lblTitle = new JLabel( "<html>\n<center>\n<big>Local Z Projector.</big>\n<p>\nv0.0.1\n</center>\n</html>" );
		lblTitle.setHorizontalAlignment( SwingConstants.CENTER );
		add( lblTitle, c );

		/*
		 * Separator. y = 1.
		 */

		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		add( new JSeparator(), c );

		/*
		 * Target image panel. y = 2.
		 */

		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		targetImagePanel = new TargetImagePanel();
		add( targetImagePanel, c );
		targetImagePanel.btnRefresh.addActionListener( l -> refreshDataset( datasetSupplier.get() ) );

		/*
		 * Separator. y = 3.
		 */

		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		add( new JSeparator(), c );

		/*
		 * Button panel. y = 7.
		 */

		c.gridy += 4;
		c.fill = GridBagConstraints.BOTH;
		add( new JPanel(), c );

		/*
		 * Check
		 */

		refreshDataset( datasetSupplier.get() );
	}

	private void refreshDataset( final Dataset dataset )
	{
		if ( null != referenceSurfacePanel )
			remove( referenceSurfacePanel );
		if ( null != localProjectionPanel )
			remove( localProjectionPanel );

		targetImagePanel.refresh( dataset );
		if ( null == dataset )
			return;

		final int nChannels = ( int ) dataset.getChannels();
		final int nZSlices = ( int ) dataset.getDepth();

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets( 5, 10, 5, 10 );
		c.gridwidth = 1;
		c.gridx = 0;

		/*
		 * Reference plane panel. y = 4.
		 */

		referenceSurfacePanel = new ReferenceSurfacePanel( nChannels, nZSlices );
		c.gridy = 4;
		add( referenceSurfacePanel, c );

		/*
		 * Separator. y = 5.
		 */

		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		add( new JSeparator(), c );

		/*
		 * Local projection panel. y = 6.
		 */

		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		localProjectionPanel = new LocalProjectionPanel( nChannels, nZSlices );
		add( localProjectionPanel, c );

		/*
		 * Pack
		 */

		revalidate();
		repaint();
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final ImageJ ij = new ImageJ();
		ij.launch( args );

		final Supplier< Dataset > datasetSupplier = () -> ij.imageDisplay().getActiveDataset();

		final JFrame frame = new JFrame();
		frame.getContentPane().add( new GuiPanel( datasetSupplier ) );
		frame.pack();
		frame.setVisible( true );
	}

}
