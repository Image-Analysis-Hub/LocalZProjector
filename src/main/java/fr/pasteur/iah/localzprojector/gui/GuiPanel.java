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

import java.util.function.Consumer;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.scijava.prefs.PrefService;
import org.scijava.util.VersionUtils;

import fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters;
import fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters;
import fr.pasteur.iah.localzprojector.util.GuiUtil;
import net.imagej.Dataset;

public class GuiPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final ImageIcon ICON;

	static
	{
		final ImageIcon imageIcon = new ImageIcon( ReferenceSurfacePanel.class.getResource( "LocalZProjectorLogo-512.png" ) );
		ICON = GuiUtil.scaleImage( imageIcon, 128, 128 );
	}

	private final PrefService prefService;

	static JFileChooser fileChooser = new JFileChooser();

	ReferenceSurfacePanel referenceSurfacePanel;

	ExtractSurfacePanel extractSurfacePanel;

	private final TargetImagePanel targetImagePanel;

	final RunPanel runPanel;

	private Dataset dataset;

	private final JPanel middlePanel;


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
	 * @param stopper
	 *            function to run with the 'Stop' button is pressed.
	 * @param prefService
	 *            used to grant persistence of settings on the GUI.
	 */
	public GuiPanel(
			final Dataset dataset,
			final Runnable previewReferencePlaneRunner,
			final Consumer< Boolean > previewLocalProjectionRunner,
			final Runnable localProjectionRunner,
			final Runnable stopper,
			final PrefService prefService )
	{
		this.prefService = prefService;
		final BoxLayout boxLayout = new BoxLayout( this, BoxLayout.PAGE_AXIS );
		setLayout( boxLayout );

		/*
		 * Title.
		 */

		final JLabel lblTitle = new JLabel(
				"<html>\n<center>\n<big>Local Z Projector.</big>\n<p>\nv" + VersionUtils.getVersion( GuiPanel.class ) + "\n</center>\n</html>",
				ICON,
				JLabel.CENTER );
		lblTitle.setHorizontalAlignment( SwingConstants.CENTER );
		lblTitle.setAlignmentX( 0.5f );
		add( lblTitle );

		/*
		 * Separator.
		 */

		add( new JSeparator() );

		/*
		 * Target image panel.
		 */

		targetImagePanel = new TargetImagePanel();
		add( targetImagePanel );

		/*
		 * Separator.
		 */

		add( new JSeparator() );

		/*
		 * Middle panel.
		 */

		this.middlePanel = new JPanel();
		middlePanel.setLayout( new BoxLayout( middlePanel, BoxLayout.LINE_AXIS ) );
		add( middlePanel );

		/*
		 * Button panel.
		 */

		add( new JSeparator() );

		this.runPanel = new RunPanel();
		add( runPanel );

		/*
		 * Check
		 */

		refreshDataset( dataset );

		/*
		 * Wire listeners.
		 */

		runPanel.btnPreviewReferencePlane.addActionListener( l -> previewReferencePlaneRunner.run() );
		runPanel.btnPreviewLocalProjection.addActionListener( l -> previewLocalProjectionRunner.accept( runPanel.showReferenceSurfacePreview() ) );
		runPanel.btnRun.addActionListener( l -> localProjectionRunner.run() );
		runPanel.btnStop.addActionListener( l -> stopper.run() );
	}

	private void refreshDataset( final Dataset dataset )
	{
		this.dataset = dataset;
		middlePanel.removeAll();

		targetImagePanel.refresh( dataset );
		if ( null == dataset )
			return;

		final int nChannels = ( int ) dataset.getChannels();
		final int nZSlices = ( int ) dataset.getDepth();


		/*
		 * Reference plane panel.
		 */

		referenceSurfacePanel = new ReferenceSurfacePanel( nChannels, nZSlices, prefService );
		middlePanel.add( referenceSurfacePanel );

		/*
		 * Separator.
		 */

		middlePanel.add( new JSeparator( JSeparator.VERTICAL ) );

		/*
		 * Local projection panel.
		 */

		extractSurfacePanel = new ExtractSurfacePanel( nChannels, nZSlices, prefService );
		middlePanel.add( extractSurfacePanel );

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
