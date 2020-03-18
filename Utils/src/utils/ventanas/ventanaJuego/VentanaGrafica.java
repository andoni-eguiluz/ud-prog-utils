package utils.ventanas.ventanaJuego;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.swing.*;

import utils.ventanas.ventanaJuego.eventosVentanaGrafica.*;

// TODO: Usar la capa cristal
// TODO: Usar el panel de control
// TODO: Hacer que la redimensión de la ventana redimensione los paneles cristal y de control

/** Clase de utilidad para poder realizar juegos o animaciones
 * utilizando una ventana con elementos gráficos.
 * @author eguiluz
 */
@SuppressWarnings("serial")
public class VentanaGrafica extends JFrame {
	private ArrayList<JLabel> lMensaje = new ArrayList<JLabel>();
	private ArrayList<JLabel> lMensajeSombra = new ArrayList<JLabel>();
	private JPanel pDibujo = new MiJPanel();
	private JPanel pAreaControl = new JPanel();
	private JPanel pCristal = new JPanel();  // Capa cristal (uso futuro para el HUD)
	private JLayeredPane layeredPane = new JLayeredPane();
	
	private ArrayList<EventoVentana> eventosVentana;  // lista de eventos pendientes de teclado/ratón
	private Point posicionRaton = null;  // posición actual del ratón (null si está fuera del panel gráfico)
	private Point posicionRatonOriginal = null;  // posición original del ratón en un drag (null si está fuera del panel gráfico)
	private boolean generarClicksYDrags;  // se generan o no los eventos de click y drag?
	private boolean todosLosEventosDeRaton = false;  // se generan todos los eventos de ratón
	
	private long tiempoAnimMsg = 500L;  // Tiempo para un paso de animación (en milisegundos).
	private long tiempoFrameAnimMsg = tiempoAnimMsg/40L;  // Msg entre cada paso de refresco de animación
	private HiloAnimacion hilo = null;  // Hilo de la animación
	private ArrayList<Animacion> animacionesPendientes = new ArrayList<Animacion>();
	
	private static final Integer CAPA_FONDO = new Integer(-100);
	private static final Integer CAPA_DIBUJO = new Integer(-50); 
	private static final int PX_SOLAPE_FONDOS = 0;
	
		private synchronized void addEvento( EventoVentana ev ) {
			eventosVentana.add( ev );
		}
		private synchronized EventoVentana remEvento( int index ) {
			return eventosVentana.remove( index );
		}
		private synchronized boolean remEvento( EventoVentana ev ) {
			return eventosVentana.remove( ev );
		}
		private synchronized EventoVentana getEvento( int index ) {
			return eventosVentana.get ( index );
		}
	
