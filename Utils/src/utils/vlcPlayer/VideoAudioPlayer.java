package utils.vlcPlayer;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.Meta;
import uk.co.caprica.vlcj.media.MetaData;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.fullscreen.FullScreenStrategy;
import uk.co.caprica.vlcj.player.embedded.fullscreen.windows.Win32FullScreenStrategy;
import uk.co.caprica.vlcj.waiter.UnexpectedWaiterErrorException;
import uk.co.caprica.vlcj.waiter.UnexpectedWaiterFinishedException;
import uk.co.caprica.vlcj.waiter.media.ParsedWaiter;

/** Clase principal de reproductor de vídeo o audio
 * Utiliza la librería externa VLCj que debe estar instalada y configurada
 *     (http://www.capricasoftware.co.uk/projects/vlcj/index.html)
 * Descargar http://capricasoftware.co.uk/downloads/vlcj/vlcj-3.10.1-dist.zip
 *   Enlazar el proyecto con el vlcj-3.10.1.jar  (o versión actualizada)
 *     (Menú Project | Properties | Java Build Path | Libraries | Add External Jar...) 
 * El path de VLC debe estar en la variable de entorno "vlc" o en la constante PATH_VLC.
 * Comprobar que la versión de 32/64 bits de Java y de VLC es compatible.
 * @author Andoni Eguíluz Morán
 * Facultad de Ingeniería - Universidad de Deusto
 */
public class VideoAudioPlayer {
	private static String PATH_VLC = "c:\\Archivos de programa\\videolan\\vlc-2.1.5";
	static {
		// Enlazar vlc buscándolo de forma nativa
        boolean found = new NativeDiscovery().discover();
        if (!found) {  // Si no lo encuentra automáticamente...
	        String vlcPath = System.getenv().get( "vlc" );  // Buscar si está la variable de entorno vlc
			if (vlcPath!=null)  // Poner vlc desde ahí
				System.setProperty( "jna.library.path", vlcPath );
			else  // Si no está, ponerlo "a mano"
				System.setProperty("jna.library.path", PATH_VLC);
        }
        try {
        	System.out.println( "VLC encontrado: " + LibVlc.libvlc_get_version() );
        } catch (Throwable e) {
        	System.out.println( "Error: VLC no encontrado." );
        	System.exit(1);  // Sale con error
        }
	}
	
	
	static ConProgresoTemporal miVideoListener = null;
	static ConProgresoTemporal miAudioListener = null;
	private static final Object syncVideoPlayer = new Object();
	private static final Object syncAudioPlayer = new Object();
	
	private static VentanaVideoAudioPlayer miVentana = null;
	private static AudioPlayer miAudio = null;
	private static ArrayList<File> ficherosRepVideo = new ArrayList<File>();
		// ficheros de reproducción en vídeo (con controlador)
	static int reproduciendoVideo = -1;  // Fichero reproduciéndose
	private static ArrayList<File> ficherosRepAudio = new ArrayList<File>();
		// ficheros de reproducción en audio (sin controlador)
	static int reproduciendoAudio = -1;  // Fichero reproduciéndose
	
	/** Inicia la ventana correspondiente a la reproducción de vídeo
	 */
	public static void startVideo() {
		if (miVentana == null) miVentana = new VentanaVideoAudioPlayer();
	}
	
	/** Detiene la reproducción de vídeo y cierra la ventana correspondiente si existe
	 */
	public static void stopVideo() {
		if (miVentana!=null) {
			miVentana.dispose();
		}
	}
	
	/** Detiene la reproducción de audio
	 */
	public static void stopAudio() {
		if (miAudio!=null) {
			miAudio.dispose();
		}
	}
	
	/** Pausa la reproducción de audio
	 */
	public static void pausaAudio() {
		if (miAudio!=null && miAudio.mediaPlayer!=null) {
			miAudio.mediaPlayer.controls().pause();
		}
	}
	
	/** Restaura la reproducción de audio
	 */
	public static void playAudio() {
		if (miAudio!=null && miAudio.mediaPlayer!=null) {
			miAudio.mediaPlayer.controls().play();
		}
	}
	
	/** Devuelve información de si está reproduciéndose el vídeo
	 * @return	true si se está reproduciendo, false en caso contrario
	 */
	public static boolean videoPlaying() {
		return miVentana!=null && miVentana.mediaPlayer!=null && miVentana.mediaPlayer.status().isPlaying();
	}
	
	/** Devuelve información de si está reproduciéndose el audio
	 * @return	true si se está reproduciendo, false en caso contrario
	 */
	public static boolean audioPlaying() {
		return miAudio!=null && miAudio.mediaPlayer!=null && miAudio.mediaPlayer.status().isPlaying();
	}
	
