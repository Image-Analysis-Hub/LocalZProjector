/*-
 * #%L
 * Image Analysis Hub support for Life Scientists.
 * %%
 * Copyright (C) 2019 - 2021 IAH developers.
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the IAH / C2RT / Institut Pasteur nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package fr.pasteur.iah.localzprojector.gui;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import fr.pasteur.iah.localzprojector.util.GuiUtil;
import net.imagej.Dataset;

public class TargetImagePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final String PAPER_LINK = "https://www.biorxiv.org/content/10.1101/2021.01.15.426809v2";

	private static final String PAPER_STR = "<html><a href=" + PAPER_LINK + ">Paper</a></html>";

	private static final String DOC_LINK = "https://gitlab.pasteur.fr/iah-public/localzprojector/-/blob/master/README.md";

	private static final String DOC_STR = "<html><a href=" + DOC_LINK + ">Documentation</a></html>";

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
		c.gridwidth = 3;
		add( labelImage, c );

		c.gridx = 7;
		c.gridwidth = 2;
		c.gridheight = 1;
		add( new JLabel( "Links to:" ), c );

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

		c.gridx = 7;
		final JLabel lblDoc = new JLabel( DOC_STR );
		lblDoc.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( final java.awt.event.MouseEvent e )
			{
				try
				{
					Desktop.getDesktop().browse( new URI( DOC_LINK ) );
				}
				catch ( URISyntaxException | IOException ex )
				{
					ex.printStackTrace();
				}
			}
		} );
		add( lblDoc, c );

		c.gridx = 8;
		final JLabel lblPaper = new JLabel( PAPER_STR );
		lblPaper.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( final java.awt.event.MouseEvent e )
			{
				try
				{
					Desktop.getDesktop().browse( new URI( PAPER_LINK ) );
				}
				catch ( URISyntaxException | IOException ex )
				{
					ex.printStackTrace();
				}
			}
		} );
		add( lblPaper, c );

		c.gridy = 0;
		c.gridx = 6;
		c.gridwidth = 1;
		c.gridheight = 2;
		c.fill = GridBagConstraints.BOTH;
		add( new JSeparator( JSeparator.VERTICAL ), c );

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
			labelImage.setText( "Please open an image before running Local-Z-Projector." );
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
