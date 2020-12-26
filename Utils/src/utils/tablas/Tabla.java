package utils.tablas;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/** Tabla de datos bidimensional de cualquier tipo para análisis posterior
 * Composición: una serie de cabeceras (columnas) con una serie de filas de datos (objetos) que tienen un dato para cada columna
 * @author andoni.eguiluz @ ingenieria.deusto.es
 */
public class Tabla implements Serializable {
	private static final long serialVersionUID = 1L;
	
	// =================================================
	// Atributos
	
	protected ArrayList<String> headers; // Nombres de las cabeceras-columnas
	protected ArrayList<ArrayList<String>> data; // Datos de la tabla (en el orden de las columnnas), implementados todos como strings
	protected ArrayList<Class<?>> types; // Tipos de cada una de las columnas (inferidos de los datos strings)
	protected List<? extends ConvertibleEnTabla> listaDatos; // Lista de datos (si se usa esta no se usa data - son alternativos)
	
	public static SimpleDateFormat sdf = new SimpleDateFormat( "dd/MM/yyyy HH:mm:ss" );
	public static SimpleDateFormat sdfDMY = new SimpleDateFormat( "dd/MM/yyyy" );
	public static SimpleDateFormat sdfDM = new SimpleDateFormat( "dd/MM" );

	// =================================================
	// Métodos

	/** Crea una tabla de datos vacía (sin cabeceras ni datos)
	 */
	public Tabla() {
		headers = new ArrayList<>();
		data = new ArrayList<>();
	}
	
	/** Crea una tabla de datos vacía con cabeceras
	 * @param headers	Nombres de las cabeceras de datos
	 */
	public Tabla( ArrayList<String> headers ) {
		this.headers = headers;
		data = new ArrayList<>();
	}
	
	/** Devuelve la fila donde se encuentra el primer código buscado 
	 * @param codigo	Código que se busca
	 * @param numCols	Columnas que componen el código (concatenadas en ese orden)
	 * @return
	 */
	public int searchFila( String codigo, int... numCols ) {
		for (int fila=0; fila<size(); fila++) {
			String codigoFila = "";
			for (int col : numCols) {
				codigoFila += get( fila, col );
			}
			if (codigo.equals( codigoFila )) {
				return fila;
			}
		}
		return -1;
	}
	
	/** Cambia las cabeceras
	 * @param headers	Nombres de las cabeceras de datos
	 */
	public void setHeaders( ArrayList<String> headers ) {
		this.headers = headers;
	}
	
	/** Cambia una cabecera
	 * @param numCol	Número de columna a modificar
	 * @param header	Nombres de la cabecera de datos de esa columna
	 */
	public void setHeader( int numCol, String header ) {
		headers.set( numCol, header );
	}
	
	/** Añade una columna al final de las existentes
	 * @param header	Nuevo nombre de cabecera para la columna
	 * @param defaultValue	Valor por defecto para asignar a cada fila existente en esa nueva columna
	 */
	public void addColumn( String header, String defaultValue ) {
		headers.add( header );
		if (data!=null)
			for (ArrayList<String> line : data) {
				line.add( defaultValue );
			}
		if (types!=null) {
			types.add( calcDataType( defaultValue ) );
		}
	}
	
	/** Añade una línea de datos al final de la tabla (no hace nada si la tabla es una tabla enlazada a lista)
	 * @param line	New data line
	 */
	public void addDataLine( ArrayList<String> line ) {
		if (data!=null) data.add( line );
	}
	
	/** Chequea si una cabecera particular existe, y devuelve su número de columna
	 * @param header	Nombre de cabecera a encontrar
	 * @param exact	true si el match debe ser exacto, false si vale con que sea parcial
	 * @return	número de la columna de primera cabecera que encaja con el nombre pedido, -1 si no existe ninguna
	 */
	public int getColumnWithHeader( String header, boolean exact ) {
		String headerUp = header.toUpperCase();
		int ret = -1;
		for (int col=0; col<headers.size();col++) {
			if (exact && headers.get(col).equals( header ) || !exact && headers.get(col).toUpperCase().contains( headerUp )) {
				ret = col;
				break;
			}
		}
		return ret;
	}
	
