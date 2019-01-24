package fr.pasteur.iah.localzprojector.gui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.imagej.Dataset;

public class TargetImagePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JLabel labelImage;

	private final JLabel labelNC;

	private final JLabel labelNZ;

	private final JLabel labelNT;

	final JButton btnRefresh;

	public TargetImagePanel()
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 114, 44, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		final JLabel lblTargetImage = new JLabel( "Target image." );
		lblTargetImage.setFont( lblTargetImage.getFont().deriveFont( lblTargetImage.getFont().getSize() + 4f ) );
		final GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets( 5, 5, 5, 5 );
		c.gridx = 0;
		c.gridy = 0;
		add( lblTargetImage, c );

		this.labelImage = new JLabel( "", JLabel.CENTER );
		labelImage.setFont( labelImage.getFont().deriveFont( Font.ITALIC ) );
		c.gridy++;
		add( labelImage, c );

		c.gridy++;
		c.gridwidth = 1;
		add( new JLabel( "N channels:" ), c );

		c.gridx = 1;
		this.labelNC = new JLabel();
		add( labelNC, c );

		c.gridy++;
		c.gridx = 0;
		add( new JLabel( "N Z-slices:" ), c );

		c.gridx = 1;
		this.labelNZ = new JLabel();
		add( labelNZ, c );

		c.gridy++;
		c.gridx = 0;
		add( new JLabel( "N time-points:" ), c );

		c.gridx = 1;
		this.labelNT = new JLabel();
		add( labelNT, c );

		c.gridy++;
		c.gridx = 0;
		this.btnRefresh = new JButton( "refresh" );
		add( btnRefresh, c );
	}

	void refresh( final Dataset dataset )
	{
		if ( null == dataset )
		{
			labelImage.setText( "Please select an image and click the refresh button." );
			labelNC.setText( "" );
			labelNT.setText( "" );
			labelNZ.setText( "" );
			return;
		}
		labelImage.setText( dataset.getName() );
		labelNC.setText( Long.toString( dataset.getChannels() ) );
		labelNT.setText( Long.toString( dataset.getFrames() ) );
		labelNZ.setText( Long.toString( dataset.getDepth() ) );
	}
}
