package utils.tablas;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.function.Predicate;

import javax.swing.*;

import utils.Utils;
import utils.tablas.FilaSQL.TipoValorSQL;

/**
 *  No procesa sintaxis completa. Por ejemplo no procesa comentarios de /*, solo los de --, y líneas empezadas en #
 * @author andoni.eguiluz @ ingenieria.deusto.es
 */
public class ScanSQLInsert {
	private static boolean LOG_FICHERO_SQL = false;
	private static int limiteDeFilas = 2000000;  // Límite de filas a cargar por limitaciones de memoria
	private static boolean LOG_LINEAS = false;  // Hace log a consola del proceso de una serie de líneas
	private static int LINEA_DESDE = 50;  // Línea inicial de la que hacer log (si LOG_LINEAS es true)
	private static int LINEA_HASTA = 60;  // Línea final de la que hacer log (si LOG_LINEAS es true)
	private static Predicate<FilaSQL> chequeoFilaOK = null;

	/* Ejemplo de chequeo
	private static Predicate<FilaSQL> CHEQUEO_FILA_OK = new Predicate<FilaSQL>() {
		private long FECHA_INI = new GregorianCalendar(2020,5,12).getTimeInMillis();  //  Long.MIN_VALUE;  // Límite inferior de fecha a cargar en logs
		private long FECHA_FIN = new GregorianCalendar(2020,6,1).getTimeInMillis();  //  Long.MAX_VALUE;  // Límite superior de fecha a cargar en logs
		@Override
		public boolean test(FilaSQL fsql) {
			if (fsql.getNombreTabla().equals("log")) { // Chequeo de fecha de log
				long fecha = fsql.getColLong( "user_timestamp" );
				if (fecha!=Long.MAX_VALUE) {
					return !(fecha<FECHA_INI || fecha>FECHA_FIN);  // No ok si está fuera de fechas
				} else {
					return true;
				}
			} else {
				return true;
			}
		}
	};
	*/
	
	private static String[] tablasAIgnorar = { };  // Tablas que quieren ignorarse
	private static String[] tablasACargar = { };   // Tablas que quieren cargarse (el resto se ignoran)
	
	public static FilaSQL FILA_TABLA_IGNORADA = new FilaSQL( "TABLA_IGNORADA", null, null, 0, "" );
	
	/** Activa un chequeo para cada fila. Si el chequeo es correcto, la fila se añade y se ignora en caso contrario.
	 * @param chequeo	Chequeo para fila, null si no se quiere hacer (todas se añaden)
	 */
	public static void setChequeoFila( Predicate<FilaSQL> chequeo ) {
		chequeoFilaOK = chequeo;
	}
	
	/** Cambia el límite de filas a cargar en cada tabla en la próxima carga. Por defecto 2 millones, si se indica 0 o negativo no se considera límite
	 * @param limite	Número límite de filas (0 o negativo = sin límite)
	 */
	public static void setLimiteDeFilas( int limite ) {
		limiteDeFilas = limite;
	}
	
	/** Define la serie de tablas que quieren ignorarse al cargar el próximo fichero SQL
	 * @param tablasAIgnorar	Array de nombres de tablas a ignorar
	 */
	public static void setTablasParaIgnorar( String[] tablasAIgnorar ) {
		ScanSQLInsert.tablasAIgnorar = tablasAIgnorar;
	}
	
	/** Define la serie de tablas que quieren cargarse en el próximo fichero SQL (el resto se ignorarán)
	 * @param tablasACargar	Array de nombres de tablas a cargar
	 */
	public static void setTablasParaCargar( String[] tablasACargar ) {
		ScanSQLInsert.tablasACargar = tablasACargar;
	}
	

	// Atributos internos del scanner de SQL
	private long carsLeidos = 0;		// Caracteres ya leídos
	private int numLinea = 0;			// Núm de línea en curso
	private String linea = null;		// Línea en curso
	private int inicioNextToken;		// Carácter en curso de línea
	private TipoValorSQL lastValorSQL;  // Ultimo tipo de token SQL leído
	private Scanner scanner;			// Scanner en uso
	private InputStream input;			// InputStream de ese Scanner en uso
	private ArrayList<String> lastCab;	// Última cabecera de INSERT encontrada
	private String lastTabla;			// Último nombre de tabla de INSERT encontrado
	private String lastToken;			// Último token encontrado
	private ArrayList<String> aIgnorar; // Tablas a ignorar
	private String[] aCargar;           // Tablas a cargar
	
	/** Crea un nuevo scanner de SQL partiendo de un stream de entrada. A partir de este momento se puede llamar al método {@link #nextFila()} para ir recuperando filas de insert de ese stream
	 * que es cerrado tras recuperar la última línea de insert (null)
	 * @param inputStream
	 */
	public ScanSQLInsert( InputStream inputStream ) {
		carsLeidos = 0;
		numLinea = 0;
		enInsert = false;
		scanner = new Scanner( inputStream, "UTF-8" );
		aIgnorar = new ArrayList<String>();
		aCargar = null;
		this.input = inputStream;
		leeSiguienteLinea();
	}
	
	/** Añade tabla a ignorar
	 * @param nombreTabla	Nombre de la tabla a ignorar
	 */
	public void addTablaAIgnorar( String nombreTabla ) {
		aIgnorar.add( nombreTabla );
	}
	
	/**
	 * @param nombreTablas
	 */
	public void setTablasACargar( String[] nombreTablas ) {
		aCargar = nombreTablas;
	}
	
	/** Devuelve el número de líneas ya leídas del stream de entrada
	 * @return	Número de líneas leídas
	 */
	public int getLineaActual() {
		return numLinea;
	}
	
	/** Devuelve los caracteres ya leídos (dando una información de progreso)
	 * @return	Caracteres ya leídos del stream de entrada
	 */
	public long getCarsYaLeidos() {
		return carsLeidos;
	}
	