	public ArrayList<String> getHeaders() {
		return headers;
	}
	
	public String getHeader( int col ) {
		return headers.get(col);
	}
	
	/** Devuelve una fila completa de headers separada por tabs
	 * @param prefijo	Prefijo a añadir a las cabeceras
	 * @return	Serie de cabeceras precedidas del prefijo indicado y separadas por tabs y acabadas en tab
	 */
	public String getHeadersTabs( String prefijo ) {
		String ret = "";
		for (int c=0; c<getWidth(); c++) {
			ret += (prefijo + getHeader(c) + "\t");
		}
		return ret;
	}
	
	public ArrayList<Class<?>> getTypes() {
		return types;
	}
	
	public Class<?> getType( int col ) {
		if (types==null || types.size()<=col) return String.class;
		return types.get(col);
	}
	
	/** Devuelve tamaño de la tabla (número de filas de datos)
	 * @return	Número de filas de datos, 0 si no hay ninguno
	 */
	public int size() {
		if (data==null) return listaDatos.size();
		return data.size();
	}
	
	/**  Devuelve el número de columnas de la tabla
	 * @return	Número de columnas
	 */
	public int getWidth() {
		return headers.size();
	}
	
	/** Devuelve un valor de dato de la tabla
	 * @param row	Número de fila
	 * @param col	Número de columna
	 * @return	Dato de ese valor
	 */
	public String get( int row, int col ) {
		if (data==null) return listaDatos.get(row).getValorColumna(col);
		return data.get( row ).get( col );
	}
	
	/** Devuelve un valor de dato de la tabla en forma de entero
	 * @param row	Número de fila
	 * @param col	Número de columna
	 * @return	Dato de ese valor, -1 si es un entero incorrecto
	 */
	public int getInt( int row, int col ) {
		try {
			if (data==null) return Integer.parseInt( listaDatos.get(row).getValorColumna(col) );
			return Integer.parseInt( data.get( row ).get( col ) );
		} catch (Exception e) {
			return -1;
		}
	}
	
	/** Devuelve un valor de dato de la tabla en forma de doble
	 * @param row	Número de fila
	 * @param col	Número de columna
	 * @return	Dato de ese valor, NaN si es un doble incorrecto
	 */
	public double getDouble( int row, int col ) {
		try {
			if (data==null) return Double.parseDouble( listaDatos.get(row).getValorColumna(col) );
			return Double.parseDouble( data.get( row ).get( col ) );
		} catch (Exception e) {
			return Double.NaN;
		}
	}
	
	/** Devuelve un valor de dato de la tabla en forma de fecha dd/mm/aaaa
	 * @param row	Número de fila
	 * @param col	Número de columna
	 * @return	Dato de ese valor, null si es una fecha incorrecta
	 */
	public Date getDate( int row, int col ) {
		try {
			try {
				if (data==null) return sdf.parse( listaDatos.get(row).getValorColumna(col) );
				return sdf.parse( data.get( row ).get( col ) );
			} catch (Exception e) {}
			if (data==null) return sdfDMY.parse( listaDatos.get(row).getValorColumna(col) );
			return sdfDMY.parse( data.get( row ).get( col ) );
		} catch (Exception e) {
			return null;
		}
	}
	
	/** Modifica un valor de dato de la tabla
	 * @param row	Número de fila
	 * @param col	Número de columna
	 * @param value	Valor a modificar en esa posición
	 */
	public void set( int row, int col, String value ) {
		if (data==null) listaDatos.get(row).setValorColumna( col, value );
		else data.get( row ).set( col, value );
		cambioEnTabla( row, col, row, col );
	}
	
	/** Devuelve una fila completa de la tabla
	 * @param row	Número de fila
	 * @return	Lista de valores de esa fila
	 */
	public ArrayList<String> getFila( int row ) {
		if (data==null) {
			ArrayList<String> ret = new ArrayList<>();
			ConvertibleEnTabla c = listaDatos.get(row);
			for (int i=0; i<c.getNumColumnas(); i++) ret.add( c.getValorColumna(i) );
			return ret;
		}
		return data.get(row);
	}
	
