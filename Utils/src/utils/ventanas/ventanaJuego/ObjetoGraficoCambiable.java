package utils.ventanas.ventanaJuego;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

/** Clase de objeto visible en pantalla en juego
 * con capacidad de escalarse y rotar
 * @author eguiluz
 *
 */
public class ObjetoGraficoCambiable extends ObjetoGraficoRotable {
	protected double zoom = 1.0;  // 1.0 = 100% zoom
	protected float opacity = 1.0f;  // 1.0 = 100% opaque / 0.0 = 0% opaque
	private static final long serialVersionUID = 1L;  // Para serialización
	
	/** Crea un nuevo objeto gráfico de ventana para juegos.<br>
	 * Si no existe el fichero de imagen, se crea un rectángulo blanco con borde rojo
	 * @param nombreImagenObjeto	Nombre fichero donde está la imagen del objeto (carpeta utils/img)
	 * @param visible	true si se quiere ver, false si se quiere tener oculto
	 * @param anchura	Anchura del objeto en píxels
	 * @param altura	Altura del objeto en píxels
	 * @param rotacion	Rotación en radianes (0=sin rotación, 2PI=vuelta completa. Sentido horario)
	 */
	public ObjetoGraficoCambiable( String nombreImagenObjeto, boolean visible, int anchura, int altura, double rotacion ) {
		super( nombreImagenObjeto, visible, anchura, altura, rotacion );
		calcStartZoom();
	}
	
	/** Crea un nuevo objeto gráfico de ventana para juegos.<br>
	 * Si no existe el fichero de imagen, se crea un rectángulo blanco con borde rojo de 10x10 píxels<br>
	 * Si existe, se toma la anchura y la altura de esa imagen.
	 * @param nombreImagenObjeto	Nombre fichero donde está la imagen del objeto (carpeta utils/img)
	 * @param visible	Panel en el que se debe dibujar el objeto
	 * @param rotacion	Rotación en radianes (0=sin rotación, 2PI=vuelta completa. Sentido horario)
	 */
	public ObjetoGraficoCambiable( String nombreImagenObjeto, boolean visible, double rotacion ) {
		super( nombreImagenObjeto, visible, rotacion );
		calcStartZoom();
	}

	/** Crea un nuevo objeto gráfico de ventana para juegos.<br>
	 * Si la URL de imagen es null, se crea un rectángulo blanco con borde rojo
	 * @param urlImagenObjeto	URL donde está la imagen del objeto
	 * @param visible	true si se quiere ver, false si se quiere tener oculto
	 * @param anchura	Anchura del objeto en píxels
	 * @param altura	Altura del objeto en píxels
	 * @param rotacion	Rotación en radianes (0=sin rotación, 2PI=vuelta completa. Sentido horario)
	 */
	public ObjetoGraficoCambiable( java.net.URL urlImagenObjeto, boolean visible, int anchura, int altura, double rotacion ) {
		super( urlImagenObjeto, visible, anchura, altura, rotacion );
		calcStartZoom();
	}
	
	/** Crea un nuevo objeto gráfico de ventana para juegos.<br>
	 * Si no existe el fichero de imagen, se crea un rectángulo blanco con borde rojo de 10x10 píxels<br>
	 * Si existe, se toma la anchura y la altura de esa imagen.
	 * @param urlImagenObjeto	URL donde está la imagen del objeto
	 * @param visible	Panel en el que se debe dibujar el objeto
	 * @param rotacion	Rotación en radianes (0=sin rotación, 2PI=vuelta completa. Sentido horario)
	 */
	public ObjetoGraficoCambiable( java.net.URL urlImagenObjeto, boolean visible, double rotacion ) {
		super( urlImagenObjeto, visible, rotacion );
		calcStartZoom();
	}

	// Calcula el zoom inicial que es el que permite ver todo el gráfico cargado en el tamaño definido
	private void calcStartZoom() {
		if (icono==null) {
			zoom = 1.0;
		} else {
			double ratioAnchura = anchuraObjeto * 1.0 / icono.getIconWidth();
			double ratioAltura = alturaObjeto * 1.0 / icono.getIconHeight();
			zoom = Math.min(ratioAnchura, ratioAltura);
		}
	}

	/** Cambia el zoom por el zoom indicado
	 * @param zoom	Valor nuevo de zoom, positivo (0.1 = 10%, 1.0 = 100%, 2.0 = 200%...)
	 */
	public void setZoom( double zoom ) {
		if (zoom>0.0) {
			this.zoom = zoom;
			repaint();
		}
	}
	
