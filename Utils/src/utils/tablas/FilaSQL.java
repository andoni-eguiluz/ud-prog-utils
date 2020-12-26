package utils.tablas;

import java.util.ArrayList;
import utils.Utils;

public class FilaSQL implements ConvertibleEnTabla {
	public static enum TipoValorSQL { NUMERO, STRING, NULL, IDENTIFICADOR, SIMBOLO, OTRO };

	private String nombreTabla;
	private ArrayList<String> camposSQL;
	private ArrayList<String> valores;
	private transient int numLinea;
	private transient String linea;
	private transient int accesoSecuencial = 0;
	protected FilaSQL(String nombreTabla, ArrayList<String> camposSQL, ArrayList<String> valores, int numLinea, String linea ) {
		this.nombreTabla = nombreTabla;
		this.camposSQL = camposSQL;
		this.valores = valores;
		accesoSecuencial = 0;
		this.numLinea = numLinea;
		this.linea = linea;
	}
	@Override
	public String toString() {
		String ret = "[" + nombreTabla + "] - " + valores + " - " + camposSQL;
		return Utils.trunca( ret, 300 );
	}
	
	public String getLineaSQL() {
		return linea;
	}
	
	public void setNumLineaSQL(int numLinea) {
		this.numLinea = numLinea;
	}
	public int getNumLineaSQL() {
		return numLinea;
	}
	/** Devuelve el nombre de la tabla de esta fila
	 * @return	Nombre de la tabla
	 */
	public String getNombreTabla() {
		return nombreTabla;
	}
	
	/** Devuelve la lista de columnas SQL de esta fila
	 * @return	Lista de columnas en el orden en el que se crean en el fichero original, null si no se conoce
	 */
	public ArrayList<String> getNomColumnas() {
		return camposSQL;
	}
	
	public boolean hayNomColumnas() {
		return camposSQL!=null && camposSQL.size()>0 && !camposSQL.get(0).isEmpty();
	}

	/** Devuelve el todos los valores de la fila SQL interpretados como strings en un arraylist
	 * @return	valores de la fila
	 */
	public ArrayList<String> getValores() {
		return valores;
	}
	
	/** Devuelve el valor de campo de la fila SQL interpretado como un String
	 * @param nomCol	Nombre de la columna/campo buscado. Si se indica a null se toma el siguiente campo en orden, empezando en el primero
	 * @return	valor, null si no existe la columna indicada
	 */
	public String getCol( String nomCol ) {
		String val = null;
		if (nomCol==null) {
			val = valores.get(accesoSecuencial);
			accesoSecuencial++;
		} else {
			int posi = camposSQL.indexOf( nomCol.toLowerCase() );
			if (posi==-1) return null;
			val = valores.get( posi );
		}
		return val;
	}
	/** Devuelve el siguiente valor de campo de la sentencia insert SQL interpretado como un int
	 * @param nomCol	Nombre de la columna/campo buscado. Si se indica a null se toma el siguiente campo en orden, empezando en el primero
	 * @return	valor, Integer.MAX_VALUE si no existe la columna indicada o es un entero incorrecto
	 */
	public int getColInt( String nomCol ) {
		int ret = Integer.MAX_VALUE;
		String val = null;
		if (nomCol==null) {
			val = valores.get(accesoSecuencial);
			accesoSecuencial++;
		} else {
			int posi = camposSQL.indexOf( nomCol.toLowerCase() );
			if (posi==-1) return ret;
			val = valores.get( posi );
		}
		try {
			ret = Integer.parseInt( val );
		} catch (NumberFormatException e) {}
		return ret;
	}
	/** Devuelve el siguiente valor de campo de la sentencia insert SQL interpretado como un Boolean
	 * @param nomCol	Nombre de la columna/campo buscado. Si se indica a null se toma el siguiente campo en orden, empezando en el primero
	 * @return	valor, null si no existe la columna indicada o es un boolean incorrecto
	 */
	public Boolean getColBool( String nomCol ) {
		Boolean ret = null;
		String val = null;
		if (nomCol==null) {
			val = valores.get(accesoSecuencial);
			accesoSecuencial++;
		} else {
			int posi = camposSQL.indexOf( nomCol.toLowerCase() );
			if (posi==-1) return ret;
			val = valores.get( posi );
		}
		try {
			ret = Boolean.parseBoolean( val );
		} catch (NumberFormatException e) {}
		return ret;
	}
	/** Devuelve el siguiente valor de campo de la sentencia insert SQL interpretado como un Boolean, con texto false/true o valor 0/1
	 * @param nomCol	Nombre de la columna/campo buscado. Si se indica a null se toma el siguiente campo en orden, empezando en el primero
	 * @return	valor, null si no existe la columna indicada o es un boolean incorrecto
	 */
	public Boolean getColBoolO01( String nomCol ) {
		Boolean ret = null;
		String val = null;
		if (nomCol==null) {
			val = valores.get(accesoSecuencial);
			accesoSecuencial++;
		} else {
			int posi = camposSQL.indexOf( nomCol.toLowerCase() );
			if (posi==-1) return ret;
			val = valores.get( posi );
		}
		if (val!=null) {
			if (val.equals("0") || "false".equalsIgnoreCase(val)) {
				ret = false;
			} else if (val.equals("1") || "true".equalsIgnoreCase(val)) {
				ret = true;
			}
		}
		return ret;
	}
	/** Devuelve el siguiente valor de campo de la sentencia insert SQL interpretado como un long
	 * @param nomCol	Nombre de la columna/campo buscado. Si se indica a null se toma el siguiente campo en orden, empezando en el primero
	 * @return	valor, Long.MAX_VALUE si no existe la columna indicada o es un long incorrecto
	 */
	public long getColLong( String nomCol ) {
		long ret = Long.MAX_VALUE;
		String val = null;
		if (nomCol==null) {
			val = valores.get(accesoSecuencial);
			accesoSecuencial++;
		} else {
			int posi = camposSQL.indexOf( nomCol.toLowerCase() );
			if (posi==-1) return ret;
			val = valores.get( posi );
		}
		try {
			ret = Long.parseLong( val );
		} catch (NumberFormatException e) {}
		return ret;
	}
	/** Devuelve el siguiente valor de campo de la sentencia insert SQL interpretado como un double
	 * @param nomCol	Nombre de la columna/campo buscado. Si se indica a null se toma el siguiente campo en orden, empezando en el primero
	 * @return	valor, Double.MAX_VALUE si no existe la columna indicada o es un double incorrecto
	 */
	public double getColDouble( String nomCol ) {
		double ret = Double.MAX_VALUE;
		String val = null;
		if (nomCol==null) {
			val = valores.get(accesoSecuencial);
			accesoSecuencial++;
		} else {
			int posi = camposSQL.indexOf( nomCol.toLowerCase() );
			if (posi==-1) return ret;
			val = valores.get( posi );
		}
		try {
			ret = Double.parseDouble( val );
		} catch (NumberFormatException e) {}
		return ret;
	}

	// Métodos de interfaz ConvertibleEnTabla

	@Override
	public int getNumColumnas() {
		return valores.size();
	}
	
	@Override
	public String getValorColumna(int col) {
		return valores.get(col);
	}
	@Override
	public void setValorColumna(int col, String valor) {
		valores.set( col,  valor );
	}
	@Override
	public String getNombreColumna(int col) {
		if (camposSQL==null || camposSQL.size()<=col) return "";
		return camposSQL.get( col );
	}
	
}