	/** Construye una nueva ventana de juego de tablero,
	 * y la muestra en el centro de la pantalla.
	 * @param anchuraVent	Anchura de la ventana en pixels
	 * @param alturaVent	Altura de la ventana en pixels
	 * @param anchuraPanelControl	Anchura del panel de control de la derecha (0 si no se quiere utilizar)
	 * @param tamFijo	false si se hace redimensionable, true en caso contrario 
	 * @param cerrable	true si el usuario la puede cerrar (ver método {@link #isClosed()}), false en caso contrario
	 * @param genCyD	true si se quieren generar eventos de click y drag, false si sólo se procesan pulsación y suelta
	 * @param titulo	Título de la ventana
	 */
	public VentanaGrafica( int anchuraVent, int alturaVent, int anchuraPanelControl, boolean tamFijo, boolean cerrable, boolean genCyD, String titulo ) {
		setSize( anchuraVent, alturaVent );
		setTitle( titulo );
		setResizable( !tamFijo );
		pAreaControl.setMaximumSize( new Dimension(anchuraPanelControl, 5000));
		pAreaControl.setMinimumSize( new Dimension(anchuraPanelControl, 0));
		lMensaje.add( new JLabel( " " ) );
		lMensajeSombra.add( new JLabel( " " ) );
		lMensaje.get(0).setBounds( 0, 0, anchuraVent, 70 );
		lMensajeSombra.get(0).setBounds( 2, 2, anchuraVent, 70 );
		generarClicksYDrags = genCyD;
		if (cerrable)
			setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
		else
			setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );			
		try {
			SwingUtilities.invokeAndWait( new Runnable() {
				@Override
				public void run() {
					eventosVentana = new ArrayList<EventoVentana>();
					setLocationRelativeTo( null );
					setLayeredPane( layeredPane );
					pCristal.setLayout( null );
					setGlassPane( pCristal );
					pCristal.setOpaque( false );
					pCristal.setVisible( true );
					pAreaControl.setOpaque( false );
					lMensaje.get(0).setOpaque( false );
					lMensajeSombra.get(0).setOpaque( false );
					lMensaje.get(0).setFont( new Font( "Arial", Font.BOLD, 30 ));
					lMensajeSombra.get(0).setFont( new Font( "Arial", Font.BOLD, 30 ));
					lMensaje.get(0).setForeground( new Color(255, 255, 0) );
					lMensajeSombra.get(0).setForeground( new Color(0, 0, 128) );
					lMensaje.get(0).setHorizontalAlignment( JLabel.CENTER );
					lMensajeSombra.get(0).setHorizontalAlignment( JLabel.CENTER );
					
					layeredPane.add( pAreaControl, JLayeredPane.PALETTE_LAYER );
					layeredPane.add( pDibujo, CAPA_DIBUJO );
					layeredPane.add( lMensaje.get(0), JLayeredPane.PALETTE_LAYER );
					layeredPane.add( lMensajeSombra.get(0), JLayeredPane.PALETTE_LAYER );
					pAreaControl.setLayout( null );  // layout de posicionamiento absoluto
					layeredPane.setFocusable( true );
					layeredPane.requestFocus();
					setVisible( true );
		    		pCristal.setBounds( 0, 0, getContentPane().getWidth(), getContentPane().getHeight() );
		    		pDibujo.setBounds( 0, 0, getContentPane().getWidth(), getContentPane().getHeight() );
		    		pDibujo.setOpaque( false );
					layeredPane.addFocusListener( new FocusAdapter() {
						@Override
						public void focusLost(FocusEvent arg0) {
							layeredPane.requestFocus();
						}
					});
					layeredPane.addMouseListener( new MouseAdapter() {
						@Override
						public void mouseReleased(MouseEvent arg0) {
							boolean anyadirSuelta = true;
							if (todosLosEventosDeRaton) {
								addEvento( new RatonSoltado(arg0) );
								if (posicionRatonOriginal!=null && posicionRatonOriginal.equals(arg0.getPoint()))
									addEvento( new RatonClick(arg0) );
							} else if (generarClicksYDrags) {
								RatonPulsado rp = null;
								for (EventoVentana ev : eventosVentana) {
									if (ev instanceof RatonPulsado) {  // Se ha pulsado un ratón y ahora se suelta: a ver qué es
										rp = (RatonPulsado) ev;
										anyadirSuelta = false;
										if (rp.getPosicion().equals( arg0.getPoint() )) {  // Igual coordenada: click
											addEvento( new RatonClick(arg0) );
										} else { // Dif coordenada: drag
											addEvento( new RatonDrag( rp.getTime(), rp.getPosicion(), arg0 ) );
										}
										break;
									}
								}
								if (rp!=null) {  // Quitar evento de pulsación de la cola
									remEvento( rp );
								}
							}
							if (anyadirSuelta && !todosLosEventosDeRaton) {
								addEvento( new RatonSoltado(arg0) );
							}
							posicionRaton = arg0.getPoint();
							posicionRatonOriginal = null;
						}
						@Override
						public void mousePressed(MouseEvent arg0) {
							addEvento( new RatonPulsado(arg0) );
							posicionRaton = arg0.getPoint();
							posicionRatonOriginal = arg0.getPoint();
						}
						@Override
						public void mouseEntered(MouseEvent e) {
							posicionRaton = e.getPoint();
						}
						@Override
						public void mouseExited(MouseEvent e) {
							posicionRaton = null;
						}
					});
					layeredPane.addMouseMotionListener( new MouseMotionListener() {
						@Override
						public void mouseMoved(MouseEvent e) {
							posicionRaton = e.getPoint();
						}
						@Override
						public void mouseDragged(MouseEvent e) {
							if (todosLosEventosDeRaton) {
								if (posicionRaton!=null) {
									addEvento( new RatonDrag( e.getWhen(), posicionRaton, e ) );
								}
							}
							posicionRaton = e.getPoint();
						}
					});
					layeredPane.addKeyListener( new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent e) {
							addEvento( new TeclaSoltada( e ) );
						}
						@Override
						public void keyPressed(KeyEvent e) {
							addEvento( new TeclaPulsada( e ) );
						}
					});
				}
			} );
		} catch (Exception e) {
		}
	}
	
	/** Indica si se generan o no todos los eventos de ratón (por defecto no se generan todos)
	 * @param on	true para todos los eventos de ratón, false no
	 */
	public void setAllMouseEvents( boolean on ) {
		todosLosEventosDeRaton = on;
	}
	
	/** Cierra y finaliza la ventana de juego (no acaba la aplicación).
	 */
	public void finish() {
		if (hilo!=null) { hilo.interrupt(); }
		dispose();
	}

	/** Pone el fondo de la ventana de juego con un objeto gráfico,
	 * ocupando todo el fondo de la ventana
	 * @param og	Objeto gráfico a poner en el fondo
	 */
	public void setFondo( ObjetoGrafico og ) {
		fondoAnimado = false;
		// Quitar posibles fondos anteriores
		for (Component c : layeredPane.getComponentsInLayer( CAPA_FONDO )) {
			layeredPane.remove( c );
		}
		layeredPane.add( og, CAPA_FONDO );
		layeredPane.repaint();
	}
	 
	/** Pone el fondo de la ventana de juego con dos objetos gráficos,
	 * ocupando todo el fondo de la ventana y alineándose en lateral.<br>
	 * Inicialmente no se mueve, para ello usar el método {@link #rodarFondoAnimado(boolean)}.
	 * @param og1	Objeto gráfico de fondo
	 * @param og2	Objeto gráfico de fondo 2 (a su derecha y sucesivamente en ciclo)
	 * @param pixDespAIzqda	Píxels que se desplazan a la izquierda cada iteración de animación
	 */
	public void setFondoAnimado( ObjetoGrafico og1, ObjetoGrafico og2, double pixDespAIzqda ) {
		// Quitar posibles fondos anteriores
		for (Component c : layeredPane.getComponentsInLayer( CAPA_FONDO )) {
			layeredPane.remove( c );
		}
		fondo1 = og1;
		fondo2 = og2;
		og1.setLocation( 0, 0 );
		og2.setLocation( og1.getWidth() - PX_SOLAPE_FONDOS, 0 );   // a la derecha de og1 [solapa un pixel]
		coorX1 = 0;
		coorX2 = og1.getWidth() - PX_SOLAPE_FONDOS;
		layeredPane.add( og1, CAPA_FONDO );
		layeredPane.add( og2, CAPA_FONDO );
		layeredPane.repaint();
		fondoAnimado = true;
		fondoRodando = false;
		this.pixDespAIzqda = pixDespAIzqda;
		if (hilo==null) { hilo = new HiloAnimacion(); hilo.start(); }
	}
		// Atributos de animación de fondo:
		private boolean fondoAnimado = false;
		private boolean fondoRodando = true;
		private double pixDespAIzqda = 0D;
		private double coorX1 = 0D;
		private double coorX2 = 0D;
		private ObjetoGrafico fondo1 = null;
		private ObjetoGrafico fondo2 = null;

	/** Permite parar o seguir haciendo el desplazamiento lateral del fondo.<br>
	 * Sólo sirve si se ha llamado antes a {@link #setFondoAnimado(ObjetoGrafico, ObjetoGrafico, double)}
	 * @param seguir	true si se quiere animar, false si se quiere detener.
	 */
	public void rodarFondoAnimado( boolean seguir ) {
		fondoRodando = seguir;
	}
		
	/** Devuelve la posición actual del ratón con respecto al panel gráfico de la ventana 
	 * @return	Posición actual del ratón en el panel gráfico, null si está fuera
	 */
	public Point getPosRaton() {
		return posicionRaton;
	}
	
	// 
	/** Indica si la coordenada está dentro del panel gráfico de la ventana
	 * @param p	Coordenada de ventana
	 * @return	true si la coordenada está dentro del tablero, false en caso contrario
	 */
	public boolean estaEnVentana( Point p ) {
		return (p.getX() >= 0 && p.getX() < layeredPane.getWidth() &&
				p.getY() >= 0 && p.getY() < layeredPane.getHeight());
	}
	
	/** Devuelve el número de pixels de ancho del panel gráfico de la ventana
	 * @return	ancho en píxels
	 */
	public int getAnchoPanelGrafico() {
		return layeredPane.getWidth();
	}
	
	/** Devuelve el número de pixels de alto del panel gráfico de la ventana
	 * @return	alto en píxels
	 */
	public int getAltoPanelGrafico() {
		return layeredPane.getHeight();
	}
	
	/** Consulta si el usuario ha realizado algún evento de ratón o teclado en la ventana
	 * @return	true si se ha realizado alguno, false en caso contrario
	 */
	public boolean hayEvento() {
		return !eventosVentana.isEmpty();
	}
	
	/** Borra los eventos de ratón o teclado
	 */
	public void borraEventos() {
		eventosVentana.clear();
	}
	
	/** Devuelve el primer evento pendiente de ratón o teclado en la ventana
	 * (y lo da por procesado)
	 * @return	Siguiente evento pendiente, null si no se ha realizado ninguno
	 */
	public EventoVentana getEvento() {
		if (eventosVentana.isEmpty()) return null;
		return remEvento(0);
	}
	
	/** Consulta si el usuario está haciendo una interacción no acabada con el ratón
	 * @return	true si el próximo evento se ha pulsado el botón del ratón pero todavía no se ha soltado, false en caso contrario
	 */
	public boolean hayClickODragAMedias() {
		if (!eventosVentana.isEmpty() && 
				(getEvento(0) instanceof RatonPulsado)) {
			for (EventoVentana ev : eventosVentana) {
				if (ev instanceof RatonSoltado) {
					return false;
				}
			}
			return true;
		} else
			return false;
	}
	
	/** Espera a que haya un evento en la ventana (de ratón o teclado) y lo devuelve.<br>
	 * Si pasa el tiempo máximo sin eventos, devuelve null al cabo de ese tiempo.<br>
	 * Si se ha configurado la ventana para procesar click o drag, no devuelve el evento
	 * si es de ratón hasta que se finaliza el movimiento (de click o de drag),
	 * o null si no se completa en el tiempo límite.<br>
	 * Si la ventana se cierra antes de que haya habido un evento, devuelve null.
	 * @param maxEspera	Milisegundos máximos de espera
	 * @return	evento producido, o null si la ventana se ha cerrado
	 */
	public EventoVentana readEvento( long maxEspera ) {
		long esperaHasta = System.currentTimeMillis()+maxEspera;
		boolean sigoEsperando = true;
		while (sigoEsperando && System.currentTimeMillis() < esperaHasta) {
			sigoEsperando = eventosVentana.isEmpty() && isVisible();
			if (!sigoEsperando && generarClicksYDrags && !todosLosEventosDeRaton) { // Si click/drag mirar si hay algo que hacer
				if (hayClickODragAMedias())
						sigoEsperando = true;  // Hay uno a medias, sigo esperando
			}
			if (sigoEsperando)
				// Espera hasta que el ratón o el teclado hagan algo
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) { }
		}
		if (generarClicksYDrags && sigoEsperando)   // final por tiempo pero a medias el drag o click
			if (!eventosVentana.isEmpty() && eventosVentana.get(0) instanceof RatonPulsado) {
				// eventosVentana.remove(0);
				return null;  // Tengo un evento a medias... devuelvo null
			}
		if (!isVisible()) return null;
		if (sigoEsperando)
			return null;
		else
			return remEvento(0);
	}
	
	/** Espera a que haya un evento en la ventana (de ratón o teclado) y lo devuelve.<br>
	 * Si se ha configurado la ventana para procesar click o drag, no devuelve el evento
	 * si es de ratón hasta que se finaliza el movimiento (de click o de drag).<br>
	 * Si la ventana se cierra antes de que haya habido un evento, devuelve null.
	 * @return	evento producido, o null si la ventana se ha cerrado
	 */
	public EventoVentana readEvento() {
		return readEvento( 3153600000000L );  // si pasan 100 años devuelve el control  :-)
	}
	
	/** Visualiza un mensaje en la línea de mensajes
	 * @param s	String a visualizar en la línea superior de la ventana
	 */
	public void showMessage( String s ) {
		lMensaje.get(0).setText( s );
		lMensajeSombra.get(0).setText( s );
	}
	
	/** Crea una nueva etiqueta de mensajes (JLabel) con las características indicadas
	 * @param x	Coordenada x de la esquina superior de la zona
	 * @param y	Coordenada y de la esquina superior de la zona
	 * @param ancho	Ancho en pixels de la zona
	 * @param alineacionJLabel	Tipo de alineamiento horizontal: JLabel.LEFT, JLabel.CENTER, JLabel.RIGHT 
	 * @param fuente	Tipo de letra para la zona
	 * @param cMensaje	Color del mensaje 
	 * @param cSombra	Color de la sombra del mensaje
	 * @return	Número de la zona de mensajes creada
	 */
	public int nuevaZonaMensajes( int x, int y, int ancho, int alineacionJLabel, Font fuente, Color cMensaje, Color cSombra ) {
		int numMens = lMensaje.size();
		lMensaje.add( new JLabel( " " ) );
		lMensajeSombra.add( new JLabel( " " ) );
		lMensaje.get(numMens).setBounds( x, y, ancho, 70 );
		lMensajeSombra.get(numMens).setBounds( x, y, ancho, 70 );
		lMensaje.get(numMens).setOpaque( false );
		lMensajeSombra.get(numMens).setOpaque( false );
		lMensaje.get(numMens).setFont( fuente );
		lMensajeSombra.get(numMens).setFont( fuente );
		lMensaje.get(numMens).setForeground( cMensaje );
		lMensajeSombra.get(numMens).setForeground( cSombra );
		lMensaje.get(numMens).setHorizontalAlignment( alineacionJLabel );
		lMensajeSombra.get(numMens).setHorizontalAlignment( alineacionJLabel );
		layeredPane.add( lMensaje.get(numMens), JLayeredPane.PALETTE_LAYER );
		layeredPane.add( lMensajeSombra.get(numMens), JLayeredPane.PALETTE_LAYER );
		return numMens;
	}
	
	
	/** Visualiza un mensaje en la zona de mensajes indicada<br>
	 * Debe haberse creado previamente con {@link #nuevaZonaMensajes(int, int, int, int, Font, Color, Color)}
	 * @param s	String a visualizar en la zona
	 * @param numZonaMensajes	Número de la zona de mensajes, previamente creada
	 */
	public void showMessage( String s, int numZonaMensajes ) {
		if (numZonaMensajes < 0 || numZonaMensajes >= lMensaje.size()) return;
		lMensaje.get(numZonaMensajes).setText( s );
		lMensajeSombra.get(numZonaMensajes).setText( s );
	}
	
	/** Informa si la ventana ha sido cerrada por el usuario
	 * @return	true si la ventana se ha cerrado, false si sigue visible
	 */
	public boolean isClosed() {
		return !isVisible();
	}
	
	/** Añade al panel gráfico un objeto de juego, que se visualizará 
	 * inmediatamente si está marcado para ser visible.<br>
	 * Atención, si el mismo objeto se añade dos veces sólo se 
	 * tiene en cuenta una.
	 * Se añade en la coordenada en la que esté definido el objeto (getLocation)
	 * @param oj	Objeto de juego a introducir
	 */
	public void addObjeto( final ObjetoGrafico oj ) {
		try {
			SwingUtilities.invokeLater( new Runnable() {
				@Override
				public void run() {
					layeredPane.add( oj, new Integer( JLayeredPane.DEFAULT_LAYER ) );
					layeredPane.repaint( oj.getX(), oj.getY(), oj.getAnchuraObjeto(), oj.getAlturaObjeto() );
//					pAreaJuego.add( oj );
//					pAreaJuego.repaint( oj.getX(), oj.getY(), oj.getAnchuraObjeto(), oj.getAlturaObjeto() );
				}
			});
		} catch (Exception e) {
		}
	}
	
	/** Añade al panel gráfico un objeto de juego, que se visualizará 
	 * inmediatamente si está marcado para ser visible.<br>
	 * Atención, si el mismo objeto se añade dos veces sólo se 
	 * tiene en cuenta una.
	 * @param oj	Objeto de juego a introducir
	 * @param p	Posición de panel en la que poner el objeto
	 */
	public void addObjeto( final ObjetoGrafico oj, Point p ) {
		oj.setLocation(p);
		addObjeto(oj);
	}
	
	/** Quita de la ventana el objeto gráfico.<br>
	 * Si el objeto no estaba, no ocurre nada.
	 * @param oj	Objeto de juego a eliminar
	 */
	public void removeObjeto( final ObjetoGrafico oj ) {
		try {
			SwingUtilities.invokeLater( new Runnable() {
				@Override
				public void run() {
					layeredPane.remove( oj );
					layeredPane.repaint( oj.getX(), oj.getY(), oj.getAnchuraObjeto(), oj.getAlturaObjeto() );
					layeredPane.validate();  // TODO: chequear si hace falta
				}
			});
		} catch (Exception e) {
		}
	}
	
	/** Devuelve el número de objetos activos en la ventana
	 * @return	Número de objetos activos
	 */
	public int getNumObjetos() {
		return layeredPane.getComponentCountInLayer( JLayeredPane.DEFAULT_LAYER );
	}

	/** Quita de la ventana todos los objetos gráficos que hubiera
	 */
	public void clearObjetos() {
		try {
			SwingUtilities.invokeLater( new Runnable() {
				@Override
				public void run() {
					for (Component c : layeredPane.getComponentsInLayer( JLayeredPane.DEFAULT_LAYER )) {
						layeredPane.remove( c );
					}
					layeredPane.repaint();
				}
			});
		} catch (Exception e) {
		}
	}
	
	/** Trae al frente de la ventana el objeto gráfico.<br>
	 * Si el objeto no estaba, no ocurre nada.
	 * @param oj	Objeto de juego a traer al frente
	 */
	public void traeObjetoAlFrente( final ObjetoGrafico oj ) {
		if (oj != null)
			try {
				SwingUtilities.invokeLater( new Runnable() {
					@Override
					public void run() {
						layeredPane.moveToFront(oj);
//						pAreaJuego.remove( oj );
//						pAreaJuego.add( oj, 0 );  // Inserta al frente (-1 valor especial lo pone al fondo del todo --- cuanto > más al fondo)
//						pAreaJuego.repaint( oj.getX(), oj.getY(), oj.getAnchuraObjeto(), oj.getAlturaObjeto() );
					}
				});
			} catch (Exception e) {
			}
	}
	
	/** Mueve un objeto de juego a la posición indicada.<br>
	 * El objeto debe ser != null y estar añadido a la ventana.
	 * @param oj	Objeto de juego a mover
	 * @param p	Posición del panel gráfico a la que mover el objeto
	 */
	public void setPosGrafico( ObjetoGrafico oj, Point p ) {
		oj.setLocation( p );
	}
	
	/** Mueve un objeto de juego a la posición indicada
	 * realizando una animación (lineal).<br>
	 * El objeto debe ser != null y estar añadido a la ventana.<p>
	 * Si el objeto ya tenía una animación en curso, se completa con esta
	 * desde donde estuviera.<br>
	 * El tiempo que dura la animación es el tiempo fijado con {@link #setTiempoPasoAnimacion(long, int)}
	 * (por defecto 500 msg).
	 * @param oj	Objeto de juego a mover
	 * @param p	Coordenada a la que mover el objeto
	 */
	public void muevePosGrafico( ObjetoGrafico oj, Point p ) {
		if (oj!=null) {
			if (hilo==null) { hilo = new HiloAnimacion(); hilo.start(); }
			Animacion a = new Animacion( oj.getX(), p.getX(), 
					oj.getY(), p.getY(), tiempoAnimMsg, oj );
			if (animacionesPendientes.indexOf(a) == -1)
				// Si el objeto es nuevo se mete en animaciones pendientes
				animacionesPendientes.add( a );
			else {  // Si ya estaba se actualiza esa animación (ojo, puede generar diagonales o cosas raras)
				int pos = animacionesPendientes.indexOf(a);
				animacionesPendientes.get(pos).xHasta = p.getX();
				animacionesPendientes.get(pos).yHasta = p.getY();
				animacionesPendientes.get(pos).msFaltan = tiempoAnimMsg;
			}
		}
	}
	
	/** Mueve un objeto de juego a la posición indicada
	 * realizando una animación (lineal).<br>
	 * El objeto debe ser != null y estar añadido a la ventana.<p>
	 * Si el objeto ya tenía una animación en curso, se completa con esta
	 * desde donde estuviera.<br>
	 * @param oj	Objeto de juego a mover
	 * @param p	Coordenada a la que mover el objeto
	 * @param msg	Tiempo que durará la animación de movimiento
	 */
	public void muevePosGrafico( ObjetoGrafico oj, Point p, long msg ) {
		if (oj!=null) {
			if (hilo==null) { hilo = new HiloAnimacion(); hilo.start(); }
			Animacion a = new Animacion( oj.getX(), p.getX(), 
					oj.getY(), p.getY(), msg, oj );
			if (animacionesPendientes.indexOf(a) == -1)
				// Si el objeto es nuevo se mete en animaciones pendientes
				animacionesPendientes.add( a );
			else {  // Si ya estaba se actualiza esa animación (ojo, puede generar diagonales o cosas raras)
				int pos = animacionesPendientes.indexOf(a);
				animacionesPendientes.get(pos).xHasta = p.getX();
				animacionesPendientes.get(pos).yHasta = p.getY();
				animacionesPendientes.get(pos).msFaltan = msg;
			}
		}
	}
	
	/** Calcula el tiempo de movimiento de un objeto en su trayectoria lineal,
	 * dada la velocidad que se quiere conseguir.
	 * @param oj	Objeto de juego a mover
	 * @param p	Coordenada a la que mover el objeto
	 * @param vel	Velocidad a la que querría mover (en píxels/segundo)
	 * @return	Tiempo de movimiento en milisegundos
	 */
	public long calcTiempoDeMovimiento( ObjetoGrafico oj, Point p, double vel ) {
		double dist = Math.sqrt( Math.pow( oj.getX()-p.getX(), 2 ) + 
								 Math.pow( oj.getY()-p.getY(), 2 ) );
		return Math.round( dist / vel * 1000 );
	}
		
	/** Para el movimiento del objeto gráfico indicado. Si
	 * no se estuviera haciendo una animación, no ocurre nada.
	 * @param oj	Objeto de juego a detener donde esté
	 */
	public void paraMovimiento( ObjetoGrafico oj ) {
		if (oj == null) return;
		for (Animacion a : animacionesPendientes) {
			if (a.oj.equals( oj )) {
				animacionesPendientes.remove( a );
				return;
			}
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
	
	/** Devuelve la posición actual del objeto gráfico indicado.<br>
	 * El objeto debe ser != null y estar añadido a la ventana.
	 * @param oj	Objeto gráfico del que devolver la posición
	 * @return	Posición de ese objeto, null si no existe
	 */
	public Point getPosicion( ObjetoGrafico oj ) {
		if (oj == null) return null;
		java.util.List<Component> l = Arrays.asList( layeredPane.getComponentsInLayer( JLayeredPane.DEFAULT_LAYER ) );
		if (!l.contains( oj )) return null;
		return oj.getLocation();
	}

	/** Comprueba si hay algún objeto gráfico en la posición indicada.<br>
	 * Para la comprobación se usa el rectángulo completo del objeto gráfico.
	 * Si hay varios objetos gráficos que coinciden con la misma posición, se devuelve el 
	 * primero en posición de visualización (ver {@link #traeObjetoAlFrente(ObjetoGrafico)}).<br>
	 * Los gráficos de fondo no se tienen en cuenta.
	 * @param p	Posición en el panel gráfico
	 * @return	Objeto que se encuentra en esa posición, null si no hay ninguno
	 */
	public ObjetoGrafico getObjetoEnPosicion( Point p ) {
		Component[] lC = layeredPane.getComponentsInLayer( JLayeredPane.DEFAULT_LAYER );
		for (Component c : lC) {
			if (c.contains( p ) && c instanceof ObjetoGrafico)
				return (ObjetoGrafico) c;
		}
		return null;
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
		
	/** Espera sin hacer nada durante el tiempo indicado en milisegundos
	 * o hasta que se produce un evento (o nada si ya hay un evento producido).<br>
	 * El evento no se consume (cogerlo con {@link #readEvento()} o {@link #readEvento(long)}) 
	 * @param msg	Tiempo a esperar
	 */
	public void esperaAEvento( int msg ) {
		long esperaHasta = System.currentTimeMillis()+msg;
		boolean sigoEsperando = true;
		while (sigoEsperando && System.currentTimeMillis() < esperaHasta) {
			sigoEsperando = eventosVentana.isEmpty() && isVisible();
			if (!sigoEsperando && generarClicksYDrags) { // Si click/drag mirar si hay algo que hacer
				if (hayClickODragAMedias())
						sigoEsperando = true;  // Hay uno a medias, sigo esperando
			}
			if (sigoEsperando)
				// Espera hasta que el ratón o el teclado hagan algo
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) { }
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
		} while (!animacionesPendientes.isEmpty());
	}

	private class HiloAnimacion extends Thread {
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
				if (fondoAnimado && fondoRodando) {
					coorX1 -= pixDespAIzqda;
					coorX2 -= pixDespAIzqda;
					int x1 = (int) Math.round( coorX1 );
					int x2 = (int) Math.round( coorX2 );
					if (x1 < -fondo1.getWidth()) {  // Se sale fondo1 por la izqda
						coorX1 = coorX2 + fondo2.getWidth() - PX_SOLAPE_FONDOS;  // solapa pixels
						x1 = (int) Math.round( coorX1 );
					} else if (x2 < -fondo2.getWidth()) {  // Se sale fondo2 por la izqda
						coorX2 = coorX1 + fondo1.getWidth() - PX_SOLAPE_FONDOS;  // solapa pixels
						x2 = (int) Math.round( coorX2 );
					}
					if (x1<x2) { // muevo primero el de más a la derecha
						fondo1.setLocation( x2, 0 );
						fondo2.setLocation( x1, 0 );
					} else {
						fondo1.setLocation( x1, 0 );
						fondo2.setLocation( x2, 0 );
					}
					layeredPane.repaint();
				}
			}
		}
	}

	private static class Animacion {
		double xDesde;    // Desde qué x
		double xHasta;    // hasta qué x
		double yDesde;    // Desde qué y
		double yHasta;    // hasta qué y
		long msFaltan;    // en cuántos msg
		ObjetoGrafico oj; // objeto a animar
		public Animacion(double xDesde, double xHasta, double yDesde,
				double yHasta, long msFaltan, ObjetoGrafico oj) {
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
	
	//
	// Dibujos directos en ventana
	//
	
		private static enum TipoDibujo { LINEA, RECTANGULO, ELIPSE };
		private static class Dibujo { TipoDibujo tipoDibujo; Color color; Stroke stroke; Point pos1; Point pos2; }
		private ArrayList<Dibujo> dibujosEnPantalla = new ArrayList<Dibujo>();
		private Object lock = new Object();
		private static boolean puntoEnDibujo( Point punto, Dibujo dibujo ) {
			switch (dibujo.tipoDibujo) {
				case LINEA: {
					double dist = Line2D.ptSegDist( dibujo.pos1.x, dibujo.pos1.y, dibujo.pos2.x, dibujo.pos2.y, punto.x, punto.y );
					return (dist < 2);  // Margen de 2 pixels de cercanía
				} case RECTANGULO: {
					Rectangle rec = new Rectangle( dibujo.pos1.x, dibujo.pos1.y, dibujo.pos2.x-dibujo.pos1.x, dibujo.pos2.y-dibujo.pos1.y );
					return rec.contains( punto );
				} case ELIPSE: {
					Ellipse2D elip = new Ellipse2D.Double(dibujo.pos1.x, dibujo.pos1.y, dibujo.pos2.x-dibujo.pos1.x, dibujo.pos2.y-dibujo.pos1.y );
					return elip.contains( punto.x, punto.y );
				}
			}
			return false;
		}
		// Devuelve el primer dibujo que está en un punto dado, empezando por el más al frente
		private Dibujo dibujoDeEstePunto( Point punto ) {
			synchronized (lock) {
				for (int i=dibujosEnPantalla.size()-1; i>=0; i--) {
					Dibujo d = dibujosEnPantalla.get(i);
					if (puntoEnDibujo( punto, d )) return d;
				}
			}
			return null;
		}
		
	/** Quita el dibujo más al frente que contenga la posición indicada
	 * @param pos	Punto (x,y) de pantalla
	 * @return	true si se ha quitado un dibujo, false si no había ninguno en ese punto
	 */
	public boolean quitaDibujoEnPosicion( Point pos ) {
		Dibujo dibujo = dibujoDeEstePunto( pos );
		if (dibujo==null) return false;
		dibujosEnPantallaRemove( dibujo );
		layeredPane.repaint();
		return true;
	}
	/** Trae al frente el dibujo más al frente que contenga la posición indicada
	 * @param pos	Punto (x,y) de pantalla
	 * @return	true si se ha traído al frente un dibujo, false si no había ninguno en ese punto
	 */
	public boolean traeDibujoAlFrente( Point pos ) {
		Dibujo dibujo = dibujoDeEstePunto( pos );
		if (dibujo==null) return false;
		dibujosEnPantallaRemove( dibujo ); // Quita de donde esté
		dibujosEnPantallaAdd( dibujo );  // Añade al frente
		layeredPane.repaint();
		return true;
	}
	public void creaDibujoLin( Point pos1, Point pos2, Color color, int anchuraDibujo ) {
		Dibujo d = new Dibujo();
		d.stroke = new BasicStroke(anchuraDibujo);
		d.color = color;
		d.tipoDibujo = TipoDibujo.LINEA;
		d.pos1 = (Point)pos1.clone();
		d.pos2 = (Point)pos2.clone();
		dibujosEnPantallaAdd( d );
		layeredPane.repaint();
	}
	public void creaDibujoRect( Point posEsquina1, Point posEsquina2, Color color, int anchuraDibujo ) {
		Dibujo d = new Dibujo();
		d.stroke = new BasicStroke(anchuraDibujo);
		d.color = color;
		d.tipoDibujo = TipoDibujo.RECTANGULO;
		d.pos1 = (Point)posEsquina1.clone();
		d.pos2 = (Point)posEsquina2.clone();
		if (d.pos1.x > d.pos2.x) { int temp = d.pos1.x; d.pos1.x = d.pos2.x; d.pos2.x = temp; }
		if (d.pos1.y > d.pos2.y) { int temp = d.pos1.y; d.pos1.y = d.pos2.y; d.pos2.y = temp; }
		dibujosEnPantallaAdd( d );
		layeredPane.repaint();
	}
	public void creaDibujoElipse( Point posEsquina1, Point posEsquina2, Color color, int anchuraDibujo ) {
		Dibujo d = new Dibujo();
		d.stroke = new BasicStroke(anchuraDibujo);
		d.color = color;
		d.tipoDibujo = TipoDibujo.ELIPSE;
		d.pos1 = (Point)posEsquina1.clone();
		d.pos2 = (Point)posEsquina2.clone();
		if (d.pos1.x > d.pos2.x) { int temp = d.pos1.x; d.pos1.x = d.pos2.x; d.pos2.x = temp; }
		if (d.pos1.y > d.pos2.y) { int temp = d.pos1.y; d.pos1.y = d.pos2.y; d.pos2.y = temp; }
		dibujosEnPantallaAdd( d );
		layeredPane.repaint();
	}
	/** Añade un dibujo de rectángulo a la ventana
	 * @param xCentro	Coordenada x del centro del rectángulo
	 * @param yCentro	Coordenada y del centro del rectángulo
	 * @param ancho	Ancho del rectángulo (valor positivo)
	 * @param alto	Alto del rectángulo (valor positivo)
	 * @param color	Color de dibujado
	 * @param anchuraDibujo	Anchura de línea de dibujado (en píxels)
	 */
	public void creaDibujoRect( int xCentro, int yCentro, int ancho, int alto, Color color, int anchuraDibujo ) {
		Dibujo d = new Dibujo();
		d.stroke = new BasicStroke(anchuraDibujo);
		d.color = color;
		d.tipoDibujo = TipoDibujo.RECTANGULO;
		d.pos1 = new Point( xCentro-ancho/2, yCentro-alto/2 );
		d.pos2 = new Point( xCentro+ancho/2, yCentro+alto/2 );
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				dibujosEnPantallaAdd( d );
				layeredPane.repaint();
			}
		});
	}
	/** Añade un dibujo de elipse a la ventana
	 * @param xCentro	Coordenada x del centro de la elipse
	 * @param yCentro	Coordenada y del centro de la elipse
	 * @param ancho	Ancho de la elipse (valor positivo)
	 * @param alto	Alto de la elipse (valor positivo)
	 * @param color	Color de dibujado
	 * @param anchuraDibujo	Anchura de línea de dibujado (en píxels)
	 */
	public void creaDibujoElipse( int xCentro, int yCentro, int ancho, int alto, Color color, int anchuraDibujo ) {
		Dibujo d = new Dibujo();
		d.stroke = new BasicStroke(anchuraDibujo);
		d.color = color;
		d.tipoDibujo = TipoDibujo.ELIPSE;
		d.pos1 = new Point( xCentro-ancho/2, yCentro-alto/2 );
		d.pos2 = new Point( xCentro+ancho/2, yCentro+alto/2 );
		dibujosEnPantallaAdd( d );
		layeredPane.repaint();
	}
	/** Borra un dibujo de línea (si existía) en la ventana
	 * @param pos1	Punto de inicio de la línea
	 * @param pos2	Punto de fin de la línea
	 */
	public void borraDibujoLin( Point pos1, Point pos2 ) {
		synchronized (lock) {
			for (Dibujo d : dibujosEnPantalla) {
				if (d.tipoDibujo == TipoDibujo.LINEA) {
					if (d.pos1.equals(pos1) && d.pos2.equals(pos2)) {
						dibujosEnPantalla.remove( d );
						break;
					}
				}
			}
		}
		layeredPane.repaint();
	}
	/** Borra un dibujo de rectángulo (si existía) en la ventana
	 * @param xCentro	Coordenada x del centro del rectángulo a borrar
	 * @param yCentro	Coordenada y del centro del rectángulo a borrar
	 * @param ancho	Ancho del rectángulo a borrar
	 * @param alto	Alto del rectángulo a borrar
	 */
	public void borraDibujoRect( int xCentro, int yCentro, int ancho, int alto ) {
		synchronized (lock) {
			for (Dibujo d : dibujosEnPantalla) {
				if (d.tipoDibujo == TipoDibujo.RECTANGULO) {
					if (d.pos1.x == xCentro-ancho/2 && d.pos1.y == yCentro-alto/2 &&
						d.pos2.x == xCentro+ancho/2 && d.pos2.y == yCentro+alto/2) {
						dibujosEnPantalla.remove( d );
						break;
					}
				}
			}
		}
		layeredPane.repaint();
	}
	/** Borra un dibujo de elipse (si existía) en la ventana
	 * @param xCentro	Coordenada x del centro de la elipse a borrar
	 * @param yCentro	Coordenada y del centro de la elipse a borrar
	 * @param ancho	Ancho de la elipse a borrar
	 * @param alto	Alto de la elipse a borrar
	 */
	public void borraDibujoElipse( int xCentro, int yCentro, int ancho, int alto ) {
		synchronized (lock) {
			for (Dibujo d : dibujosEnPantalla) {
				if (d.tipoDibujo == TipoDibujo.ELIPSE) {
					if (d.pos1.x == xCentro-ancho/2 && d.pos1.y == yCentro-alto/2 &&
						d.pos2.x == xCentro+ancho/2 && d.pos2.y == yCentro+alto/2) {
						dibujosEnPantalla.remove( d );
						break;
					}
				}
			}
		}
		layeredPane.repaint();
	}
	
		private void dibujosEnPantallaAdd( Dibujo d ) {
			synchronized (lock) {
				dibujosEnPantalla.add( d );
			}
		}
		private void dibujosEnPantallaRemove( Dibujo d ) {
			synchronized (lock) {
				dibujosEnPantalla.remove( d );
			}
		}

	private class MiJPanel extends JPanel {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			synchronized (lock) {
				for (Dibujo d : dibujosEnPantalla) {
					switch (d.tipoDibujo) {
						case LINEA: {
							g2.setColor( d.color );
							g2.setStroke( d.stroke );
							g2.drawLine( d.pos1.x, d.pos1.y, d.pos2.x, d.pos2.y );
							break;
						}
						case RECTANGULO: {
							g2.setColor( d.color );
							g2.setStroke( d.stroke );
							g2.drawRect( d.pos1.x, d.pos1.y, d.pos2.x-d.pos1.x, d.pos2.y-d.pos1.y );
							break;
						}
						case ELIPSE: {
							g2.setColor( d.color );
							g2.setStroke( d.stroke );
							g2.drawOval( d.pos1.x, d.pos1.y, d.pos2.x-d.pos1.x, d.pos2.y-d.pos1.y );
							break;
						}
					}
				}
			}
		}
	}
	
	
		// método y atributos privados para facilitar los offsets de puntos
		// del programa de prueba
		private static int moviendoOffsetX = 0;
		private static int moviendoOffsetY = 0;
		private static Point aplicaOffset( Point p ) {
			return new Point( (int) Math.round(p.getX() - moviendoOffsetX),
					(int) Math.round(p.getY() - moviendoOffsetY) );
		}
		private static Random rand = new Random(); 
		
	/** Método de prueba de la clase.
	 * @param args	No utilizado
	 */
	public static void main(String[] args) {
		int tipoPrueba = 3;  /* 0=sin drag, 1=con drag, 2=figuras, 3=con todos los eventos de ratón */
		if (tipoPrueba==0) {
			// PRUEBA SIN DRAG
			VentanaGrafica v = new VentanaGrafica(960, 720, 0, false, true, false, "Ventana gráfica de prueba" );
			v.setTiempoPasoAnimacion( 2000, 50 );
			EventoVentana ev = null;
			ObjetoGrafico moviendo = null;
			Point coordInicioRaton = null;
			ObjetoGrafico o1 = new ObjetoGrafico( "UD-blue.png", true, 300, 400 );
			ObjetoGrafico o2 = new ObjetoGrafico( "UD-red.png", true );
			ObjetoGrafico o3 = new ObjetoGrafico( "UD-green.png", true );
			ObjetoGraficoRotable o4 = new ObjetoGraficoRotable( "UD-green.png", true, 0 );
			v.addObjeto( o1, new Point( 0, 0 ) );
			v.addObjeto( o2, new Point( 100, 250 ) );
			v.addObjeto( o3, new Point( 250, 140 ) );
			v.addObjeto( o4, new Point( 250, 180 ) );
			v.setFondoAnimado(new ObjetoGrafico( "UD-roller.jpg", true ),
					new ObjetoGrafico( "UD-roller.jpg", true ), 3 );
			v.showMessage( "Pulsa y arrastra para mover cada objeto. Click para traer al frente. Si lo arrastras fuera volverá a su sitio." );
			v.esperaAEvento( 3000 );
			v.rodarFondoAnimado( true );
			v.showMessage( "Empezando prueba de ventana!" );
			int gradosRotacion = 0;
			while (!v.isClosed()) {
				o4.setRotacionGrados( gradosRotacion );  // Vamos rotando un UD verde mientras haya eventos
				gradosRotacion += 10;
				if (moviendo != null)
					ev = v.readEvento( 40 );  // Lee evento o espera 40 msg  (algo moviéndose)
				else
					ev = v.readEvento();  // Espera hasta que pase algo (nada moviéndose)
				if (ev == null){  // Si no hay evento pero se está moviendo, seguimos al ratón
					if (moviendo != null) {
						Point posRaton = v.getPosRaton();
						if (posRaton != null) {
							v.setPosGrafico( moviendo, aplicaOffset( posRaton ) );
							v.showMessage( "Moviendo objeto " + moviendo.getName() );
							o1.comoChocaCon(o2, 2);
						}
					}
				} else if (ev instanceof EventoTeclado) {
					System.out.println( "Evento de teclado: " + ((EventoTeclado)ev).getCodigoTecla() + " --> " + ev );
				} else if (ev instanceof RatonPulsado) {
					RatonPulsado rp = (RatonPulsado) ev;
					coordInicioRaton = rp.getPosicion();
					ObjetoGrafico og = v.getObjetoEnPosicion( rp.getPosicion() );
					if (og == null) {
						v.showMessage( "Inicio click en posición: " + rp.getPosicion() + " sin objeto" );
						moviendo = null;
					} else {
						moviendo = og;
						v.paraMovimiento( moviendo );
						moviendoOffsetX = (int) Math.round(rp.getPosicion().getX() - og.getX());
						moviendoOffsetY = (int) Math.round(rp.getPosicion().getY() - og.getY());
						v.showMessage( "Tocando objeto " + og.getName() );
					}
				} else if (ev instanceof RatonSoltado) {
					if (moviendo != null) {
						RatonSoltado rs = (RatonSoltado) ev;
						if (coordInicioRaton != null && coordInicioRaton.equals( rs.getPosicion() )) {
							// click en vez de drag
							v.traeObjetoAlFrente( moviendo );
							v.showMessage( "Traido al frente objeto " + moviendo.getName() );
						} else {
							// drag
							if (v.estaEnVentana( rs.getPosicion() )) {
								v.setPosGrafico( moviendo, aplicaOffset( rs.getPosicion() ) );
								v.showMessage( "Soltado objeto " + moviendo.getName() );
							} else {  // Drag off-screen
								v.muevePosGrafico( moviendo, aplicaOffset( coordInicioRaton ) );
								v.showMessage( "DRAG FUERA: Moviendo objeto " + moviendo.getName() + " a su posición inicial." );
							}
						}
						moviendo = null;
					}
				}
			}
			v.finish();
		} else if (tipoPrueba==1) {
			// PRUEBA CON DRAG
			VentanaGrafica v = new VentanaGrafica( 960, 720, 0, false, true, true, "Ventana gráfica de prueba" );
			v.setTiempoPasoAnimacion( 2000, 50 );
			EventoVentana ev = null;
			ObjetoGrafico moviendo = null;
			ObjetoGrafico o1 = new ObjetoGrafico( "UD-blue.png", true );
			ObjetoGrafico o2 = new ObjetoGrafico( "UD-red.png", true );
			ObjetoGrafico o3 = new ObjetoGrafico( "UD-green.png", true );
			ObjetoGrafico o4 = new ObjetoGrafico( "noExiste.png", true, 100, 100 );
			v.addObjeto( o1, new Point( 0, 0 ) );
			v.addObjeto( o2, new Point( 100, 250 ) );
			v.addObjeto( o3, new Point( 250, 140 ) );
			v.addObjeto( o4, new Point( 400, 25 ) );
			v.showMessage( "Pulsa y arrastra para mover cada objeto. Click para traer al frente." );
			v.esperaAEvento( 3000 );
			while (!v.isClosed()) {
				ev = v.readEvento( 40 );  // Lee evento o espera 40 msg  (algo moviéndose)
				if (ev == null){  // Si no hay evento no se hace nada
				} else if (ev instanceof EventoTeclado) {
					System.out.println( "Evento de teclado: " + ((EventoTeclado)ev).getCodigoTecla() + " --> " + ev );
				} else if (ev instanceof RatonPulsado) {
					System.out.println( "Esto no debería pasar");
				} else if (ev instanceof RatonSoltado) {
					System.out.println( "Esto no debería pasar");
				} else if (ev instanceof RatonClick) {
					RatonClick rc = (RatonClick) ev;
					moviendo = v.getObjetoEnPosicion( rc.getPosicion() );
					if (moviendo != null) {
						v.traeObjetoAlFrente( moviendo );
						v.showMessage( "CLICK -> Traido al frente objeto " + moviendo.getName() );
					}
				} else if (ev instanceof RatonDrag) {
					RatonDrag rd = (RatonDrag) ev;
					moviendo = v.getObjetoEnPosicion( rd.getPosicionIni() );
					if (moviendo != null) {
						if (v.estaEnVentana( rd.getPosicion() )) {
							moviendoOffsetX = (int) Math.round(rd.getPosicionIni().getX() - moviendo.getX());
							moviendoOffsetY = (int) Math.round(rd.getPosicionIni().getY() - moviendo.getY());
							v.muevePosGrafico( moviendo, aplicaOffset( rd.getPosicion() ), 
									v.calcTiempoDeMovimiento( moviendo, aplicaOffset(rd.getPosicion()), 100 ) );  // a 100 pixels/sg
							v.showMessage( "DRAG -> Movido objeto " + moviendo.getName() );
						} else {
							v.showMessage( "DRAG -> Coordenada fuera de ventana (no se hce nada)" );
						}
					}
				}
			}
			v.finish();
		} else if (tipoPrueba==2) {   // PRUEBA DE FIGURAS
			VentanaGrafica v = new VentanaGrafica( 1024, 600, 0, false, true, true, "Ventana gráfica de prueba" );
			v.setFondo(new ObjetoGrafico( "UD-roller.jpg", true ));
			v.setTiempoPasoAnimacion( 2000, 50 );
			EventoVentana ev = null;
			v.showMessage( "Drag crea nueva figura random. Click trae al frente. Doble-click borra." );
			v.esperaAEvento( 3000 );
			while (!v.isClosed()) {
				ev = v.readEvento( 40 );  // Lee evento o espera 40 msg
				if (ev == null){  // Si no hay evento no se hace nada
				} else if (ev instanceof RatonClick) {
					RatonClick rc = (RatonClick) ev;
					if (rc.hayDobleClick()) { // Click doble
						v.quitaDibujoEnPosicion( rc.getPosicion() );
					} else { // Click simple
						v.traeDibujoAlFrente( rc.getPosicion() );
					}
				} else if (ev instanceof RatonDrag) {
					RatonDrag rd = (RatonDrag) ev;
					switch (rand.nextInt(3)) {
						case 0: {
							v.creaDibujoLin( rd.getPosicionIni(), rd.getPosicion(), Color.blue, 3 );
							break;
						}
						case 1: {
							v.creaDibujoRect( rd.getPosicionIni(), rd.getPosicion(), Color.red, 2 );
							break;
						}
						case 2: {
							v.creaDibujoElipse( rd.getPosicionIni(), rd.getPosicion(), Color.green, 4 );
							break;
						}
					}
				}
			}
			v.finish();
		} else if (tipoPrueba==3) {
			// PRUEBA CON DRAG
			VentanaGrafica v = new VentanaGrafica( 960, 720, 0, false, true, true, "Ventana gráfica de prueba" );
			v.setAllMouseEvents( true );
			v.setTiempoPasoAnimacion( 2000, 50 );
			EventoVentana ev = null;
			ObjetoGrafico moviendo = null;
			ObjetoGrafico o1 = new ObjetoGrafico( "UD-blue.png", true );
			ObjetoGrafico o2 = new ObjetoGrafico( "UD-red.png", true );
			ObjetoGrafico o3 = new ObjetoGrafico( "UD-green.png", true );
			ObjetoGrafico o4 = new ObjetoGrafico( "noExiste.png", true, 100, 100 );
			v.addObjeto( o1, new Point( 0, 0 ) );
			v.addObjeto( o2, new Point( 100, 250 ) );
			v.addObjeto( o3, new Point( 250, 140 ) );
			v.addObjeto( o4, new Point( 400, 25 ) );
			v.showMessage( "Pulsa y arrastra para mover cada objeto. Click para traer al frente." );
			v.esperaAEvento( 3000 );
			while (!v.isClosed()) {
				ev = v.readEvento( 40 );  // Lee evento o espera 40 msg  (algo moviéndose)
				if (ev == null){  // Si no hay evento no se hace nada
				} else if (ev instanceof EventoTeclado) {
					System.out.println( "Evento de teclado: " + ((EventoTeclado)ev).getCodigoTecla() + " --> " + ev );
				} else if (ev instanceof RatonPulsado) {
					System.out.println( "Se pulsa en " + ((RatonPulsado)ev).getPosicion() );
					RatonPulsado rp = (RatonPulsado) ev;
					moviendo = v.getObjetoEnPosicion( rp.getPosicion() );
					if (moviendo != null) {
						if (v.estaEnVentana( rp.getPosicion() )) {
							moviendoOffsetX = (int) Math.round(rp.getPosicion().getX() - moviendo.getX());
							moviendoOffsetY = (int) Math.round(rp.getPosicion().getY() - moviendo.getY());
						} else
							moviendo = null;
					}
				} else if (ev instanceof RatonSoltado) {
					System.out.println( "Se suelta en " + ((RatonSoltado)ev).getPosicion() );
					moviendo = null;
				} else if (ev instanceof RatonClick) {
					System.out.println( "Click en " + ((RatonClick)ev).getPosicion() );
					RatonClick rc = (RatonClick) ev;
					moviendo = v.getObjetoEnPosicion( rc.getPosicion() );
					if (moviendo != null) {
						v.traeObjetoAlFrente( moviendo );
						v.showMessage( "CLICK -> Traido al frente objeto " + moviendo.getName() );
					}
					moviendo = null;
				} else if (ev instanceof RatonDrag) {
					System.out.println( "Drag en " + ((RatonDrag)ev).getPosicionIni() + " a " + ((RatonDrag)ev).getPosicion() );
					RatonDrag rd = (RatonDrag) ev;
					if (moviendo != null) {
						v.muevePosGrafico( moviendo, aplicaOffset( rd.getPosicion() ), 
								v.calcTiempoDeMovimiento( moviendo, aplicaOffset(rd.getPosicion()), 200 ) );  // a 100 pixels/sg
						v.showMessage( "DRAG -> Movido objeto " + moviendo.getName() );
					}
				}
			}
			v.finish();
		}
	}

}
