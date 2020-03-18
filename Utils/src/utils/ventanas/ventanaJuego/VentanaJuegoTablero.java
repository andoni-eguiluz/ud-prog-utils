package utils.ventanas.ventanaJuego;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

/** Clase de utilidad para poder realizar juegos de tablero
 * utilizando una ventana con elementos gráficos.
 * @author andoni.eguiluz.moran
 */
public class VentanaJuegoTablero {
	private JFrame frame;
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
	
	private ArrayList<CoordTablero> arrastresRaton;  // lista de arrastres pendientes en pares pulsación - suelta
	private CoordTablero pulsacionRaton = null;
	
	private long tiempoAnimMsg = 500L;  // Tiempo para un paso de animación (en milisegundos).
	private long tiempoFrameAnimMsg = tiempoAnimMsg/40L;  // Msg entre cada paso de refresco de animación
	private HiloAnimacion hilo = null;  // Hilo de la animación
	private ArrayList<Animacion> animacionesPendientes = new ArrayList<Animacion>();
	private ArrayList<ObjetoDeJuego> disminucionesPendientes = new ArrayList<ObjetoDeJuego>();
	
	/** Construye una nueva ventana de juego de tablero,
	 * y la muestra en el centro de la pantalla.
	 * @param anchuraVent	Anchura de la ventana en pixels
	 * @param alturaVent	Altura de la ventana en pixels
	 * @param filas	Filas del tablero
	 * @param columnas	Columnas del tablero
	 * @param casCuadradas	true si las casillas del tablero son cuadradas, false en caso contrario
	 */
	public VentanaJuegoTablero( int anchuraVent, int alturaVent, int filas, int columnas, boolean casCuadradas ) {
		frame = new JFrame();
		anchVentana = anchuraVent;
		altVentana = alturaVent;
		filasTablero = filas;
		colsTablero = columnas;
		casillasCuadradas = casCuadradas;
		try {
			SwingUtilities.invokeAndWait( new Runnable() {
				@Override
				public void run() {
					arrastresRaton = new ArrayList<CoordTablero>();
					frame.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
					frame.setSize( anchVentana, altVentana );
					frame.setLocationRelativeTo( null );
					frame.setResizable( false );
					frame.setTitle( "Ventana de juego de tablero" );
					frame.getContentPane().add( pAreaJuego, BorderLayout.CENTER );
					frame.getContentPane().add( lMensaje, BorderLayout.SOUTH );
					pAreaJuego.setBackground( Color.white );
					pAreaJuego.setLayout( null );  // layout de posicionamiento absoluto
					lMensaje.setHorizontalAlignment( JLabel.CENTER );
					frame.setVisible( true );
					calcTamanyo();
					pAreaJuego.addFocusListener( new FocusAdapter() {
						@Override
						public void focusLost(FocusEvent arg0) {
							pAreaJuego.requestFocus();
						}
					});
					pAreaJuego.addMouseListener( new MouseAdapter() {
						@Override
						public void mouseReleased(MouseEvent arg0) {
							CoordTablero sueltaRaton = pixsToCoord( arg0.getX(), arg0.getY() );
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
							pulsacionRaton = pixsToCoord( arg0.getX(), arg0.getY() );
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
			pixelsPorColumna = pAreaJuego.getWidth() *1.0F / colsTablero;
			origenX = 0;
			origenY = 0;
			finX = pAreaJuego.getWidth() - 1;
			finY = pAreaJuego.getHeight() - 1;
			if (casillasCuadradas) {  // si son cuadradas hay que centrar el tablero en el panel
				if (pAreaJuego.getHeight() > pAreaJuego.getWidth()) {  // Más alto que ancho
					int pixelsSobran = pAreaJuego.getHeight() - pAreaJuego.getWidth();
					origenY = pixelsSobran / 2;
					pixelsPorFila = pixelsPorColumna;
					finY = Math.round( origenY + pixelsPorFila*filasTablero + 0.5f );
					pRelleno1.setBounds( 0, 0, pAreaJuego.getWidth(), origenY );
					pRelleno2.setBounds( finY, 0, pAreaJuego.getHeight(), origenY );
					pAreaJuego.add( pRelleno1 );
					pAreaJuego.add( pRelleno2 );
				} else {  // Más ancho que alto
					int pixelsSobran = pAreaJuego.getWidth() - pAreaJuego.getHeight();
					origenX = pixelsSobran / 2;
					pixelsPorColumna = pixelsPorFila;
					finX = Math.round( origenX + pixelsPorColumna*colsTablero + 0.5f );
					pRelleno1.setBounds( 0, 0, origenX, pAreaJuego.getHeight());
					pRelleno2.setBounds( finX, 0, origenX, pAreaJuego.getHeight());
					pAreaJuego.add( pRelleno1 );
					pAreaJuego.add( pRelleno2 );
				}
			}
		}

	/** Cierra y finaliza la ventana de juego (no acaba la aplicación). Llamar siempre a este método al final. 
	 * ATENCION: Si no se llama a este método Java sigue activo aunque el main que lo ejecuta se acabe
	 * (Si sabes de hilos... este hilo cierra la ventana para que Swing pueda acabar)
	 */
	public void finish() {
		if (hilo!=null) { hilo.interrupt(); }
		frame.dispose();
	}
	
	// 
	/** Indica si la coordenada es válida para el tablero especificado
	 * @param ct	Coordenada de tablero
	 * @return	true si la coordenada está dentro del tablero, false en caso contrario
	 */
	public boolean estaEnTablero( CoordTablero ct ) {
		return (ct.getFila() >= 0 && ct.getFila() < filasTablero &&
			    ct.getColumna() >= 00 && ct.getColumna() < colsTablero);
	}
	
	/** Devuelve el número de pixels de ancho de cada casilla del tablero
	 * @return	Número de píxels de ancho
	 */
	public int getAnchoCasilla() {
		return (int) (pixelsPorColumna);
	}
	
	/** Devuelve el número de pixels de alto de cada casilla del tablero
	 * @return	Número de píxels de alto
	 */
	public int getAltoCasilla() {
		return (int) (pixelsPorFila);
	}
	
	/** Espera a que haya un drag en el tablero y devuelve la casilla de origen
	 * del desplazamiento. (usar SIEMPRE #getDragEnd() para coger la casilla de final)<p>
	 * Si la ventana se cierra, devuelve null.
	 * @return	Casilla de origen, o null si la ventana se ha cerrado
	 */
	public CoordTablero readInicioDrag() {
		while (arrastresRaton.isEmpty() && frame.isVisible()) {
			// Espera hasta que el ratón haga algún drag
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) { }
		}
		if (!frame.isVisible()) return null;
		return arrastresRaton.get(0);
	}
	
	/** Devuelve la casilla de final de un desplazamiento. Debe realizarse
	 * siempre después de haber leído la casilla inicial con #waitForDrag()
	 * @return	casilla de final de desplazamiento, null si no lo ha habido
	 */
	public CoordTablero getFinalDrag() {
		if (arrastresRaton.size() < 2)
			return null;
		CoordTablero destino = arrastresRaton.get(1);
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
		return !frame.isVisible();
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
	
	/** Sustituye del tablero un objeto de juego por otro (en su misma posición).<br>
	 * Realiza un efecto de intercambio para que visualmente se aprecie.
	 * Si el objeto no estaba, no ocurre nada.
	 * @param objeto	Objeto de juego a sustituir
	 * @param objetoNuevo	Objeto de juego a poner en su lugar
	 */
	public void cambiaObjeto( final ObjetoDeJuego objeto, final ObjetoDeJuego objetoNuevo ) {
		(new Thread() {
			@Override
			public void run() {
				try {
					CoordTablero c = getPosTablero( objeto );
					setPosTablero( objetoNuevo, c );
					pAreaJuego.add( objetoNuevo );
					objeto.setVisible( false );
					Thread.sleep( 100 );
					// objetoNuevo.setVisible( false );
					objeto.setVisible( true );
					Thread.sleep( 100 );
					// objetoNuevo.setVisible( true );
					objeto.setVisible( false );
					Thread.sleep( 100 );
					// objetoNuevo.setVisible( false );
					objeto.setVisible( true );
					Thread.sleep( 100 );
					// objetoNuevo.setVisible( true );
					SwingUtilities.invokeLater( new Runnable() {
						@Override
						public void run() {
							pAreaJuego.repaint();
							pAreaJuego.remove( objeto );
						}
					});
				} catch (Exception e) {
				}
			}
		}).start();
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
	 * @param ct	Coordenada de tablero a la que mover el objeto
	 */
	public void setPosTablero( ObjetoDeJuego oj, CoordTablero ct ) {
		oj.setLocation( coordToPixs(ct) );
	}
	
	/** Mueve un objeto de juego a la posición indicada
	 * realizando una animación (de un paso).<br>
	 * El objeto debe ser != null y estar añadido al tablero.<p>
	 * Si el objeto ya tenía una animación en curso, se completa con esta
	 * desde donde estuviera.
	 * @param oj	Objeto de juego a mover
	 * @param ct	Coordenada de tablero a la que mover el objeto
	 */
	public void movePosTablero( ObjetoDeJuego oj, CoordTablero ct ) {
		if (oj!=null) {
			if (hilo==null) { hilo = new HiloAnimacion(); hilo.start(); }
			Point pHasta = coordToPixs(ct);
			Animacion a = new Animacion( oj.getX(), pHasta.getX(), 
					oj.getY(), pHasta.getY(), tiempoAnimMsg, oj );
			if (animacionesPendientes.indexOf(a) == -1)
				// Si el objeto es nuevo se mete en animaciones pendientes
				animacionesPendientes.add( a );
			else {  // Si ya estaba se actualiza esa animación (ojo, puede generar diagonales o cosas raras)
				int pos = animacionesPendientes.indexOf(a);
				animacionesPendientes.get(pos).xHasta = pHasta.getX();
				animacionesPendientes.get(pos).yHasta = pHasta.getY();
				animacionesPendientes.get(pos).msFaltan = tiempoAnimMsg;
			}
			// oj.setLocation( coordToPixs(ct) );  // Lo hace la animación, no se hace aquí
		}
	}
	
	/** Anima varios objetos de juego haciendo un escalado en disminución hasta desaparecer.<br>
	 * Los objetos deben ser != null y estar añadidos al tablero.<p>
	 * No se quitan del tablero ni de la ventana después de la animación (debe hacerse aparte llamando después al método de eliminación)
	 * @param loj	Lista de objetos de juego a disminuir
	 */
	public void disminuyeObjetos( ArrayList<ObjetoDeJuego> loj ) {
		if (loj!=null) {
			if (hilo==null) { hilo = new HiloAnimacion(); hilo.start(); }
			disminucionesPendientes.addAll( loj );
		}
	}
	
	/** Pone los tiempos para realizar las animaciones visuales en pantalla.
	 * @param tiempoAnimMsg	Tiempo para un paso de animación (en milisegundos).
	 * Debe ser mayor o igual que 100 msg (si no, este método no hace nada).
	 * Por defecto es de 500 msg.
	 * @param numMovtos	Número de fotogramas para cada paso de animación
	 * (veces que se refresca la animación dentro de cada paso).
	 * Debe ser un valor entre 2 y tiempoAnimMsg (si no, este método no hace nada).
	 * Por defecto es de 40 msg.
	 */
	public void setTiempoPasoAnimacion( long tiempoAnimMsg, int numMovtos ) {
		if (tiempoAnimMsg < 100L || numMovtos < 2 || numMovtos > tiempoAnimMsg) 
			return;  // Error: no se hace nada
		this.tiempoAnimMsg = tiempoAnimMsg;
		this.tiempoFrameAnimMsg = tiempoAnimMsg/numMovtos;
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
		
	/** Espera sin hacer nada a que acaben las animaciones
	 */
	public void esperaAFinAnimaciones() {
		do {
			try {
				Thread.sleep( tiempoFrameAnimMsg );
			} catch (InterruptedException e) {
			}
		} while (!animacionesPendientes.isEmpty() || !disminucionesPendientes.isEmpty());
	}
		
		private static class Animacion {
			double xDesde;    // Desde qué x
			double xHasta;    // hasta qué x
			double yDesde;    // Desde qué y
			double yHasta;    // hasta qué y
			long msFaltan;    // en cuántos msg
			ObjetoDeJuego oj; // objeto a animar
			public Animacion(double xDesde, double xHasta, double yDesde,
					double yHasta, long msFaltan, ObjetoDeJuego oj) {
				this.xDesde = xDesde;
				this.xHasta = xHasta;
				this.yDesde = yDesde;
				this.yHasta = yHasta;
				this.msFaltan = msFaltan;
				this.oj = oj;
			}
			Point calcNextFrame( long msPasados ) {
				if (msFaltan <= msPasados) {  // Llegar al final
					msFaltan = 0;
					return new Point( (int)Math.round(xHasta), (int)Math.round(yHasta) );
				} else if (msPasados <= 0) {  // No se ha movido
					return new Point( (int)Math.round(xDesde), (int)Math.round(yDesde) );
				} else {  // Movimiento normal
					xDesde = xDesde + (xHasta-xDesde)/msFaltan*msPasados;
					yDesde = yDesde + (yHasta-yDesde)/msFaltan*msPasados;
					msFaltan -= msPasados;
					return new Point( (int)Math.round(xDesde), (int)Math.round(yDesde) );
				}
			}
			boolean finAnimacion() {
				return (msFaltan <= 0);
			}
			// equals para buscar varias animaciones del mismo objeto
			// (se compara solo el oj)
			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof Animacion)) return false;
				return (oj == ((Animacion)obj).oj);
			}
			@Override
			public String toString() {
				return "Animacion (" + xDesde + "," + yDesde + ") -> ("
						+ xHasta + "," + yHasta + ") msg: " + msFaltan;
			}
		}
	
	class HiloAnimacion extends Thread {
		@Override
		public void run() {
			while (!interrupted()) {
				try {
					Thread.sleep( tiempoFrameAnimMsg );
				} catch (InterruptedException e) {
					break;  // No haría falta, el while se interrumpe en cualquier caso y se acaba el hilo
				}
				for (int i=animacionesPendientes.size()-1; i>=0; i--) {  // Al revés porque puede haber que quitar animaciones si se acaban
					Animacion a = animacionesPendientes.get(i);
					if (a.oj != null) a.oj.setLocation( 
						a.calcNextFrame( tiempoFrameAnimMsg ) );  // Actualizar animación
					if (a.finAnimacion()) animacionesPendientes.remove(i);  // Quitar si se acaba
				}
				for (int i=disminucionesPendientes.size()-1; i>=0; i--) {
					ObjetoDeJuego oj = disminucionesPendientes.get(i);
					if (oj.getAnchuraObjeto()<2 || oj.getAlturaObjeto()<2) {
						disminucionesPendientes.remove(i);
					} else {
						oj.setLocation( oj.getX()+1, oj.getY()+1 );
						oj.setSize( oj.getAnchuraObjeto()-2, oj.getAlturaObjeto()-2 );
					}
				}
			}
		}
	}


	/** Método de prueba de la clase.
	 * @param args	No utilizado
	 */
	public static void main(String[] args) {
		int FILAS = 6;
		int COLS = 8;
		ObjetoDeJuego[][] tablero = new ObjetoDeJuego[FILAS][COLS];
		VentanaJuegoTablero v = new VentanaJuegoTablero( 960, 520, FILAS, COLS, false );
		v.showMessage( "Juego en curso" );
		ObjetoDeJuego o1 = new ObjetoDeJuego( "UD-blue.png", true, v.getAnchoCasilla(), v.getAltoCasilla() );
		ObjetoDeJuego o2 = new ObjetoDeJuego( "UD-red.png", true, v.getAnchoCasilla(), v.getAltoCasilla() );
		ObjetoDeJuego o3 = new ObjetoDeJuego( "UD-green.png", true, v.getAnchoCasilla(), v.getAltoCasilla() );
		ObjetoDeJuego o4 = new ObjetoDeJuego( "noExiste.png", true, v.getAnchoCasilla(), v.getAltoCasilla() );
		v.addObjeto( o1, new CoordTablero(0, 0) );
		v.addObjeto( o2, new CoordTablero(0, 4) );
		v.addObjeto( o3, new CoordTablero(2, 2) );
		v.addObjeto( o4, new CoordTablero(5, 3) );
		tablero[0][0] = o1;
		tablero[0][4] = o2;
		tablero[2][2] = o3;
		tablero[5][3] = o4;
		while (!v.isClosed()) {
			CoordTablero c1 = v.readInicioDrag();
			if (c1 != null) {
				CoordTablero c2 = v.getFinalDrag();
				System.out.println( "Drag en " + c1 + " --> " + c2 );
				if (tablero[c1.getFila()][c1.getColumna()] != null) {
					ObjetoDeJuego inicio = tablero[c1.getFila()][c1.getColumna()];
					ObjetoDeJuego finl = tablero[c2.getFila()][c2.getColumna()];
					tablero[c1.getFila()][c1.getColumna()] = finl;
					tablero[c2.getFila()][c2.getColumna()] = inicio;
					if (inicio.getNombreImagen().equals("UD-green.png"))
						v.movePosTablero( inicio, c2 );
					else
						v.setPosTablero( inicio, c2 );
					if (finl!=null) {
						v.setPosTablero( finl, c1 );
					}
				}
			}
		}
		v.finish();
	}
	
}
