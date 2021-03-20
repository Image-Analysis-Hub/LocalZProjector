package fr.pasteur.iah.localzprojector.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.scijava.prefs.PrefService;

import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters.Builder;
import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters.ProjectionMethod;

public class ExtractSurfaceChannelPanel extends JPanel
{

	private static final long serialVersionUID = 1L;
	
	// For parameter persistence.
	private final static String OFFSETS_PREF_NAME = "offset_ch";
	private final static String DELTA_Z_PREF_NAME = "deltaZ_ch";
	private final static String METHOD_PREF_NAME = "projectionMethod_ch";

	private final JComboBox< ProjectionMethod > comboBoxMethod;

	private final SpinnerNumberModel spinnerModelOffset;

	private final SpinnerNumberModel spinnerModelDeltaZ;

	private final int channel;

	public ExtractSurfaceChannelPanel( final int channel, int nZSlices, final PrefService prefs )
	{
		nZSlices = Math.abs( nZSlices );

		/*
		 * Default and persistence.
		 */

		int offset = prefs.getInt( ExtractSurfaceParameters.class, OFFSETS_PREF_NAME + channel, 0 );
		offset = Math.min( nZSlices, offset );
		offset = Math.max( -nZSlices, offset );

		int deltaZ = prefs.getInt( ExtractSurfaceParameters.class, DELTA_Z_PREF_NAME + channel, 0 );
		deltaZ = Math.min( nZSlices, deltaZ );
		deltaZ = Math.max( -nZSlices, deltaZ );

		final ProjectionMethod method = ProjectionMethod.values()[ prefs.getInt( ExtractSurfaceParameters.class, METHOD_PREF_NAME + channel, 0 ) ];

		/*
		 * UI.
		 */

		this.channel = channel;

		final Font lblFont = getFont().deriveFont( getFont().getSize2D() - 2f );
		setBorder( new TitledBorder( new LineBorder( new Color( 192, 192, 192 ), 1, true ), "Ch" + channel, TitledBorder.LEADING, TitledBorder.TOP, lblFont, null ) );
		final GridBagLayout gridBagLayout = new GridBagLayout();
		setLayout( gridBagLayout );

		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets( 2, 2, 2, 2 );
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add( new JLabel( "Method:" ), c );

		c.gridx = 1;
		this.comboBoxMethod = new JComboBox<>( ProjectionMethod.values() );
		comboBoxMethod.setSelectedItem( method );
		add( comboBoxMethod, c );

		c.gridx = 0;
		c.gridy++;
		add( new JLabel( "Offset:" ), c );

		c.gridx = 1;
		this.spinnerModelOffset = new SpinnerNumberModel( offset, -nZSlices, nZSlices, 1 );
		add( new JSpinner( spinnerModelOffset ), c );

		c.gridx = 0;
		c.gridy++;
		add( new JLabel( "DeltaZ:" ), c );

		c.gridx = 1;
		this.spinnerModelDeltaZ = new SpinnerNumberModel( deltaZ, 0, nZSlices, 1 );
		add( new JSpinner( spinnerModelDeltaZ ), c );

		/*
		 * Listeners.
		 */

		spinnerModelOffset.addChangeListener( e -> prefs.put( ExtractSurfaceParameters.class,
				OFFSETS_PREF_NAME + channel, ( ( Number ) spinnerModelOffset.getValue() ).intValue() ) );
		spinnerModelDeltaZ.addChangeListener( e -> prefs.put( ExtractSurfaceParameters.class,
				DELTA_Z_PREF_NAME + channel, ( ( Number ) spinnerModelDeltaZ.getValue() ).intValue() ) );
		comboBoxMethod.addItemListener( e -> {
			if ( e.getStateChange() == ItemEvent.SELECTED )
				prefs.put( ExtractSurfaceParameters.class,
						METHOD_PREF_NAME + channel, comboBoxMethod.getSelectedIndex() );
		} );
	}

	public Builder update( final Builder builder )
	{
		builder
				.projectionMethod( channel, ( ProjectionMethod ) comboBoxMethod.getSelectedItem() )
				.zOffset( channel, ( ( Number ) spinnerModelOffset.getValue() ).intValue() )
				.deltaZ( channel, ( ( Number ) spinnerModelDeltaZ.getValue() ).intValue() );
		return builder;
	}

	public void setParameters( final ExtractSurfaceParameters params )
	{
		comboBoxMethod.setSelectedItem( params.projectionMethod( channel ) );
		spinnerModelOffset.setValue( Integer.valueOf( params.offset( channel ) ) );
		spinnerModelDeltaZ.setValue( Integer.valueOf( params.deltaZ( channel ) ) );
	}
}
