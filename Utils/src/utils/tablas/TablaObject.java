package utils.tablas;

import java.awt.Color;
import java.awt.Insets;
import java.io.*;
import java.util.*;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.table.TableColumn;

/** Tabla de datos bidimensional de cualquier tipo para análisis posterior
 * Composición: una serie de cabeceras (columnas) con una serie de filas de datos (objetos) que tienen un dato para cada columna
 * Dato de cada celda puede ser cualquier objeto
 * @author andoni.eguiluz @ ingenieria.deusto.es
 */
public class TablaObject extends Tabla implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/** Método principal de prueba
	 * @param args	No utilizado
	 */
	public static void main( String[] args ) {
		try {  // Intenta poner L&F Nimbus
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("Nimbus".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		            UIManager.getLookAndFeelDefaults().put("Table.cellNoFocusBorder", new Insets(0,0,0,0));
		            UIManager.getLookAndFeelDefaults().put("Table.focusCellHighlightBorder", new Insets(0,0,0,0));
		            UIManager.getLookAndFeelDefaults().put("Button.contentMargins", new Insets(3, 6, 3, 6));		            
		            UIManager.getLookAndFeelDefaults().put("DesktopIcon.contentMargins", new Insets(0,0,0,0));
		            UIManager.getLookAndFeelDefaults().put("Label.contentMargins", new Insets(0,0,0,0));		            
		            break;
		        }
		    }
		} catch (Exception e) {} // Si no está disponible nimbus, no se hace nada
		TablaObject to = new TablaObject();
		to.addColumnO( "datoI", new Integer(0) );
		to.addColumnO( "datoD", new Double(0) );
		to.addColumnO( "datoD2", new Double(0) );
		to.addColumnO( "datoB", new Boolean(false) );
		to.addColumnO( "datoS", "" );
		to.addColumnO( "tipo", "" );
		to.addDataLineO( new ArrayList<Object>( Arrays.asList( 8, 8.1, 4.5, true, "8", "dataset1" ) ) );
		to.addDataLineO( new ArrayList<Object>( Arrays.asList( 7, 7.1, 3.2, false, "7", "dataset1" ) ) );
		to.addDataLineO( new ArrayList<Object>( Arrays.asList( 0, null, 1.8, true, "sin Double", "dataset1" ) ) );
		to.addDataLineO( new ArrayList<Object>( Arrays.asList( null, 1.0, 7.3, false, "sin Integer", "dataset1" ) ) );
		to.addDataLineO( new ArrayList<Object>( Arrays.asList( 7, 7.0, 6.2, false, "7", "dataset1" ) ) );
		to.addDataLineO( new ArrayList<Object>( Arrays.asList( 7, 7.0, 1.7, false, "7", "dataset1" ) ) );
		to.addDataLineO( new ArrayList<Object>( Arrays.asList( 7, 7.0, 4.3, false, "7", "dataset1" ) ) );
		to.addDataLineO( new ArrayList<Object>( Arrays.asList( 4, 3.8, 2.8, false, "3.8", "dataset1" ) ) );
		to.addDataLineO( new ArrayList<Object>( Arrays.asList( 1, 1.0, 2.3, false, "1", "dataset2" ) ) );
		to.addDataLineO( new ArrayList<Object>( Arrays.asList( 2, 2.8, 1.8, false, "2.8", "dataset2" ) ) );
		to.addDataLineO( new ArrayList<Object>( Arrays.asList( 0, 0.8, 0.4, false, "0.8", "dataset2" ) ) );
		to.addCalcColumn( "Par-Impar", String.class, new CalculadorColumna() {
			@Override
			public Object calcula(TablaObject to, int fila) {
				Object o = to.getO( fila, "datoI" ); 
				if (o==null) return "nulo";
				if (o instanceof Integer) {
					Integer i = (Integer) o;
					if (i==0) return "Cero";
					else if (i%2==0) return "Par";
					else return "Impar";
				} else {
					return null;
				}
			}
		});
		System.out.println( to );
		System.out.println( "Tipos:" );
		for (int i=0; i<to.getHeaders().size(); i++) System.out.print( to.getType(i) + "\t" );
		System.out.println();
		VentanaGeneralTablas vgt = new VentanaGeneralTablas();
		VentanaTabla v = mainNuevaVent( vgt, to, "Test" );
		TablaObject nuevaTabla = to.creaTablaEstad( TipoEstad.MEDIA, "Par-Impar", "N", true );
		VentanaTabla v2 = mainNuevaVent( vgt, nuevaTabla, "Media" );
		TablaObject nuevaTabla2 = to.creaTablaEstad( TipoEstad.MEDIANA, "Par-Impar", "N", true, "datoD", "datoD2" );
		VentanaTabla v3 = mainNuevaVent( vgt, nuevaTabla2, "Mediana" );
		vgt.setSize( 1400, 1000 );
		vgt.setVisible( true );
		v2.setLocation( 0, 300 );
		v3.setLocation( 300, 300 );
		v.setVisible( true );
		v2.setVisible( true );
		v3.setVisible( true );
		v.pack();
		v2.pack();
		v3.pack();
		// TablaObject dataset1 = to.creaTablaFiltro( "tipo", "dataset1", null );
		TablaObject dataset1 = to.creaTablaFiltro( "tipo", true, "DATASET1", null, "datoI", "datoD", "datoD2", "Par-Impar" );
		VentanaTabla v4 = mainNuevaVent( vgt, dataset1, "dataset1" );
		TablaObject dataset1b = dataset1.creaTablaEstad( TipoEstad.MEDIA, "Par-Impar", "N", true );
		VentanaTabla v5 = mainNuevaVent( vgt, dataset1b, "Media dataset1" );
		TablaObject dataset1c = dataset1.creaTablaEstad( TipoEstad.MEDIANA, "Par-Impar", "N", true, "datoD", "datoD2" );
		VentanaTabla v6 = mainNuevaVent( vgt, dataset1c, "Mediana dataset1" );
		v4.setLocation( 600, 0 );
		v5.setLocation( 600, 300 );
		v6.setLocation( 900, 300 );
		v4.setVisible( true );
		v5.setVisible( true );
		v6.setVisible( true );
		v4.pack();
		v5.pack();
		v6.pack();
		ArrayList<String> hs = new ArrayList<>( Arrays.asList( "t", "d1", "d2", "d3", "d4", "d5", "N", "n1", "n2", "n3", "n4", "n5" ) );
		TablaObject testValores = new TablaObject( hs );
		Random r = new Random();
		for (int i=0; i<10; i++) {
			ArrayList<Object> l = new ArrayList<>();
			l.add( "dato " + i );
			for (int j=1; j<=5; j++) l.add( i*1.0 + j*0.2 );
			int val = i*5+25;
			l.add( val );
			for (int j=1; j<=5; j++) { if (val>0) val -= r.nextInt(2*val/3); l.add( val ); }
			testValores.addDataLineO( l );
		}
		VentanaTabla v7 = mainNuevaVent( vgt, testValores, "Valores prueba render" );
		v7.setLocation( 0, 500 );
		v7.pack();
		v7.setVisible( true );
		v7.pack();
		final Color ROJO_CLARO = new Color( 255, 180, 180 );
		final Color VERDE_CLARO = new Color( 180, 255, 180 );
		v7.setRenderer4Cuartiles( true, "d1", "d5", Color.RED, ROJO_CLARO, VERDE_CLARO, Color.GREEN );
		// sería lo mismo
		// double[] est = testValores.getEstadValores( "d1", "d5" );
		// v7.setRendererCuatroColores( true, "d1", "d5", est[4], est[5], est[7], est[8], Color.RED, ROJO_CLARO, VERDE_CLARO, Color.GREEN );
		v7.setRenderer3Colores( false, "n1", "n5", Color.RED, Color.YELLOW, Color.GREEN );
		// sería lo mismo
		// est = testValores.getEstadValores( "n1", "n5" );
		// v7.setRendererTresColores( false, "n1", "n5", est[4], est[2], est[8], Color.RED, Color.WHITE, Color.GREEN );
	}
		private static VentanaTabla mainNuevaVent( VentanaGeneralTablas vgt, TablaObject to, String tit ) {
			VentanaTabla v = new VentanaTabla( vgt, tit, true );
			v.setTabla( to );
			vgt.addVentanaInterna( v, tit );
			for (int i=0; i<to.getHeaders().size(); i++) {
				TableColumn tCol = v.getJTable().getColumnModel().getColumn(i);
			    if (to.getType(i) == Integer.class) {
			        tCol.setPreferredWidth(25);
			    } else if (to.getType(i) == Double.class) {
			        tCol.setPreferredWidth(50);
			    } else if (to.getType(i) == Boolean.class) {
			        tCol.setPreferredWidth(20);
			    } else {
			        tCol.setPreferredWidth(80);
			    }
			}
			return v;
		}

	public static interface CalculadorColumna {
		public Object calcula( TablaObject to, int fila );
	}
	
	public static enum TipoEstad { MEDIANA, MEDIA };
	
	// Atributos heredados
	// private ArrayList<String> headers; // Nombres de las cabeceras-columnas
	// private ArrayList<ArrayList<String>> data; // Datos de la tabla (en el orden de las columnnas), implementados todos como strings
	// private ArrayList<Class<?>> types; // Tipos de cada una de las columnas (inferidos de los datos strings)
	// private List<? extends ConvertibleEnTabla> listaDatos; // Lista de datos (si se usa esta no se usa data - son alternativos)
	// 
	// public static SimpleDateFormat sdf = new SimpleDateFormat( "dd/MM/yyyy HH:mm:ss" );
	// public static SimpleDateFormat sdfDMY = new SimpleDateFormat( "dd/MM/yyyy" );
	// public static SimpleDateFormat sdfDM = new SimpleDateFormat( "dd/MM" );

	// =================================================
	// Atributos nuevos
	private ArrayList<ArrayList<Object>> dataO; // Datos de la tabla (en el orden de las columnnas), implementados todos como objects (en lugar de strings)

	// =================================================
	// Métodos

	/** Crea una tabla de datos vacía con cabeceras
	 * @param headers	Nombres de las cabeceras de datos
	 */
	public TablaObject( ArrayList<String> headers ) {
		super( headers );
		dataO = new ArrayList<>();
		types = new ArrayList<>();
	}
	
	/** Crea una tabla de datos vacía (sin cabeceras ni datos)
	 */
	public TablaObject() {
		this( new ArrayList<String>() );
	}
	
	/** Crea una tabla de datos vacía (sin cabeceras ni datos)
	 * @param capacidad	Capacidad inicial de la tabla
	 */
	public TablaObject( int capacidad ) {
		super( new ArrayList<String>() );
		dataO = new ArrayList<>( capacidad );
		types = new ArrayList<>();
	}
	
	/** Borra la tabla de datos (mantiene las cabeceras y los tipos)
	 */
	public void clear() {
		if (dataO!=null) dataO.clear();
	}
	
	/** Devuelve la fila donde se encuentra el primer código buscado 
	 * @param codigo	Código que se busca
	 * @param numCol	Columna de código
	 * @return	Fila donde está el código, -1 si no se encuentra
	 */
	public int searchFilaO( Object codigo, int numCol ) {
		for (int fila=0; fila<size(); fila++) {
			if (codigo.equals( getO( fila, numCol ) )) {
				return fila;
			}
		}
		return -1;
	}
	
	/** Añade una columna al final de las existentes
	 * @param header	Nuevo nombre de cabecera para la columna
	 * @param defaultValue	Valor por defecto para asignar a cada fila existente en esa nueva columna
	 */
	public void addColumnO( String header, Object defaultValue ) {
		headers.add( header );
		if (dataO!=null) {
			for (ArrayList<Object> line : dataO) {
				line.add( defaultValue );
			}
			if (types!=null) {
				types.add( defaultValue.getClass() );
			}
		} else {
			super.addColumn( header, defaultValue==null ? null : defaultValue.toString() );
		}
	}
	
	/** Añade una columna al final de las existentes, con valores nulos
	 * @param header	Nuevo nombre de cabecera para la columna
	 * @param tipo	Tipo de esa nueva columna
	 */
	public void addColumnO( String header, Class<?> tipo ) {
		if (dataO==null) return;
		if (types==null) calcTypes();
		headers.add( header );
		types.add( tipo );
		if (dataO!=null)
			for (ArrayList<Object> line : dataO) {
				line.add( null );
			}
	}
	
	/** Añade una línea de datos al final de la tabla (no hace nada si la tabla es una tabla enlazada a lista)
	 * @param line	New data line
	 */
	public void addDataLineO( ArrayList<Object> line ) {
		if (dataO!=null) dataO.add( line );
	}
	
	/** Añade una línea de datos vacía (rellena a nulos) al final de la tabla
	 */
	public void addDataLineO() {
		if (dataO!=null) {
			ArrayList<Object> line = new ArrayList<>();
			for (int i=0; i<headers.size(); i++) line.add( null );
			dataO.add( line );
		}
	}
	
	/** Añade a la actual las líneas de valores de una segunda tabla
	 * @param tabla2	Segunda tabla. Solo se añaden aquellos valores cuyas columnas ya existan en la tabla original
	 */
	public void addLines( TablaObject tabla2 ) {
		if (tabla2.dataO==null || dataO==null) return;
		for (int fila = 0; fila<tabla2.dataO.size(); fila++) {
			addDataLineO();
			for (int col=0; col<tabla2.headers.size(); col++) {
				String nomCol = tabla2.headers.get(col);
				if (headers.contains(nomCol)) {
					setO( nomCol, tabla2.getO( fila, col ) );
				}
			}
		}
	}
	
	/** Devuelve tamaño de la tabla (número de filas de datos)
	 * @return	Número de filas de datos, 0 si no hay ninguno
	 */
	@Override
	public int size() {
		if (dataO!=null) return dataO.size();
		else return super.size();
	}
	
	/** Devuelve un valor de dato de la tabla
	 * @param row	Número de fila
	 * @param col	Número de columna
	 * @return	Dato de ese valor
	 */
	@Override
	public String get( int row, int col ) {
		if (dataO!=null) {
			Object o = dataO.get( row ).get( col );
			return o==null ? null : o.toString();
		} else {
			return super.get( row, col );
		}
	}
	
	/** Devuelve un valor de dato de la tabla
	 * @param row	Número de fila
	 * @param col	Número de columna
	 * @return	Dato de ese valor
	 */
	public Object getO( int row, int col ) {
		if (dataO!=null) return dataO.get( row ).get( col );
		else return super.get( row, col );
	}
	
	/** Devuelve un valor de dato de la tabla
	 * @param row	Número de fila
	 * @param nomCol	Nombre de columna
	 * @return	Dato de ese valor
	 */
	public Object getO( int row, String nomCol ) {
		int col = headers.indexOf( nomCol );
		if (col==-1) throw new IndexOutOfBoundsException( "Columna no encontrada: " + nomCol );
		return getO( row, col );
	}
	
	/** Devuelve un valor de dato de la tabla en forma de entero
	 * @param row	Número de fila
	 * @param col	Número de columna
	 * @return	Dato de ese valor, -1 si es un entero incorrecto
	 */
	@Override
	public int getInt( int row, int col ) {
		if (dataO!=null) {
			Object o = dataO.get( row ).get( col );
			if (o instanceof Integer) {
				return (Integer) o;
			} else {
				return -1;
			}
		} else {
			return super.getInt( row, col );
		}
	}
	
	/** Devuelve un valor de dato de la tabla en forma de doble
	 * @param row	Número de fila
	 * @param col	Número de columna
	 * @return	Dato de ese valor, NaN si es un doble incorrecto
	 */
	@Override
	public double getDouble( int row, int col ) {
		if (dataO!=null) {
			Object o = dataO.get( row ).get( col );
			if (o instanceof Double) {
				return (Double) o;
			} else {
				return Double.NaN;
			}
		} else {
			return super.getDouble( row, col );
		}
	}
	
	/** Devuelve un valor de dato de la tabla en forma de fecha dd/mm/aaaa
	 * @param row	Número de fila
	 * @param col	Número de columna
	 * @return	Dato de ese valor, null si es una fecha incorrecta
	 */
	@Override
	public Date getDate( int row, int col ) {
		if (dataO!=null) {
			Object o = dataO.get( row ).get( col );
			if (o instanceof Date) {
				return (Date) o;
			} else {
				return null;
			}
		} else {
			return super.getDate( row, col );
		}
	}
	
	/** Modifica un valor de dato de la tabla
	 * @param row	Número de fila
	 * @param col	Número de columna
	 * @param value	Valor a modificar en esa posición
	 */
	@Override
	public void set( int row, int col, String value ) {
		if (dataO!=null) {
			dataO.get( row ).set( col, value );
			cambioEnTabla( row, col, row, col );
		} else {
			super.set( row, col, value );
		}
	}
	
	/** Modifica un valor de dato de la tabla
	 * @param row	Número de fila
	 * @param col	Número de columna
	 * @param value	Valor a modificar en esa posición
	 */
	public void setO( int row, int col, Object value ) {
		if (dataO!=null) {
			if (types.size()>=col+1) {
				if (value!=null && !types.get(col).isAssignableFrom( value.getClass() )) {
					// Control de posible conversión de tipos String a entero o doble
					boolean error = true;
					if (types.get(col).getSimpleName().equals("Integer") && value instanceof String) {
						try {
							int entero = Integer.parseInt( (String)value );
							value = new Integer( entero );
							error = false;
						} catch (NumberFormatException e) {}
					} else if (types.get(col).getSimpleName().equals("Double") && value instanceof String) {
						try {
							double doble = Double.parseDouble( (String)value );
							value = new Double( doble );
							error = false;
						} catch (NumberFormatException e) {}
					}
					if (error)
						System.err.println( "Error en setO: intentando asignar (" + row + "," + col + ") <- [" + value + "] un " + value.getClass().getSimpleName() + " en un " + types.get(col).getSimpleName() + " en columna " + col );
				}
			}
			dataO.get( row ).set( col, value );
			cambioEnTabla( row, col, row, col );
		} else {
			super.set( row, col, value==null ? null : value.toString() );
		}
	}
	
	/** Modifica un valor de dato de la tabla
	 * @param row	Número de fila
	 * @param nomCol	Nombre de columna
	 * @param value	Valor a modificar en esa posición
	 */
	public void setO( int row, String nomCol, Object value ) {
		if (dataO!=null) {
			int col = headers.indexOf( nomCol );
			if (col==-1) throw new IndexOutOfBoundsException( "Columna no encontrada: " + nomCol );
			if (types.size()>=col+1) {
				if (value!=null && !types.get(col).isAssignableFrom( value.getClass() )) {
					// Control de posible conversión de tipos String a entero o doble
					boolean error = true;
					if (types.get(col).getSimpleName().equals("Integer") && value instanceof String) {
						try {
							int entero = Integer.parseInt( (String)value );
							value = new Integer( entero );
							error = false;
						} catch (NumberFormatException e) {}
					} else if (types.get(col).getSimpleName().equals("Double") && value instanceof String) {
						try {
							double doble = Double.parseDouble( (String)value );
							value = new Double( doble );
							error = false;
						} catch (NumberFormatException e) {}
					}
					if (error)
						System.err.println( "Error en setO: intentando asignar [" + value + "] un " + value.getClass().getSimpleName() + " en un " + types.get(col).getSimpleName() + " en columna " + nomCol );
				}
			}
			dataO.get( row ).set( col, value );
			cambioEnTabla( row, col, row, col );
		}
	}
	
	/** Modifica un valor de dato de la tabla de la última fila añadida
	 * @param nomCol	Nombre de columna
	 * @param value	Valor a modificar en esa posición
	 */
	public void setO( String nomCol, Object value ) {
		setO( dataO.size()-1, nomCol, value );
	}
	
	/** Devuelve una fila completa de la tabla
	 * @param row	Número de fila
	 * @return	Lista de valores de esa fila
	 */
	@Override
	public ArrayList<String> getFila( int row ) {
		if (dataO!=null) {
			ArrayList<String> ret = new ArrayList<>();
			for (Object o : dataO.get(row)) {
				ret.add( o==null ? null : o.toString() );
			}
			return ret;
		} else {
			return super.getFila( row );
		}
	}
	
	/** Devuelve una fila completa de la tabla
	 * @param row	Número de fila
	 * @return	Lista de valores de esa fila
	 */
	public ArrayList<Object> getFilaO( int row ) {
		if (dataO!=null) {
			return dataO.get(row);
		} else {
			ArrayList<Object> ret = new ArrayList<>();
			for (String s : super.getFila(row)) {
				ret.add( s );
			}
			return ret;
		}
	}
	
	/** Quita una fila completa de la tabla
	 * @param row	Número de fila a quitar
	 */
	public void removeFilaO( int row ) {
		if (dataO!=null) {
			dataO.remove( row );
		}
	}
	
	/** Procesa tabla de datos con los datos ya existentes. Calcula los tipos de datos (inferidos de los valores)
	 * @return	0 si el proceso es correcto, otro valor si se detecta algún error (número de líneas de datos erróneas - no hay el mismo número de datos que número de cabeceras)
	 */
	@Override
	public int calcTypes() {
		if (dataO!=null) {
			// 1.- calcs errors
			int numErrs = 0;
			int lin = 1;
			if (data!=null) {
				for (ArrayList<Object> line : dataO) if (line.size()!=headers.size()) {
					numErrs++;
					if (LOG_CONSOLE_CSV) {
						System.out.println( "Error en línea " + lin + ": " + line.size() + " valores en vez de " + headers.size() );
					}
					lin++;
				}
			}
			// 2.- calcs data types (if not error)
			if (numErrs>0) return numErrs;
			types = new ArrayList<>();
			for (int col=0; col<headers.size(); col++) {
				Class<?> tipo = Object.class;  // Object por defecto
				for (int fila=0; fila<dataO.size(); fila++) {
					Object o = dataO.get(fila).get(col);
					if (o!=null) {
						tipo = o.getClass();
						break;
					}
				}
				types.add( tipo );
			}
			return 0;
		} else {
			return super.calcTypes();
		}
	}

		
	// =================================================
	// toString
		
	@Override
	public String toString() {
		if (dataO!=null) {
			String ret = "";
			boolean ini = true;
			for (String header : headers) {
				if (!ini) ret += "\t";
				ret += header;
				ini = false;
			}
			ret += "\n";
			for (ArrayList<Object> lin : dataO) {
				ini = true;
				for (Object val : lin) {
					if (!ini) ret += "\t";
					ret += val;
					ini = false;
				}
				ret += "\n";
			}
			return ret;
		} else {
			return super.toString();
		}
	}
	
	/** Genera un fichero csv (codificado UTF-8) partiendo de los datos actuales de la tabla
	 * @param file	Fichero de salida
	 * @param comasEnVezDePuntos	Si se pone true, los dobles se generan con coma decimal en vez de punto decimal
	 * @throws IOException
	 */
	public void generarCSV( File file, boolean comasEnVezDePuntos ) 
	throws IOException // Error de E/S
	{
		PrintStream ps = new PrintStream( file, "UTF-8" );
		for (int i=0; i<headers.size()-1; i++) {
			ps.print( headers.get(i) + ";" );
		}
		ps.println( headers.get(headers.size()-1) );
		for (int lin=0; lin<size(); lin++) {
			for (int col=0; col<getWidth(); col++) {
				Object o = get( lin, col );
				if (o==null) o = " ";
				else if (o instanceof Double && comasEnVezDePuntos) o = o.toString().replaceAll( "\\.", "," );
				ps.print( o + (col==getWidth()-1 ? "" : ";") );
			}
			ps.println();
		}
		ps.close();
	}
	
	/** Genera un fichero csv (codificado UTF-8) partiendo de los datos actuales de la tabla
	 * @param file	Fichero de salida
	 * @param cabeceras	Cabeceras a generar para el csv
	 * @param genLin	Opcional, si se indica y la tabla es enlazada, la línea csv se genera desde este método en lugar de desde los datos de la tabla.
	 * @throws IOException
	 */
	public void generarCSV( File file, String[] cabeceras, GenerarLineaCSVO... genLin ) 
	throws IOException // Error de E/S
	{
		if (dataO!=null) {
			PrintStream ps = new PrintStream( file, "UTF-8" );
			for (int i=0; i<cabeceras.length-1; i++) {
				ps.print( cabeceras[i] + ";" );
			}
			ps.println( cabeceras[cabeceras.length-1] );
			for (int lin=0; lin<size(); lin++) {
				if (listaDatos!=null && genLin.length>0) {
					String linea = genLin[0].generaLinea( dataO.get( lin ) );
					ps.println( linea );
				} else {
					for (int col=0; col<getWidth()-1; col++) {
						Object o = get( lin, col );
						String dato = o==null ? "" : o.toString();
						if (o!=null && (o instanceof Double || o instanceof Float)) {
							dato = dato.replaceAll( "\\.", "," );  // , españolas en lugar de . decimal
						}
						ps.print( dato + ";" );
					}
					ps.println( get( lin, getWidth()-1 ) );
				}
			}
			ps.close();
		}
	}
		public static interface GenerarLineaCSVO { public String generaLinea( ArrayList<Object> linea ); }
	
	
	// =================================================
	// Métodos relacionados con el modelo de tabla (cuando se quiere utilizar esta tabla en una JTable)
	
	/** Devuelve un modelo de tabla de este objeto tabla para poderse utilizar como modelo de datos en una JTable
	 * @return	modelo de datos de la tabla
	 */
	@Override
	public TablaTableModel getTableModel() {
		if (miModelo==null) {
			miModelo = new TablaTableModelO();
		}
		return miModelo;
	}
		
	public class TablaTableModelO extends TablaTableModel {
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return getO(rowIndex,columnIndex);
		}
	}

	
	// =================================================
	// Métodos estadísticos
	
	/** Calcula estadísticos básicos para los valores de la tabla
	 * @param nomColD	Columna inicial desde la que tomar valores (inclusive). Solo se consideran los valores no nulos Integer, Long o Double
	 * @param nomColH	Columna final hasta la que tomar valores (inclusive). Solo se consideran los valores no nulos Integer, Long o Double
	 * @return	Array de doubles: [ N, media, mediana, desv.típica, mínimo, primerCuartil, segundoCuartil, tercerCuartil, máximo ]<br>
	 * 			Si no hay valores o si algún nombre de columna es incorrecto, se devuelve null<br>
	 * 			Apunte: la desv.típica se calcula dividiendo entre N-1 si N es mayor que 10, entre N si N es 10 o menor
	 */
	public double[] getEstadValores( String nomColD, String nomColH ) {
		if (dataO==null) return null;
		ArrayList<Double> lValores = new ArrayList<>();
		int colD = headers.indexOf( nomColD );
		int colH = headers.indexOf( nomColH );
		if (colD==-1 || colH==-1) return null;
		for (int fila=0; fila<dataO.size(); fila++) {
			for (int col=colD; col<=colH; col++) {
				Object valor = getO( fila, col );
				if (valor!=null) {
					if (valor instanceof Integer) lValores.add( new Double((Integer)valor) );
					else if (valor instanceof Long) lValores.add( new Double((Long)valor) );
					else if (valor instanceof Double) lValores.add( (Double)valor );
				}
			}
		}
		if (lValores.size()==0) return null;
		Collections.sort( lValores );
		return getEstadValores( lValores );
	}
	
	/** Calcula estadísticos básicos para los valores de una lista de números
	 * @param lValoresOrdenados	Lista de valores YA ORDENADOS
	 * @return	Array de doubles: [ N, media, mediana, desv.típica, mínimo, primerCuartil, segundoCuartil, tercerCuartil, máximo ]<br>
	 * 			Si no hay valores o si algún nombre de columna es incorrecto, se devuelve null<br>
	 * 			Apunte: la desv.típica se calcula dividiendo entre N-1 si N es mayor que 10, entre N si N es 10 o menor
	 */
	public static double[] getEstadValores( ArrayList<Double> lValoresOrdenados ) {
		if (lValoresOrdenados.size()==0) return null;
		double[] ret = new double[9];
		int n = lValoresOrdenados.size();
		ret[0] = n;  // N
		double suma = 0; for (double d : lValoresOrdenados) suma += d;
		ret[1] = suma / n;  // media
		ret[2] = (n%2==0) ? (lValoresOrdenados.get(n/2) + lValoresOrdenados.get(n/2-1))/2.0 : lValoresOrdenados.get(n/2);  // mediana
		double sumaCuadsDist = 0;
		for (double d : lValoresOrdenados) { double distAMedia = ret[1] - d; sumaCuadsDist += distAMedia * distAMedia; }
		final double DIVISOR= (n>10) ? (n-1) : n;
		ret[3] = Math.sqrt( sumaCuadsDist / DIVISOR );  // desviación típica
		ret[4] = lValoresOrdenados.get(0);  // min
		ret[5] = lValoresOrdenados.get(n/4);  // primer cuartil
		ret[6] = lValoresOrdenados.get(2*n/4);  // segundo cuartil
		ret[7] = lValoresOrdenados.get(3*n/4);  // tercer cuartil
		ret[8] = lValoresOrdenados.get(n-1);  // max
		return ret;
	}
	
	/** Añade a la tabla una nueva columna y realiza los cálculos de sus valores en todas las filas existentes
	 * @param colNueva	Nombre de la columna nueva
	 * @param tipoCol	Tipo de la columna nueva
	 * @param calc	Función de cálculo de los valores de esa columna
	 */
	public void addCalcColumn(String colNueva, Class<?> tipoCol, CalculadorColumna calc) {
		addColumnO( colNueva, tipoCol );
		for (int fila=0; fila<dataO.size(); fila++) {
			setO( fila, colNueva, calc.calcula( this, fila ) );
		}
	}

	/** Crea una nueva tabla filtro de la actual
	 * @param columnaFiltro	Columna por la que se filtra
	 * @param aMayusculas	Si es true considera los strings pasados a mayúsculas (si es false, los diferencia por capitalización)
	 * @param valor1	Valor de filtro. Si es el único, es por igual. Si es el primero, es por mayor o igual a él
	 * @param valor2	Valor de filtro, por menor o igual a él. Si es nulo solo se considera el primero
	 * @param colsADejar	Si no se indica, se copian todas las columnas. Si se indica, solo se incluyen esas
	 * @return	tabla filtrada nueva
	 */
	public TablaObject creaTablaFiltro( String columnaFiltro, boolean aMayusculas, Object valor1, Object valor2, String... colsADejar ) {
		if (aMayusculas && valor1!=null && valor1 instanceof String) valor1 = valor1.toString().toUpperCase();
		if (aMayusculas && valor2!=null && valor2 instanceof String) valor2 = valor2.toString().toUpperCase();
		// 1. Crear estructura de tabla
		if (dataO==null) return null;
		if (types==null) calcTypes();
		if (colsADejar.length==0) {
			ArrayList<String> lC = new ArrayList<>( headers );
			colsADejar = new String[ lC.size() ];
			for (int i=0; i<lC.size(); i++) colsADejar[i] = lC.get(i);
		}
		TablaObject to = new TablaObject();
		for (String col : colsADejar) {
			int colN = headers.indexOf( col );
			if (colN==-1) return null;  // Columna incorrecta
			to.addColumnO( col, types.get(colN) );
		}
		// 2. Llenar tabla con los valores filtrados
		for (int fila=0; fila<dataO.size(); fila++) {
			Object o = getO( fila, columnaFiltro );
			if (aMayusculas && o instanceof String) o = ((String)o).toUpperCase();
			boolean enFiltro = false;
			if (valor2==null) {  // Por valor == con valor1
				enFiltro = (o==null && valor1==null) || (o!=null && o.equals(valor1));
			} else {  // Por valor1 <= valor <= valor2
				if (valor1!=null) {
					if (o!=null) {
						if (o instanceof Integer && valor1 instanceof Integer && valor2 instanceof Integer) {
							enFiltro = ((Integer) o >= (Integer) valor1) && ((Integer) o <= (Integer) valor2);
						} else if (o instanceof Double && valor1 instanceof Double && valor2 instanceof Double) {
							enFiltro = ((Double) o >= (Double) valor1) && ((Double) o <= (Double) valor2);
						} else {
							enFiltro = (o.toString().compareTo(valor2.toString())>=0 && o.toString().compareTo(valor2.toString())<=0);
						}
					}
				}
			}
			if (enFiltro) {
				to.addDataLineO();
				for (String col : colsADejar) {
					Object valor = getO( fila, col );
					to.setO( col, valor );
				}
			}
		}
		return to;
	}

	private double[] valoresEstadisticos;
	private double[] nEstadisticos;
	/** Devuelve los valores estadísticos de la tabla estadística recién creada (tras llamar a {@link #creaTablaEstad(TipoEstad, String, String, boolean, String...)} o {@link #creaTablaPorc(String, String, Object, String[])}) */
	public double[] getValoresEstadisticos() { return valoresEstadisticos; }
	/** Devuelve los valores estadísticos de las N de la tabla estadística recién creada (tras llamar a {@link #creaTablaEstad(TipoEstad, String, String, boolean, String...)} o {@link #creaTablaPorc(String, String, Object, String[])}) */
	public double[] getValoresEstadisticosN() { return nEstadisticos; }
	
	/** Crea una nueva tabla estadística desde la actual
	 * @param tipo	Tipo de estadística a calcular
	 * @param columnaAgrup	Columna cuyos valores se utilizan como agrupación (filas posteriores)
	 * @param anyadirN	Nombre de la columna a añadir con el tamaño de cada segmentación (y se añade también una columna con ese prefijo y un guión de cada columna de datos), null o string vacío si no se quiere añadir esta columna
	 * @param incluirNull	Incluir null en los valores de agrupación (true) o no (false)
	 * @param colsCalculadas	Columnas que se quieren calcular con estadística (columnas posteriores), si no se indica se incluyen todas las numéricas (int o double)
	 * @return	tabla estadística nueva
	 */
	public TablaObject creaTablaEstad( TipoEstad tipo, String columnaAgrup, String anyadirN, boolean incluirNull, String... colsCalculadas ) {
		// 1. Crear estructura de tabla
		if (dataO==null) return null;
		int colAgrup = headers.indexOf( columnaAgrup );
		if (colAgrup==-1) return null;
		if (types==null) calcTypes();
		if (colsCalculadas.length==0) {
			ArrayList<String> lC = new ArrayList<>();
			for (int i=0; i<types.size(); i++) {
				Class<?> c = types.get(i);
				String n = headers.get(i);
				if (!n.equals(columnaAgrup) && (c.equals(Integer.class) || c.equals(Double.class))) {  // Si no es la columna de agrupación y es numérica se añade
					lC.add( n );
				}
			}
			colsCalculadas = new String[ lC.size() ];
			for (int i=0; i<lC.size(); i++) colsCalculadas[i] = lC.get(i);
		}
		TablaObject to = new TablaObject();
		to.addColumnO( columnaAgrup, String.class ); // Para la cabecera se usa String, no se respeta el original types.get(colAgrup) );
		for (String col : colsCalculadas) {
			to.addColumnO( col, Double.class );
		}
		// 2. Calcular los valores de segmentación
		ArrayList<Object> valoresSeg = new ArrayList<>();
		for (int fila=0; fila<dataO.size(); fila++) {
			Object o = getO(fila,columnaAgrup);
			if (o!=null || incluirNull) {
				if (!valoresSeg.contains(o)) valoresSeg.add( o );
			}
		}
		valoresSeg.sort( new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				if (o1==null) if (o2==null) return 0; else return -1;
				else if (o1 instanceof Integer) {
					if (o2!=null && o2 instanceof Integer) {
						return ((Integer)o1).compareTo( (Integer)o2 );
					} else {
						if (o2==null) return +1;
						return -1;
					}
				} else {
					if (o2!=null) {
						return o1.toString().compareTo( o2.toString() );
					} else {
						return +1;
					}
				}
			}
		});
		// 2b. Añadir columna N si procede
		if (anyadirN!=null && !anyadirN.isEmpty()) {
			to.addColumnO( anyadirN, Integer.class );
			for (String col : colsCalculadas) {
				to.addColumnO( anyadirN + "-" + col, Integer.class );
			}
		}
		// 3. Calcular los estadísticos para la tabla
		ArrayList<Double> listaValores = new ArrayList<>();
		ArrayList<Double> listaNs = new ArrayList<>();
		for (Object valorSeg : valoresSeg) {
			to.addDataLineO();
			to.setO( columnaAgrup, valorSeg==null ? "null" : valorSeg.toString() );  // La cabecera se convierte a String
			int n = 0;
			for (String col : colsCalculadas) {
				n = 0;
				int nCol = 0;
				// Calculamos todos los valores de cada columna y segmentación
				ArrayList<Double> vals = new ArrayList<Double>();
				for (int fila=0; fila<dataO.size(); fila++) {
					Object seg = getO(fila,columnaAgrup);
					if (valorSeg==null && seg==null || (valorSeg!=null && seg!=null && valorSeg.toString().equals(seg.toString()))) {  // La cabecera se compara por String
						n++;
						Object o = getO(fila,col);
						if (o!=null) {
							nCol++;
							if (o instanceof Integer) vals.add( new Double(((Integer) o).intValue()) );
							else if (o instanceof Long) vals.add( new Double(((Long) o).longValue()) );
							else if (o instanceof Double) vals.add( (Double) o );
						}
					}
				}
				if (anyadirN!=null && !anyadirN.isEmpty()) {
					to.setO( anyadirN + "-" + col, nCol );
					listaNs.add( new Double(nCol) );
				}
				// Se calcula el estadístico
				Double estad = null;
				if (vals.size()>0) {
					if (tipo==TipoEstad.MEDIA) {
						double suma = 0;
						for (double d : vals) suma += d;
						estad = suma / vals.size();
					} else if (tipo==TipoEstad.MEDIANA) {
						Collections.sort( vals );
						if (vals.size()%2==0) {  // Nº par de valores: media de los internos
							estad = (vals.get( vals.size()/2 ) + vals.get( vals.size()/2 - 1 ))/2.0;
						} else { // Nº impar de valores: mediana pura
							estad = vals.get( vals.size()/2 );
						}
					}
				}
				// Se pone el estadístico en la tabla
				if (estad!=null) {
					to.setO( col, estad );
					listaValores.add( estad );
				}
			}
			if (anyadirN!=null && !anyadirN.isEmpty()) {
				to.setO( anyadirN, n );
				listaNs.add( new Double( n ) );
			}
		}
		// Last.- (interno) Calcular estadísticos de valores y Ns
		Collections.sort( listaValores );
		Collections.sort( listaNs );
		valoresEstadisticos = getEstadValores( listaValores );
		nEstadisticos = getEstadValores( listaNs );
		return to;
	}
	
	/** Crea una nueva tabla estadística desde la actual
	 * @param tipo	Tipo de estadística a calcular
	 * @param columnaAgrup	Columna cuyos valores se utilizan como agrupación (filas posteriores)
	 * @param anyadirN	Nombre de la columna a añadir con el tamaño de cada segmentación (y se añade también una columna con ese prefijo y un guión de cada columna de datos), null o string vacío si no se quiere añadir esta columna
	 * @param incluirNull	Incluir null en los valores de agrupación (true) o no (false)
	 * @param colsFiltro	Columnas que se van a parear para filtrar (mismo número de columnas que colsCalculadas)
	 * @param valFiltro	Valor que se compara con cada una de las columnas de filtro. Si el valor es distinto, no se considera en el estadístico
	 * @param colsCalculadas	Columnas que se quieren calcular con estadística (columnas posteriores), si no se indica se incluyen todas las numéricas (int o double)
	 * @return	tabla estadística nueva
	 */
	public TablaObject creaTablaEstadFiltrada( TipoEstad tipo, String columnaAgrup, String anyadirN, boolean incluirNull, String[] colsFiltro, Object valFiltro, String... colsCalculadas ) {
		// 1. Crear estructura de tabla
		if (dataO==null) return null;
		int colAgrup = headers.indexOf( columnaAgrup );
		if (colAgrup==-1) return null;
		if (types==null) calcTypes();
		if (colsCalculadas.length==0) {
			ArrayList<String> lC = new ArrayList<>();
			for (int i=0; i<types.size(); i++) {
				Class<?> c = types.get(i);
				String n = headers.get(i);
				if (!n.equals(columnaAgrup) && (c.equals(Integer.class) || c.equals(Double.class))) {  // Si no es la columna de agrupación y es numérica se añade
					lC.add( n );
				}
			}
			colsCalculadas = new String[ lC.size() ];
			for (int i=0; i<lC.size(); i++) colsCalculadas[i] = lC.get(i);
		}
		TablaObject to = new TablaObject();
		to.addColumnO( columnaAgrup, String.class ); // Para la cabecera se usa String, no se respeta el original types.get(colAgrup) );
		for (String col : colsCalculadas) {
			to.addColumnO( col, Double.class );
		}
		// 2. Calcular los valores de segmentación
		ArrayList<Object> valoresSeg = new ArrayList<>();
		for (int fila=0; fila<dataO.size(); fila++) {
			Object o = getO(fila,columnaAgrup);
			if (o!=null || incluirNull) {
				if (!valoresSeg.contains(o)) valoresSeg.add( o );
			}
		}
		valoresSeg.sort( new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				if (o1==null) if (o2==null) return 0; else return -1;
				else if (o1 instanceof Integer) {
					if (o2!=null && o2 instanceof Integer) {
						return ((Integer)o1).compareTo( (Integer)o2 );
					} else {
						if (o2==null) return +1;
						return -1;
					}
				} else {
					if (o2!=null) {
						return o1.toString().compareTo( o2.toString() );
					} else {
						return +1;
					}
				}
			}
		});
		// 2b. Añadir columna N si procede
		if (anyadirN!=null && !anyadirN.isEmpty()) {
			to.addColumnO( anyadirN, Integer.class );
			for (String col : colsCalculadas) {
				to.addColumnO( anyadirN + "-" + col, Integer.class );
			}
		}
		// 3. Calcular los estadísticos para la tabla
		ArrayList<Double> listaValores = new ArrayList<>();
		ArrayList<Double> listaNs = new ArrayList<>();
		for (Object valorSeg : valoresSeg) {
			to.addDataLineO();
			to.setO( columnaAgrup, valorSeg==null ? "null" : valorSeg.toString() );  // La cabecera se convierte a String
			int n = 0;
			int nColCalc = 0;
			for (String col : colsCalculadas) {
				int nColFiltro = headers.indexOf( colsFiltro[nColCalc] );
				n = 0;
				int nCol = 0;
				// Calculamos todos los valores de cada columna y segmentación
				ArrayList<Double> vals = new ArrayList<Double>();
				for (int fila=0; fila<dataO.size(); fila++) {
					Object seg = getO(fila,columnaAgrup);
					if (valorSeg==null && seg==null || (valorSeg!=null && seg!=null && valorSeg.toString().equals(seg.toString()))) {  // La cabecera se compara por String
						n++;
						Object o = getO(fila,col);
						Object oFiltro = getO(fila,nColFiltro);
						if (o!=null && ((oFiltro==null && valFiltro==null) || (oFiltro!=null && oFiltro.equals(valFiltro)))) {
							nCol++;
							if (o instanceof Integer) vals.add( new Double(((Integer) o).intValue()) );
							else if (o instanceof Long) vals.add( new Double(((Long) o).longValue()) );
							else if (o instanceof Double) vals.add( (Double) o );
						}
					}
				}
				if (anyadirN!=null && !anyadirN.isEmpty()) {
					to.setO( anyadirN + "-" + col, nCol );
					listaNs.add( new Double(nCol) );
				}
				// Se calcula el estadístico
				Double estad = null;
				if (vals.size()>0) {
					if (tipo==TipoEstad.MEDIA) {
						double suma = 0;
						for (double d : vals) suma += d;
						estad = suma / vals.size();
					} else if (tipo==TipoEstad.MEDIANA) {
						Collections.sort( vals );
						if (vals.size()%2==0) {  // Nº par de valores: media de los internos
							estad = (vals.get( vals.size()/2 ) + vals.get( vals.size()/2 - 1 ))/2.0;
						} else { // Nº impar de valores: mediana pura
							estad = vals.get( vals.size()/2 );
						}
					}
				}
				// Se pone el estadístico en la tabla
				if (estad!=null) {
					to.setO( col, estad );
					listaValores.add( estad );
				}
				nColCalc++;
			}
			if (anyadirN!=null && !anyadirN.isEmpty()) {
				to.setO( anyadirN, n );
				listaNs.add( new Double( n ) );
			}
		}
		// Last.- (interno) Calcular estadísticos de valores y Ns
		Collections.sort( listaValores );
		Collections.sort( listaNs );
		valoresEstadisticos = getEstadValores( listaValores );
		nEstadisticos = getEstadValores( listaNs );
		return to;
	}
	
	/** Crea una nueva tabla estadística de supervivencia desde la actual
	 * @param columnaAgrup	Columna cuyos valores se utilizan como agrupación (filas posteriores)
	 * @param anyadirN	Nombre de la columna a añadir con el tamaño de cada segmentación (y se añade también una columna con ese prefijo y un guión de cada columna de datos), null o string vacío si no se quiere añadir esta columna
	 * @param valorSupervivencia	Valor que se considera como de supervivencia
	 * @param colsCalculadas	Columnas que se quieren calcular con estadística (columnas posteriores)
	 * @return	tabla estadística nueva
	 */
	public TablaObject creaTablaPorc( String columnaAgrup, String anyadirN, Object valorSupervivencia, String[] colsCalculadas ) {
		// 1. Crear estructura de tabla
		if (dataO==null || colsCalculadas.length==0) return null;
		int colAgrup = headers.indexOf( columnaAgrup );
		if (colAgrup==-1) return null;
		if (types==null) calcTypes();
		TablaObject to = new TablaObject();
		to.addColumnO( columnaAgrup, String.class ); // Para la cabecera se usa String, no se respeta el original types.get(colAgrup) );
		for (String col : colsCalculadas) {
			to.addColumnO( col, Double.class );
		}
		// 2. Calcular los valores de segmentación
		ArrayList<Object> valoresSeg = new ArrayList<>();
		for (int fila=0; fila<dataO.size(); fila++) {
			Object o = getO(fila,columnaAgrup);
			if (o!=null) {
				if (!valoresSeg.contains(o)) valoresSeg.add( o );
			}
		}
		valoresSeg.sort( new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				if (o1==null) if (o2==null) return 0; else return -1;
				else if (o1 instanceof Integer) {
					if (o2!=null && o2 instanceof Integer) {
						return ((Integer)o1).compareTo( (Integer)o2 );
					} else {
						if (o2==null) return +1;
						return -1;
					}
				} else {
					if (o2!=null) {
						return o1.toString().compareTo( o2.toString() );
					} else {
						return +1;
					}
				}
			}
		});
		// 2b. Añadir columna N si procede
		if (anyadirN!=null && !anyadirN.isEmpty()) {
			to.addColumnO( anyadirN, Integer.class );
			for (String col : colsCalculadas) {
				to.addColumnO( anyadirN + "-" + col, Integer.class );
			}
		}
		// 3. Calcular los estadísticos para la tabla
		ArrayList<Double> listaValores = new ArrayList<>();
		ArrayList<Double> listaNs = new ArrayList<>();
		for (Object valorSeg : valoresSeg) {
			to.addDataLineO();
			to.setO( columnaAgrup, valorSeg==null ? "null" : valorSeg.toString() );  // La cabecera se convierte a String
			int n = 0;
			int nColAnt = Integer.MAX_VALUE;
			for (String col : colsCalculadas) {
				n = 0;
				int nCol = 0;
				// Calculamos todos los valores de cada columna y segmentación
				for (int fila=0; fila<dataO.size(); fila++) {
					Object seg = getO(fila,columnaAgrup);
					if (valorSeg==null && seg==null || (valorSeg!=null && seg!=null && valorSeg.toString().equals(seg.toString()))) {  // La cabecera se compara por String
						n++;
						Object o = getO(fila,col);
						if (o!=null) {
							if (o.equals(valorSupervivencia)) {
								nCol++;
							}
						}
					}
				}
				if (anyadirN!=null && !anyadirN.isEmpty()) {
					to.setO( anyadirN + "-" + col, nCol );
					listaNs.add( new Double(nCol) );
				}
				if (nColAnt>0) {
					double d = 1.0 * nCol / n;
					to.setO( col, new Double( d ) );
					listaValores.add( new Double( d ) );
				} // else { // null  to.setO( col, null );
			}
			if (anyadirN!=null && !anyadirN.isEmpty()) {
				to.setO( anyadirN, n );
				listaNs.add( new Double(n) );
			}
		}
		// Last.- (interno) Calcular estadísticos de valores y Ns
		Collections.sort( listaValores );
		Collections.sort( listaNs );
		valoresEstadisticos = getEstadValores( listaValores );
		nEstadisticos = getEstadValores( listaNs );
		return to;
	}

	public Object[] getValoresUnicos( String colNom, boolean incluirNull, boolean aMayusculas ) {
		if (dataO==null) return null;
		ArrayList<Object> valoresSeg = new ArrayList<>();
		for (int fila=0; fila<dataO.size(); fila++) {
			Object o = getO(fila, colNom);
			if (o!=null || incluirNull) {
				if (aMayusculas && o instanceof String) o = ((String)o).toUpperCase();
				if (!valoresSeg.contains(o)) valoresSeg.add( o );
			}
		}
		valoresSeg.sort( new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				if (o1==null) if (o2==null) return 0; else return -1;
				else if (o1 instanceof Integer) {
					if (o2!=null && o2 instanceof Integer) {
						return ((Integer)o1).compareTo( (Integer)o2 );
					} else {
						if (o2==null) return +1;
						return -1;
					}
				} else {
					if (o2!=null) {
						return o1.toString().compareTo( o2.toString() );
					} else {
						return +1;
					}
				}
			}
		});
		Object[] ret = new Object[valoresSeg.size()];
		int i = 0;
		for (Object o : valoresSeg) { ret[i] = o; i++; }
		return ret;
	}
			
}