	/** Espera los milisegundos indicados
	 * @param milis	Milisegundos a esperar (positivo)
	 */
	public static void espera( long milis ) {
		try { Thread.sleep( milis ); } catch (Exception e) {}
	}
	
	
	/** Va a video anterior
	 * @return	true si correcto, false si no hay anterior
	 */
	public static boolean irAVideoAnterior() {
		if (reproduciendoVideo>=0) {
			miVentana.paraVideo();
			reproduciendoVideo--;
			miVentana.lanzaVideo();
			return true;  // No hay error
		}
		return false;  // Error (no hay anterior)
	}
	/** Va a audio anterior
	 * @return	true si correcto, false si no hay anterior
	 */
	public static boolean irAAudioAnterior() {
		if (reproduciendoAudio>=0) {
			miAudio.paraAudio();
			reproduciendoAudio--;
			miAudio.lanzaAudio();
			return true;  // No hay error
		}
		return false;  // Error (no hay anterior)
	}
	/** Va a video siguiente
	 * @return	true si correcto, false si no hay siguiente
	 */
	public static boolean irAVideoSiguiente() {
		if (reproduciendoVideo<ficherosRepVideo.size()-1) {
			miVentana.paraVideo();
			reproduciendoVideo++;
			miVentana.lanzaVideo();
			return true;  // No hay error
		}
		return false;  // Error (no hay siguiente)
	}
	/** Va a audio siguiente
	 * @return	true si correcto, false si no hay siguiente
	 */
	public static boolean irAAudioSiguiente() {
		if (reproduciendoAudio<ficherosRepAudio.size()-1) {
			miAudio.paraAudio();
			reproduciendoAudio++;
			miAudio.lanzaAudio();
			return true;  // No hay error
		}
		return false;  // Error (no hay siguiente)
	}
	/** Indica si hay más vídeos después del actual
	 * @return	true si los hay, false en caso contrario
	 */
	public static boolean hayMasVideosDespues() {
		return (reproduciendoVideo<ficherosRepVideo.size()-1);
	}
	/** Indica si hay más audios después del actual
	 * @return	true si los hay, false en caso contrario
	 */
	public static boolean hayMasAudiosDespues() {
		return (reproduciendoAudio<ficherosRepAudio.size()-1);
	}
	
	private static void marcaVideoErroneo() {
		if (reproduciendoVideo>=0 && reproduciendoVideo<ficherosRepVideo.size()) {
			ficherosRepVideo.remove(reproduciendoVideo);
		}
	}
	private static void marcaAudioErroneo() {
		if (reproduciendoAudio>=0 && reproduciendoAudio<ficherosRepAudio.size()) {
			ficherosRepAudio.remove(reproduciendoAudio);
		}
	}

	
	/** Devuelve el nombre de fichero de vídeo en reproducción actual
	 * @return	Nombre y extensión del fichero
	 */
	public static File getVideoActual() {
		if (reproduciendoVideo<0 || reproduciendoVideo>=ficherosRepVideo.size())
			return null;
		else
			return ficherosRepVideo.get(reproduciendoVideo);
	}
	/** Devuelve el nombre de fichero de audio en reproducción actual
	 * @return	Nombre y extensión del fichero
	 */
	public static File getAudioActual() {
		if (reproduciendoAudio<0 || reproduciendoAudio>=ficherosRepAudio.size())
			return null;
		else
			return ficherosRepAudio.get(reproduciendoAudio);
	}

	/** Activa un escuchador de tiempo de reproducción de los vídeos
	 * @param cpt	Escuchador
	 */
	public static void setVideoTimeListener( ConProgresoTemporal cpt ) {
		miVideoListener = cpt;
	}

	/** Activa un escuchador de tiempo de reproducción de los audios
	 * @param cpt	Escuchador
	 */
	public static void setAudioTimeListener( ConProgresoTemporal cpt ) {
		miAudioListener = cpt;
	}
	
	/** Pone el audio actual en reproducción en el tiempo indicado
	 * @param milis	Milisegundos del audio actual donde saltar
	 */
	public static void ponAudioEn( long milis ) {
		miAudio.seekAudio( milis );
	}

	/** Pone el audio actual en reproducción en el tiempo indicado
	 * @param milis	Milisegundos del audio actual donde saltar
	 */
	public static void ponVideoEn( long milis ) {
		miVentana.seekVideo( milis );
	}


	/**	Pide interactivamente una carpeta para coger vídeos o audios
	 * @return	Carpeta elegida, o null si no se selecciona ninguna
	 */
	public static File pedirCarpeta() {
		File dirActual = new File( System.getProperty("user.dir") );
		JFileChooser chooser = new JFileChooser( dirActual );
		chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
		int returnVal = chooser.showOpenDialog( null );
		if (returnVal == JFileChooser.APPROVE_OPTION)
			return chooser.getSelectedFile();
		else 
			return null;
	}
	
	
	/** Añade a la reproducción un fichero. Si se está reproduciendo ya alguno (o varios), 
	 * lo mantiene y el indicado se reproduce al final.
	 * @param f	Nombre de fichero (con path correcto) a añadir a la reproducción
	 */
	public static void reproduceVideo( String f ) {
		reproduceVideo( new File(f) );
	}
	
	/** Añade a la reproducción un fichero. Si se está reproduciendo ya alguno (o varios), 
	 * lo mantiene y el indicado se reproduce al final.
	 * @param f	Fichero a añadir a la reproducción. Si no existe, no se añade
	 */
	public static void reproduceVideo( File f ) {
		if (f.exists()) {
			ficherosRepVideo.add( f );
			if (reproduciendoVideo==-1) reproduciendoVideo = 0;
			iniciaReproduccionVideo();
		}
	}
	
