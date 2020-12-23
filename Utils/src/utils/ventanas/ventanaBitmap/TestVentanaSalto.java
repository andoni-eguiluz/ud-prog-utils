package utils.ventanas.ventanaBitmap;
import java.awt.*;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;

import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

/** Clase ventana sencilla para dibujado directo a la ventana
 * v 1.1.6 - Incorpora método para dibujar texto centrado
 * v 1.1.5 - Incorpora método para cambiar el tipo de letra de la línea de mensajes, método para consultar el mensaje actual
 * v 1.1.4 - Incorpora métodos para pedir datos desde teclado
 */
public class TestVentanaSalto {
	
	// ====================================================
	//   Parte estática - pruebas de funcionamiento
	// ====================================================

	private static VentanaGrafica v;
	private static boolean DIRECTAMENTE_SALTO = true; // true sale solo salto, false salen otras opciones
	
	/** Método main de prueba de la clase
	 * @param args	No utilizado
	 */
	public static void main(String[] args) {
		v = new VentanaGrafica( 750, 600, "Test Salto en ventana gráfica" );
		v.setDibujadoInmediato( false );
		if (DIRECTAMENTE_SALTO) {
			salto();
		} else {
			Object opcion = JOptionPane.showInputDialog( v.getJFrame(), "¿Qué quieres probar?",
					"Selección de test", JOptionPane.QUESTION_MESSAGE, null, 
					new String[] { "Movimiento", "Giros", "Tiro", "Texto", "Salto con opciones" }, "Salto con opciones" );
			if ( "Movimiento".equals( opcion ) ) {
				movimiento();
			} else if ( "Giros".equals( opcion ) ) {
				giros();
			} else if ( "Tiro".equals( opcion ) ) {
				tiro();
			} else if ( "Texto".equals( opcion ) ) {
				texto();
			} else if ( "Salto con opciones".equals( opcion ) ) {
				salto();
			}
		}
		v.acaba();
	}

	// Prueba 1: escudo verde que se mueve y sube y baja
	private static void movimiento() {
		int altura = v.getAltura();
		boolean subiendo = true;
		for (int i=0; i<=800; i++) {
			v.borra();
			v.dibujaTexto( i+100, 100+(i/4), "texto móvil", new Font( "Arial", Font.PLAIN, 16 ), Color.black );
			v.dibujaImagen( "img/UD-green.png", i, altura, 1.0, 0.0, 1.0f );
			if (subiendo) {
				altura--;
				if (altura<=0) subiendo = false;
			} else {
				altura++;
				if (altura>=v.getAltura()) subiendo = true;
			}
			v.repaint();
			v.espera( 10 );
		}
		v.espera( 5000 );
		v.acaba();
	}
	
	// Prueba 2: escudos y formas girando y zoomeando
	private static void giros() {
		for (int i=0; i<=1000; i++) {
			v.borra();
			v.dibujaImagen( "img/UD-green.png", 100, 100, 0.5+i/200.0, Math.PI / 100 * i, 0.9f );
			v.dibujaImagen( "img/UD-magenta.png", 500, 100, 100, 50, 1.2, Math.PI / 100 * i, 0.1f );
			v.dibujaRect( 20, 20, 160, 160, 0.5f, Color.red );
			v.dibujaRect( 0, 0, 100, 100, 1.5f, Color.blue );
			v.dibujaCirculo( 500, 100, 50, 1.5f, Color.orange );
			v.dibujaPoligono( 2.3f, Color.magenta, true, 
				new Point(200,250), new Point(300,320), new Point(400,220) );
			v.repaint();
			v.espera( 10 );
		}
		v.espera( 5000 );
		v.acaba();
	}

