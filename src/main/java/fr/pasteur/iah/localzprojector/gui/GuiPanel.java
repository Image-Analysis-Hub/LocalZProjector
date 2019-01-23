package fr.pasteur.iah.localzprojector.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class GuiPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	public GuiPanel()
	{
		this( 2, 41 );
	}

	public GuiPanel( final int nChannels, final int nZSlices )
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[] { 1., 0., 1. };
		setLayout( gridBagLayout );

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets( 5, 10, 5, 10 );
		c.gridwidth = 3;
		c.gridx = 0;
		c.gridy = 0;

		final JLabel lblTitle = new JLabel( "<html>\n<center>\n<big>Local Z Projector.</big>\n<p>\nv0.0.1\n</center>\n</html>" );
		lblTitle.setHorizontalAlignment( SwingConstants.CENTER );
		add( lblTitle, c );

		final ReferenceSurfacePanel referenceSurfacePanel = new ReferenceSurfacePanel( nChannels, nZSlices );
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.gridy++;
		add( referenceSurfacePanel, c );

		c.gridx = 1;
		add( new JSeparator( SwingConstants.VERTICAL ), c );

		final LocalProjectionPanel localProjectionPanel = new LocalProjectionPanel( nChannels, nZSlices );
		c.gridx = 2;
		add( localProjectionPanel, c );
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		final JFrame frame = new JFrame();
		frame.getContentPane().add( new GuiPanel( 3, 41 ) );
		frame.pack();
		frame.setVisible( true );
	}

}