	/** Añade a la reproducción todos los ficheros que haya en la 
	 * carpeta indicada, que cumplan el filtro indicado.
	 * Si hay cualquier error, la lista de reproducción queda solo con los ficheros
	 * que hayan podido ser cargados de forma correcta.
	 * @param carpetaFicheros	Path de la carpeta donde buscar los ficheros
	 * @param filtroFicheros	Filtro del formato que tienen que tener los nombres de
	 * 							los ficheros para ser cargados.
	 * 							String con cualquier letra o dígito. Si tiene un asterisco
	 * 							hace referencia a cualquier conjunto de letras o dígitos.
	 * 							Por ejemplo p*.* hace referencia a cualquier fichero de nombre
	 * 							que empiece por p y tenga cualquier extensión.
	 * @return	Número de ficheros que han sido añadidos a la lista
	 */
	public static int reproduceVideos(String carpetaFicheros, String filtroFicheros) {
		int ficsAnyadidos = 0;
		if (carpetaFicheros!=null) {
			try {
				filtroFicheros = filtroFicheros.replaceAll( "\\.", "\\\\." );  // Pone el símbolo de la expresión regular \. donde figure un .
				filtroFicheros = filtroFicheros.replaceAll( "\\*", ".*" );  // Pone el símbolo de la expresión regular .* donde figure un *
				// System.out.println( "expresión regular del filtro: " + filtroFicheros );
				Pattern pFics = Pattern.compile( filtroFicheros, Pattern.CASE_INSENSITIVE );
				File fInic = new File(carpetaFicheros); 
				if (fInic.isDirectory()) {
					for( File f : fInic.listFiles() ) {
						if ( pFics.matcher(f.getName()).matches() ) {
							// Si cumple el patrón, se añade
							ficsAnyadidos++;
							reproduceVideo( f );
						}
					}
				}
			} catch (PatternSyntaxException e) {
				System.out.println( "Error en patrón de expresión regular " + e );
			}
		}
		if (ficsAnyadidos>0) {
			iniciaReproduccionVideo();
		}
		return ficsAnyadidos;
	}

	/** Añade a la reproducción de audio un fichero. Si se está reproduciendo ya alguno (o varios), 
	 * lo mantiene y el indicado se reproduce al final.
	 * @param f	Nombre de fichero (con path correcto) a añadir a la reproducción de audio
	 */
	public static void reproduceAudio( String f ) {
		reproduceAudio( new File(f) );
	}
	
	/** Añade a la reproducción de audio un fichero. Si se está reproduciendo ya alguno (o varios), 
	 * lo mantiene y el indicado se reproduce al final.
	 * @param f	Fichero a añadir a la reproducción de audio. Si no existe, no se añade
	 */
	public static void reproduceAudio( File f ) {
		if (f.exists()) {
			ficherosRepAudio.add( f );
			if (reproduciendoAudio==-1) reproduciendoAudio = 0;
			iniciaReproduccionAudio();
		}
	}
	
	/** Añade a la reproducción de audio todos los ficheros que haya en la 
	 * carpeta indicada, que cumplan el filtro indicado.
	 * Si hay cualquier error, la lista de reproducción queda solo con los ficheros
	 * que hayan podido ser cargados de forma correcta.
	 * @param carpetaFicheros	Path de la carpeta donde buscar los ficheros
	 * @param filtroFicheros	Filtro del formato que tienen que tener los nombres de
	 * 							los ficheros para ser cargados.
	 * 							String con cualquier letra o dígito. Si tiene un asterisco
	 * 							hace referencia a cualquier conjunto de letras o dígitos.
	 * 							Por ejemplo p*.* hace referencia a cualquier fichero de nombre
	 * 							que empiece por p y tenga cualquier extensión.
	 * @return	Número de ficheros que han sido añadidos a la lista
	 */
	public static int reproduceAudios(String carpetaFicheros, String filtroFicheros) {
		int ficsAnyadidos = 0;
		if (carpetaFicheros!=null) {
			try {
				filtroFicheros = filtroFicheros.replaceAll( "\\.", "\\\\." );  // Pone el símbolo de la expresión regular \. donde figure un .
				filtroFicheros = filtroFicheros.replaceAll( "\\*", ".*" );  // Pone el símbolo de la expresión regular .* donde figure un *
				// System.out.println( "expresión regular del filtro: " + filtroFicheros );
				Pattern pFics = Pattern.compile( filtroFicheros, Pattern.CASE_INSENSITIVE );
				File fInic = new File(carpetaFicheros); 
				if (fInic.isDirectory()) {
					for( File f : fInic.listFiles() ) {
						if ( pFics.matcher(f.getName()).matches() ) {
							// Si cumple el patrón, se añade
							ficsAnyadidos++;
							reproduceAudio( f );
						}
					}
				}
			} catch (PatternSyntaxException e) {
				System.out.println( "Error en patrón de expresión regular " + e );
			}
		}
		if (ficsAnyadidos>0) {
			iniciaReproduccionAudio();
		}
		return ficsAnyadidos;
	}
	
