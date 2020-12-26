package utils;

import java.io.File;
import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Utilidades varias de strings, numéricas e interactivas
 * @author andoni.eguiluz at ingenieria.deusto.es
 */
public class Utils {
	
	/** Pide interactivamente un fichero de datos especificado
	 * @param extsPermitidas	Array de extensiones de fichero permitidas (p ej "txt"). Si es nulo o vacío, se permiten todas. 
	 * @param f	Opcional, indica un fichero de datos o directorio ya existente de donde partir
	 * @return	Fichero elegido, null si no se ha elegido ninguno
	 */
	public static File pideFichero( String[] extsPermitidas, File... f ) {
		JFileChooser fc = new JFileChooser( "." );
		if (f.length>0 && f[0].exists()) {
			if (f[0].isDirectory()) {
				fc.setCurrentDirectory( f[0] );
			} else {
				fc.setCurrentDirectory( f[0].getParentFile() );
				fc.setSelectedFile( f[0] );
			}
		}
		if (extsPermitidas!=null && extsPermitidas.length>0) {
			fc.setFileFilter( new FileNameExtensionFilter( null, extsPermitidas ) );
		}
		int ret = fc.showOpenDialog( null ); // Global.vConfig );  Pero luego se queda el filechooser clavado con la ventana y no se cierra (?)
		if (ret==JFileChooser.APPROVE_OPTION) {
			return fc.getSelectedFile();
		}
		return null;
	}

	
	/** Trunca un string
	 * @param string	String original
	 * @param largo	Largo máximo (positivo)
	 * @return	El string original si su largo es menor o igual al indicado. Truncado a ese largo y con puntos suspensivos, si es mayor.
	 */
	public static String trunca( String string, int largo ) {
		return (string.length() > largo ? string.substring(0, largo) + "..." : string );
	}
	
	/** Devuelve la media de una serie de elementos
	 * @param dato	Serie de datos numéricos
	 * @return	media aritmética, Double.NaN si no puede calcularse (no hay datos)
	 */
	public static double media( double... dato ) {
		if (dato==null || dato.length==0) return Double.NaN;
		double suma = 0.0;
		for (double d : dato) suma += d;
		return suma / dato.length;
	}
	
	/** Devuelve la mediana de una serie de elementos
	 * @param dato	Serie de datos numéricos
	 * @return	media aritmética, Double.NaN si no puede calcularse (no hay datos)
	 */
	public static double mediana( double... dato ) {
		if (dato==null || dato.length==0) return Double.NaN;
		double[] dato2 = Arrays.copyOf( dato, dato.length );
		Arrays.sort( dato2 );
		if (dato.length % 2 == 1) {
			return dato2[ dato.length/2 ];
		} else {
			return (dato2[ dato.length/2-1 ] + dato2[ dato.length/2 ])/2.0;
		}
	}
	
	/** Devuelve la desviación típica poblacional de una serie de elementos
	 * @param dato	Serie de datos numéricos
	 * @return	desviación típica, Double.NaN si no puede calcularse (ho hay datos)
	 */
	public static double desvTipicaPob( double... dato ) {
		return desvTipicaPobM( media( dato ), dato );
	}
	
	/** Devuelve la desviación típica poblacional de una serie de elementos
	 * @param dato	Serie de datos numéricos
	 * @param media	Media ya calculada de esos datos
	 * @return	media aritmética, Double.NaN si no puede calcularse (ho hay datos)
	 */
	public static double desvTipicaPobM( double media, double... dato) {
		if (dato==null || dato.length==0) return Double.NaN;
		double suma = 0.0;
		for (double d : dato) suma += (d-media)*(d-media);
		return Math.sqrt( suma / dato.length );
	}
	
	/** Devuelve la desviación típica muestral de una serie de elementos
	 * @param dato	Serie de datos numéricos
	 * @return	desviación típica, Double.NaN si no puede calcularse (hay menos de dos datos)
	 */
	public static double desvTipica2( double... dato ) {
		return desvTipica( media( dato ), dato );
	}
	
	/** Devuelve la desviación típica muestral de una serie de elementos
	 * @param dato	Serie de datos numéricos
	 * @param media	Media ya calculada de esos datos
	 * @return	media aritmética, Double.NaN si no puede calcularse (hay menos de dos datos)
	 */
	public static double desvTipica( double media, double... dato) {
		if (dato==null || dato.length<2) return Double.NaN;
		double suma = 0.0;
		for (double d : dato) suma += (d-media)*(d-media);
		return Math.sqrt( suma / (dato.length-1) );
	}
	
}