	/** Devuelve una fila completa de la tabla separada por tabs y quitando saltos de línea
	 * @param row	Número de fila
	 * @return	Serie de valores de esa fila separados por tabs y acabados en tab
	 */
	public String getFilaTabs( int row ) {
		String ret = "";
		for (int c=0; c<getWidth(); c++) {
			String s = get(row,c);
			if (s==null) s = "";
			ret += (s.replaceAll("\n", "") + "\t");
		}
		return ret;
	}
	
	/** Procesa tabla de datos con los datos ya existentes. Calcula los tipos de datos (inferidos de los valores)
	 * @return	0 si el proceso es correcto, otro valor si se detecta algún error (número de líneas de datos erróneas - no hay el mismo número de datos que número de cabeceras)
	 */
	public int calcTypes() {
		// 1.- calcs errors
		int ok = 0;
		int lin = 1;
		if (data!=null) {
			for (ArrayList<String> line : data) if (line.size()!=headers.size()) {
				ok++;
				if (LOG_CONSOLE_CSV) {
					System.out.println( "Error en línea " + lin + ": " + line.size() + " valores en vez de " + headers.size() );
				}
				lin++;
			}
		}
		// 2.- calcs data types (if not error)
		if (ok>0) return ok;
		types = new ArrayList<>();
		for (int col=0; col<headers.size(); col++) {
			Class<?> type = String.class;
			if (size()>0) {
				if (data==null)
					type = calcDataType( listaDatos.get(0).getValorColumna(col));
				else
					type = calcDataType( data.get(0).get(col) );
				if (type!=String.class) { // String is the most general - not more info needed
					for (int row=1; row<(data==null?listaDatos.size():data.size()); row++) {
						Class<?> nextType = null;
						if (data==null)
							nextType = calcDataType( listaDatos.get(row).getValorColumna(col) );
						else
							nextType = calcDataType( data.get(row).get(col) );
						if (type==null) {
							type = nextType;
						} else {
							if (nextType==String.class) { // String is the most general - not more info needed
								type = String.class;
								break;
							} else {
								if (nextType!=null) {
									if (type!=nextType) {
										if (type==Integer.class && nextType==Double.class) {
											type = Double.class;
										} else if (type==Double.class && nextType==Integer.class) {
											// nothing (type = Double)
										} else { // Dif type: must be consdered as string
											type = String.class;
											break;
										}
									}
								}
							}
						}
					}
				}
			}
			types.add( type );
		}
		return ok;
	}
		private Class<?> calcDataType( String value ) {
			if (value.isEmpty()) return null;
			Class<?> ret = String.class;
			try {
				Integer.parseInt( value );
				ret = Integer.class;
			} catch (NumberFormatException e) {
				try {
					Double.parseDouble( value.replaceAll(",", ".") );
					ret = Double.class;
				} catch (NumberFormatException e2) {
					try {
						sdf.parse( value );
						ret = Date.class;
					} catch (ParseException e3) {
					}
				}
			}
			return ret;
		}

		
	/** Informa a la tabla que se ha producido un cambio en los datos internos<br/>
	 * No hace falta hacerlo si se hace directamente con el set de la tabla, pero es necesario si se cambia el modelo enlazado a la tabla ya que de esa forma la tabla no puede saberlo de otra forma
	 * @param fila1	Fila inicial del cambio
	 * @param col1	Columna inicial del cambio
	 * @param fila2	Fila final del cambio (incluida)
	 * @param col2	Columna final del cambio (incluida)
	 */
	public void cambioEnTabla( int fila1, int col1, int fila2, int col2 ) {
		if (miModelo==null || miModelo.lListeners==null) return;
		for (int col=col1; col<=col2; col++) {
			for (TableModelListener l : miModelo.lListeners) {
				l.tableChanged( new TableModelEvent( miModelo, fila1, fila2, col, TableModelEvent.UPDATE ) );
			}
		}
	}
		