	private static void iniciaReproduccionVideo() {
		if (miVentana==null) {
			try {
				SwingUtilities.invokeAndWait( new Runnable() {
					@Override
					public void run() {
						miVentana = new VentanaVideoAudioPlayer();
						miVentana.setVisible( true );
					}
				});
			} catch (InvocationTargetException | InterruptedException e) {
				e.printStackTrace();
			}
			miVentana.lanzaVideo();
		}
	}

	private static void iniciaReproduccionAudio() {
		if (miAudio==null) {
			try {
				SwingUtilities.invokeAndWait( new Runnable() {
					@Override
					public void run() {
						miAudio = new AudioPlayer();
						miAudio.setVisible( true );
						miAudio.lanzaAudio();
						miAudio.setVisible( false );
					}
				});
			} catch (Exception e) { }
		}
	}

	static class VentanaVideoAudioPlayer extends JFrame {
		private static final long serialVersionUID = 1L;
		// Datos
		private boolean finalDeAnterior = false;
		// Atributos de VLCj
		EmbeddedMediaPlayerComponent mediaPlayerComponent;
		EmbeddedMediaPlayer mediaPlayer;
		// Atributos manipulables de swing
		private JProgressBar pbVideo = null;      // Barra de progreso del vídeo en curso
		private JLabel lMensaje = null;           // Label para mensaje de reproducción
		private JLabel lMensaje2 = null;          // Label para timer de reproducción
		JPanel pBotonera;                         // Panel botonera (superior)
		ArrayList<JButton> botones;               // Lista de botones
		// Array auxiliar y enumerado para gestión de botones
		static String[] ficsBotones = new String[] { "Button Rewind", "Button Play Pause", "Button Fast Forward", "Button Maximize" };
		static enum BotonDe { ATRAS, PLAY_PAUSA, AVANCE, MAXIMIZAR };  // Mismo orden que el array

