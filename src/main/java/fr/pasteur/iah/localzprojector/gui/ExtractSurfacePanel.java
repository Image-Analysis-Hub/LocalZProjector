package fr.pasteur.iah.localzprojector.gui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

import org.scijava.prefs.DefaultPrefService;
import org.scijava.prefs.PrefService;

import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters.Builder;
import fr.pasteur.iah.localzprojector.util.GuiUtil;

public class ExtractSurfacePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final static ImageIcon DEFAULT_PARAM_ICON = new ImageIcon( ReferenceSurfacePanel.class.getResource( "page.png" ) );

	private final static ImageIcon LOAD_ICON = new ImageIcon( ReferenceSurfacePanel.class.getResource( "page_go.png" ) );

	private final static ImageIcon SAVE_ICON = new ImageIcon( ReferenceSurfacePanel.class.getResource( "page_save.png" ) );

	private final List< ExtractSurfaceChannelPanel > channels;

	private final static String suffix = ".localproj.lzp.json";
	
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
			return "Local-Projection parameters";
		}

	};

	public ExtractSurfacePanel( final int nChannels, final int nZSlices, final PrefService prefs )
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		setLayout( gridBagLayout );

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets( 2, 5, 2, 5 );
		c.gridx = 0;
		c.gridy = 0;

		final JLabel lblLocalProjection = new JLabel( "Local-Projection." );
		lblLocalProjection.setHorizontalAlignment( JLabel.CENTER );
		lblLocalProjection.setFont( lblLocalProjection.getFont().deriveFont( lblLocalProjection.getFont().getSize() + 2f ) );
		add( lblLocalProjection, c );

		final JPanel panelChannels = new JPanel();
		final BoxLayout layout = new BoxLayout( panelChannels, BoxLayout.LINE_AXIS );
		panelChannels.setLayout( layout );

		this.channels = new ArrayList<>();
		for ( int channel = 0; channel < nChannels; channel++ )
		{
			panelChannels.add( Box.createHorizontalStrut( 5 ) );
			final ExtractSurfaceChannelPanel ci = new ExtractSurfaceChannelPanel( channel, nZSlices, prefs );
			channels.add( ci );
			panelChannels.add( ci );
			panelChannels.add( Box.createHorizontalStrut( 1 ) );
		}

		final JScrollPane scrollPane = new JScrollPane( panelChannels, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
		scrollPane.setBorder( null );
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.;
		c.weighty = 1.;
		add( scrollPane, c );

		final JPanel panelButtons = new JPanel();
		c.weightx = 0.;
		c.weighty = 0.;
		c.gridy++;
		c.gridy++;
		c.anchor = GridBagConstraints.SOUTH;
		add( panelButtons, c );
		panelButtons.setLayout( new BoxLayout( panelButtons, BoxLayout.X_AXIS ) );

		final JButton btnDefaultParams = new JButton( "default", DEFAULT_PARAM_ICON );
		btnDefaultParams.setFont( btnDefaultParams.getFont().deriveFont( btnDefaultParams.getFont().getSize() - 2f ) );
		final JButton btnLoadParams = new JButton( "load", LOAD_ICON );
		btnLoadParams.setFont( btnLoadParams.getFont().deriveFont( btnLoadParams.getFont().getSize() - 2f ) );
		final JButton btnSaveParams = new JButton( "save", SAVE_ICON );
		btnSaveParams.setFont( btnSaveParams.getFont().deriveFont( btnSaveParams.getFont().getSize() - 2f ) );

		panelButtons.add( btnDefaultParams );
		panelButtons.add( Box.createHorizontalGlue() );
		panelButtons.add( btnLoadParams );
		panelButtons.add( Box.createHorizontalGlue() );
		panelButtons.add( btnSaveParams );

		// Change font size - more compact.
		final Font lblFont = getFont().deriveFont( getFont().getSize2D() - 2f );
		GuiUtil.changeFont( this, lblFont, JLabel.class, lblLocalProjection );
		GuiUtil.changeFont( this, lblFont, JComboBox.class );
		GuiUtil.changeFont( this, lblFont, JSpinner.class );
		GuiUtil.changeFont( this, lblFont, JTextField.class );

		/*
		 * Listeners.
		 */
		
		btnDefaultParams.addActionListener( l -> defaultParameters() );
		btnLoadParams.addActionListener( l -> loadParameters() );
		btnSaveParams.addActionListener( l -> saveParameters() );
	}

	private void defaultParameters()
	{
		setParameters( ExtractSurfaceParameters.df );
	}

	private void saveParameters()
	{
		GuiPanel.fileChooser.setDialogTitle( "Save Extract-Surface parameters" );
		GuiPanel.fileChooser.setFileFilter( filter );
		final int answer = GuiPanel.fileChooser.showSaveDialog( getParent() );
		if ( JFileChooser.APPROVE_OPTION != answer )
			return;

		File file = GuiPanel.fileChooser.getSelectedFile();
		if ( !file.getAbsolutePath().endsWith( suffix ) )
			file = new File( file.getAbsolutePath() + suffix );

		try
		{
			ExtractSurfaceParameters.serialize( getParameters(), file );
		}
		catch ( final IOException e )
		{
			System.err.println( "Cannot write to " + file + ":\n" + e.getMessage() );
			e.printStackTrace();
		}
	}

	private void loadParameters()
	{
		GuiPanel.fileChooser.setDialogTitle( "Load Extract-Surface parameters" );
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
			final ExtractSurfaceParameters params = ExtractSurfaceParameters.deserialize( file );
			setParameters( params );
		}
		catch ( ClassNotFoundException | IOException e )
		{
			System.err.println( "Cannot read from " + file + ":\n" + e.getMessage() );
		}
	}

	public ExtractSurfaceParameters getParameters()
	{
		final Builder builder = ExtractSurfaceParameters.create();
		for ( final ExtractSurfaceChannelPanel channel : channels )
			channel.update( builder );

		return builder.get();
	}

	public void setParameters( final ExtractSurfaceParameters params )
	{
		for ( final ExtractSurfaceChannelPanel channel : channels )
			channel.setParameters( params );
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		final JFrame frame = new JFrame();
		frame.getContentPane().add( new ExtractSurfacePanel( 3, 41, new DefaultPrefService() ) );
		frame.pack();
		frame.setVisible( true );
	}
}