	private void leeSiguienteLinea() {
		if (scanner==null) { linea=null; return; }
		if (scanner.hasNext()) {
			linea = scanner.nextLine();
			numLinea++;
			carsLeidos += linea.length();
			if (LOG_FICHERO_SQL) System.err.println( Utils.trunca( linea, 200 ) );
		} else {
			linea = null;
			scanner.close();
			scanner = null;
			try {
				input.close();
				input = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		inicioNextToken = 0;
		lastValorSQL = null;
	}
	
	public static void main(String[] args) {
		String path = "d:/";
		String fileName = path + "Tichnology_2020-12-16.sql";
		File f = Utils.pideFichero( new String[] { "sql" }, new File(fileName) );
		if (f!=null && f.exists()) {
			System.out.println( f.getAbsolutePath() );
			// sacarInfoSQLAConsola( f.getAbsolutePath() );
			visualizarTablasSQL( f.getAbsolutePath() );
		}
	}
	
	/** Carga las tablas SQL del fichero indicado
	 * @param fileName	Fichero .sql del que cargar los datos
	 * @return	Mapa de tablas con clave nombre de tabla 
	 */
	public static HashMap<String,Tabla> cargarTablasSQL( String fileName ) {
		HashMap<String,ArrayList<FilaSQL>> mapaAL = new HashMap<>();  // Mapa para las listas de datos
		HashMap<String,Tabla> mapaT = new HashMap<>();  // Mapa para las tablas
		try {
			File f = new File(fileName);
			ScanSQLInsert ssi = new ScanSQLInsert( new FileInputStream( f ) );
			if (tablasAIgnorar.length>0) {
				for (String tai : tablasAIgnorar) ssi.addTablaAIgnorar( tai );
			}
			if (tablasACargar!=null && tablasACargar.length>0) {
				ssi.setTablasACargar( tablasACargar );
			} else {
				ssi.setTablasACargar( null );
			}
			FilaSQL fsql;
			while ((fsql = ssi.nextFila()) != null) {
				if (fsql==FILA_TABLA_IGNORADA) continue;
				quitarImagenes( fsql.getValores() );
				ArrayList<FilaSQL> lista = mapaAL.get( fsql.getNombreTabla() );
				if (lista==null) {
					lista = new ArrayList<>();
					mapaAL.put( fsql.getNombreTabla(), lista );
					Tabla t = Tabla.linkTablaToList( mapaAL.get( fsql.getNombreTabla() ) );
					mapaT.put( fsql.getNombreTabla(), t );
				} else {
					if (limiteDeFilas>0 && lista.size()>limiteDeFilas) continue;  // Para que no cargue ninguna tabla con más de N filas
					if (chequeoFilaOK!=null && !chequeoFilaOK.test(fsql)) {
						continue;
					}
				}
				lista.add( fsql );
			}
			ssi.cerrar();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return mapaT;
	}
	
	
		private static boolean cortarCarga = false;
		private static JProgressBar pbProgreso;
		private static JLabel lProgreso;
	public static void visualizarTablasSQL( String fileName ) {
		pbProgreso = new JProgressBar( 0, 1000 );
		pbProgreso.setPreferredSize( new Dimension( 500, 20 ) );
		lProgreso = new JLabel( " " );
		VentanaGeneralTablas vent = new VentanaGeneralTablas();
		vent.setVisible( true );
		vent.setAccionCierre( new Runnable() {
			@Override
			public void run() {
				cortarCarga = true;
			}
		});
		((FlowLayout)vent.getPanelSuperior().getLayout()).setAlignment( FlowLayout.RIGHT );
		vent.getPanelSuperior().add( lProgreso );
		vent.getPanelSuperior().add( pbProgreso );
		try {
			HashMap<String,ArrayList<FilaSQL>> mapaAL = new HashMap<>();  // Mapa para las listas de datos
			HashMap<String,Tabla> mapaT = new HashMap<>();  // Mapa para las tablas
			File f = new File(fileName);
			ScanSQLInsert ssi = new ScanSQLInsert( new FileInputStream( f ) );
			if (tablasAIgnorar.length>0) {
				for (String tai : tablasAIgnorar) ssi.addTablaAIgnorar( tai );
			}
			if (tablasACargar!=null && tablasACargar.length>0) {
				ssi.setTablasACargar( tablasACargar );
			} else {
				ssi.setTablasACargar( null );
			}
			FilaSQL fsql;
			int numFilas = 0;
			int x = 0;
			int y = 0;
			while (!cortarCarga && (fsql = ssi.nextFila()) != null) {
				if (ssi.getLineaActual()%50==0) {
					progreso( ssi, f.length(), numFilas );
				}
				if (fsql==FILA_TABLA_IGNORADA) continue;
				// if (fsql.getNombreTabla().equalsIgnoreCase("log")) continue;  // Para que no cargue la tabla log
				quitarImagenes( fsql.getValores() );
				numFilas++;
				ArrayList<FilaSQL> lista = mapaAL.get( fsql.getNombreTabla() );
				if (lista==null) {
					lista = new ArrayList<>();
					lista.add( fsql );
					mapaAL.put( fsql.getNombreTabla(), lista );
					Tabla t = Tabla.linkTablaToList( mapaAL.get( fsql.getNombreTabla() ) );
					mapaT.put( fsql.getNombreTabla(), t );
					VentanaTabla v = new VentanaTabla( vent, "Datos de " + fsql.getNombreTabla() );
					v.setLocation( x, y );
					x += 20; y += 20;
					v.setTabla( t );
					ArrayList<String> nomCabs = fsql.getNomColumnas();
					v.setDobleClickHeader( new VentanaTabla.EventoEnCelda() {
						private ArrayList<String> nomCols;
						{
							nomCols = nomCabs;
						}
						@Override
						public void evento(int fila, int columna) {
							if (fila==-1) recuentoColumna( columna, nomCols.get(columna), v );
						}
					});
					vent.addVentanaInterna( v, fsql.getNombreTabla() );
					v.setVisible( true );
				} else {
					if (limiteDeFilas>0 && lista.size()>limiteDeFilas) continue;  // Para que no cargue ninguna tabla con más de N filas
					if (chequeoFilaOK!=null && !chequeoFilaOK.test(fsql)) {
						continue;
					}
					lista.add( fsql );
					mapaT.get( fsql.getNombreTabla() ).cambioEnTabla( lista.size()-1 );
				}
			}
			ssi.cerrar();
			progreso( null, 0, 0 );
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
		private static void progreso( ScanSQLInsert ssi, long longFic, int numFilas ) {
			if (ssi==null) {
				pbProgreso.setValue( 1000 );
				lProgreso.setText( "Lectura finalizada." );
			} else {
				double prog = 1000.0 * ssi.getCarsYaLeidos() / longFic;
				pbProgreso.setValue( (int) prog );
				lProgreso.setText( ssi.getLineaActual() + " líneas de fichero SQL -- " + numFilas + " filas leídas." );
				// String progreso = String.format( "%1$6.2f" , prog ) + "%";
				// System.out.println( ssi.getLineaActual() + " -- " + progreso + "   " + numFilas + " filas leídas."  );  // Da info de progreso
			}
		}
		// quitar data:image/
		private static void quitarImagenes( ArrayList<String> l ) {
			for (int i=0; i<l.size(); i++) if (l.get(i).startsWith("data:image/")) l.set(i, "data:image/...");
		}
		private static void recuentoColumna( int col, String nomCol, VentanaTabla vent ) {
			Tabla tabla = vent.getTabla();
			TreeMap<String,ContadorConObjeto> mapa = new TreeMap<>();
			for (int fila = 0; fila<tabla.size(); fila++) {
				String val = tabla.get(fila, col);
				if (val!=null) {
					ContadorConObjeto cont = mapa.get( val );
					if (cont==null) {
						mapa.put( val, new ContadorConObjeto( 1, val ) );
					} else {
						cont.inc();
					}
				}
			}
			ArrayList<ContadorConObjeto> lc = new ArrayList<>();
			for (ContadorConObjeto cco : mapa.values()) lc.add( cco );
			lc.sort( new Comparator<ContadorConObjeto>() {
				@Override
				public int compare(ContadorConObjeto o1, ContadorConObjeto o2) {
					int ret = o2.getCont() - o1.getCont();
					if (ret==0) ret = o1.getObject().toString().compareTo( o2.getObject().toString() );
					return ret;
				}
			});
			String show = "";
			for (int i=0; i<lc.size(); i++) {
				if (i>15) break;
				show += lc.get(i).getCont() + " " + lc.get(i).getObject() + "\n";
			}
			JOptionPane.showInternalMessageDialog( vent, show, "Frecuencia de valores en columna " + nomCol, JOptionPane.INFORMATION_MESSAGE );
		}

	
	// Temporal para prueba
	public static void sacarInfoSQLAConsola( String fileName ) {
		try {
			HashMap<String,ArrayList<FilaSQL>> mapa10 = new HashMap<>();  // Un mapa para guardar las 10 primeras filas de cada tabla
			File f = new File(fileName);
			ScanSQLInsert ssi = new ScanSQLInsert( new FileInputStream( f ) );
			FilaSQL fsql;
			int numFilas = 0;
			while ((fsql = ssi.nextFila()) != null) {
				numFilas++;
				ArrayList<FilaSQL> lista = mapa10.get( fsql.getNombreTabla() );
				if (lista ==null) {
					lista = new ArrayList<>();
					mapa10.put( fsql.getNombreTabla(), lista );
				}
				if (lista.size() < 10) {
					System.out.println( fsql );
				}
				lista.add( fsql );
				if (ssi.getLineaActual()%1000==0) {
					double prog = 100.0 * ssi.getCarsYaLeidos() / f.length();
					String progreso = String.format( "%1$6.2f" , prog ) + "%";
					System.out.println( ssi.getLineaActual() + " -- " + progreso + "   " + numFilas + " filas leídas."  );  // Da info de progreso
				}
			}
			for (String tabla : mapa10.keySet()) {
				System.out.println( "Tabla " + tabla + ": " + mapa10.get(tabla).size() + " filas." );
			}
			/* Prueba de tokens sin más
			for (int i=0; i<1000; i++) {!"
				String token = ssi.leeSiguienteToken();
				System.out.println( ssi.lastValorSQL + " - " + (token.length()>100 ? token.substring(0, 100)+"..." : token) ); 
			}
			*/
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void cerrar() {
		if (scanner!=null) { scanner.close(); scanner = null; }
		try {
			if (input!=null) { input.close(); input = null; }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Sintaxis insert
	// INSERT INTO nombre-tabla ( string , string ... ) VALUES
	// ( dato , dato , ... )      // dato = NUMERO o STRING 

	
		private boolean enInsert;
	/** Devuelve la siguiente fila del fichero de sentencias insert SQL
	 * @return	Siguiente valor, null si no hay ninguno más (y en ese caso se cierra el stream), FILA_TABLA_IGNORADA si la tabla se quiere ignorar
	 */
	public FilaSQL nextFila() {
		FilaSQL ret = null;
		FILA_TABLA_IGNORADA.setNumLineaSQL( numLinea );
		if (enInsert) {
			ArrayList<String> campos = leeLista();
			if (campos==null) {
				System.err.println( "Error en línea " + numLinea + " de SQL: fila de insert incompleta" ); 
				System.err.println( linea ); 
				enInsert = false;
			} else {
				lastToken = leeSiguienteToken();
				if (lastToken!=null && lastToken.equals(",")) enInsert = true; 
				else if (lastToken!=null && lastToken.equals(";")) enInsert = false;
				else enInsert = false;  // Suponemos aunque sintácticamente es incorrecto
				if (aIgnorar.contains(lastTabla)) return FILA_TABLA_IGNORADA;
				if (aCargar != null && !Arrays.asList(aCargar).contains(lastTabla)) return FILA_TABLA_IGNORADA;
				ret = new FilaSQL( lastTabla, lastCab, campos, numLinea, linea );
			}
		} else {
			boolean fin = leeHasta( "INSERT", "INTO" );
			if (!fin) return ret;
			lastTabla = leeSiguienteToken();
			if (lastTabla==null) return ret;
			lastCab = leeLista(true);
			if (lastCab==null) return null; // No se ha encontrado un insert con fila correcta
			ArrayList<String> campos = null;
			if (lastCab.isEmpty() && "VALUES".equals(lastToken)) {  // Insert sin lista de campos
				campos = leeLista();
			} else if (lastCab.isEmpty()) { // Insert sin lista de campos y sin values
				System.err.println( "Error en línea " + numLinea + " de SQL: Sintaxis de insert incorrecta" ); 
				System.err.println( linea ); 
			} else {
				String values = leeSiguienteToken();
				if (values==null) return null;
				if (values.equals("VALUES")) {
					campos = leeLista();
				} else {
					System.err.println( "Error en línea " + numLinea + " de SQL: Sintaxis de insert incorrecta" ); 
					System.err.println( linea ); 
				}
			}
			lastToken = leeSiguienteToken();
			if (lastToken!=null && lastToken.equals(",")) enInsert = true; 
			else if (lastToken!=null && lastToken.equals(";")) enInsert = false;
			else enInsert = false;  // Suponemos aunque sintácticamente es incorrecto
			// String st = ((""+campos).length()>300 ? (""+campos).substring(0, 300) : ""+campos );
			// System.out.println( "** " + lastTabla + " - " + lastCab + " - " + st );
			if (aIgnorar.contains(lastTabla)) return FILA_TABLA_IGNORADA;
			if (aCargar != null && !Arrays.asList(aCargar).contains(lastTabla)) return FILA_TABLA_IGNORADA;
			ret = new FilaSQL( lastTabla, lastCab, campos, numLinea, linea );
		}
		return ret;
	}
		
	public TipoValorSQL tipoDeUltimoValor() {
		return lastValorSQL;
	}
	
	// Lee hasta que ocurra una combinación de tokens dada en mayúsculas (pasándolos) - true si lo encuentra
	private boolean leeHasta( String... strings ) {
		int iguales = 0;
		while (iguales<strings.length) {
			String sig = leeSiguienteToken();
			if (sig==null) return false;  // EOF
			if (sig.toUpperCase().equals( strings[iguales] )) {
				iguales++;
			} else {
				iguales = 0;
			}
		}
		return true;
	}
	
	// Lee lista a continuación
	private ArrayList<String> leeLista( boolean... convierteAMinusc ) {
		ArrayList<String> ret = new ArrayList<>();
		String sig = leeSiguienteToken();
		if (sig==null) return null;
		if (sig.equals("(")) {
			sig = leeSiguienteToken();
			while (lastValorSQL == TipoValorSQL.IDENTIFICADOR || lastValorSQL == TipoValorSQL.STRING || lastValorSQL == TipoValorSQL.NUMERO || lastValorSQL == TipoValorSQL.NULL) {
				if (convierteAMinusc.length>0 && convierteAMinusc[0]) sig = sig.toLowerCase();
				ret.add( sig );
				sig = leeSiguienteToken();
				if (sig!=null && sig.equals(",")) {
					sig = leeSiguienteToken();
				} else if (sig!=null && sig.equals(")")) {
					break;
				} else {
					System.err.println( "Error en línea " + numLinea + " de SQL: Lista inacabada" ); 
					System.err.println( linea ); 
					break;
				}
			}
		}
		lastToken = sig.toUpperCase();
		return ret;
	}
	
	// Devuelve token y actualiza su tipo en lastValorSQL. Si no hay más tokens devuelve null
	private String leeSiguienteToken() {
		String tok = leeSiguienteToken2();
		if (LOG_LINEAS && numLinea>=LINEA_DESDE && numLinea<=LINEA_HASTA) System.err.println( "<- " + tok + " (" + numLinea + ")" );
		return tok;
	}
	private String leeSiguienteToken2() {
		while (linea!=null) {
			while (inicioNextToken<linea.length() && " \t".indexOf(linea.charAt(inicioNextToken))!=-1) inicioNextToken++;  // Separadores
			if (inicioNextToken>=linea.length()) {
				leeSiguienteLinea();
			} else {
				if (linea.charAt(inicioNextToken)==')') {
					inicioNextToken++;
					lastValorSQL = TipoValorSQL.SIMBOLO;
					return ")";
				} else if (linea.charAt(inicioNextToken)=='(') {
					inicioNextToken++;
					lastValorSQL = TipoValorSQL.SIMBOLO;
					return "(";
				} else if (linea.charAt(inicioNextToken)==',') {
					inicioNextToken++;
					lastValorSQL = TipoValorSQL.SIMBOLO;
					return ",";
				} else if (linea.charAt(inicioNextToken)==';') {
					inicioNextToken++;
					lastValorSQL = TipoValorSQL.SIMBOLO;
					return ";";
				} else if (linea.charAt(inicioNextToken)=='#') {  // Comentario
					inicioNextToken++;
					leeSiguienteLinea();
				} else if (linea.charAt(inicioNextToken)=='-' && inicioNextToken+1<linea.length() && linea.charAt(inicioNextToken+1)=='-') {  // Comentario de línea
					inicioNextToken++; inicioNextToken++;
					leeSiguienteLinea();
				} else if (linea.charAt(inicioNextToken)=='/') {  // Carácter de escape
					inicioNextToken++;
					if (inicioNextToken>=linea.length()) {  // Error de sintaxis SQL - carácter de escape inacabado
						System.err.println( "Error en línea " + numLinea + " de SQL: Carácter de escape inacabado" ); 
						System.err.println( linea );
					} else {
						if (linea.charAt(inicioNextToken)=='*') {  // Inicio de comentario
							inicioNextToken++;
							while (linea!=null) {
								if (inicioNextToken+1<linea.length() && linea.charAt(inicioNextToken)=='*' && linea.charAt(inicioNextToken+1)=='/') {
									inicioNextToken++; inicioNextToken++;
									break; // Fin de comentario
								}
								inicioNextToken++;  // Interior de comentario
								if (inicioNextToken>=linea.length()) leeSiguienteLinea();
							}
						} else {
							System.err.println( "Error en línea " + numLinea + " de SQL:  Carácter de escape inacabado" ); 
							System.err.println( linea ); 
						}
					}
				} else {
					String valor = "";
					char letra = linea.charAt(inicioNextToken);
					int inicio = inicioNextToken;
					if (Character.isDigit(letra) || letra == '-') {  // Número
						inicioNextToken++;
						while (inicioNextToken<linea.length() && (Character.isDigit(linea.charAt(inicioNextToken)) || linea.charAt(inicioNextToken)=='.')) inicioNextToken++;
						valor = linea.substring(inicio, inicioNextToken);
						lastValorSQL = TipoValorSQL.NUMERO;
						return valor;
					} else if (letra == '\'' || letra == '`') {  // String con comilla simple
						char letraFin = '\'';
						if (letra=='`') letraFin = '`';
						inicioNextToken++;
						char carAnt = ' ';
						int numBS = 0;
						valor = "";
						while (linea!=null) {
							while (inicioNextToken<linea.length() && 
								   ((linea.charAt(inicioNextToken)!=letraFin) ||
								    ((linea.charAt(inicioNextToken)=='\'' && carAnt == '\\') && (numBS%2 != 0)))) {    // Número de backslash impar porque si es par es que son backslash que se escapan a sí mismos
								carAnt = linea.charAt(inicioNextToken);
								if (carAnt=='\\') numBS++ ; else numBS = 0;
								inicioNextToken++;
							}
							valor = valor + linea.substring(inicio+1, inicioNextToken);
							if (inicioNextToken<linea.length()) {  // Ha acabado el string
								break;
							} else {  // Se ha llegado al final de la línea y no ha acabado el string - se busca en la siguiente línea
								valor = valor + "\n";
								inicio = -1;
								leeSiguienteLinea();
							}
						}
						inicioNextToken++;
						lastValorSQL = TipoValorSQL.STRING;
						return valor;
					} else if (Character.isAlphabetic(letra)) {  // NULL o similar
						inicioNextToken++;
						while (inicioNextToken<linea.length() && (
								Character.isAlphabetic(linea.charAt(inicioNextToken)) || Character.isDigit(linea.charAt(inicioNextToken)) ||
								linea.charAt(inicioNextToken)=='_')) inicioNextToken++;
						valor = linea.substring(inicio, inicioNextToken);
						if (valor.toUpperCase().equals("NULL"))
							lastValorSQL = TipoValorSQL.NULL;
						else 
							lastValorSQL = TipoValorSQL.IDENTIFICADOR;
						return valor;
					} else {  // No debería - si hay reales y tal habría que contemplarlo
						inicioNextToken++;
						valor = linea.substring(inicio, inicioNextToken);
						lastValorSQL = TipoValorSQL.OTRO;
					}
				}
			}
		}
		lastValorSQL = null;
		return null;
	}
	
}


/* Versión kodetu gen 201901 -

CREATE TABLE `asignacion` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `grupo` int(11) DEFAULT NULL,
  `challenge` int(11) DEFAULT NULL,
  `tryings` int(11) DEFAULT NULL,
  `visible` tinyint(1) DEFAULT NULL,
  `limite` datetime DEFAULT NULL,
  `deleted` tinyint(1) DEFAULT NULL,
  `date` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `timestamp` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

[asignacion] - [2, 15/01/2018 - TALLER, 8, 24, NULL, NULL, NULL, NULL, NULL, NULL] - [id, name, grupo, challenge, tryings, visible, limite, deleted, date, timestamp]
[asignacion] - [3, 15/01/2018 - TALLER, 9, 24, NULL, NULL, NULL, NULL, NULL, NULL] - [id, name, grupo, challenge, tryings, visible, limite, deleted, date, timestamp]
[asignacion] - [4, 15/01/2018 - TALLER, 10, 25, NULL, NULL, NULL, NULL, NULL, NULL] - [id, name, grupo, challenge, tryings, visible, limite, deleted, date, timestamp]
[asignacion] - [5, 15/01/2019 - TALLER, 12, 24, NULL, NULL, NULL, NULL, NULL, NULL] - [id, name, grupo, challenge, tryings, visible, limite, deleted, date, timestamp]
...

CREATE TABLE `challenge` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `creator` int(11) DEFAULT NULL,
  `username` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `levels` int(11) DEFAULT NULL,
  `lives` int(11) DEFAULT NULL,
  `jumps` tinyint(1) DEFAULT NULL,
  `ranking` tinyint(1) DEFAULT NULL,
  `creation_date` datetime DEFAULT NULL,
  `remixed_from` int(11) DEFAULT NULL,
  `image1` longtext COLLATE utf8mb4_unicode_ci,
  `published` tinyint(1) DEFAULT NULL,
  `image2` longtext COLLATE utf8mb4_unicode_ci,
  `type_image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stars` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

[challenge] - [24, 5ejvne, KODETU GEN 2, , 4, LearningLab, public, 15, -1, 0, 0, NULL, NULL, data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAA0gAAAMgCAYAAAAZQELsAAAAAXNSR0IArs4c6QAAQABJREFUeAFM3de2NEt2nuda5jfbdQPdjSYoAiRFDYlj8Ip0KN2iDngnGkMnFAlCIglAbLfdb5bR+3xRtXdXrazMjJh+zphh0qy7//1/+19f//2//z8uz6+Xy...
[challenge] - [25, 7dkwwd, KODETU, , 4, LearningLab, public, 15, -1, 0, 0, NULL, NULL, data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAA0gAAAMgCAYAAAAZQELsAAAAAXNSR0IArs4c6QAAQABJREFUeAFM3de2NEt2nuda5jfbdQPdjSYoAiRFDYlj8Ip0KN2iDngnGkMnFAlCIglAbLfdb5bR+3xRtXdXrazMjJh+zphh0qy7//1/+19f//2//z8uz6+Xy8vF5/F...

CREATE TABLE `challenge_level` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `challenge` int(11) DEFAULT NULL,
  `level_index` int(11) DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `creator` int(11) DEFAULT NULL,
  `username` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `map1` longtext COLLATE utf8mb4_unicode_ci,
  `map2` longtext COLLATE utf8mb4_unicode_ci,
  `dir1` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dir2` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `block_limit` int(11) DEFAULT NULL,
  `run_limit` int(11) DEFAULT NULL,
  `time_limit` int(11) DEFAULT NULL,
  `extra_life` tinyint(1) DEFAULT NULL,
  `repeat_level` tinyint(1) DEFAULT NULL,
  `checkpoint` int(11) DEFAULT NULL,
  `ranking` tinyint(1) DEFAULT NULL,
  `opt_blocks` int(11) DEFAULT NULL,
  `opt_time` int(11) DEFAULT NULL,
  `show_ranking` tinyint(1) DEFAULT NULL,
  `show_average` tinyint(1) DEFAULT NULL,
  `video` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `maze_if` int(11) DEFAULT NULL,
  `maze_if_else` int(11) DEFAULT NULL,
  `forever` int(11) DEFAULT NULL,
  `answer` longtext COLLATE utf8mb4_unicode_ci,
  `remixed_from` int(11) DEFAULT NULL,
  `images` int(11) DEFAULT NULL,
  `map1editor` longtext COLLATE utf8mb4_unicode_ci,
  `map2editor` longtext COLLATE utf8mb4_unicode_ci,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

[challenge_level] - [7, 24, 1, 1, NULL, NULL, [[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,2,0,0,0],[0,0,0,1,0,0,0],[0,0,0,3,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0]], , right, down, -1, -1, -1, 0, 0, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, 0, NULL, NULL, 1, {\"c2array\":true,\"size\":[7,7,1],\"data\":[[[0],[0...
[challenge_level] - [8, 24, 2, 2, NULL, NULL, [[0,0,0,0,0,0],[0,0,0,0,0,0],[0,0,2,0,0,0],[0,0,1,0,0,0],[0,0,1,3,0,0],[0,0,0,0,0,0]], , right, down, -1, -1, -1, 0, 0, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, 0, NULL, NULL, 2, {\"c2array\":true,\"size\":[6,6,1],\"data\":[[[0],[0],[0],[0],[0],[0]],[[0],[0],...
[challenge_level] - [12, 24, 3, 3, NULL, NULL, [[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,2,1,3,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0]], [[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,3,1,2,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0]], right, left, -1, -1, -1...
[challenge_level] - [13, 24, 4, 4, NULL, NULL, [[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,3,0,0,0],[0,0,0,1,0,0,0],[0,0,0,1,0,0,0],[0,0,0,2,0,0,0],[0,0,0,0,0,0,0]], [[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,3,1,1,1,2,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0]], right, down, -1, -1, -1...
[challenge_level] - [14, 24, 5, 5, NULL, NULL, [[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,2,0,0],[0,0,0,1,1,0,0],[0,0,0,3,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0]], [[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,3,0,0],[0,0,0,1,1,0,0],[0,0,0,2,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0]], right, left, -1, -1, -1...
...

CREATE TABLE `challenge_level_image` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `image1` longtext COLLATE utf8mb4_unicode_ci,
  `image2` longtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

[challenge_level_image] - [1, data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAA0gAAAMgCAYAAAAZQELsAAAAAXNSR0IArs4c6QAAQABJREFUeAFM3de2NEt2nuda5jfbdQPdjSYoAiRFDYlj8Ip0KN2iDngnGkMnFAlCIglAbLfdb5bR+3xRtXdXrazMjJh+zphh0qy7//1/+19f//2//z8uz6+Xy8vF5/Fyd3m4PNw9Xh7vHy6vry9tr5e7l/vL5fnh8vJ6d3m5C+z+5XL38Hy5u3u+...
[challenge_level_image] - [2, data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAA0gAAAMgCAYAAAAZQELsAAAAAXNSR0IArs4c6QAAQABJREFUeAFM3de2NEt2nuda5jfbdQPdjSYoAiRFDYlj8Ip0KN2iDngnGkMnFAlCIglAbLfdb5bR+3xRtXdXrazMjJh+zphh0qy7//1/+19f//2//z8uz6+Xy8vF5/Fyd3m4PNw9Xh7vHy6vry9tr5e7l/vL5fnh8vJ6d3m5C+z+5XL38Hy5u3u+...
[challenge_level_image] - [3, data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAA0gAAAMgCAYAAAAZQELsAAAgAElEQVR4XlS9abOl+VXduZ/pnHNv5q2sSaUBSgyCBkSYIPy22y/6W5hBCPVHNFLRrzrC7nB3BMbRqMEY2whLGCQkVENWZt57z3mmjt9a639udZWPVWTe4Xn+wx7WXnvt7jt//K/3jz76N7XuVVvxz1hdDTV0Y439UPu+1b7v1W191TrUtne1dVXVb9UNa3XdWntt+nfZ...
...

CREATE TABLE `challenge_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user` int(11) DEFAULT NULL,
  `challenge` int(11) DEFAULT NULL,
  `lives` int(11) DEFAULT NULL,
  `level` int(11) DEFAULT NULL,
  `level_ref` int(11) DEFAULT NULL,
  `timestamp` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `totaltime` int(11) DEFAULT NULL,
  `user_timestamp` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `finished` tinyint(1) DEFAULT '0',
  `stars` double DEFAULT NULL,
  `age` int(11) DEFAULT NULL,
  `user_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `points` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

[challenge_user] - [1, 285, 24, NULL, 14, 27, 2019-01-14 23:53:04, 226, 1547509984305, 0, NULL, 7, dzzm, 99] - [id, user, challenge, lives, level, level_ref, timestamp, totaltime, user_timestamp, finished, stars, age, user_name, points]
[challenge_user] - [2, 166, 24, NULL, 12, 24, 2019-01-15 11:52:19, 1307, 1547553139895, 0, NULL, 12, e4xk, 88] - [id, user, challenge, lives, level, level_ref, timestamp, totaltime, user_timestamp, finished, stars, age, user_name, points]
[challenge_user] - [3, 176, 24, NULL, 10, 22, 2019-01-15 11:55:12, 1831, 1547553312278, 0, NULL, 12, dn48, 62] - [id, user, challenge, lives, level, level_ref, timestamp, totaltime, user_timestamp, finished, stars, age, user_name, points]
[challenge_user] - [4, 182, 24, NULL, 11, 23, 2019-01-15 11:58:32, 1576, 1547553512469, 0, NULL, 13, dw84, 28] - [id, user, challenge, lives, level, level_ref, timestamp, totaltime, user_timestamp, finished, stars, age, user_name, points]
[challenge_user] - [5, 183, 24, NULL, 12, 24, 2019-01-15 11:53:18, 1590, 1547553198262, 0, NULL, 12, gx8q, 86] - [id, user, challenge, lives, level, level_ref, timestamp, totaltime, user_timestamp, finished, stars, age, user_name, points]


CREATE TABLE `evaluation` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user` int(11) DEFAULT NULL,
  `challenge` int(11) DEFAULT NULL,
  `level` int(11) DEFAULT NULL,
  `level_ref` int(11) DEFAULT NULL,
  `points` int(11) DEFAULT NULL,
  `date` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `timestamp` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

[evaluation] - [1, 285, 24, 1, 7, 5, 2019-01-14 23:38:29, 1547509109375] - [id, user, challenge, level, level_ref, points, date, timestamp]
[evaluation] - [2, 285, 24, 2, 8, 0, 2019-01-14 23:38:43, 1547509123712] - [id, user, challenge, level, level_ref, points, date, timestamp]
[evaluation] - [3, 285, 24, 3, 12, 0, 2019-01-14 23:39:00, 1547509140307] - [id, user, challenge, level, level_ref, points, date, timestamp]
[evaluation] - [4, 285, 24, 4, 13, 0, 2019-01-14 23:39:27, 1547509167480] - [id, user, challenge, level, level_ref, points, date, timestamp]
[evaluation] - [5, 285, 24, 5, 14, 0, 2019-01-14 23:39:49, 1547509189968] - [id, user, challenge, level, level_ref, points, date, timestamp]
...


CREATE TABLE `grupo` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `creator` int(11) DEFAULT NULL,
  `username` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `members` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

[grupo] - [3, emve, Marianistas-G1, code, 4, andoni, 30] - [id, code, name, type, creator, username, members]
[grupo] - [4, dnkd, Marianistas-G2, code, 4, andoni, 30] - [id, code, name, type, creator, username, members]
[grupo] - [5, go4d, Marianistas-G3, code, 4, andoni, 30] - [id, code, name, type, creator, username, members]
[grupo] - [6, ep4e, Marianistas-G4, code, 4, andoni, 30] - [id, code, name, type, creator, username, members]
[grupo] - [7, dqvg, Zunzunegui-T1, code, 4, andoni, 30] - [id, code, name, type, creator, username, members]
[grupo] - [8, gryd, Zunzunegui-T2, code, 4, andoni, 30] - [id, code, name, type, creator, username, members]
[grupo] - [9, ev2g, Zunzunegui-T3, code, 4, andoni, 30] - [id, code, name, type, creator, username, members]
[grupo] - [10, dwkd, Zunzunegui-T4, code, 4, andoni, 30] - [id, code, name, type, creator, username, members]
[grupo] - [12, ey0d, ACCESO TALLER, code, 4, LearningLab, 4] - [id, code, name, type, creator, username, members]
[grupo] - [13, dz3e, ACCESO TALLER 2, code, 4, LearningLab, 4] - [id, code, name, type, creator, username, members]


CREATE TABLE `log` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user` int(11) DEFAULT NULL,
  `username` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_timestamp` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `challenge` int(11) DEFAULT NULL,
  `challenge_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `challenge_user` int(11) DEFAULT NULL,
  `level_ref` int(11) DEFAULT NULL,
  `level` int(11) DEFAULT NULL,
  `result` int(11) DEFAULT NULL,
  `workspace` mediumtext COLLATE utf8mb4_unicode_ci,
  `code` mediumtext COLLATE utf8mb4_unicode_ci,
  `db_timestamp` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `time` int(11) DEFAULT '0',
  `punt` int(11) DEFAULT '0',
  `age` int(11) DEFAULT '0',
  `speed` int(11) DEFAULT '0',
  `blocks` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

[log] - [1, 285, dzzm, 1547509080068, 24, 5ejvne, 1, 7, 1, 0, START, , 2019-01-14 23:38:00, 0, 10, 7, -1, 0] - [id, user, username, user_timestamp, challenge, challenge_code, challenge_user, level_ref, level, result, workspace, code, db_timestamp, time, punt, age, speed, blocks]
[log] - [2, 285, dzzm, 1547509094119, 24, 5ejvne, 1, 7, 1, 0, <xml xmlns=\"http://www.w3.org/1999/xhtml\"></xml>, , 2019-01-14 23:38:14, 4, 10, 7, -1, 0] - [id, user, username, user_timestamp, challenge, challenge_code, challenge_user, level_ref, level, result, workspace, code, db_timestamp, time, p...
[log] - [3, 285, dzzm, 1547509101554, 24, 5ejvne, 1, 7, 1, 0, <xml xmlns=\"http://www.w3.org/1999/xhtml\"><block type=\"maze_moveForward\" id=\"4\" x=\"15\" y=\"43\"></block></xml>, Maze.moveForward(\'block_id_4\');\n, 2019-01-14 23:38:21, 12, 10, 7, -1, 1] - [id, user, username, user_timestamp, cha...
[log] - [4, 285, dzzm, 1547509102567, 24, 5ejvne, 1, 7, 1, 0, <xml xmlns=\"http://www.w3.org/1999/xhtml\"><block type=\"maze_moveForward\" id=\"4\" x=\"15\" y=\"43\"></block></xml>, Maze.moveForward(\'block_id_4\');\n, 2019-01-14 23:38:22, 13, 10, 7, -1, 1] - [id, user, username, user_timestamp, cha...
[log] - [5, 285, dzzm, 1547509104229, 24, 5ejvne, 1, 7, 1, 0, <xml xmlns=\"http://www.w3.org/1999/xhtml\"><block type=\"maze_moveForward\" id=\"4\" x=\"15\" y=\"43\"><next><block type=\"maze_moveForward\" id=\"5\"></block></next></block></xml>, Maze.moveForward(\'block_id_4\');\nMaze.moveForward(\'b...
[log] - [6, 285, dzzm, 1547509105302, 24, 5ejvne, 1, 7, 1, 0, EXECUTE, Maze.moveForward(\'block_id_4\');\nMaze.moveForward(\'block_id_5\');\n, 2019-01-14 23:38:25, 16, 10, 7, 0, 2] - [id, user, username, user_timestamp, challenge, challenge_code, challenge_user, level_ref, level, result, workspace, ...
[log] - [7, 285, dzzm, 1547509107131, 24, 5ejvne, 1, 7, 1, 1, <xml xmlns=\"http://www.w3.org/1999/xhtml\"><block type=\"maze_moveForward\" id=\"4\" x=\"15\" y=\"43\"><next><block type=\"maze_moveForward\" id=\"5\"></block></next></block></xml>, Maze.moveForward(\'block_id_4\');\nMaze.moveForward(\'b...
[log] - [8, 285, dzzm, 1547509107394, 24, 5ejvne, 1, 7, 1, 1, EXECUTE -> SUCCESS, Maze.moveForward(\'block_id_4\');\nMaze.moveForward(\'block_id_5\');\n, 2019-01-14 23:38:27, 16, 10, 7, 0, 2] - [id, user, username, user_timestamp, challenge, challenge_code, challenge_user, level_ref, level, result, ...
[log] - [9, 285, dzzm, 1547509110105, 24, 5ejvne, 1, 8, 2, 0, START, , 2019-01-14 23:38:30, 0, 10, 7, -1, 0] - [id, user, username, user_timestamp, challenge, challenge_code, challenge_user, level_ref, level, result, workspace, code, db_timestamp, time, punt, age, speed, blocks]
[log] - [10, 285, dzzm, 1547509115111, 24, 5ejvne, 1, 8, 2, 0, <xml xmlns=\"http://www.w3.org/1999/xhtml\"><block type=\"maze_moveForward\" id=\"4\" x=\"39\" y=\"36\"><next><block type=\"maze_moveForward\" id=\"5\"></block></next></block></xml>, Maze.moveForward(\'block_id_4\');\nMaze.moveForward(\'...
...


CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lang` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `school` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `course` int(11) DEFAULT NULL,
  `plan` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `age` int(11) DEFAULT NULL,
  `gender` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `country` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `location` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `registration_date` datetime DEFAULT NULL,
  `avatar` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT NULL,
  `tech_skills` int(11) DEFAULT NULL,
  `play_kodetu` int(11) DEFAULT NULL,
  `tech_like` int(11) DEFAULT NULL,
  `db_timestamp` int(11) DEFAULT NULL,
  `role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tos` tinyint(1) DEFAULT NULL,
  `grupo` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNIQ_1483A5E9F85E0677` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

[users] - [4, LearningLab, $2y$13$O8m4CJWcuO1dEu15DtKhEO4O4Z3ol2V00Xprd7rQ1E9GawOqIruoS, NULL, es, Deusto, 1, pri, 24, male, ES, , NULL, NULL, 1, 1, 1, 10, 1544790000, ALUMNO, 1, NULL] - [id, username, password, name, lang, school, course, plan, age, gender, country, location, registration_date, ava...
[users] - [41, andoni, $2y$13$JXPI0WjJa0yb2UwM0G7/.uXd1qDRsFl.1F/taFncDclmIQwj/Yv5m, NULL, es, Test, 6, pri, 99, other, ES, Bilbao, NULL, NULL, 1, 1, 1, 10, 1546856880, ALUMNO, 1, NULL] - [id, username, password, name, lang, school, course, plan, age, gender, country, location, registration_date, av...
[users] - [44, g6lp, $2y$13$JX96m67l.kl0ToibD/v5v.L7JlCTnPuW91dVIfJ0bvpsTQP0/PVka, NULL, es, Santa María Marianistas, 6, pri, 11, female, ES, Vitoria-Gasteiz, NULL, NULL, 1, 0, 0, 10, 1547083716, INVITADO, 1, 3] - [id, username, password, name, lang, school, course, plan, age, gender, country, locat...
[users] - [45, e7j6, $2y$13$3fXQberkqWl8CUEMLUKw5uBACbIu6k7nxPGde0nFupOxv8ON5CSES, NULL, es, Marianistas, 6, pri, 11, male, ES, Vitoria-Gasteiz, NULL, NULL, 1, 0, 0, 8, 1547083716, INVITADO, 1, 3] - [id, username, password, name, lang, school, course, plan, age, gender, country, location, registrati...
[users] - [46, d80o, $2y$13$fvevMiFckTS9h81drh0L5e2ty9WLVVsUnvmlv2AbHEGPGyRusVauy, NULL, es, Marianistas, 6, pri, 11, male, ES, Vitoria-Gasteiz, NULL, NULL, 1, 0, 0, 6, 1547083716, INVITADO, 1, 3] - [id, username, password, name, lang, school, course, plan, age, gender, country, location, registrati...
[users] - [47, g9o8, $2y$13$z3uTwzrtfk4nF9cUyqZlwObv3PYfUS5kWBvhLnwOcYle/432J7.9m, NULL, es, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, NULL, NULL, NULL, 1547083716, INVITADO, 0, 3] - [id, username, password, name, lang, school, course, plan, age, gender, country, location, registratio...
[users] - [48, ejjn, $2y$13$2Lp1ve6x7C/9St9YLnxc4.TbmRmtNLlFNHdvRRsRiC3T81CHAYbTO, NULL, es, santamaria marianistas, 6, sec, 11, male, ES, vitoria, NULL, NULL, 1, 1, 1, 10, 1547083716, INVITADO, 1, 3] - [id, username, password, name, lang, school, course, plan, age, gender, country, location, regist...
[users] - [49, dkkw, $2y$13$3/RvtHeylhBZHBPA8X2p9.M9dyVBnoBJKtKm5cXaziycxIqmWotsi, NULL, es, Santa María Marianistas , 6, pri, 11, female, ES, Vitoria-Gasteiz, NULL, NULL, 1, 1, 0, 10, 1547083716, INVITADO, 1, 3] - [id, username, password, name, lang, school, course, plan, age, gender, country, loca...
[users] - [50, gl33, $2y$13$6bwlra82jDegYYkthcfK8.Iq6b.65bWcIIiFjfgJva/1yAjNTYJM2, NULL, es, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, NULL, NULL, NULL, 1547083720, INVITADO, 0, 3] - [id, username, password, name, lang, school, course, plan, age, gender, country, location, registratio...
[users] - [51, emqv, $2y$13$FlcA/GKBkMfX.IhcagZSy.yiX9196rVSpbVgUNlghGsB1.qQBH1Ny, NULL, es, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, NULL, NULL, NULL, 1547083720, INVITADO, 0, 3] - [id, username, password, name, lang, school, course, plan, age, gender, country, location, registratio...
[users] - [52, dnrk, $2y$13$kSKbjFExKPSug64vfifjR.dIl6Ui4zNB5O0PfOFNLMlBnzj7pL0wW, NULL, es, Santa María Marianistas, 6, pri, 11, male, ES, Vitoria - Gasteiz, NULL, NULL, 1, 0, 0, 8, 1547083720, INVITADO, 1, 3] - [id, username, password, name, lang, school, course, plan, age, gender, country, locati...
...
	
+*/
