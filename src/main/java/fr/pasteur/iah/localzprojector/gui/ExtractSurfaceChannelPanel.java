package fr.pasteur.iah.localzprojector.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters.Builder;
import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters.ProjectionMethod;

public class ExtractSurfaceChannelPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JComboBox< ProjectionMethod > comboBoxMethod;

	private final SpinnerNumberModel spinnerModelOffset;

	private final SpinnerNumberModel spinnerModelDeltaZ;

	private final int channel;

	public ExtractSurfaceChannelPanel( final int channel, final int nZSlices )
	{
		this.channel = channel;
		setBorder( new TitledBorder( new LineBorder( new Color( 192, 192, 192 ), 1, true ), "Ch" + channel, TitledBorder.LEADING, TitledBorder.TOP, null, null ) );
		final GridBagLayout gridBagLayout = new GridBagLayout();
		setLayout( gridBagLayout );

		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets( 5, 5, 5, 5 );
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add( new JLabel( "Method:" ), c );

		c.gridx = 1;
		this.comboBoxMethod = new JComboBox<>( ProjectionMethod.values() );
		add( comboBoxMethod, c );

		c.gridx = 0;
		c.gridy++;
		add( new JLabel( "Offset:" ), c );

		c.gridx = 1;
		this.spinnerModelOffset = new SpinnerNumberModel( 0, -nZSlices, nZSlices, 1 );
		add( new JSpinner( spinnerModelOffset ), c );

		c.gridx = 0;
		c.gridy++;
		add( new JLabel( "DeltaZ:" ), c );

		c.gridx = 1;
		this.spinnerModelDeltaZ = new SpinnerNumberModel( 0, -nZSlices, nZSlices, 1 );
		add( new JSpinner( spinnerModelDeltaZ ), c );
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
