package fr.pasteur.iah.localzprojector.command;

import org.scijava.command.ContextCommand;
import org.scijava.plugin.Plugin;

import fr.pasteur.iah.localzprojector.gui.GuiController;

@Plugin( type = LocalZProjectionCommand.class, name = "Local Z Projector", menuPath = "Plugins > Process > Local Z Projector" )
public class LocalZProjectionCommand extends ContextCommand
{

	@Override
	public void run()
	{
		new GuiController( context() );
	}

}