	// Prueba 3: tiro parabólico
	private static void tiro() {
		boolean seguir = true;
		v.setMensaje( "Click ratón para disparar (con fuerza y ángulo)");
		double xLanz = 20;
		double yLanz = v.getAltura()-20;
		while (seguir) {
			Point pMovto = v.getRatonMovido();
			Point pPuls = v.getRatonPulsado();
			v.borra();
			v.dibujaCirculo( xLanz, yLanz, 10, 3.0f, Color.MAGENTA );
			if (pPuls!=null) {  // Se hace click: disparar!
				disparar( xLanz, yLanz, pPuls.getX(), pPuls.getY() );
			} else if (pMovto!=null) {  // No se hace click: dibujar flecha
				v.dibujaFlecha( xLanz, yLanz, pMovto.getX(), pMovto.getY(), 2.0f, Color.GREEN, 25 );
			}
			v.repaint();
			if (v.estaCerrada()) seguir = false;  // Acaba cuando el usuario cierra la ventana
			v.espera( 20 ); // Pausa 20 msg (aprox 50 frames/sg)
		}
	}
		// Hace un disparo con la velocidad marcada por el vector con gravedad
		private static void disparar( double x1, double y1, double x2, double y2 ) {
			double velX = x2-x1; double velY = y2-y1;
			double G = 9.8; // Aceleración de la gravedad
			dispararVA( x1, y1, velX, velY, G );
		}
		// Hace un disparo con la velocidad y ángulo indicados
		private static void dispararVA( double x1, double y1, double velX, double velY, double acel ) {
			v.setMensaje( "Calculando disparo con velocidad (" + velX + "," + velY + ")" );
			double x = x1; double y = y1;  // Punto de disparo
			int pausa = 10; // msg de pausa de animación
			double tempo = pausa / 1000.0; // tiempo entre frames de animación (en segundos)
			do {
				v.dibujaCirculo( x, y, 1.0, 1.0f, Color.blue );  // Dibuja punto
				x = x + velX * tempo; // Mueve x (según la velocidad)
				y = y + velY * tempo;// Mueve y (según la velocidad)
				velY = velY + acel * 10 * tempo; // Cambia la velocidad de y (por efecto de la gravedad)
				v.repaint();
				v.espera( pausa ); // Pausa 20 msg (aprox 50 frames/sg)
			} while (y<y1);  // Cuando pasa hacia abajo la vertical se para
			v.espera( 2000 ); // Pausa de 2 segundos
			v.setMensaje( "Vuelve a disparar!" );
		}
	
	// Prueba 4: petición de texto en la ventana
	private static void texto() {
		v.setDibujadoInmediato( true );
		v.dibujaImagen( "/img/UD-roller.jpg", 400, 300, 1.0, 0.0, 1.0f );
		Font f = new Font( "Arial", Font.PLAIN, 30 );
		String t1 = v.leeTexto( 100, 100, 200, 50, "Modifica texto", f, Color.magenta );
		v.setMensaje( "Introduce texto" );
		v.dibujaTexto( 100, 200, "Texto introducido: " + t1, f, Color.white );
		v.setMensaje( "Introduce texto otra vez" );
		t1 = v.leeTexto( 100, 300, 200, 50, "", f, Color.blue );
		v.setMensaje( "Textos leídos." );
		v.dibujaTexto( 100, 400, "Texto introducido: " + t1, f, Color.white );
		v.espera( 5000 );
		v.acaba();
	}
	