	/** Informa a la tabla que se ha producido un cambio en los datos internos<br/>
	 * No hace falta hacerlo si se hace directamente con el set de la tabla, pero es necesario si se cambia el modelo enlazado a la tabla ya que de esa forma la tabla no puede saberlo de otra forma
	 * @param fila1	Fila del cambio
	 * @param tipo	Tipo del cambio (TableModelEvent.UPDATE, TableModelEvent.INSERT, TableModelEvent.DELETE)
	 */
	public void cambioEnTabla( int fila1 ) {
		if (miModelo==null || miModelo.lListeners==null) return;
		for (TableModelListener l : miModelo.lListeners) {
			l.tableChanged( new TableModelEvent( miModelo, fila1, fila1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT ) );
		}
	}
		
		
	// =================================================
	// toString
		
	@Override
	public String toString() {
		String ret = "";
		boolean ini = true;
		for (String header : headers) {
			if (!ini) ret += "\t";
			ret += header;
			ini = false;
		}
		ret += "\n";
		if (data==null) {
			for (ConvertibleEnTabla conv : listaDatos) {
				ini = true;
				for (int c=0; c<conv.getNumColumnas(); c++) {
					String val = conv.getValorColumna(c);
					if (!ini) ret += "\t";
					ret += val;
					ini = false;
				}
				ret += "\n";
			}
		} else {
			for (ArrayList<String> lin : data) {
				ini = true;
				for (String val : lin) {
					if (!ini) ret += "\t";
					ret += val;
					ini = false;
				}
				ret += "\n";
			}
		}
		return ret;
	}
	
	// =================================================
	// Métodos estáticos
	
	/** Crea una tabla partiendo de una colección de valores convertibles
	 * @param vals	Valores a convertir en tabla
	 * @return	Tabla creada partiendo de esos valores
	 */
	public static Tabla createTablaFromColl( Collection<? extends ConvertibleEnTabla> vals  ) {
		Tabla ret = new Tabla();
		if (vals.size()>0) {
			Iterator<? extends ConvertibleEnTabla> it = vals.iterator();
			ConvertibleEnTabla fila = it.next();
			for (int col=0; col<fila.getNumColumnas(); col++) {
				ret.addColumn( fila.getNombreColumna(col), "" );
			}
			do {
				ArrayList<String> linea = new ArrayList<String>();
				for (int col=0; col<fila.getNumColumnas(); col++) {
					linea.add( fila.getValorColumna(col) );
				}
				ret.addDataLine( linea );
				if (it.hasNext()) fila = it.next();
				else fila = null;
			} while (fila!=null);
		}
		return ret;
	}

	/** Crea una tabla enlazada a una lista de valores convertibles
	 * @param vals	Lista de valores a visualizar como tabla, no puede ser null y tiene que tener al menos un valor ya introducido
	 * @return	Tabla enlazada con esos valores, null si hay cualquier error
	 */
	public static Tabla linkTablaToList( List<? extends ConvertibleEnTabla> vals  ) {
		Tabla ret = new Tabla();
		ret.listaDatos = vals;
		ret.data = null;
		if (vals==null || vals.size()==0) {
			return null;
		} else {
			// Crear las columnas
			ConvertibleEnTabla fila = vals.get(0);
			for (int col=0; col<fila.getNumColumnas(); col++) {
				ret.addColumn( fila.getNombreColumna(col), "" );
			}
		}
		return ret;
	}

		protected static boolean LOG_CONSOLE_CSV = false;  // Log en consola del csv
		
	// Método de carga de tabla desde CSV
	/** Procesa un fichero csv (codificado UTF-8) y lo carga devolviéndolo en una nueva tabla
	 * @param file	Fichero del csv
	 * @return	Nuevo objeto tabla con los contenidos de ese csv
	 * @throws IOException
	 */
	public static Tabla processCSV( File file ) 
	throws IOException // Error de I/O
	{
		Tabla tabla = processCSV( file.toURI().toURL() );
		return tabla;
	}
	
