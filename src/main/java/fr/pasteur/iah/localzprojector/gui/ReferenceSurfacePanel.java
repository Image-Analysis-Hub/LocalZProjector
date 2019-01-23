package fr.pasteur.iah.localzprojector.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters.Method;

public class ReferenceSurfacePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final static ImageIcon LOAD_ICON = new ImageIcon( ReferenceSurfacePanel.class.getResource( "page_go.png" ) );

	private final static ImageIcon SAVE_ICON = new ImageIcon( ReferenceSurfacePanel.class.getResource( "page_save.png" ) );

	private final SpinnerNumberModel spinnerModelChannel;

	private final JComboBox< Method > comboBoxMethod;

	private final SpinnerNumberModel spinnerModelSize;

	private final SpinnerNumberModel spinnerModelZMin;

	private final SpinnerNumberModel spinnerModelZMax;

	private final JFormattedTextField ftfSigma;

	private final SpinnerNumberModel spinnerModelMedian;

	public ReferenceSurfacePanel( final int nChannels, final int nZSlices )
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[] { 1., 1. };
		setLayout( gridBagLayout );

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		c.insets = new Insets( 5, 5, 5, 5 );
		c.gridx = 0;
		c.gridy = 0;

		final JLabel lblReferenceSurface = new JLabel( "Reference surface." );
		lblReferenceSurface.setHorizontalAlignment( JLabel.CENTER );
		lblReferenceSurface.setFont( lblReferenceSurface.getFont().deriveFont( lblReferenceSurface.getFont().getSize() + 4f ) );
		add( lblReferenceSurface, c );

		c.gridy++;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		add( new JLabel( "Target channel:" ), c );

		c.gridx = 1;
		this.spinnerModelChannel = new SpinnerNumberModel( 0, 0, nChannels - 1, 1 );
		add( new JSpinner( spinnerModelChannel ), c );

		c.gridx = 0;
		c.gridy++;
		add( new JLabel( "Method:" ), c );

		c.gridx = 1;
		comboBoxMethod = new JComboBox<>( Method.values() );
		add( comboBoxMethod, c );

		c.gridx = 0;
		c.gridy++;
		add( new JLabel( "Neighborhood size (pixels):" ), c );

		c.gridx = 1;
		this.spinnerModelSize = new SpinnerNumberModel( 21, 1, 1001, 2 );
		add( new JSpinner( spinnerModelSize ), c );

		c.gridx = 0;
		c.gridy++;
		add( new JLabel( "Z search min:" ), c );

		c.gridx = 1;
		this.spinnerModelZMin = new SpinnerNumberModel( 0, 0, nZSlices - 1, 1 );
		add( new JSpinner( spinnerModelZMin ), c );

		c.gridx = 0;
		c.gridy++;
		add( new JLabel( "Z search max:" ), c );

		c.gridx = 1;
		this.spinnerModelZMax = new SpinnerNumberModel( nZSlices - 1, 0, nZSlices - 1, 1 );
		add( new JSpinner( spinnerModelZMax ), c );

		c.gridx = 0;
		c.gridy++;
		add( new JLabel( "Gaussian pre-filter sigma (pixels):" ), c );

		c.gridx = 1;
		final NumberFormat format = DecimalFormat.getInstance();
		format.setMinimumFractionDigits( 2 );
		format.setMaximumFractionDigits( 2 );
		this.ftfSigma = new JFormattedTextField( format );
		ftfSigma.setHorizontalAlignment( JTextField.TRAILING );
		ftfSigma.setValue( Double.valueOf( 1. ) );
		add( ftfSigma, c );

		c.gridx = 0;
		c.gridy++;
		add( new JLabel( "Median post-filter size (pixels):" ), c );

		c.gridx = 1;
		this.spinnerModelMedian = new SpinnerNumberModel( 41, 1, 1001, 2 );
		add( new JSpinner( spinnerModelMedian ), c );

		c.gridx = 0;
		c.gridy++;
		c.anchor = GridBagConstraints.SOUTH;
		c.gridwidth = 2;
		final JPanel panelButtons = new JPanel();
		add( panelButtons, c );
		panelButtons.setLayout( new BoxLayout( panelButtons, BoxLayout.X_AXIS ) );

		final JButton btnLoadParams = new JButton( "load params", LOAD_ICON );
		btnLoadParams.setFont( btnLoadParams.getFont().deriveFont( btnLoadParams.getFont().getSize() - 2f ) );
		final JButton btnSaveParams = new JButton( "save params", SAVE_ICON );
		btnSaveParams.setFont( btnSaveParams.getFont().deriveFont( btnSaveParams.getFont().getSize() - 2f ) );

		panelButtons.add( btnLoadParams );
		panelButtons.add( Box.createHorizontalGlue() );
		panelButtons.add( btnSaveParams );

		/*
		 * Listeners.
		 */

		btnLoadParams.addActionListener( l -> loadParameters() );
		btnSaveParams.addActionListener( l -> saveParameters() );
	}

	public ReferenceSurfaceParameters getParameters()
	{
		return ReferenceSurfaceParameters.create()
				.targetChannel( ( ( Number ) spinnerModelChannel.getValue() ).intValue() )
				.method( ( Method ) comboBoxMethod.getSelectedItem() )
				.halfWindowSize( ( ( ( Number ) spinnerModelSize.getValue() ).intValue() - 1 ) / 2 )
				.zMin( ( ( Number ) spinnerModelZMin.getValue() ).intValue() )
				.zMax( ( ( Number ) spinnerModelZMax.getValue() ).intValue() )
				.gaussianPreFilter( ((Number)ftfSigma.getValue() ).doubleValue() )
				.medianPostFilterHalfSize( ( ( ( Number ) spinnerModelMedian.getValue() ).intValue() - 1 ) / 2 )
				.get();
	}

	public void setParameters( final ReferenceSurfaceParameters params )
	{
		spinnerModelChannel.setValue( Integer.valueOf( params.targetChannel ) );
		comboBoxMethod.setSelectedItem( params.method );
		spinnerModelSize.setValue( Integer.valueOf( 2 * params.halfWindowSize + 1 ) );
		spinnerModelZMin.setValue( Integer.valueOf( params.zMin ) );
		spinnerModelZMax.setValue( Integer.valueOf( params.zMax ) );
		ftfSigma.setValue( Double.valueOf( params.sigma ) );
		spinnerModelMedian.setValue( Integer.valueOf( 2 * params.medianHalfSize + 1 ) );
	}

	private void loadParameters()
	{
		// TODO Auto-generated method stub
	}

	private void saveParameters()
	{
		// TODO Auto-generated method stub
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		final JFrame frame = new JFrame();
		frame.getContentPane().add( new ReferenceSurfacePanel( 3, 41 ) );
		frame.pack();
		frame.setVisible( true );
	}

}
