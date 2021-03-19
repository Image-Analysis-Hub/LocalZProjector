package fr.pasteur.iah.localzprojector.gui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

import org.scijava.prefs.DefaultPrefService;
import org.scijava.prefs.PrefService;

import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters.Method;
import fr.pasteur.iah.localzprojector.util.GuiUtil;

public class ReferenceSurfacePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final static ImageIcon LOAD_ICON = new ImageIcon( ReferenceSurfacePanel.class.getResource( "page_go.png" ) );

	private final static ImageIcon SAVE_ICON = new ImageIcon( ReferenceSurfacePanel.class.getResource( "page_save.png" ) );

	// For parameter persistence.
	private final static String TARGET_CHANNEL_PREF_NAME = "targetChannel";
	private final static String METHOD_PREF_NAME = "method";
	private final static String FILTER_WINDOW_SIZE_PREF_NAME = "filterWindowSize";
	private final static String SIGMA_PREF_NAME = "sigma";
	private final static String MEDIAN_SIZE_PREF_NAME = "medianSize";
	private final static String BINNING_PREF_NAME = "binning";

	private final static String suffix = ".referencesurface.localzprojector";

	private final static FileFilter filter = new FileFilter()
	{

		@Override
		public boolean accept( final File f )
		{
			return f.getAbsolutePath().toLowerCase().endsWith( suffix.toLowerCase() );
		}

		@Override
		public String getDescription()
		{
			return "Height-Map parameters";
		}

	};

	private final SpinnerNumberModel spinnerModelChannel;

	private final JComboBox< Method > comboBoxMethod;

	private final SpinnerNumberModel spinnerModelSize;

	private final SpinnerNumberModel spinnerModelZMin;

	private final SpinnerNumberModel spinnerModelZMax;

	private final JFormattedTextField ftfSigma;

	private final SpinnerNumberModel spinnerModelMedian;

	private SpinnerNumberModel spinnerModelBinning;

	public ReferenceSurfacePanel( final int nChannels, final int nZSlices, final PrefService prefs )
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[] { 1., 1. };
		setLayout( gridBagLayout );

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		c.insets = new Insets( 2, 5, 2, 5 );
		c.gridx = 0;
		c.gridy = 0;

		/*
		 * Default and persistence.
		 */

		final int targetChannel = Math.max( 0, Math.min( nChannels - 1, prefs.getInt( ReferenceSurfaceParameters.class, TARGET_CHANNEL_PREF_NAME, 0 ) ) );
		final Method method = ReferenceSurfaceParameters.Method.values()[ prefs.getInt( ReferenceSurfaceParameters.class, METHOD_PREF_NAME, 0 ) ];
		final int filterWindowSize = Math.max( 1, Math.min( 100, prefs.getInt( ReferenceSurfaceParameters.class, FILTER_WINDOW_SIZE_PREF_NAME, 21 ) ) );
		final double sigma = Math.max( 0., prefs.getDouble( ReferenceSurfaceParameters.class, SIGMA_PREF_NAME, 0. ) );
		final int medianSize = Math.max( 1, Math.min( 1000, prefs.getInt( ReferenceSurfaceParameters.class, MEDIAN_SIZE_PREF_NAME, 41 ) ) );
		final int binning = Math.max( 1, Math.min( 100, prefs.getInt( ReferenceSurfaceParameters.class, BINNING_PREF_NAME, 1 ) ) );

		final JLabel lblReferenceSurface = new JLabel( "Height-Map." );
		lblReferenceSurface.setHorizontalAlignment( JLabel.CENTER );
		lblReferenceSurface.setFont( lblReferenceSurface.getFont().deriveFont( lblReferenceSurface.getFont().getSize() + 2f ) );
		add( lblReferenceSurface, c );

		c.gridy++;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		add( new JLabel( "Target channel:" ), c );

		c.gridx = 1;
		this.spinnerModelChannel = new SpinnerNumberModel( targetChannel, 0, nChannels - 1, 1 );
		add( new JSpinner( spinnerModelChannel ), c );

		c.gridx = 0;
		c.gridy++;
		add( new JLabel( "Binning:" ), c );

		c.gridx = 1;
		this.spinnerModelBinning = new SpinnerNumberModel( binning, 1, 100, 1 );
		add( new JSpinner( spinnerModelBinning ), c );

		final JPanel methodPanel = new JPanel();
		final BoxLayout boxLayout = new BoxLayout( methodPanel, BoxLayout.LINE_AXIS );
		methodPanel.setLayout( boxLayout );
		methodPanel.add( new JLabel( "Method:" ) );
		methodPanel.add( Box.createHorizontalGlue() );
		comboBoxMethod = new JComboBox<>( Method.values() );
		comboBoxMethod.setSelectedItem( method );
		methodPanel.add( comboBoxMethod );
		c.gridx = 0;
		c.gridwidth = 2;
		c.gridy++;
		add( methodPanel, c );

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		add( new JLabel( "Neighborhood size (pixels):" ), c );

		c.gridx = 1;
		this.spinnerModelSize = new SpinnerNumberModel( filterWindowSize, 1, 1001, 2 );
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
		ftfSigma.setValue( Double.valueOf( sigma ) );
		add( ftfSigma, c );

		c.gridx = 0;
		c.gridy++;
		add( new JLabel( "Median post-filter size (pixels):" ), c );

		c.gridx = 1;
		this.spinnerModelMedian = new SpinnerNumberModel( medianSize, 1, 1001, 2 );
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

		// Change font size - more compact.
		final Font lblFont = getFont().deriveFont( getFont().getSize2D() - 2f );
		GuiUtil.changeFont( this, lblFont, JLabel.class, lblReferenceSurface );
		GuiUtil.changeFont( this, lblFont, JComboBox.class );
		GuiUtil.changeFont( this, lblFont, JSpinner.class );
		GuiUtil.changeFont( this, lblFont, JTextField.class );

		/*
		 * Listeners.
		 */

		btnLoadParams.addActionListener( l -> loadParameters() );
		btnSaveParams.addActionListener( l -> saveParameters() );

		spinnerModelChannel.addChangeListener( e -> prefs.put( ReferenceSurfaceParameters.class,
				TARGET_CHANNEL_PREF_NAME, ( ( Number ) spinnerModelChannel.getValue() ).intValue() ) );
		spinnerModelBinning.addChangeListener( e -> prefs.put( ReferenceSurfaceParameters.class,
				BINNING_PREF_NAME, ( ( Number ) spinnerModelBinning.getValue() ).intValue() ) );
		comboBoxMethod.addItemListener( e -> {
			if ( e.getStateChange() == ItemEvent.SELECTED )
				prefs.put( ReferenceSurfaceParameters.class,
						METHOD_PREF_NAME, comboBoxMethod.getSelectedIndex() );
		} );
		spinnerModelSize.addChangeListener( e -> prefs.put( ReferenceSurfaceParameters.class,
				FILTER_WINDOW_SIZE_PREF_NAME, ( ( Number ) spinnerModelSize.getValue() ).intValue() ) );
		ftfSigma.addPropertyChangeListener( "value", e -> prefs.put( ReferenceSurfaceParameters.class,
				SIGMA_PREF_NAME, ( ( Number ) ftfSigma.getValue() ).doubleValue() ) );
		spinnerModelMedian.addChangeListener( e -> prefs.put( ReferenceSurfaceParameters.class,
				MEDIAN_SIZE_PREF_NAME, ( ( ( Number ) spinnerModelMedian.getValue() ).intValue() - 1 ) / 2 ) );
	}

	public ReferenceSurfaceParameters getParameters()
	{
		return ReferenceSurfaceParameters.create()
				.targetChannel( ( ( Number ) spinnerModelChannel.getValue() ).intValue() )
				.binning( ( ( Number ) spinnerModelBinning.getValue() ).intValue() )
				.method( ( Method ) comboBoxMethod.getSelectedItem() )
				.filterWindowSize( ( ( ( Number ) spinnerModelSize.getValue() ).intValue() ) )
				.zMin( ( ( Number ) spinnerModelZMin.getValue() ).intValue() )
				.zMax( ( ( Number ) spinnerModelZMax.getValue() ).intValue() )
				.gaussianPreFilter( ( ( Number ) ftfSigma.getValue() ).doubleValue() )
				.medianPostFilterHalfSize( ( ( ( Number ) spinnerModelMedian.getValue() ).intValue() - 1 ) / 2 )
				.get();
	}

	public void setParameters( final ReferenceSurfaceParameters params )
	{
		spinnerModelChannel.setValue( Integer.valueOf( params.targetChannel ) );
		spinnerModelBinning.setValue( Integer.valueOf( params.binning ) );
		comboBoxMethod.setSelectedItem( params.method );
		spinnerModelSize.setValue( Integer.valueOf( params.filterWindowSize ) );
		spinnerModelZMin.setValue( Integer.valueOf( params.zMin ) );
		spinnerModelZMax.setValue( Integer.valueOf( params.zMax ) );
		ftfSigma.setValue( Double.valueOf( params.sigma ) );
		spinnerModelMedian.setValue( Integer.valueOf( 2 * params.medianHalfSize + 1 ) );
	}

	private void saveParameters()
	{
		GuiPanel.fileChooser.setDialogTitle( "Save Reference-Surface parameters" );
		GuiPanel.fileChooser.setFileFilter( filter );
		final int answer = GuiPanel.fileChooser.showSaveDialog( getParent() );
		if ( JFileChooser.APPROVE_OPTION != answer )
			return;

		File file = GuiPanel.fileChooser.getSelectedFile();
		if ( !file.getAbsolutePath().endsWith( suffix ) )
			file = new File( file.getAbsolutePath() + suffix );

		try
		{
			ReferenceSurfaceParameters.serialize( getParameters(), file );
		}
		catch ( final IOException e )
		{
			System.err.println( "Cannot write to " + file + ":\n" + e.getMessage() );
			e.printStackTrace();
		}
	}

	private void loadParameters()
	{
		GuiPanel.fileChooser.setDialogTitle( "Load Reference-Surface parameters" );
		GuiPanel.fileChooser.setFileFilter( filter );
		final int answer = GuiPanel.fileChooser.showOpenDialog( getParent() );
		if ( JFileChooser.APPROVE_OPTION != answer )
			return;

		final File file = GuiPanel.fileChooser.getSelectedFile();
		if ( !file.canRead() )
		{
			System.err.println( "Cannot read from " + file );
			return;
		}

		try
		{
			final ReferenceSurfaceParameters params = ReferenceSurfaceParameters.deserialize( file );
			setParameters( params );
		}
		catch ( ClassNotFoundException | IOException e )
		{
			System.err.println( "Cannot read from " + file + ":\n" + e.getMessage() );
		}
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		final JFrame frame = new JFrame();
		frame.getContentPane().add( new ReferenceSurfacePanel( 3, 41, new DefaultPrefService() ) );
		frame.pack();
		frame.setVisible( true );
	}

}