	/** Procesa un fichero csv (codificado UTF-8) y lo carga devolviéndolo en una nueva tabla
	 * @param urlCompleta	URL del csv
	 * @param lineaCabs	Por defecto es 1 (número de línea que incluye las cabeceras) pero si es otra se puede indicar (>= 1)
	 * @return	Nuevo objeto tabla con los contenidos de ese csv
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws FileNotFoundException
	 * @throws ConnectException
	 * @return
	 */
	public static Tabla processCSV( URL url, int... lineaCabs ) 
	throws MalformedURLException,  // URL incorrecta 
	 IOException, // Error al abrir conexión
	 UnknownHostException, // servidor web no existente
	 FileNotFoundException, // En algunos servidores, acceso a página inexistente
	 ConnectException // Error de timeout
	{
		int linCab = 1;
		if (lineaCabs.length>0) linCab = lineaCabs[0];
		Tabla table = new Tabla();
		BufferedReader input = null;
		InputStream inStream = null;
		try {
		    URLConnection connection = url.openConnection();
		    connection.setDoInput(true);
		    inStream = connection.getInputStream();
		    input = new BufferedReader(new InputStreamReader( inStream, "UTF-8" ));  // Supone utf-8 en la codificación de texto
		    String line = "";
		    int numLine = 0;
		    while ((line = input.readLine()) != null) {
		    	numLine++;
		    	if (LOG_CONSOLE_CSV) System.out.println( numLine + "\t" + line );
		    	try {
			    	ArrayList<String> l = processCSVLine( input, line, numLine );
			    	if (LOG_CONSOLE_CSV) System.out.println( "\t" + l.size() + "\t" + l );
			    	if (numLine==linCab) {
			    		table.setHeaders( l );
			    	} else {
			    		if (!l.isEmpty())
			    			table.addDataLine( l );
			    	}
		    	} catch (StringIndexOutOfBoundsException e) {
		    		/* if (LOG_CONSOLE_CSV) */ System.err.println( "\tError: " + e.getMessage() );
		    	}
		    }
		} finally {
			try {
				inStream.close();
				input.close();
			} catch (Exception e2) {
			}
		}
		table.calcTypes();
		for (int fila=table.size()-1; fila>=0; fila--) {  // Iteramos de arriba abajo porque vamos a ir quitando filas
			if (table.getFila(fila).size()!=table.headers.size()) {
				table.data.remove( fila );
			}
		}
	    return table;
	}
	
	
	/** Genera un fichero csv (codificado UTF-8) partiendo de los datos actuales de la tabla
	 * @param file	Fichero de salida
	 * @param cabeceras	Cabeceras a generar para el csv
	 * @param genLin	Opcional, si se indica y la tabla es enlazada, la línea csv se genera desde este método en lugar de desde los datos de la tabla.
	 * @throws IOException
	 */
	public void generarCSV( File file, String[] cabeceras, GenerarLineaCSV... genLin ) 
	throws IOException // Error de E/S
	{
		PrintStream ps = new PrintStream( file, "UTF-8" );
		for (int i=0; i<cabeceras.length-1; i++) {
			ps.print( cabeceras[i] + ";" );
		}
		ps.println( cabeceras[cabeceras.length-1] );
		for (int lin=0; lin<size(); lin++) {
			if (listaDatos!=null && genLin.length>0) {
				String linea = genLin[0].generaLinea( listaDatos.get( lin ) );
				ps.println( linea );
			} else {
				for (int col=0; col<getWidth()-1; col++) {
					ps.print( get( lin, col ) + ";" );
				}
				ps.println( get( lin, getWidth()-1 ) );
			}
		}
		ps.close();
	}
		public static interface GenerarLineaCSV { public String generaLinea( ConvertibleEnTabla dato ); }
	
	
	// Método de carga de lista de datos desde CSV
	/** Procesa un fichero csv (codificado UTF-8) y lo carga en una lista
	 * @param file	Fichero del csv
	 * @param lista	Lista ya inicializada a la que se van a añadir los datos cargados del CSV. Compatible con claseACargar
	 * @param claseACargar	tipo de datos que se van a intentar crear desde cada línea del CSV
	 * @return	Número de datos erróneos (líneas CSV que no se han podido crear como objetos de tipo claseACargar
	 * @throws IOException
	 */
	public static int processCSV( File file, ArrayList<? extends CargableDesdeCSV> lista, Class<? extends CargableDesdeCSV> claseACargar ) 
	throws IOException // Error de I/O
	{
		return processCSV( file.toURI().toURL(), lista, claseACargar );
	}
	