	/** Devuelve el zoom actual
	 * @return	Zoom actual
	 */
	public double getZoom() {
		return zoom;
	}
	
	/** Cambia la opacidad por la indicada
	 * @param opacity	Valor nuevo de opacidad, entre 0.0 (transparente) y 1.0 (opaco)
	 */
	public void setOpacity( float opacity ) {
		if (opacity>=0.0 && opacity<=1.0) {
			this.opacity = opacity;
			repaint();
		}
	}
	
	/** Devuelve la opacidad actual
	 * @return	Opacidad actual
	 */
	public float getOpacity() {
		return opacity;
	}
	
	// Dibuja este componente de una forma no habitual (si es proporcional)
	@Override
	protected void paintComponent(Graphics g) {
		if (imagenObjeto==null || icono==null) {
			super.paintComponent(g);
		} else {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR); // Configuración para mejor calidad del gráfico escalado
			g2.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);	
	        g2.rotate( radsRotacion, anchuraObjeto/2, alturaObjeto/2 );  // Incorporar al gráfico la rotación definida
	        g2.setComposite(AlphaComposite.getInstance( AlphaComposite.SRC_OVER, opacity ) ); // Incorporar la transparencia definida
	        int anchoDibujado = (int)Math.round(icono.getIconWidth()*zoom);  // Calcular las coordenadas de dibujado con el zoom, siempre centrado en el label
	        int altoDibujado = (int)Math.round(icono.getIconHeight()*zoom);
	        int difAncho = (anchuraObjeto - anchoDibujado) / 2;  // Offset x para centrar
	        int difAlto = (alturaObjeto - altoDibujado) / 2;     // Offset y para centrar
	        g2.drawImage(imagenObjeto, difAncho, difAlto, anchoDibujado, altoDibujado, null);  // Dibujar la imagen con el tamaño calculado tras aplicar el zoom
		}
	}

		private static boolean acaba = false;
	/** Método main de prueba que saca un escudo UD y lo va rotando y cambiando zoom de manera continua.
	 * Si se hace click en él, se hace un ciclo de desaparición y aparición cambiando su opacidad.
	 * Si se hace drag, se mueve en el panel
	 * @param args
	 */
	public static void main(String[] args) {
		ObjetoGraficoCambiable o = new ObjetoGraficoCambiable( "UD-blue.png", true, 400, 400, 0.0 );
		javax.swing.JFrame v = new javax.swing.JFrame();
		v.getContentPane().setLayout( null );
		v.setDefaultCloseOperation( javax.swing.JFrame.DISPOSE_ON_CLOSE );
		v.getContentPane().add( o );
		v.setSize( 800, 600 );
		try { javax.swing.SwingUtilities.invokeAndWait( new Runnable() { @Override public void run() {
			v.setVisible( true );
		} }); } catch (Exception e) {}
		boolean zoomCreciendo = false;
		v.addWindowListener( new java.awt.event.WindowAdapter() { @Override
			public void windowClosing(java.awt.event.WindowEvent e) { acaba = true; } });
		o.addMouseListener( new java.awt.event.MouseAdapter() { 
			@Override public void mouseClicked(java.awt.event.MouseEvent e) {
				(new Thread() { @Override public void run() {
					float f = 1.0f;
					while (f>=0.0f) {
						o.setOpacity( f );
						f -= 0.01f;
						try { Thread.sleep(15); } catch (Exception e) {}
					}
					while (f<=1.0f) {
						o.setOpacity( f );
						f += 0.01f;
						try { Thread.sleep(15); } catch (Exception e) {}
					}
				} }).start();
			}
			private Point pPressed = null;
			@Override public void mousePressed(java.awt.event.MouseEvent e) {
				pPressed = e.getPoint();
			}
			@Override public void mouseReleased(java.awt.event.MouseEvent e) {
				if (!e.getPoint().equals(pPressed))
					o.setLocation( o.getX() - pPressed.x + e.getX(), o.getY() - pPressed.y + e.getY() );
			}
		} );
		while (!acaba) {
			if (o.getZoom() < 0.1) zoomCreciendo = true; else if (o.getZoom() > 4.0) zoomCreciendo = false;
			if (zoomCreciendo) o.setZoom( o.getZoom()*1.02 );
			else o.setZoom( o.getZoom()*0.98 );
			try { Thread.sleep(15); } catch (Exception e) {}
			o.incRotacion( 0.01 );
		}
	}
}
