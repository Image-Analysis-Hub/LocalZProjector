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

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class RunPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final static ImageIcon REFERENCE_PLANE_ICON = new ImageIcon( RunPanel.class.getResource( "anchor.png" ) );

	private final static ImageIcon LOCAL_PROJECTION_ICON = new ImageIcon( RunPanel.class.getResource( "bullet_arrow_bottom.png" ) );

	private final static ImageIcon START_ICON = new ImageIcon( RunPanel.class.getResource( "control_play_blue.png" ) );

	private final static ImageIcon STOP_ICON = new ImageIcon( RunPanel.class.getResource( "control_stop_blue.png" ) );

	private static final boolean DEFAULT_SHOW_REFERENCE_PLANE_MOVIE = false;

	private static final boolean DEFAULT_SHOW_REFERENCE_PLANE_PREVIEW = false;

	private static final JFileChooser chooser = new JFileChooser()
	{

		private static final long serialVersionUID = 1L;

		@Override
		public void approveSelection()
		{
			if ( getSelectedFile().isFile() )
			{
				return;
			}
			else
				super.approveSelection();
		}
	};
	static
	{
		chooser.setDialogTitle( "Select a folder to save in" );
		chooser.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );
	}

	final JButton btnRun;

	final JButton btnStop;

	final JButton btnPreviewLocalProjection;

	final JButton btnPreviewReferencePlane;

	private final JCheckBox chckbxShowReferencePlanePreview;

	private final JCheckBox chckbxShowReferenceFrame;

	private JCheckBox chckbxSaveEachTimepoint;

	private JButton btnBrowse;

	private JTextField textField;

	private JPanel panelBrowse;

	private JLabel lblSaveTo;

	private Component horizontalGlue;

	public RunPanel()
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 239, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 40, 129, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		final JLabel lblTitle = new JLabel( "Execute." );
		lblTitle.setFont( lblTitle.getFont().deriveFont( 2f + lblTitle.getFont().getSize2D() ) );
		final GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.gridwidth = 2;
		gbc_lblNewLabel.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add( lblTitle, gbc_lblNewLabel );

		final JPanel panelPreview = new JPanel();
		panelPreview.setBorder( new TitledBorder( new LineBorder( new Color( 192, 192, 192 ), 1, true ), "Preview on current time-point", TitledBorder.LEADING, TitledBorder.TOP, null, new Color( 0, 0, 0 ) ) );
		final GridBagConstraints gbc_panelPreview = new GridBagConstraints();
		gbc_panelPreview.insets = new Insets( 0, 0, 0, 5 );
		gbc_panelPreview.fill = GridBagConstraints.BOTH;
		gbc_panelPreview.gridx = 0;
		gbc_panelPreview.gridy = 1;
		add( panelPreview, gbc_panelPreview );
		final GridBagLayout gbl_panelPreview = new GridBagLayout();
		gbl_panelPreview.columnWidths = new int[] { 0, 0, 0 };
		gbl_panelPreview.rowHeights = new int[] { 0, 0, 0, 0 };
		gbl_panelPreview.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_panelPreview.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panelPreview.setLayout( gbl_panelPreview );

		btnPreviewReferencePlane = new JButton( REFERENCE_PLANE_ICON );
		final GridBagConstraints gbc_btnRf = new GridBagConstraints();
		gbc_btnRf.insets = new Insets( 0, 0, 5, 5 );
		gbc_btnRf.gridx = 0;
		gbc_btnRf.gridy = 0;
		panelPreview.add( btnPreviewReferencePlane, gbc_btnRf );

		final JLabel lblReferencePlane = new JLabel( "Reference plane" );
		final GridBagConstraints gbc_lblReferencePlane = new GridBagConstraints();
		gbc_lblReferencePlane.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblReferencePlane.insets = new Insets( 0, 0, 5, 0 );
		gbc_lblReferencePlane.gridx = 1;
		gbc_lblReferencePlane.gridy = 0;
		panelPreview.add( lblReferencePlane, gbc_lblReferencePlane );

		btnPreviewLocalProjection = new JButton( LOCAL_PROJECTION_ICON );
		final GridBagConstraints gbc_btnLp = new GridBagConstraints();
		gbc_btnLp.insets = new Insets( 0, 0, 5, 5 );
		gbc_btnLp.gridx = 0;
		gbc_btnLp.gridy = 1;
		panelPreview.add( btnPreviewLocalProjection, gbc_btnLp );

		final JLabel lblLocalProjection = new JLabel( "Local projection" );
		final GridBagConstraints gbc_lblLocalProjection = new GridBagConstraints();
		gbc_lblLocalProjection.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblLocalProjection.insets = new Insets( 0, 0, 5, 0 );
		gbc_lblLocalProjection.gridx = 1;
		gbc_lblLocalProjection.gridy = 1;
		panelPreview.add( lblLocalProjection, gbc_lblLocalProjection );

		this.chckbxShowReferencePlanePreview = new JCheckBox( "Show reference plane", DEFAULT_SHOW_REFERENCE_PLANE_PREVIEW );
		final GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_chckbxNewCheckBox.gridwidth = 2;
		gbc_chckbxNewCheckBox.insets = new Insets( 0, 0, 0, 5 );
		gbc_chckbxNewCheckBox.gridx = 0;
		gbc_chckbxNewCheckBox.gridy = 2;
		panelPreview.add( chckbxShowReferencePlanePreview, gbc_chckbxNewCheckBox );

		final JPanel panelRun = new JPanel();
		panelRun.setBorder( new TitledBorder( new LineBorder( new Color( 192, 192, 192 ), 1, true ), "Run", TitledBorder.LEADING, TitledBorder.TOP, null, null ) );
		final GridBagConstraints gbc_panelRun = new GridBagConstraints();
		gbc_panelRun.fill = GridBagConstraints.BOTH;
		gbc_panelRun.gridx = 1;
		gbc_panelRun.gridy = 1;
		add( panelRun, gbc_panelRun );
		final GridBagLayout gbl_panelRun = new GridBagLayout();
		gbl_panelRun.columnWidths = new int[] { 252, 0 };
		gbl_panelRun.rowHeights = new int[] { 28, 23, 23, 0, 0, 0, 0 };
		gbl_panelRun.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panelRun.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
		panelRun.setLayout( gbl_panelRun );

		final JPanel panelButton = new JPanel();
		panelButton.setLayout( new BoxLayout( panelButton, BoxLayout.X_AXIS ) );
		btnRun = new JButton( "Run", START_ICON );
		panelButton.add( btnRun );
		panelButton.add( Box.createHorizontalGlue() );
		btnStop = new JButton( "Stop", STOP_ICON );
		panelButton.add( btnStop );

		final GridBagConstraints gbc_panelButton = new GridBagConstraints();
		gbc_panelButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_panelButton.insets = new Insets( 0, 0, 5, 0 );
		gbc_panelButton.gridx = 0;
		gbc_panelButton.gridy = 0;
		panelRun.add( panelButton, gbc_panelButton );

		this.chckbxShowReferenceFrame = new JCheckBox( "Show reference plane movie", DEFAULT_SHOW_REFERENCE_PLANE_MOVIE );
		final GridBagConstraints gbc_chckbxShowReferenceFrame = new GridBagConstraints();
		gbc_chckbxShowReferenceFrame.anchor = GridBagConstraints.WEST;
		gbc_chckbxShowReferenceFrame.insets = new Insets( 0, 0, 5, 0 );
		gbc_chckbxShowReferenceFrame.gridx = 0;
		gbc_chckbxShowReferenceFrame.gridy = 1;
		panelRun.add( chckbxShowReferenceFrame, gbc_chckbxShowReferenceFrame );

		chckbxSaveEachTimepoint = new JCheckBox( "Save each time-point" );
		final GridBagConstraints gbc_chckbxSaveEachTimepoint = new GridBagConstraints();
		gbc_chckbxSaveEachTimepoint.insets = new Insets( 0, 0, 5, 0 );
		gbc_chckbxSaveEachTimepoint.anchor = GridBagConstraints.WEST;
		gbc_chckbxSaveEachTimepoint.gridx = 0;
		gbc_chckbxSaveEachTimepoint.gridy = 2;
		panelRun.add( chckbxSaveEachTimepoint, gbc_chckbxSaveEachTimepoint );

		panelBrowse = new JPanel();
		final GridBagConstraints gbc_panelBrowse = new GridBagConstraints();
		gbc_panelBrowse.insets = new Insets( 0, 0, 5, 0 );
		gbc_panelBrowse.fill = GridBagConstraints.BOTH;
		gbc_panelBrowse.gridx = 0;
		gbc_panelBrowse.gridy = 3;
		panelRun.add( panelBrowse, gbc_panelBrowse );
		panelBrowse.setLayout( new BoxLayout( panelBrowse, BoxLayout.X_AXIS ) );

		lblSaveTo = new JLabel( "Save to:" );
		panelBrowse.add( lblSaveTo );

		horizontalGlue = Box.createHorizontalGlue();
		panelBrowse.add( horizontalGlue );

		btnBrowse = new JButton( "Browse" );
		btnBrowse.addActionListener( l -> browse() );
		panelBrowse.add( btnBrowse );

		textField = new JTextField();
		textField.setFont( getFont().deriveFont( getFont().getSize2D() - 2f ) );
		final GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.insets = new Insets( 0, 0, 5, 0 );
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 0;
		gbc_textField.gridy = 4;
		panelRun.add( textField, gbc_textField );
		textField.setColumns( 10 );

		/*
		 * Wire listeners.
		 */

		final ItemListener il = ( l ) -> {
			final boolean enabled = chckbxSaveEachTimepoint.isSelected();
			textField.setEnabled( enabled );
			for ( final Component c : panelBrowse.getComponents() )
				c.setEnabled( enabled );
		};
		chckbxSaveEachTimepoint.addItemListener( il );

		/*
		 * Default values.
		 */

		textField.setText( System.getProperty( "user.home" ) );
		chckbxSaveEachTimepoint.setSelected( false );
		il.itemStateChanged( null );
	}

	private void browse()
	{
		final int returnVal = chooser.showOpenDialog( this );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
			textField.setText( chooser.getSelectedFile().getAbsolutePath() );
	}

	public boolean showReferenceSurfaceMovie()
	{
		return chckbxShowReferenceFrame.isSelected();
	}

	public void setShowReferenceSurfaceMovie( final boolean showReferencePlaneMovie )
	{
		chckbxShowReferenceFrame.setSelected( showReferencePlaneMovie );
	}

	public boolean showReferenceSurfacePreview()
	{
		return chckbxShowReferencePlanePreview.isSelected();
	}

	public void setShowReferenceSurfacePreview( final boolean showReferencePlane )
	{
		chckbxShowReferencePlanePreview.setSelected( showReferencePlane );
	}

	public boolean saveEachTimepoint()
	{
		return chckbxSaveEachTimepoint.isSelected();
	}

	public void setSaveEachTimepoint( final boolean saveEachTimepoint )
	{
		chckbxSaveEachTimepoint.setSelected( saveEachTimepoint );
	}

	public String getSavePath()
	{
		return textField.getText();
	}

	public void setSavePath( final String path )
	{
		textField.setText( path );
	}
}