	/** Procesa un fichero csv (codificado UTF-8) y lo carga en una lista
	 * @param urlCompleta	URL del csv
	 * @param lista	Lista ya inicializada a la que se van a añadir los datos cargados del CSV. Compatible con claseACargar
	 * @param claseACargar	tipo de datos que se van a intentar crear desde cada línea del CSV
	 * @param lineaCabs	Por defecto es 1 (número de línea que incluye las cabeceras) pero si es otra se puede indicar (>= 1)
	 * @return	Número de datos erróneos (líneas CSV que no se han podido crear como objetos de tipo claseACargar
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws FileNotFoundException
	 * @throws ConnectException
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static int processCSV( URL url, ArrayList<? extends CargableDesdeCSV> lista, Class<? extends CargableDesdeCSV> claseACargar, int... lineaCabs ) 
	throws MalformedURLException,  // URL incorrecta 
	 IOException, // Error al abrir conexión
	 UnknownHostException, // servidor web no existente
	 FileNotFoundException, // En algunos servidores, acceso a página inexistente
	 ConnectException // Error de timeout
	{
		int linCab = 1;
		if (lineaCabs.length>0) linCab = lineaCabs[0];
		ArrayList<String> headers = null;
		ArrayList<ArrayList<String>> lineas = new ArrayList<>();
		BufferedReader input = null;
		InputStream inStream = null;
		try {
		    URLConnection connection = url.openConnection();
		    connection.setDoInput(true);
		    inStream = connection.getInputStream();
		    input = new BufferedReader(new InputStreamReader( inStream, "UTF-8" ));  // Supone utf-8 en la codificación de texto
		    String line = "";
		    int numLine = 0;
		    while ((line = input.readLine()) != null) {
		    	numLine++;
		    	if (LOG_CONSOLE_CSV) System.out.println( numLine + "\t" + line );
		    	try {
			    	ArrayList<String> l = processCSVLine( input, line, numLine );
			    	if (LOG_CONSOLE_CSV) System.out.println( "\t" + l.size() + "\t" + l );
			    	if (numLine==linCab) {
			    		headers = l;
			    	} else {
			    		if (!l.isEmpty())
			    			lineas.add( l );
			    	}
		    	} catch (StringIndexOutOfBoundsException e) {
		    		/* if (LOG_CONSOLE_CSV) */ System.err.println( "\tError: " + e.getMessage() );
		    	}
		    }
		} finally {
			try {
				inStream.close();
				input.close();
			} catch (Exception e2) {
			}
		}
		if (headers==null) return lineas.size(); // Si no hay cabeceras no se puede procesar
		int errores = 0;
		for (ArrayList<String> linea : lineas) {
			try {
				CargableDesdeCSV objeto = claseACargar.newInstance();
				boolean ok = objeto.cargaDesdeCSV( headers, linea );
				if (!ok) { 
					errores++;  // Si hay error se incrementa el contador
				} else {
					((ArrayList<CargableDesdeCSV>) lista).add( objeto );  // Y si no lo hay se añade a la lista
				}
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				errores++;
			}
		}
	    return errores;
	}
	
	
	
		/** Procesa una línea de entrada de csv	
		 * @param input	Stream de entrada ya abierto
		 * @param line	La línea YA LEÍDA desde input
		 * @param numLine	Número de línea ya leída
		 * @return	Lista de strings procesados en el csv. Si hay algún string sin acabar en la línea actual, lee más líneas del input hasta que se acaben los strings o el input
		 * @throws StringIndexOutOfBoundsException
		 */
		public static ArrayList<String> processCSVLine( BufferedReader input, String line, int numLine ) throws StringIndexOutOfBoundsException {
			ArrayList<String> ret = new ArrayList<>();
			int posCar = 0;
			boolean inString = false;
			boolean finString = false;
			String stringActual = "";
			char separador = 0;
			while (line!=null && (posCar<line.length() || line.isEmpty() && posCar==0)) {
				if (line.isEmpty() && posCar==0) {
					if (!inString) return ret;  // Línea vacía
				} else {
					char car = line.charAt( posCar );
					if (car=='"') {
						if (inString) {
							if (nextCar(line,posCar)=='"') {  // Doble "" es un "
								posCar++;
								stringActual += "\"";
							} else {  // " de cierre
								inString = false;
								finString = true;
							}
						} else {  // !inString
							if (stringActual.isEmpty()) {  // " de apertura
								inString = true;
							} else {  // " después de valor - error
								throw new StringIndexOutOfBoundsException( "\" after data in char " + posCar + " of line [" + line + "]" );
							}
						}
					} else if (car==',' || car==';') {
						if (inString) {  // separador dentro de string
							stringActual += car;
						} else {  // separador que separa valores
							if (separador==0) { // Si no se había encontrado separador hasta ahora
								separador = car;
								ret.add( stringActual );
								stringActual = "";
								finString = false;
							} else { // Si se había encontrado, solo vale el mismo (, o ;)
								if (separador==car) {  // Es un separador
									ret.add( stringActual );
									stringActual = "";
									finString = false;
								} else {  // Es un carácter normal
									if (finString) throw new StringIndexOutOfBoundsException( "Data after string in char " + posCar + " of line [" + line + "]");  // valor después de string - error
									stringActual += car;
								}
							}
						}
					} else {  // Carácter dentro de valor
						if (finString) throw new StringIndexOutOfBoundsException( "Data after string in char " + posCar + " of line [" + line + "]");  // valor después de string - error
						stringActual += car;
					}
					posCar++;
				}
				if (posCar>=line.length() && inString) {  // Se ha acabado la línea sin acabarse el string. Eso es porque algún string incluye salto de línea. Se sigue con la siguiente línea
					line = null;
				    try {
						line = input.readLine();
				    	if (LOG_CONSOLE_CSV) System.out.println( "  " + numLine + " (add)\t" + line );
						posCar = 0;
						stringActual += "\n";
					} catch (IOException e) {}  // Si la línea es null es que el fichero se ha acabado ya o hay un error de I/O
				}
			}
			if (inString) throw new StringIndexOutOfBoundsException( "String not closed in line " + numLine + ": [" + line + "]");
			ret.add( stringActual );
			return ret;
		}

			// Devuelve el siguiente carácter (car 0 si no existe el siguiente carácter)
			private static char nextCar( String line, int posCar ) {
				if (posCar+1<line.length()) return line.charAt( posCar + 1 );
				else return Character.MIN_VALUE;
			}

	
	// =================================================
	// Métodos relacionados con el modelo de tabla (cuando se quiere utilizar esta tabla en una JTable)
	
	protected TablaTableModel miModelo = null;
	/** Devuelve un modelo de tabla de este objeto tabla para poderse utilizar como modelo de datos en una JTable
	 * @return	modelo de datos de la tabla
	 */
	public TablaTableModel getTableModel() {
		if (miModelo==null) {
			miModelo = new TablaTableModel();
		}
		return miModelo;
	}
	
	public class TablaTableModel implements TableModel {
		@Override
		public int getRowCount() {
			return size();
		}
		@Override
		public int getColumnCount() {
			return headers.size();
		}
		@Override
		public String getColumnName(int columnIndex) {
			return headers.get(columnIndex);
		}
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (types!=null && types.size()>=columnIndex+1) return types.get(columnIndex);
			else return String.class;
		}
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) { // Estas tablas de datos no son editables por defecto
			return false;
		}
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return get(rowIndex,columnIndex);
		}
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			for (TableModelListener l : lListeners) {
				l.tableChanged( new TableModelEvent( this, rowIndex, rowIndex, columnIndex, TableModelEvent.UPDATE ) );
			}
		}
		
		private ArrayList<TableModelListener> lListeners = new ArrayList<>();
		@Override
		public void addTableModelListener(TableModelListener l) {
			lListeners.add( l );
		}
		@Override
		public void removeTableModelListener(TableModelListener l) {
			lListeners.remove( l );
		}
		
	}

}
