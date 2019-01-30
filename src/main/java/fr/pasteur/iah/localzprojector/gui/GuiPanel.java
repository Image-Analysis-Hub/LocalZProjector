package fr.pasteur.iah.localzprojector.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.util.AppUtil;
import net.imagej.Dataset;

public class GuiPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	ReferenceSurfacePanel referenceSurfacePanel;

	ExtractSurfacePanel extractSurfacePanel;

	private final TargetImagePanel targetImagePanel;

	final RunPanel runPanel;

	private Dataset dataset;

	/**
	 * Creates the GUI panel.
	 *
	 * @param datasetSupplier
	 *            supplier that returns the currently selected {@link Dataset}.
	 * @param previewReferencePlaneRunner
	 *            function to run when the 'Preview reference plane' button is
	 *            pressed.
	 * @param previewLocalProjectionRunner
	 *            function to run when the 'Preview local projection' button is
	 *            pressed. The boolean returned is <code>true</code> if the
	 *            checkbox 'Display reference plane' is checked.
	 * @param localProjectionRunner
	 *            function to run when the 'Local projection' button is pressed.
	 *            The boolean returned is <code>true</code> if the checkbox
	 *            'Display reference plane' is checked.
	 * @param stopper
	 *            function to run with the 'Stop' button is pressed.
	 */
	public GuiPanel(
			final Supplier< Dataset > datasetSupplier,
			final Runnable previewReferencePlaneRunner,
			final Consumer< Boolean > previewLocalProjectionRunner,
			final Consumer< Boolean > localProjectionRunner,
			final Runnable stopper )
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[] { 1. };
		gridBagLayout.rowWeights = new double[] { 0., 0., 0., 0., 0., 0., 0., 1. };
		gridBagLayout.rowHeights = new int[] { 0, 20, 0, 20, 0, 20 };
		setLayout( gridBagLayout );

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets( 5, 10, 5, 10 );
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;

		/*
		 * Title. y = 0.
		 */

		final JLabel lblTitle = new JLabel( "<html>\n<center>\n<big>Local Z Projector.</big>\n<p>\nv" + AppUtil.getVersion() + "\n</center>\n</html>" );
		lblTitle.setHorizontalAlignment( SwingConstants.CENTER );
		add( lblTitle, c );

		/*
		 * Separator. y = 1.
		 */

		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		add( new JSeparator(), c );

		/*
		 * Target image panel. y = 2.
		 */

		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		targetImagePanel = new TargetImagePanel();
		add( targetImagePanel, c );
		targetImagePanel.btnRefresh.addActionListener( l -> refreshDataset( datasetSupplier.get() ) );

		/*
		 * Separator. y = 3.
		 */

		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		add( new JSeparator(), c );

		/*
		 * Button panel. y = 7.
		 */

		c.gridy += 4;
		c.anchor = GridBagConstraints.SOUTH;
		add( new JSeparator(), c );

		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		this.runPanel = new RunPanel();
		add( runPanel, c );

		/*
		 * Check
		 */

		refreshDataset( datasetSupplier.get() );

		/*
		 * Wire listeners.
		 */

		runPanel.btnPreviewReferencePlane.addActionListener( l -> previewReferencePlaneRunner.run() );
		runPanel.btnPreviewLocalProjection.addActionListener( l -> previewLocalProjectionRunner.accept( runPanel.showReferenceSurfacePreview() ) );
		runPanel.btnRun.addActionListener( l -> localProjectionRunner.accept( runPanel.showReferenceSurfaceMovie() ) );
		runPanel.btnStop.addActionListener( l -> stopper.run() );
	}

	private void refreshDataset( final Dataset dataset )
	{
		this.dataset = dataset;
		if ( null != referenceSurfacePanel )
			remove( referenceSurfacePanel );
		if ( null != extractSurfacePanel )
			remove( extractSurfacePanel );

		targetImagePanel.refresh( dataset );
		if ( null == dataset )
			return;

		final int nChannels = ( int ) dataset.getChannels();
		final int nZSlices = ( int ) dataset.getDepth();

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets( 5, 10, 5, 10 );
		c.gridwidth = 1;
		c.gridx = 0;

		/*
		 * Reference plane panel. y = 4.
		 */

		referenceSurfacePanel = new ReferenceSurfacePanel( nChannels, nZSlices );
		c.gridy = 4;
		add( referenceSurfacePanel, c );

		/*
		 * Separator. y = 5.
		 */

		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		add( new JSeparator(), c );

		/*
		 * Local projection panel. y = 6.
		 */

		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		extractSurfacePanel = new ExtractSurfacePanel( nChannels, nZSlices );
		add( extractSurfacePanel, c );

		/*
		 * Pack
		 */

		revalidate();
		repaint();
	}

	public ReferenceSurfaceParameters getReferenceSurfaceParameters()
	{
		if ( null == referenceSurfacePanel )
			return null;
		return referenceSurfacePanel.getParameters();
	}

	public ExtractSurfaceParameters getExtractSurfaceParameters()
	{
		if ( null == extractSurfacePanel )
			return null;
		return extractSurfacePanel.getParameters();
	}

	public Dataset getSelectedDataset()
	{
		return dataset;
	}
}
