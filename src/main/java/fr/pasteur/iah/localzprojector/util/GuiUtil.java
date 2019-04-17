package fr.pasteur.iah.localzprojector.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.util.Arrays;

public class GuiUtil
{

	public static void changeFont( final Component component, final Font font, final Class< ? extends Component > klass, final Component... toSpare )
	{
		if ( Arrays.asList( toSpare ).contains( component ) )
			return;

		if ( klass.isAssignableFrom( component.getClass() ) )
			component.setFont( font );

		if ( component instanceof Container )
			for ( final Component child : ( ( Container ) component ).getComponents() )
				changeFont( child, font, klass, toSpare );
	}

	private GuiUtil()
	{}
}