		public VentanaVideoAudioPlayer() {
			// Hilo de relanzamiento después de final
			Thread t = new Hilo();
			t.setDaemon( true );
			t.start();
			
			// Creación de componentes/contenedores de swing
			pbVideo = new JProgressBar( 0, 10000 );
			lMensaje = new JLabel( "" );
			lMensaje2 = new JLabel( "" );
			pBotonera = new JPanel();
			// En vez de "a mano":
			// JButton bAnyadir = new JButton( new ImageIcon( VideoPlayer.class.getResource("img/Button Rewind.png")) );
			// ...
			// Lo hacemos con un bucle porque mucho de la creación se repite y lo del formato que hagamos luego también	
			// (ver array ficsBotones en lista de atributos)
			botones = new ArrayList<>();
			for (String fic : ficsBotones) {
				JButton boton = new JButton( new ImageIcon( VideoAudioPlayer.class.getResource( "img/" + fic + ".png" )) );
				botones.add( boton );
				boton.setName(fic);  // Pone al botón el nombre del fichero
			}
			
			// Componente de VCLj
	        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
	        mediaPlayer = mediaPlayerComponent.mediaPlayer();

			// Configuración de componentes/contenedores
	        int indBoton = 0;
	        for (JButton boton : botones) {  // Formato de botones para que se vea solo el gráfico
	        	boton.setOpaque(false);            // Fondo Transparente (los gráficos son png transparentes)
	        	boton.setContentAreaFilled(false); // No rellenar el área
	        	boton.setBorderPainted(false);     // No pintar el borde
	        	boton.setBorder(null);             // No considerar el borde (el botón se hace solo del tamaño del gráfico)
	        	boton.setRolloverIcon(             // Pone imagen de rollover
	        		new ImageIcon( VideoAudioPlayer.class.getResource( "img/" + ficsBotones[indBoton] + "-RO.png" ) ) );
	        	boton.setPressedIcon(             // Pone imagen de click
	            		new ImageIcon( VideoAudioPlayer.class.getResource( "img/" + ficsBotones[indBoton] + "-CL.png" ) ) );
	        	indBoton++;
	        }
	        lMensaje2.setForeground( Color.white );
	        lMensaje2.setFont( new Font( "Arial", Font.BOLD, 18 ));
	        lMensaje.setForeground( Color.blue );
			setTitle("Video Player (VLCj) - Deusto Ingeniería");
			setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
			setSize( 800, 600 );
			pBotonera.setLayout( new FlowLayout( FlowLayout.LEFT ));
			
			// Enlace de componentes y contenedores
			for (JButton boton : botones ) pBotonera.add( boton );
			pBotonera.add( lMensaje2 );
			pBotonera.add( lMensaje );
			getContentPane().add( mediaPlayerComponent, BorderLayout.CENTER );
			getContentPane().add( pBotonera, BorderLayout.NORTH );
			getContentPane().add( pbVideo, BorderLayout.SOUTH );
			
			// Escuchadores
			// Repetir canción
			botones.get(BotonDe.ATRAS.ordinal()).addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (mediaPlayer.status().time()>10000)
						reiniciaVideo();
					else {
						VideoAudioPlayer.irAVideoAnterior();
					}
				}
			});
			// Pausa / Play
			botones.get(BotonDe.PLAY_PAUSA.ordinal()).addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (mediaPlayer.status().isPlayable()) {
						synchronized (syncVideoPlayer) {
							if (mediaPlayer.status().isPlaying())
								mediaPlayer.controls().pause();
							else
								mediaPlayer.controls().play();
						}
					} else {
						lanzaVideo();
					}
				}
			});
			// Canción siguiente (acaba)
			botones.get(BotonDe.AVANCE.ordinal()).addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (VideoAudioPlayer.hayMasVideosDespues()) {
						VideoAudioPlayer.irAVideoSiguiente();
					} else
						acabaVideo();
				}
			});
			// Maximizar / desmaximizar
			botones.get(BotonDe.MAXIMIZAR.ordinal()).addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					synchronized (syncVideoPlayer) {
						if (mediaPlayer.fullScreen().isFullScreen()) {
							mediaPlayer.fullScreen().set(false);
							pBotonera.setBackground( Color.LIGHT_GRAY );
						} else {
							mediaPlayer.fullScreen().set(true);
							pBotonera.setBackground( Color.BLACK );
						}
					}
				}
			});
			// Click en barra de progreso para saltar al tiempo del vídeo de ese punto
			pbVideo.addMouseListener( new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (mediaPlayer.status().isPlayable()) {
						// Seek en el vídeo
						seekVideo( (float)e.getX() / pbVideo.getWidth() );
					}
				}
			});
			// Cierre del player cuando se cierra la ventana
			addWindowListener( new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					System.out.println( "Cerrando ventana player vídeo");
					synchronized (syncVideoPlayer) {
						if (mediaPlayer!=null) {
							mediaPlayer.controls().stop();
							mediaPlayer.release();
							mediaPlayer = null;
						}
					}
				}
			});
			// Eventos del propio player
			mediaPlayer.events().addMediaPlayerEventListener( 
				new MediaPlayerEventAdapter() {
					
					// El vídeo se acaba
					@Override
					public void finished(MediaPlayer mediaPlayer) {
						finalDeAnterior = true;  // Es el hilo quien lanza la siguiente
					}
					// Hay error en el formato o en el fichero del vídeo
					@Override
					public void error(MediaPlayer mediaPlayer) {
						System.out.println( "Error en reproducción de fichero " + VideoAudioPlayer.getVideoActual().getName() );
						lMensaje.setText( "Error en reproducción de fichero " + VideoAudioPlayer.getVideoActual().getName() );
						VideoAudioPlayer.marcaVideoErroneo();
						lanzaVideo();
					}
					// Evento que ocurre al cambiar el tiempo (cada 3 décimas de segundo aproximadamente)
				    @Override
				    public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
						pbVideo.setValue( (int) (10000.0 * 
								mediaPlayer.status().time() /
								mediaPlayer.status().length()) );
						pbVideo.repaint();
						lMensaje2.setText( formatoHora.format( new Date(mediaPlayer.status().time()-3600000L) ) );
						if (VideoAudioPlayer.miVideoListener!=null) {
							VideoAudioPlayer.miVideoListener.progresa( mediaPlayer.status().time() );
						}
				    }
			});
		}

		
		@Override
		public void dispose() {
			super.dispose();
			synchronized (syncVideoPlayer) {
				if (mediaPlayer!=null) {
					mediaPlayer.controls().stop();
					mediaPlayer.release();
					mediaPlayer = null;
				}
			}
		}

		
		//
		// Métodos sobre el player de vídeo
		//
		

		// Para la reproducción del vídeo en curso
		private void paraVideo() {
			if (mediaPlayer!=null) 
				synchronized (syncVideoPlayer) {
					mediaPlayer.controls().stop();
				}
		}

		// Reinicia el vídeo en curso
		private void reiniciaVideo() {
			lanzaVideo();
		}
		
		// Lleva el vídeo en curso al final
		private void acabaVideo() {
			if (mediaPlayer.status().isPlayable()) {
				synchronized (syncVideoPlayer) {
					mediaPlayer.controls().setPosition( 1.0f );
				}
			}
		}

		// Lleva el vídeo a un porcentaje
		private void seekVideo( float porcentaje ) {
			if (mediaPlayer!=null && mediaPlayer.status().isPlaying())
				synchronized (syncVideoPlayer) {
					mediaPlayer.controls().setPosition( porcentaje );
				}
		}

		// Lleva el vídeo a unos milisegundos
		private void seekVideo( long msegs ) {
			if (mediaPlayer!=null && mediaPlayer.status().isPlaying())
				synchronized (syncVideoPlayer) {
					mediaPlayer.controls().setTime( msegs );
				}
		}
				
		
			private static DateFormat formatoFechaLocal = 
				DateFormat.getDateInstance( DateFormat.SHORT, Locale.getDefault() );
			private static DateFormat formatoHora = new SimpleDateFormat( "HH:mm:ss" );
		private void lanzaVideo() {
			if (mediaPlayer!=null) {
				File ficVideo = VideoAudioPlayer.getVideoActual();
				finalDeAnterior = false;
				if (ficVideo!=null) {
					paraVideo();
					synchronized (syncVideoPlayer) {
						mediaPlayer.media().play( ficVideo.getAbsolutePath() );
						muestraMetadatos( mediaPlayer );
					}
					Date fechaVideo = new Date( ficVideo.lastModified() );
					String nom = ficVideo.getName();
					if (nom.length()>60) nom = nom.substring(0,60);
					lMensaje.setText( nom + " - Fecha fichero: " + formatoFechaLocal.format( fechaVideo ) );
					lMensaje.repaint();
				}
			}
		}
		
		private class Hilo extends Thread {
			@Override
			public void run() {
				while (true) {
					try { Thread.sleep(100); } catch (Exception e) {}
					if (finalDeAnterior && mediaPlayer!=null && !mediaPlayer.status().isPlaying() && hayMasVideosDespues()) {
						finalDeAnterior = false;
						SwingUtilities.invokeLater( new Runnable() {
							@Override
							public void run() {
								VideoAudioPlayer.irAVideoSiguiente();
							}
						});
					}
				}
			}
		}
		
	}
	
	// Muestra en consola los metadatos del fichero
	private static void muestraMetadatos( EmbeddedMediaPlayer mediaPlayer ) {
        try {
            final Media media = mediaPlayer.media().newMedia();
            ParsedWaiter parsed = new ParsedWaiter(media) {
                @Override
                protected boolean onBefore(Media component) {
                    return media.parsing().parse();
                }
            };
			parsed.await(); // Espera a que se acabe el parsing de los metadatos (es asíncrono)
	        MetaData mm = media.meta().asMetaData();
			System.out.println( "*" + mm );
			System.out.println( "*" + mm.get(Meta.TITLE) + " | " + mm.get(Meta.ARTIST) + " | " + mm.get(Meta.ALBUM) + " | " + mm.get(Meta.GENRE) + " | " + mm.get(Meta.DATE) );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	static class AudioPlayer extends JFrame {
		private static final long serialVersionUID = 1L;
		// Datos
		private boolean finalDeAnterior = false;
		// Atributos de VLCj
		EmbeddedMediaPlayerComponent mediaPlayerComponent;
		EmbeddedMediaPlayer mediaPlayer;

		public EmbeddedMediaPlayer getMediaPlayer() { return mediaPlayer; }
		
		public AudioPlayer() {
			// Hilo de relanzamiento después de final
			Thread t = new Hilo();
			t.setDaemon( true );
			t.start();
			
			// Componente de VCLj
	        mediaPlayerComponent = new EmbeddedMediaPlayerComponent() {
				private static final long serialVersionUID = 1L;
	        };
	        mediaPlayer = mediaPlayerComponent.mediaPlayer();

			// Configuración de componentes/contenedores
			setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
			setSize( 100, 50 );
			
			// Enlace de componentes y contenedores
			getContentPane().add( mediaPlayerComponent, BorderLayout.CENTER );
			
			// Escuchadores
			// Cierre del player cuando se cierra la ventana
			addWindowListener( new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					System.out.println( "Cerrando ventana player vídeo");
					synchronized (syncAudioPlayer) {
						if (mediaPlayer!=null) {
							mediaPlayer.controls().stop();
							mediaPlayer.release();
							mediaPlayer = null;
						}
					}
				}
			});
			// Eventos del propio player
			mediaPlayer.events().addMediaPlayerEventListener( 
				new MediaPlayerEventAdapter() {
					
					// El vídeo se acaba
					@Override
					public void finished(MediaPlayer mediaPlayer) {
						finalDeAnterior = true;  // Es el hilo quien lanza la siguiente
					}
					// Hay error en el formato o en el fichero del vídeo
					@Override
					public void error(MediaPlayer mediaPlayer) {
						System.out.println( "Error en reproducción de fichero " + VideoAudioPlayer.getAudioActual().getName() );
						VideoAudioPlayer.marcaAudioErroneo();
						lanzaAudio();
					}
					// Evento que ocurre al cambiar el tiempo (cada 3 décimas de segundo aproximadamente)
				    @Override
				    public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
						if (VideoAudioPlayer.miAudioListener!=null) {
							VideoAudioPlayer.miAudioListener.progresa( mediaPlayer.status().time() );
						}
				    }
			});
		}

		@Override
		public void dispose() {
			super.dispose();
			synchronized (syncAudioPlayer) {
				if (mediaPlayer!=null) {
					mediaPlayer.controls().stop();
					mediaPlayer.release();
					mediaPlayer = null;
				}
			}
		}

		
		//
		// Métodos sobre el player de audio
		//
		

		// Para la reproducción del audio en curso
		private void paraAudio() {
			if (mediaPlayer!=null) 
				synchronized (syncAudioPlayer) {
					mediaPlayer.controls().stop();
				}
		}

		// Reinicia el audio en curso - no usada
		@SuppressWarnings("unused")
		private void reiniciaAudio() {
			lanzaAudio();
		}
		
		// Lleva el vídeo en curso al final - no usada
		@SuppressWarnings("unused")
		private void acabaAudio() {
			if (mediaPlayer.status().isPlayable()) {
				synchronized (syncAudioPlayer) {
					mediaPlayer.controls().setPosition( 1.0f );
				}
			}
		}
		
		// Lleva el audio a un porcentaje - no usada
		@SuppressWarnings("unused")
		private void seekAudio( float porcentaje ) {
			if (mediaPlayer!=null && mediaPlayer.status().isPlaying())
				synchronized (syncAudioPlayer) {
					mediaPlayer.controls().setPosition( porcentaje );
				}
		}

		// Lleva el audio a unos milisegundos
		private void seekAudio( long msegs ) {
			if (mediaPlayer!=null && mediaPlayer.status().isPlaying())
				synchronized (syncAudioPlayer) {
					mediaPlayer.controls().setTime( msegs );
				}
		}
				
		private void lanzaAudio() {
			if (mediaPlayer!=null) {
				File ficAudio = VideoAudioPlayer.getAudioActual();
				finalDeAnterior = false;
				if (ficAudio!=null) {
					paraAudio();
					synchronized (syncAudioPlayer) {
						mediaPlayer.media().play( ficAudio.getAbsolutePath() );
						muestraMetadatos( getMediaPlayer() );
					}
					System.out.println( ficAudio.getName() );
				}
			}
		}
		
		private class Hilo extends Thread {
			@Override
			public void run() {
				while (true) {
					try { Thread.sleep(100); } catch (Exception e) {}
					if (finalDeAnterior && mediaPlayer!=null && !mediaPlayer.status().isPlaying() && hayMasAudiosDespues()) {
						finalDeAnterior = false;
						SwingUtilities.invokeLater( new Runnable() {
							@Override
							public void run() {
								VideoAudioPlayer.irAAudioSiguiente();
							}
						});
					}
				}
			}
		}
		
	}
	

	
	
