package utils.ventanas.ventanaJuego;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/** Clase de utilidad para poder realizar juegos de tablero
 * utilizando una ventana con elementos gráficos.
 * @author eguiluz
 */
@SuppressWarnings("serial")
public class VentanaEdicionTablero extends JFrame {
	private JLabel lMensaje = new JLabel( " " );
	private JPanel pAreaJuego = new JPanel();
	private int anchVentana;  // pixels de anchura de la ventana
	private int altVentana;  // pixels de altura de la ventana
	private int filasTablero;  // filas con las que se inicializa el tablero
	private int colsTablero;  // columnas con las que se inicializa el tablero
	private boolean casillasCuadradas;  // las casillas del tablero son cuadradas?
	private float pixelsPorFila;  // pixels por Fila
	private float pixelsPorColumna;  // pixels por Columna
	private int origenX;  // pixel de origen X de tablero
	private int origenY;  // pixel de origen Y de tablero
	private int finX; // pixel de fin X de tablero
	private int finY; // pixel de fin Y de tablero

	private JPanel pEdicion = new JPanel();
	private JButton bPut = new JButton( "Put" );
	private JButton bGet = new JButton( "Get" );
	private JTextField tfTexto = new JTextField( "" );
	private int filasEdicion;  // filas de zona de edición
	private int colsEdicion;   // columnas de zona de edición
	private ObjetoDeJuego[] objetosEdicion;  // objetos en la zona de edición
	private int numObjsEdicion;  // núm max. objetos en la zona de edición
	private static int PIXELS_SEPARACION = 10;
	private int origenEdX;
	private int finEdX;
	private boolean pulsadoBotPut = false;
	private boolean pulsadoBotGet = false;