	// Prueba 5: salto con opciones
	private static void salto() {
		JDialog dialog = new MiDialog( v.getJFrame() ); 
		dialog.setVisible( true );
		v.acaba();
	}
		@SuppressWarnings("serial")
		private static class MiDialog extends JDialog {
			private JTextField tfTiempoMax = new JTextField( "15.0", 3 );
			private JTextField tfVel = new JTextField( "9.0", 3 );
			private JTextField tfAng = new JTextField( "-30", 3 );
			private JTextField tfGravedad = new JTextField( "9.8", 3 );
			private JTextField tfPixelsMetro = new JTextField( "40.0", 3 );
			private JCheckBox cbDouble = new JCheckBox( "Usa valores reales para posición", true );
			private JTextField tfTiempoPausa = new JTextField( "20", 3 );
			private JCheckBox cbTiempoReal = new JCheckBox( "Pausa de game loop sobre tiempo real", false );
			private JCheckBox cbPausa = new JCheckBox( "Hacer pausa en cada game loop", true );
			private JCheckBox cbRebote = new JCheckBox( "Con rebote", false );
			private JCheckBox cbCirculo = new JCheckBox( "Con círculo", false );
			private boolean enFrames;
			private boolean pararFotograma;
			private boolean botonPulsadoEnFrame;
			private long inicioPulsacionEnFrame;
			private double alturaSalto;
			MiDialog( JFrame vent ) {
				super( vent, "Control de salto" );
				setSize( 280, 500 );
				setLocation(v.getJFrame().getX() + v.getAnchura(), v.getJFrame().getY());
				setModalityType( ModalityType.APPLICATION_MODAL );
				JPanel pCentral = new JPanel();
				pCentral.setLayout( new BoxLayout( pCentral, BoxLayout.Y_AXIS ));
				JPanel pLin = new JPanel( new FlowLayout(FlowLayout.LEFT ) ); pLin.add( new JLabel( "Tiempo máximo de animación (sg.):" ) ); pLin.add( tfTiempoMax ); pCentral.add( pLin );
				pLin = new JPanel( new FlowLayout(FlowLayout.LEFT ) ); pLin.add( new JLabel( "Velocidad de salto (m/sg.):" ) ); pLin.add( tfVel ); pCentral.add( pLin );
				pLin = new JPanel( new FlowLayout(FlowLayout.LEFT ) ); pLin.add( new JLabel( "Ángulo de salto (grados):" ) ); pLin.add( tfAng ); pCentral.add( pLin );
				pLin = new JPanel( new FlowLayout(FlowLayout.LEFT ) ); pLin.add( new JLabel( "Gravedad (metros/s2):" ) ); pLin.add( tfGravedad ); pCentral.add( pLin );
				pLin = new JPanel( new FlowLayout(FlowLayout.LEFT ) ); pLin.add( new JLabel( "Píxels / metro:" ) ); pLin.add( tfPixelsMetro ); pCentral.add( pLin );
				pLin = new JPanel( new FlowLayout(FlowLayout.LEFT ) ); pLin.add( cbDouble );  pCentral.add( pLin );
				pLin = new JPanel( new FlowLayout(FlowLayout.LEFT ) ); pLin.add( new JLabel( "Pausa entre frames (msg.):" ) ); pLin.add( tfTiempoPausa ); pCentral.add( pLin );
				pLin = new JPanel( new FlowLayout(FlowLayout.LEFT ) ); pLin.add( cbTiempoReal ); pCentral.add( pLin );
				pLin = new JPanel( new FlowLayout(FlowLayout.LEFT ) ); pLin.add( cbPausa ); pCentral.add( pLin );
				pLin = new JPanel( new FlowLayout(FlowLayout.LEFT ) ); pLin.add( cbRebote ); pLin.add( cbCirculo ); pCentral.add( pLin );
				JPanel pInferior = new JPanel();
				JButton bProbar = new JButton( "Play" );
				JButton bFrame = new JButton( "Frame" );
				JButton bAcabar = new JButton( "Fin" );
				pInferior.add( bProbar ); pInferior.add( bFrame ); pInferior.add( bAcabar );
				add( pCentral, BorderLayout.NORTH );
				add( pInferior, BorderLayout.SOUTH );
				bAcabar.addActionListener( new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				bProbar.addActionListener( new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						(new Thread( new Runnable() {
							@Override
							public void run() {
								enFrames = false;
								bProbar.setEnabled( false );
								double vel = Double.parseDouble( tfVel.getText() ) * Double.parseDouble( tfPixelsMetro.getText() );
								double ang = Double.parseDouble( tfAng.getText() ) / 180.0 * Math.PI;
								double velX = vel*Math.cos(ang);
								double velY = vel*Math.sin(ang);
								moverVA( 30, 500, velX, velY, Double.parseDouble( tfGravedad.getText() ) );
								bProbar.setEnabled( true );
							}
						})).start();
					}
				});
				bFrame.addActionListener( new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						avanzaFrame( bProbar );
					}
				});
				JRootPane rootPane = SwingUtilities.getRootPane(bProbar); 
				rootPane.setDefaultButton(bProbar);			
				addComponentListener( new ComponentAdapter() {
					@Override
					public void componentMoved(ComponentEvent e) {
						v.getJFrame().setLocation(getX() - v.getAnchura(), getY());
					}
				});
				// Detección de pulsación continuada de ratón...
				bFrame.addMouseListener( new MouseAdapter() {  
					@Override
					public void mousePressed(MouseEvent e) {
						botonPulsadoEnFrame = true;  // Puede empezarse a repetir
						inicioPulsacionEnFrame = System.currentTimeMillis();
					}
					@Override
					public void mouseReleased(MouseEvent e) {
						botonPulsadoEnFrame = false;  // Se acaba la repetición al soltar el ratón
					}
				});
				bFrame.addMouseMotionListener( new MouseMotionAdapter() {
					@Override
					public void mouseDragged(MouseEvent e) {
						botonPulsadoEnFrame = false;  // Cuando se mueve el ratón también se deja de repetir
					}
				});
				Thread hiloPulsacionRaton = new Thread( new Runnable() {
					public void run() {
						while (true) {
							try {
								Thread.sleep(100);  // Se lanza "click" una vez cada décima de segundo
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (botonPulsadoEnFrame && (System.currentTimeMillis()-inicioPulsacionEnFrame)>500) {  // Cada décima con un retardo inicial de medio segundo
								avanzaFrame( bProbar );
							}
						}
					}
				} );
				hiloPulsacionRaton.setDaemon( true );  // Que se acabe solo al final del programa
				hiloPulsacionRaton.start();
				double radio = 1.5 / 2.0 * Double.parseDouble( tfPixelsMetro.getText() );  // Altura = diámetro = 1.5 m 
				dibujaMundo( 40, 500, 0.0, 0.0, 40, 500, radio, 0.0, 0.0, 0, 0.0, 0 );
			}
				private void avanzaFrame( JButton bProbar ) {
					if (!enFrames) {
						enFrames = true;
						cbTiempoReal.setSelected( false );  // En este caso no tiene sentido hacer pausa con tiempo real porque vamos haciendo pausas a mano
						(new Thread( new Runnable() {
							@Override
							public void run() {
								pararFotograma = false;
								bProbar.setEnabled( false );
								double vel = Double.parseDouble( tfVel.getText() ) * Double.parseDouble( tfPixelsMetro.getText() );
								double ang = Double.parseDouble( tfAng.getText() ) / 180.0 * Math.PI;
								double velX = vel*Math.cos(ang);
								double velY = vel*Math.sin(ang);
								moverVA( 40, 500, velX, velY, Double.parseDouble( tfGravedad.getText() ) );
								bProbar.setEnabled( true );
								enFrames = false;
							}
						})).start();
					} else {
						pararFotograma = false;
					}
				}

			private Font font = new Font( "Arial", Font.PLAIN, 16 );
			// Mueve un círculo con la velocidad y ángulo indicados
			private void moverVA( double xIni, double yIni, double velX, double velY, double acel ) {
				alturaSalto = 0;
				double radio = 1.5 / 2.0 * Double.parseDouble( tfPixelsMetro.getText() );  // Altura = diámetro = 1.5 m 
				v.setMensaje( "Calculando movimiento de salto" );
				double x = xIni; double y = yIni;  // Punto de disparo
				int pausa = Integer.parseInt( tfTiempoPausa.getText() ); // msg de pausa de animación
				double frec = 1000.0 / pausa;  // Número aproximado de fotogramas por segundo
				double tempo = 1.0 / frec; // tiempo entre frames de animación (en segundos)
				double tiempoSimulado = 0.0;
				double distanciaAcum = 0.0;
				long tiempoIni = System.currentTimeMillis();
				dibujaMundo( x, y, velX, velY, xIni, yIni, radio, tiempoSimulado, 0.0, tiempoIni, 0.0, 0 );
				long tiempoiniUpdate = System.currentTimeMillis();
				v.espera( pausa ); // Pausa 20 msg (aprox 50 frames/sg)
				int numFrames = 1;
				do {
					// Input - no hay
					// Update
					double deltaTime = tempo;
					if (cbTiempoReal.isSelected()) {
						deltaTime = 0.001 * (System.currentTimeMillis()-tiempoiniUpdate);
					}
					tiempoiniUpdate = System.currentTimeMillis();
					// Cálculo físicas
					double xAnt = x; double yAnt = y;
					if (cbDouble.isSelected()) {  // Cálculo con doubles
						x = x + velX * deltaTime; // Mueve x (según la velocidad)
						y = y + velY * deltaTime;// Mueve y (según la velocidad)
					} else {
						x = (int)x + (int)(velX * deltaTime); // Mueve x (según la velocidad)
						y = (int)y + (int)(velY * deltaTime);// Mueve y (según la velocidad)
					}
					velY = velY + (acel * Double.parseDouble(tfPixelsMetro.getText())) * deltaTime; // Cambia la velocidad de y (por efecto de la gravedad)
					distanciaAcum = distanciaAcum + Math.sqrt( (x-xAnt)*(x-xAnt) + (y-yAnt)*(y-yAnt) );
					tiempoSimulado += deltaTime;
					numFrames++;
					double alturaActual = yIni - y;
					if (alturaActual > alturaSalto) alturaSalto = alturaActual;
					// Render
					dibujaMundo( x, y, velX, velY, xIni, yIni, radio, tiempoSimulado, 0.001*(System.currentTimeMillis()-tiempoIni), tiempoIni, distanciaAcum, numFrames );
					if (cbPausa.isSelected()) {
						if (cbTiempoReal.isSelected()) {
							long espera = pausa - (System.currentTimeMillis()-tiempoiniUpdate);
							if (espera>0) v.espera( espera ); // Pausa msg ajustado al fps
						} else {
							v.espera( pausa ); // Pausa 20 msg (aprox 50 frames/sg)
						}
					}
					if (cbRebote.isSelected()) {
						if (velY>0 && y>=yIni) velY=-velY;  // Rebote en suelo
						if (x>v.getAnchura() & velX>0 || x<0 & velX<0) velX = -velX;  // Rebote en bordes
					}
					if (enFrames) {
						pararFotograma = true;
						while (pararFotograma && (System.currentTimeMillis()-tiempoIni)<Double.parseDouble(tfTiempoMax.getText())*1000) {
							v.espera( 10 );
						}
					}
				} while ((y<yIni || velY<0) && (System.currentTimeMillis()-tiempoIni)<Double.parseDouble(tfTiempoMax.getText())*1000);  // Cuando pasa hacia abajo la vertical se para o cuando está segundos máximos
				v.setMensaje( "Cambia los parámetros si quieres hacer otro salto." );
				v.dibujaTexto( 20, 60, "Animación detenida.", font, Color.red );
				v.repaint();
			}
			
				private void dibujaMundo( double x, double y, double velX, double velY, double xIni, double yIni, double radio, double tiempoSimulado, double tiempoReal, long tiempoIni, double distanciaAcum, int numFrames ) {
					double tMaximo = Double.parseDouble(tfTiempoMax.getText())*1000;
					double pixM = Double.parseDouble(tfPixelsMetro.getText());
					double velActual = Math.sqrt( velX*velX + velY*velY );
					v.borra();
					v.dibujaLinea( 0, yIni+radio, v.getAnchura(), yIni+radio, 2.0f, Color.black );
					v.dibujaTexto( 10, yIni+radio+20, String.format( "%.0f píxels = 1 metro, %.0f píxels = 10 metros", Double.parseDouble(tfPixelsMetro.getText()) , 10*Double.parseDouble(tfPixelsMetro.getText()) ), font, Color.magenta );
					v.dibujaFlecha( 10, yIni+radio+28, 10+Double.parseDouble(tfPixelsMetro.getText()), yIni+radio+28, 2.0f, Color.magenta, 10 );
					v.dibujaFlecha( 10, yIni+radio+35, 10+10*Double.parseDouble(tfPixelsMetro.getText()), yIni+radio+35, 2.0f, Color.magenta, 10 );
					if (cbCirculo.isSelected()) v.dibujaCirculo( x, y, radio, 2.0f, Color.orange );  // Dibuja punto
					v.dibujaImagen( "img/sonic.png", x, y, (int)(radio*2*1.5), (int)(radio*2*1.7), 1, 0, 1 );   // El ajuste de 1.7 es por el gráfico particular que tiene mucho 'aire'
					v.dibujaTexto( 400, 20, String.format( "Suelo inicial: %.0f     Altura salto: %.2f", yIni, alturaSalto ), font, Color.blue );
					v.dibujaTexto( 400, 40, String.format( "Coordenada personaje: ( %.3f , %.3f )", x, y ), font, Color.blue );
					v.dibujaTexto( 400, 60, String.format( "Tiempo real: %.3f - Tiempo simulado: %.3f", tiempoReal , tiempoSimulado ), font, Color.blue );
					v.dibujaTexto( 400, 80, String.format( "Velocidad píxels x,y: ( %.3f , %.3f )", velX, velY ), font, Color.blue );
					if (tiempoReal>0.0) v.dibujaTexto( 400, 100, String.format( "Velocidad (m/s) %.3f (actual %.3f)", (distanciaAcum/pixM/tiempoReal), velActual/pixM ), font, Color.blue );
					v.dibujaTexto( 400, 120, String.format( "Distancia recorrida (m): %.3f - horizontal %.3f", distanciaAcum/pixM, (x - xIni)/pixM ), font, Color.blue );
					if (tiempoReal>0) v.dibujaTexto( 20, 20, String.format( "%.2f frames por segundo", (1.0*numFrames/tiempoReal) ), font, Color.blue );
					v.dibujaRect( 20, 25, 200, 15, 1f, Color.black, Color.orange );
					v.dibujaRect( 20, 25, 200 / tMaximo * (System.currentTimeMillis()-tiempoIni), 15, 1f, Color.black, Color.green );
					v.dibujaTexto( 230, 40, String.format( "%.1f segundos", tMaximo*0.001 ), font, Color.black );
					if (cbPausa.isSelected()) {
						v.repaint();
					} else {
						v.pintadoInmediato();
					}
				}
		
		}
			
}
