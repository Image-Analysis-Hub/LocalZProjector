package fr.pasteur.iah.localzprojector.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Image;
import java.util.Arrays;

import javax.swing.ImageIcon;

public class GuiUtil
{

	public static final ImageIcon scaleImage( final ImageIcon icon, final int w, final int h )
	{
		int nw = icon.getIconWidth();
		int nh = icon.getIconHeight();

		if ( icon.getIconWidth() > w )
		{
			nw = w;
			nh = ( nw * icon.getIconHeight() ) / icon.getIconWidth();
		}

		if ( nh > h )
		{
			nh = h;
			nw = ( icon.getIconWidth() * nh ) / icon.getIconHeight();
		}

		return new ImageIcon( icon.getImage().getScaledInstance( nw, nh, Image.SCALE_SMOOTH ) );
	}

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