// Prueba con player solo de audio - saca automáticamente video con los flvs...
//	
//	static class AudioPlayer {
//		// Datos
//		private boolean finalDeAnterior = false;
//		// Atributos de VLCj
//		MediaPlayer mediaPlayer;
//
//		public AudioPlayer() {
//			// Hilo de relanzamiento después de final
//			Thread t = new Hilo();
//			t.setDaemon( true );
//			t.start();
//			// Componente de VCLj
//			
//			// Eventos del player
//	        AudioMediaPlayerComponent audioPlayer = new AudioMediaPlayerComponent() {
//	        	// El recurso se acaba
//	            @Override
//	            public void finished(MediaPlayer mediaPlayer) {
//					finalDeAnterior = true;  // Es el hilo quien lanza la siguiente
//	            }
//				// Hay error en el formato o en el fichero del vídeo
//	            @Override
//	            public void error(MediaPlayer mediaPlayer) {
//					System.out.println( "Error en reproducción de fichero " + VideoAudioPlayer.getAudioActual().getName() );
//					VideoAudioPlayer.marcaAudioErroneo();
//					lanzaAudio();
//	            }
//	        };
//	    	mediaPlayer =  audioPlayer.getMediaPlayer();
//	    	
////	    	String n = "d:\\GDrive\\d\\media\\videos\\AOrdenar\\Musica\\Ana Torroja - No me canso.mp3";
//	    	String n = "d:\\GDrive\\d\\media\\videos\\AOrdenar\\Musica\\AGUA VIVA -- Poetas Andaluces (1974).flv";
//	    	mediaPlayer.playMedia( n );
//			try { Thread.sleep(10000); } catch (Exception e) {}
//			
//		}
//
//		// Para la reproducción del vídeo en curso
//		private void paraAudio() {
//			if (mediaPlayer!=null) 
//				synchronized (syncAudioPlayer) {
//					mediaPlayer.stop();
//				}
//		}
//
//		// Reinicia el audio en curso
//		private void reiniciaAudio() {
//			lanzaAudio();
//		}
//		
//		// Lleva el audio en curso al final
//		private void acabaAudio() {
//			if (mediaPlayer.isPlayable()) {
//				synchronized (syncAudioPlayer) {
//					mediaPlayer.setPosition( 1.0f );
//				}
//			}
//		}
//		
//		private void lanzaAudio() {
//			if (mediaPlayer!=null) {
//				File ficAudio = VideoAudioPlayer.getAudioActual();
//				finalDeAnterior = false;
//				if (ficAudio!=null) {
//					paraAudio();
//					synchronized (syncAudioPlayer) {
//						mediaPlayer.playMedia( ficAudio.getAbsolutePath() );
//					}
//					System.out.println( ficAudio.getName() );
//				}
//			}
//		}
//		
//		private class Hilo extends Thread {
//			@Override
//			public void run() {
//				while (true) {
//					try { Thread.sleep(100); } catch (Exception e) {}
//					if (finalDeAnterior && mediaPlayer!=null && !mediaPlayer.isPlaying() && hayMasAudiosDespues()) {
//						finalDeAnterior = false;
//						SwingUtilities.invokeLater( new Runnable() {
//							@Override
//							public void run() {
//								VideoAudioPlayer.irAAudioSiguiente();
//							}
//						});
//					}
//					if (mediaPlayer!=null && mediaPlayer.isPlaying() && VideoAudioPlayer.miAudioListener!=null) {
//						VideoAudioPlayer.miAudioListener.progresa( mediaPlayer.getTime() );
//					}
//				}
//			}
//		}
//		
//	}

	

	// Método principal de clase para test

		private static String ficheros;
		private static String path;
	/** Ejecuta una ventana de VideoPlayer.
	 * El path de VLC debe estar en la variable de entorno "vlc".
	 * Comprobar que la versión de 32/64 bits de Java y de VLC es compatible.
	 * @param args	Un array de dos strings. El primero es el nombre (con comodines) de los ficheros,
	 * 				el segundo el path donde encontrarlos.  Si no se suministran, se piden de forma interactiva. 
	 */
	public static void main(String[] args) {
		// test 1 = reproducir varios vídeos desde una carpeta
		// test 2 = reproducir un vídeo
		// test 3 = reproducir varios audios desde una carpeta
		// test 4 = reproducir un audios

		EmbeddedMediaPlayerComponent mC = new EmbeddedMediaPlayerComponent();
		mC.mediaPlayer().media().prepare( "d:\\media\\videos\\AOrdenar\\Musica\\Ana Torroja - No me canso.mp3" );
		muestraMetadatos( mC.mediaPlayer() );

		int test = 1;
		if (test==1) {  // Varios vídeos
			// (Si se pasan argumentos al main, los usará)
			if (args==null || args.length==0) {
				args = new String[] { "*.mp4", "d:/media/videos/AOrdenar/Musica/" };
			}
			if (args.length < 2) {
				// No hay argumentos: selección manual
				File fPath = pedirCarpeta();
				if (fPath==null) return;
				path = fPath.getAbsolutePath();
				ficheros = JOptionPane.showInputDialog( null,
						"Nombre de ficheros a elegir (* para cualquier cadena)",
						"Selección de ficheros dentro de la carpeta", JOptionPane.QUESTION_MESSAGE );
			} else {
				ficheros = args[0];
				path = args[1];
			}
			reproduceVideos( path, ficheros );
		} else if (test==2) {  // Un vídeo
			reproduceVideo( "d:/GDrive/d/media/videos/AOrdenar/Musica/AGUA VIVA -- Poetas Andaluces (1974).flv" );
			// try { Thread.sleep(3000); } catch (Exception e) {}
			// stop();
		} else if (test==3) {  // Varios audios
			// (Si se pasan argumentos al main, los usará)
			if (args==null || args.length==0) {
				args = new String[] { "*.*", "d:/GDrive/d/media/videos/AOrdenar/Musica/" };
				// args = new String[] { "*new york*.mp4", "d:/GDrive/d/media/videos/AOrdenar/Musica/" };
			}
			if (args.length < 2) {
				// No hay argumentos: selección manual
				File fPath = pedirCarpeta();
				if (fPath==null) return;
				path = fPath.getAbsolutePath();
				ficheros = JOptionPane.showInputDialog( null,
						"Nombre de ficheros a elegir (* para cualquier cadena)",
						"Selección de ficheros dentro de la carpeta", JOptionPane.QUESTION_MESSAGE );
			} else {
				ficheros = args[0];
				path = args[1];
			}
			reproduceAudios( path, ficheros );
			espera(500);
			while (audioPlaying()) {
				espera( 1000 );
			}
			stopAudio();
		} else if (test==4) {  // Un audio
			reproduceAudio( "d:\\media\\videos\\AOrdenar\\Musica\\Ana Torroja - No me canso.mp3" );
			reproduceAudio( "d://media/videos/AOrdenar/Musica/AGUA VIVA -- Poetas Andaluces (1974).flv" );
			espera( 10000 );     // 10 sgs de la primera canción
			ponAudioEn( 60000 ); // La pone en un minuto
			espera( 5000 );      // 5 sgs de la primera canción
			irAAudioSiguiente(); // Avanza a siguiente canción
			espera( 5000 );      // 5 sgs de la segunda canción
			pausaAudio();        // La pausa dos segundos
			espera( 2000 );
			playAudio();         // Se vuelve a escuchar
			espera( 5000 );      // 5 sgs de la segunda canción
			irAAudioAnterior();  // Vuelve a la primera
			espera( 5000 );      // 5 sgs
			ponAudioEn( 120000 ); // Va al minuto 2
			espera( 5000 );      // 5 sgs
			irAAudioSiguiente(); // Vuelve a siguiente
			espera( 5000 );      // 5 sgs
			ponAudioEn( 240000 ); // La pone en 4 minutos y se oye hasta que acaba
			setAudioTimeListener( new ConProgresoTemporal() {  // Marcando los tiempos en consola
				@Override
				public void progresa(long milis) {
					System.out.println( "  Tiempo de canción: " + milis );
				}
			});
			while (audioPlaying()) {
				espera( 1000 );
			}
			stopAudio();
		}
		
	}

	/** Interfaz que permite gestionar el progreso temporal de cualquier objeto
	 * @author andoni.eguiluz.moran
	 */
	public static interface ConProgresoTemporal {
		/** Marca información de progreso de un objeto
		 * @param milis	Milisegundos de progreso del objeto
		 */
		void progresa( long milis );
	}
	
}
