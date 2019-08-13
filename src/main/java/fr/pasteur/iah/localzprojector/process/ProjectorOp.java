package fr.pasteur.iah.localzprojector.process;

import org.scijava.Cancelable;

import net.imagej.ImgPlus;
import net.imagej.ops.special.computer.BinaryComputerOp;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public interface ProjectorOp< T extends RealType< T > & NativeType< T > > extends BinaryComputerOp< ImgPlus< T >, RandomAccessibleInterval< UnsignedShortType >, RandomAccessibleInterval< T > >, Cancelable
{}
