package fr.pasteur.iah.localzprojector.gui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fr.pasteur.iah.localzprojector.util.GuiUtil;
import net.imagej.Dataset;

public class TargetImagePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JLabel labelImage;

	private final JLabel labelNC;

	private final JLabel labelNZ;

	private final JLabel labelNT;

	public TargetImagePanel()
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		setLayout( gridBagLayout );

		final JLabel lblTargetImage = new JLabel( "Target image:", JLabel.CENTER );
		lblTargetImage.setFont( lblTargetImage.getFont().deriveFont( lblTargetImage.getFont().getSize() + 2f ) );
		final GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		c.weightx = 1.;
		c.insets = new Insets( 2, 5, 2, 5 );
		c.gridx = 0;
		c.gridy = 0;
		add( lblTargetImage, c );

		this.labelImage = new JLabel( "", JLabel.CENTER );
		c.gridx = 2;
		c.gridwidth = 5;
		add( labelImage, c );

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		add( new JLabel( "N channels:" ), c );

		c.gridx++;
		c.anchor = GridBagConstraints.WEST;
		this.labelNC = new JLabel();
		add( labelNC, c );

		c.gridx++;
		c.anchor = GridBagConstraints.EAST;
		add( new JLabel( "N Z-slices:" ), c );

		c.gridx++;
		c.anchor = GridBagConstraints.WEST;
		this.labelNZ = new JLabel();
		add( labelNZ, c );

		c.gridx++;
		c.anchor = GridBagConstraints.EAST;
		add( new JLabel( "N time-points:" ), c );

		c.gridx++;
		c.anchor = GridBagConstraints.WEST;
		this.labelNT = new JLabel();
		add( labelNT, c );

		// Change font size - more compact.
		final Font lblFont = getFont().deriveFont( getFont().getSize2D() - 2f );
		GuiUtil.changeFont( this, lblFont, JLabel.class, lblTargetImage );
		GuiUtil.changeFont( this, lblFont, JButton.class );
		labelImage.setFont( labelImage.getFont().deriveFont( Font.ITALIC ) );
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
