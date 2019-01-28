package fr.pasteur.iah.localzprojector.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class RunPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final static ImageIcon REFERENCE_PLANE_ICON = new ImageIcon( RunPanel.class.getResource( "anchor.png" ) );

	private final static ImageIcon LOCAL_PROJECTION_ICON = new ImageIcon( RunPanel.class.getResource( "bullet_arrow_bottom.png" ) );

	private final static ImageIcon START_ICON = new ImageIcon( RunPanel.class.getResource( "control_start_blue.png" ) );

	private final static ImageIcon STOP_ICON = new ImageIcon( RunPanel.class.getResource( "stop.png" ) );

	private static final boolean DEFAULT_SHOW_REFERENCE_PLANE_MOVIE = false;

	private static final boolean DEFAULT_SHOW_REFERENCE_PLANE_PREVIEW = false;

	final JButton btnRun;

	final JButton btnStop;

	final JButton btnPreviewLocalProjection;

	final JButton btnPreviewReferencePlane;

	private final JCheckBox chckbxShowReferencePlanePreview;

	private final JCheckBox chckbxShowReferenceFrame;

	public RunPanel()
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 239, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 40, 129, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		final JLabel lblTitle = new JLabel( "Execute." );
		lblTitle.setFont( lblTitle.getFont().deriveFont( 4f + lblTitle.getFont().getSize2D() ) );
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
		panelRun.setLayout( new BoxLayout( panelRun, BoxLayout.Y_AXIS ) );

		btnRun = new JButton( "Run", START_ICON );
		panelRun.add( btnRun );

		btnStop = new JButton( "Stop", STOP_ICON );
		panelRun.add( btnStop );

		this.chckbxShowReferenceFrame = new JCheckBox( "Show reference plane movie", DEFAULT_SHOW_REFERENCE_PLANE_MOVIE );
		panelRun.add( chckbxShowReferenceFrame );
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
}