	private ArrayList<Object> arrastresRaton;  // lista de arrastres pendientes en pares pulsación - suelta
	private Object pulsacionRaton = null;  // CoordTablero (en tablero) o Integer (en edición)

	
	/** Construye una nueva ventana de juego de tablero,
	 * y la muestra en el centro de la pantalla.
	 * @param anchuraVent	Anchura de la ventana en pixels
	 * @param alturaVent	Altura de la ventana en pixels
	 * @param filas	Filas del tablero
	 * @param columnas	Columnas del tablero
	 * @param casCuadradas	true si las casillas del tablero son cuadradas, false en caso contrario
	 * @param pNumPiezasEdicion	Número de piezas que se van a poner en el panel derecho para edición de tablero
	 * (se pueden definir con el método addPiezaEdicion)
	 */
	public VentanaEdicionTablero( int anchuraVent, int alturaVent, int filas, int columnas, boolean casCuadradas, int pNumPiezasEdicion ) {
		anchVentana = anchuraVent;
		altVentana = alturaVent;
		filasTablero = filas;
		colsTablero = columnas;
		casillasCuadradas = casCuadradas;
		numObjsEdicion = pNumPiezasEdicion;
		filasEdicion = filasTablero - 1;
		colsEdicion = (numObjsEdicion-1) / filasEdicion + 1;
		try {
			SwingUtilities.invokeAndWait( new Runnable() {
				@Override
				public void run() {
					arrastresRaton = new ArrayList<Object>();
					setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
					setSize( anchVentana, altVentana );
					setLocationRelativeTo( null );
					setResizable( false );
					setTitle( "Ventana de juego de tablero" );
					getContentPane().add( pAreaJuego, BorderLayout.CENTER );
					getContentPane().add( lMensaje, BorderLayout.SOUTH );
					pEdicion.setBackground( Color.lightGray );
					pEdicion.setLayout( null );
					pAreaJuego.setBackground( Color.white );
					pAreaJuego.setLayout( null );  // layout de posicionamiento absoluto
					lMensaje.setHorizontalAlignment( JLabel.CENTER );
					setVisible ( true );
					calcTamanyo();
					objetosEdicion = new ObjetoDeJuego[numObjsEdicion];
					bPut.addActionListener( new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							pulsadoBotPut = true;
						}
					});
					bGet.addActionListener( new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							pulsadoBotGet = true;
						}
					});
					pAreaJuego.addFocusListener( new FocusAdapter() {
						@Override
						public void focusLost(FocusEvent arg0) {
							pAreaJuego.requestFocus();
						}
					});
					pAreaJuego.addMouseListener( new MouseAdapter() {
						@Override
						public void mouseReleased(MouseEvent arg0) {
							Object sueltaRaton = null;
							if (arg0.getX() < origenEdX) {
								sueltaRaton = pixsToCoord( arg0.getX(), arg0.getY() );
							} else if (arg0.getX() >= origenEdX) {
								sueltaRaton = pixsToPosEdicion( arg0.getX()-origenEdX, arg0.getY() );
							}
							if (pulsacionRaton == null) {  // No ha habido pressed
								// Nada que hacer
							} else if (pulsacionRaton.equals( sueltaRaton )) {  // Misma posición tablero pulsación y suelta
								// No se controlan clicks (si se controlaran, se haría aquí)
							} else {  // Arrastre (distinta posición)
								if (estaEnTablero(pulsacionRaton) && estaEnTablero(sueltaRaton)) {
									arrastresRaton.add( pulsacionRaton );
									arrastresRaton.add( sueltaRaton );
								}
							}
							pulsacionRaton = null;
						}
						@Override
						public void mousePressed(MouseEvent arg0) {
							if (arg0.getX() >= origenEdX) {
								pulsacionRaton = pixsToPosEdicion( arg0.getX()-origenEdX, arg0.getY() );
							} else {
								pulsacionRaton = pixsToCoord( arg0.getX(), arg0.getY() );
							}
						}
						@Override
						public void mouseClicked(MouseEvent arg0) {
							// Nada que hacer, se controla con released y pressed
						}
					});
				}
			} );
		} catch (Exception e) {
		}
	}
	
		// Calcula la posición del tablero correspondiente a una posición
		// real de pixels
		private CoordTablero pixsToCoord( int pixX, int pixY ) {
			pixX -= origenX;
			pixY -= origenY;
			return new CoordTablero( Math.round( pixY / pixelsPorFila - 0.5f ), 
					Math.round( pixX / pixelsPorColumna - 0.5f ) );
		}
		
		// Calcula la posición de edición correspondiente a una posición
		// real de pixels
		private Integer pixsToPosEdicion( int pixX, int pixY ) {
			pixY -= origenY;
			int fila = Math.round( pixY / pixelsPorFila - 0.5f ); 
			int col = Math.round( pixX / pixelsPorColumna - 0.5f );
			return new Integer( fila + col*filasEdicion );
		}
		
		// Calcula la posición de pixels de panel correspondiente a una posición
		// de tablero
		private Point coordToPixs( CoordTablero ct ) {
			return new Point( Math.round( origenX + ct.getColumna() * pixelsPorColumna ), 
					Math.round( origenY + ct.getFila() * pixelsPorFila ) );
		}
	
		// Calcula el tamaño interno de la ventana y el tablero
		// junto a las variables necesarias para el dibujado
			private JPanel pRelleno1 = new JPanel();
			private JPanel pRelleno2 = new JPanel();
		private void calcTamanyo() {
			pixelsPorFila = pAreaJuego.getHeight() *1.0F / filasTablero; 
			pixelsPorColumna = (pAreaJuego.getWidth() - PIXELS_SEPARACION) *1.0F / (colsTablero + colsEdicion);
			origenX = 0;
			origenY = 0;
			finX = pAreaJuego.getWidth() - 1;
			finY = pAreaJuego.getHeight() - 1;
			if (casillasCuadradas) {  // si son cuadradas hay que centrar el tablero en el panel
				if (pixelsPorColumna >= pixelsPorFila) {  // Sobra de ancho
					pixelsPorColumna = pixelsPorFila;
					int pixelsSobran = (int) (pAreaJuego.getWidth() - PIXELS_SEPARACION - pixelsPorColumna * (colsEdicion+colsTablero));
					origenX = pixelsSobran / 2;
					finX = Math.round( origenX + pixelsPorColumna*colsTablero + 0.5f );
					origenEdX = finX + PIXELS_SEPARACION;
					finEdX = Math.round( origenEdX + pixelsPorColumna*colsEdicion + 0.5f );
					pEdicion.setBounds( origenEdX, 0, (finEdX-origenEdX), pAreaJuego.getHeight());
					pRelleno1.setBounds( 0, 0, origenX, pAreaJuego.getHeight());
					pRelleno2.setBounds( finEdX, 0, origenX, pAreaJuego.getHeight());
				} else {  // Sobra de alto
					pixelsPorFila = pixelsPorColumna;
					int pixelsSobran = (int) (pAreaJuego.getHeight() - filasTablero*pixelsPorFila);
					origenY = pixelsSobran / 2;
					finY = Math.round( origenY + pixelsPorFila*filasTablero + 0.5f );
					origenEdX = finX + PIXELS_SEPARACION;
					finEdX = pAreaJuego.getWidth();
					pEdicion.setBounds( origenEdX, origenY, (finEdX-origenEdX), (finY-origenY) );
					pRelleno1.setBounds( 0, 0, pAreaJuego.getWidth(), origenY );
					pRelleno2.setBounds( finY, 0, pAreaJuego.getHeight(), origenY );
				}
			} else {
				origenEdX = finX + PIXELS_SEPARACION;
				finEdX = Math.round( origenEdX + pixelsPorColumna*colsEdicion + 0.5f );
				pEdicion.setBounds( origenEdX, 0, (finEdX-origenEdX), pAreaJuego.getHeight());
				pRelleno1.setBounds( 0, 0, origenX, pAreaJuego.getHeight());
				pRelleno2.setBounds( finEdX, 0, origenX, pAreaJuego.getHeight());
			}
			pAreaJuego.add( pRelleno1 );
			pAreaJuego.add( pRelleno2 );
			pAreaJuego.add( pEdicion );
			tfTexto.setBounds( 0, (int)(origenY + pixelsPorFila*filasEdicion), (int)(pixelsPorColumna*colsEdicion), (int)(pixelsPorFila/2) );
			bPut.setBounds( 0, (int)(origenY + pixelsPorFila*filasEdicion + pixelsPorFila / 2), (int)(pixelsPorColumna*colsEdicion/2), (int)(pixelsPorFila/2) );
			bGet.setBounds( (int)(pixelsPorColumna*colsEdicion/2), (int)(origenY + pixelsPorFila*filasEdicion + pixelsPorFila / 2), (int)(pixelsPorColumna*colsEdicion/2), (int)(pixelsPorFila/2) );
			pEdicion.add( tfTexto );
			pEdicion.add( bPut );
			pEdicion.add( bGet );
			pEdicion.validate();
		}
		
	/** Añade al tablero un objeto de juego en la zona de edición.<br>
	 * Atención, si el mismo objeto se añade dos veces sólo se 
	 * tiene en cuenta una.
	 * @param oj	Objeto de juego a introducir en la edición
	 * @param posi	Posición en la que poner el objeto en la zona de edición
	 * (de 0 a n-1)
	 */
	public void addObjetoEdicion( final ObjetoDeJuego oj, final int posi ) {
		if (posi >= numObjsEdicion) return;
		oj.setLocation( (int)((posi/filasEdicion) * pixelsPorColumna), (int)(origenY + (posi%filasEdicion) * pixelsPorFila) );
		oj.setPanelJuego( pEdicion );
		try {
			SwingUtilities.invokeAndWait( new Runnable() {
				@Override
				public void run() {
					if (objetosEdicion[posi] != null) pEdicion.remove( oj );
					objetosEdicion[posi] = oj;
					pEdicion.add( oj );
					pEdicion.repaint();
				}
			});
		} catch (Exception e) {
		}
	}

	/** Devuelve el objeto de juego indicado de la zona de edición.<br>
	 * @param posi	Posición secuencial del objeto de edición (de 0 a n-1)
	 * @return null si no hay objeto, Objeto de juego de la posición en caso contrario
	 */
	public ObjetoDeJuego getObjetoEdicion( int posi ) {
		if (posi >= numObjsEdicion) return null;
		return objetosEdicion[posi];
	}
	
	/** Consulta si se ha pulsado el botón, y lo reinicia
	 * @return	true si se ha pulsado el botón Put, false en caso contrario
	 */
	public boolean pulsadoBotPut() {
		boolean p = pulsadoBotPut;
		pulsadoBotPut = false;
		return p;
	}
	
	/** Consulta si se ha pulsado el botón, y lo reinicia
	 * @return	true si se ha pulsado el botón Get, false en caso contrario
	 */
	public boolean pulsadoBotGet() {
		boolean p = pulsadoBotGet;
		pulsadoBotGet = false;
		return p;
	}
	
	/** Consulta el texto de edición existente en la ventana
	 * @return	Texto editado por el usuario en la casilla
	 */
	public String getTextoEdicion() {
		return tfTexto.getText();
	}
	
	/** Cierra y finaliza la ventana de juego (no acaba la aplicación).
	 */
	public void finish() {
		dispose();
	}
	
	// 
	/** Indica si la coordenada es válida para el tablero especificado
	 * @param coor	Coordenada de tablero (CoordTablero) o posición de edición (Integer)
	 * @return	true si la coordenada está dentro del tablero, false en caso contrario
	 */
	public boolean estaEnTablero( Object coor ) {
		if (coor instanceof CoordTablero) {
			CoordTablero ct = (CoordTablero) coor;
			return (ct.getFila() >= 0 && ct.getFila() < filasTablero &&
				    ct.getColumna() >= 00 && ct.getColumna() < colsTablero);
		} else {  // Integer
			Integer i = (Integer) coor;
			return (i.intValue() < numObjsEdicion);
		}
	}
	
	/** Devuelve el número de pixels de ancho de cada casilla del tablero
	 * @return
	 */
	public int getAnchoCasilla() {
		return (int) (pixelsPorColumna);
	}
	
	/** Devuelve el número de pixels de alto de cada casilla del tablero
	 * @return
	 */
	public int getAltoCasilla() {
		return (int) (pixelsPorFila);
	}
	
	/** Espera a que haya un drag en el tablero y devuelve la casilla o posición de origen
	 * del desplazamiento. (usar SIEMPRE getDragEnd() para coger la casilla de final)<p>
	 * Si la ventana se cierra, devuelve null.
	 * @param milis	Milisegundos a esperar antes del drag
	 * @return	Casilla de origen (CoordTablero) si es en el tablero, posición (Integer) si es en el espacio de edición
	 * o null si la ventana se ha cerrado o ha pasado el tiempo indicado sin haber drag
	 */
	public Object readInicioDrag( long milis ) {
		long milisDesde = System.currentTimeMillis();
		while (arrastresRaton.isEmpty() && isVisible() && (milisDesde + milis > System.currentTimeMillis())) {
			// Espera hasta que el ratón haga algún drag o a que pase el tiempo
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) { }
		}
		if (!isVisible()) return null;
		if (arrastresRaton.isEmpty()) return null;
		return arrastresRaton.get(0);
	}
	
	/** Devuelve la casilla de final de un desplazamiento. Debe realizarse
	 * siempre después de haber leído la casilla inicial con #waitForDrag()
	 * @return	Casilla de final (CoordTablero) si es en el tablero, posición (Integer) si es en el espacio de edición
	 * o null si no ha habido desplazamiento
	 */
	public Object getFinalDrag() {
		if (arrastresRaton.size() < 2)
			return null;
		Object destino = arrastresRaton.get(1);
		arrastresRaton.remove(0);
		arrastresRaton.remove(0);
		return destino;
	}
	
	/** Visualiza un mensaje en la línea de mensajes
	 * @param s	String a visualizar en la línea inferior de la ventana
	 */
	public void showMessage( String s ) {
		lMensaje.setText( s );
	}
	
	/** Informa si la ventana ha sido cerrada por el usuario
	 * @return	true si la ventana se ha cerrado, false si sigue visible
	 */
	public boolean isClosed() {
		return !isVisible();
	}
	
	/** Añade al tablero un objeto de juego, que se visualizará 
	 * inmediatamente si está marcado para ser visible.<br>
	 * Atención, si el mismo objeto se añade dos veces sólo se 
	 * tiene en cuenta una.
	 * @param oj	Objeto de juego a introducir
	 * @param c	Coordenada de tablero en la que poner el objeto
	 */
	public void addObjeto( final ObjetoDeJuego oj, CoordTablero c ) {
		setPosTablero( oj, c );
		oj.setPanelJuego( pAreaJuego );
		try {
			SwingUtilities.invokeAndWait( new Runnable() {
				@Override
				public void run() {
					pAreaJuego.add( oj );
					pAreaJuego.repaint();
				}
			});
		} catch (Exception e) {
		}
	}
	
	/** Quita del tablero el objeto de juego.<br>
	 * Si el objeto no estaba, no ocurre nada.
	 * @param oj	Objeto de juego a eliminar
	 */
	public void removeObjeto( final ObjetoDeJuego oj ) {
		try {
			SwingUtilities.invokeAndWait( new Runnable() {
				@Override
				public void run() {
					pAreaJuego.remove( oj );
					pAreaJuego.repaint();
				}
			});
		} catch (Exception e) {
		}
	}

	/** Quita del tablero todos los objetos de juego.<br>
	 */
	public void quitarTodosLosObjetos() {
		try {
			SwingUtilities.invokeAndWait( new Runnable() {
				@Override
				public void run() {
					for (Component c : pAreaJuego.getComponents()) {
						if (c instanceof ObjetoDeJuego)
							pAreaJuego.remove( c );
					}
					pAreaJuego.repaint();
				}
			});
		} catch (Exception e) {
		}
	}
	
	/** Mueve un objeto de juego a la posición indicada.<br>
	 * El objeto debe ser != null y estar añadido al tablero.
	 * @param oj	Objeto de juego a mover
	 * @param c	Coordenada de tablero a la que mover el objeto
	 */
	public void setPosTablero( ObjetoDeJuego oj, CoordTablero ct ) {
		oj.setLocation( coordToPixs(ct) );
	}
		
	/** Devuelve la posición de tablero del objeto de juego indicado.<br>
	 * El objeto debe ser != null y estar añadido al tablero.
	 * @param oj	Objeto de juego del que devolver la posición
	 * @return	Posición de tablero más cercana a ese objeto
	 */
	public CoordTablero getPosTablero( ObjetoDeJuego oj ) {
		CoordTablero ct = pixsToCoord( Math.round(oj.getX()+pixelsPorColumna/2),
				Math.round(oj.getY()+pixelsPorFila/2) );
		return ct;
	}

	/** Espera sin hacer nada durante el tiempo indicado en milisegundos
	 * @param msg	Tiempo a esperar
	 */
	public void esperaUnRato( int msg ) {
		try {
			Thread.sleep( msg );
		} catch (InterruptedException e) {
		}
	}
		
	/** Método de prueba de la clase.
	 * @param args
	 */
	public static void main(String[] args) {
		int FILAS = 10;
		int COLS = 10;
		ObjetoDeJuego[][] tablero = new ObjetoDeJuego[FILAS][COLS];
		VentanaEdicionTablero v = new VentanaEdicionTablero( 960, 600, FILAS, COLS, true, 15 );
		v.showMessage( "Juego en curso" );
		ObjetoDeJuego o1 = new ObjetoDeJuego( "UD-blue.png", true, v.getAnchoCasilla(), v.getAltoCasilla() );
		ObjetoDeJuego o2 = new ObjetoDeJuego( "UD-red.png", true, v.getAnchoCasilla(), v.getAltoCasilla() );
		ObjetoDeJuego o3 = new ObjetoDeJuego( "UD-green.png", true, v.getAnchoCasilla(), v.getAltoCasilla() );
		ObjetoDeJuego o4 = new ObjetoDeJuego( "noExiste.png", true, v.getAnchoCasilla(), v.getAltoCasilla() );
		v.addObjeto( o1, new CoordTablero(0, 0) );
		v.addObjeto( o2, new CoordTablero(0, 4) );
		v.addObjeto( o3, new CoordTablero(2, 2) );
		v.addObjeto( o4, new CoordTablero(7, 3) );
		v.addObjetoEdicion( new ObjetoDeJuego( "UD-blue.png", true, v.getAnchoCasilla(), v.getAltoCasilla() ), 9 );
		tablero[0][0] = o1;
		tablero[0][4] = o2;
		tablero[2][2] = o3;
		tablero[7][3] = o4;
		while (!v.isClosed()) {
			Object od1 = v.readInicioDrag( 40 );
			if (od1 != null) {
				Object od2 = v.getFinalDrag();
				System.out.println( "Drag en " + od1 + " --> " + od2 );
				if (od1 instanceof CoordTablero && od2 instanceof CoordTablero) {
					CoordTablero c1 = (CoordTablero) od1;
					CoordTablero c2 = (CoordTablero) od2;
					if (tablero[c1.getFila()][c1.getColumna()] != null) {
						ObjetoDeJuego inicio = tablero[c1.getFila()][c1.getColumna()];
						ObjetoDeJuego finl = tablero[c2.getFila()][c2.getColumna()];
						tablero[c1.getFila()][c1.getColumna()] = finl;
						tablero[c2.getFila()][c2.getColumna()] = inicio;
						v.setPosTablero( inicio, c2 );
						if (finl!=null) {
							v.setPosTablero( finl, c1 );
						}
					}
				} else if (od1 instanceof Integer && od2 instanceof CoordTablero) {
					int edi = ((Integer) od1).intValue();
					CoordTablero c2 = (CoordTablero) od2;
					if (tablero[c2.getFila()][c2.getColumna()] == null) {
						ObjetoDeJuego edicion = v.getObjetoEdicion( edi );
						if (edicion != null) {
							ObjetoDeJuego nuevo = (ObjetoDeJuego) (edicion.clone());
							tablero[c2.getFila()][c2.getColumna()] = nuevo;
							v.addObjeto(nuevo, c2);
						}
					}
				}
			}
			if (v.pulsadoBotPut()) {
				System.out.println( "Pulsado botón!! Texto = " + v.getTextoEdicion() );
			}
			if (v.pulsadoBotGet()) {
				System.out.println( "Pulsado botón!! Texto = " + v.getTextoEdicion() );
			}
		}
		v.finish();
	}

}
