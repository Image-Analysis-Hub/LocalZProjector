package fr.pasteur.iah.localzprojector.process.offline;

import org.scijava.Cancelable;

import net.imagej.ImgPlus;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public interface OnePassProjectorOp< T extends RealType< T > & NativeType< T > > extends
		UnaryComputerOp< ImgPlus< T >, ImgPlus< T > >,
		Cancelable
{

	/**
	 * Returns the height-map computed by this projection.
	 * 
	 * @return the height-map as a new {@link Img}.
	 */
	public Img< UnsignedShortType > getHeightMap();
}
