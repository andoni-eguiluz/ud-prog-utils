package utils.webAccess;

import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class HTTPAccess {
	
	private static String mandatoryText = "la informaci";
	private static String forbiddenText = "";
	private static String[] errorPageText = {
		"Error 404", "<title>"    // Must have all strings in same line
	};
	private static int maxNumThreads = 10;
	private static ArrayList<MiThreadParaURL> threadsActivos = new ArrayList<MiThreadParaURL>();
	private static int millisBetweenTests = 1000;
	private static int millisToWaitThreadFinishes = 60000;
	private static int millisToWaitWhenMaxThreadsAlive = 5000;
	private static boolean finishMain = false;
	private static JFrame myWindow;
	
	private static int numTimeOuts = 0;
	private static PrintStream logFile;
	
	public static void writeURLcontentToConsole( String urlCompleta ) {
	   BufferedReader input = null;
	   InputStream inStream = null;
	   try {
	      URL url = new URL(urlCompleta);
	      URLConnection connection = url.openConnection();
	      connection.setDoInput(true);
	      inStream = connection.getInputStream();
	      input = new BufferedReader(new InputStreamReader(inStream));
	      String line = "";
	      while ((line = input.readLine()) != null)
	         System.out.println(line);
	   } catch (Exception e) {
	      System.out.println(e.toString());
	   } finally {
		   try {
			   inStream.close();
			   input.close();
		   } catch (IOException e) {
		   }
	   }
	}
	
	public static boolean checkURLcontent( String urlCompleta, String mandatoryText, String forbiddenText ) 
	throws MalformedURLException,  // URL incorrecta 
	 IOException, // Error al abrir conexión
	 UnknownHostException, // servidor web no existente
	 FileNotFoundException, // En algunos servidores, acceso a página inexistente
	 ConnectException // Error de timeout
	{
		BufferedReader input = null;
		InputStream inStream = null;
		boolean checkOk = false;
		try {
		    URL url = new URL(urlCompleta);
		    URLConnection connection = url.openConnection();
		    connection.setDoInput(true);
		    inStream = connection.getInputStream();
		    input = new BufferedReader(new InputStreamReader(inStream));
		    String line = "";
		    if (mandatoryText.equals("")) checkOk = true;
		    while ((line = input.readLine()) != null) {
		    	System.out.println( line );
		    	if (!forbiddenText.equals("") && line.contains( forbiddenText )) { checkOk = false; break; }
		    	if (!mandatoryText.equals("") && line.contains( mandatoryText )) { checkOk = true; }
		    	if (errorPageText.length > 0) {  // Se chequea que contenga TODAS las cadenas
	    			boolean allStrings = true;
		    		for (String s : errorPageText) {
		    			if (!line.contains( s )) allStrings = false;
		    		}
	    			if (allStrings) { 
	    				throw new FileNotFoundException( errorPageText[0] ); 
	    			}
		    	}
		    }
		} finally {
			try {
				inStream.close();
				input.close();
			} catch (Exception e2) {
			}
		}
	    return checkOk;
	}
		
	public static void main( String[] s ) {
		try {
			logFile = new PrintStream( new FileOutputStream( "HTTPAccess.log", true ));
			logFile.println( "----------------------------------------" );
			logFile.println( "HTTPAccess - lanzamiento " + new Date() );
			logFile.println( "----------------------------------------" );
		} catch (FileNotFoundException e) {}
		String[] urlToTest = { "http://www.deusto.es", "http://www.google.es", "http://marca.com" };
		// writeURLcontentToConsole();
		// Ventana de parada
		myWindow = new JFrame();
		myWindow.setTitle( "HTTPAccess" );
		myWindow.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		JButton myButton = new JButton( "Cancelar!" );
		myWindow.getContentPane().add( myButton, "Center" );
		JLabel mens = new JLabel( " " );
		mens.setHorizontalAlignment( SwingConstants.CENTER );
		myWindow.getContentPane().add( mens, "South" );
		myButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				finishMain = true;
				myWindow.dispose();
			}
		});
		myWindow.setSize(280,100);
		myWindow.setLocationRelativeTo(null);
		myWindow.setVisible( true );
		// Lanzamiento de threads
		int num = 0;
		while (!finishMain) {
			if (num % 100 == 0) {
				if (num%1000==0) logFile.println( num + " consultas lanzadas..." );
				System.out.println( num + " consultas lanzadas..." );
				limpiaThreadsActivos( false );
			}
			for (String url : urlToTest) {
				Thread t = new MiThreadParaURL( url, num );
				t.start();
				num++;
			}
			// Espera temporización
			try { Thread.sleep( millisBetweenTests ); } catch (InterruptedException e) {}
			// Espera máximo de threads
			while (MiThreadParaURL.getThreadsAbiertos() > maxNumThreads) {
				// logFile.println( maxNumThreads + " abiertos..." );
				System.out.println( maxNumThreads + " abiertos vivos... esperando unos segundos para continuar" );
				limpiaThreadsActivos( false );
				try { Thread.sleep( millisToWaitWhenMaxThreadsAlive ); } catch (InterruptedException e) {}
			}
			mens.setText( "Lanzadas " + num + " consultas (" + numTimeOuts + " timeouts)" );
		}
		limpiaThreadsActivos( true );
		logFile.println( "Número de consultas: " + num + "   -- Número de timeouts: " + numTimeOuts );
		System.out.println( "Número de consultas: " + num + "   -- Número de timeouts: " + numTimeOuts );
		try { Thread.sleep(2000); } catch (InterruptedException e) {}
		System.exit(0);
	}
	
	/** Limpia la lista de threads activos, de acuerdo al tiempo máximo marcado por millisToWaitThreadFinishes.
	 * Si el thread tiene más de ese tiempo de vida, lo quita de la lista y lo da como timeout
	 * @param esperaAQueAcaben	Si true, espera a que acabe el thread ese tiempo (join) antes de matarlo
	 */
	static private void limpiaThreadsActivos( boolean esperaAQueAcaben ) {
		boolean hayThreadsActivos;
		int posiPrimero = 0;
		while (true) {
			MiThreadParaURL primero = null;
			synchronized (threadsActivos) {
				primero = (threadsActivos.size() <= posiPrimero) ? null : threadsActivos.get(posiPrimero);
				if (primero == null) break;
			}
			boolean matarThreadPrimero = true;
			if (primero != null) {
				if (esperaAQueAcaben) {
					try {
						primero.join( millisToWaitThreadFinishes );
					} catch (InterruptedException e) {
					}
				} else {
					if (primero.getLifeInMillis() < millisToWaitThreadFinishes) {
						matarThreadPrimero = false;
						posiPrimero++;
					}
				}
			}
			if (primero!= null && matarThreadPrimero && primero.isAlive()) {
				// El thread se ha bloqueado, lo quitamos de la lista
				numTimeOuts++;
				logFile.println( "Timeout! -> consulta hecha " + (new Date(primero.time)) );
				System.out.println( "Timeout! -> consulta hecha " + (new Date(primero.time)) );
				synchronized (threadsActivos) {
					threadsActivos.remove( primero );
				}
			}
		}
	}
	
	
	static class MiThreadParaURL extends Thread {
		String url;
		int num;
		static Integer threadsAbiertos = 0;
		long time;
		public MiThreadParaURL( String url, int num ) {
			this.url = url;
			this.num = num;
			time = System.currentTimeMillis();
		}
		public static int getThreadsAbiertos() {
			return threadsAbiertos;
		}
		public long getLifeInMillis() {
			return (System.currentTimeMillis() - time);
		}
		public void run() {
			// System.out.println( "Abriendo thread " + num );
			synchronized (threadsAbiertos) { threadsAbiertos++; }
			synchronized (threadsActivos) {
				threadsActivos.add( (MiThreadParaURL)Thread.currentThread() );
			}
			try {
				if (!checkURLcontent( url, mandatoryText, forbiddenText )) {
					// check erróneo
					logFile.println( "Consulta errónea: " + (new Date(time)) );
					System.out.println( "Consulta errónea: " + (new Date(time)) );
				}
				// System.out.println( "  Acceso correcto " + num );
			} catch (java.net.ConnectException e1) {  // Error de timeout
				logFile.println( e1.getMessage() + " -> consulta hecha " + (new Date(time)) );
				System.out.println( e1.getMessage() + " -> consulta hecha " + (new Date(time)) );
				numTimeOuts++;
			} catch (MalformedURLException e1) {  // Error URL incorrecta 
				System.out.println( "Error en URL: " + e1.getMessage() + " -> consulta hecha " + (new Date(time))  );
				logFile.println( "Error en URL: " + e1.getMessage() + " -> consulta hecha " + (new Date(time))  );
				e1.printStackTrace();
			} catch (UnknownHostException e1) {  // Error servidor web no existente
				System.out.println( "Error web no existe: " + e1.getMessage() + " -> consulta hecha " + (new Date(time))  );
				logFile.println( "Error web no existe: " + e1.getMessage() + " -> consulta hecha " + (new Date(time))  );
				e1.printStackTrace();
			} catch (FileNotFoundException e1) { // En algunos servidores, acceso a página inexistente
				System.out.println( "Error página inexistente: " + e1.getMessage() + " -> consulta hecha " + (new Date(time))  );
				logFile.println( "Error página inexistente: " + e1.getMessage() + " -> consulta hecha " + (new Date(time))  );
				e1.printStackTrace();
			} catch (IOException e1) {  // Error al abrir conexión
				System.out.println( "Error de conexión: " + e1.getMessage() + " -> consulta hecha " + (new Date(time))  );
				logFile.println( "Error de conexión: " + e1.getMessage() + " -> consulta hecha " + (new Date(time))  );
				e1.printStackTrace();
			} catch (Exception e1) {
				System.out.println( "Error indefinido: " + e1.getMessage() + " -> consulta hecha " + (new Date(time))  );
				logFile.println( "Error indefinido: " + e1.getMessage() + " -> consulta hecha " + (new Date(time))  );
				e1.printStackTrace();
			}
			synchronized (threadsActivos) {
				threadsActivos.remove( Thread.currentThread() );
			}
			synchronized (threadsAbiertos) { threadsAbiertos--; }
			// System.out.println( "Cerrando thread " + num );
		}
	}
}
