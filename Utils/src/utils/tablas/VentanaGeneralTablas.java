package utils.tablas;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.*;

@SuppressWarnings("serial")
public class VentanaGeneralTablas extends JFrame {
	private JDesktopPane desktop;
	private JLabel lMensaje = new JLabel( " " );
	private JPanel pSuperior = new JPanel();
	private JMenu menuVentanas;
	private JMenu menuAcciones;
	private Runnable accionCierre;
	private ArrayList<JInternalFrame> misSubventanas;
	
	public VentanaGeneralTablas() {
		misSubventanas = new ArrayList<>();
		// Configuración general
		setTitle( "Ventana General" );
		setSize( 1200, 800 ); // Tamaño por defecto
		setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		// Creación de componentes y contenedores
		desktop = new JDesktopPane();
		add( desktop, BorderLayout.CENTER );
		// setContentPane( desktop );
		add( lMensaje, BorderLayout.SOUTH );
		add( pSuperior, BorderLayout.NORTH );
		// Menú y eventos
		addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				cerrarVentana();
			}
		});
		JMenuBar menuBar = new JMenuBar();
		menuVentanas = new JMenu( "Ventanas" ); menuVentanas.setMnemonic( KeyEvent.VK_V );
		menuBar.add( menuVentanas );
		menuAcciones = new JMenu( "Acciones" ); menuAcciones.setMnemonic( KeyEvent.VK_A );
		menuBar.add( menuAcciones );
		setJMenuBar( menuBar );
	}
	
		private void cerrarVentana() {
			if (accionCierre!=null) accionCierre.run();
		}
	
		private ActionListener alMenu = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String vent = e.getActionCommand();
				for (JInternalFrame vd : misSubventanas) if (vd.getName().equals( vent )) { vd.setVisible( true ); vd.moveToFront(); return; }
			}
		};
		
	private HashMap<JInternalFrame,JMenuItem> menusInternos = new HashMap<>();
	public void addVentanaInterna( JInternalFrame f, String codVentana ) {
		desktop.add( f );
		JMenuItem menuItem = new JMenuItem( codVentana ); 
		menuItem.setActionCommand( codVentana );
		menuItem.addActionListener( alMenu );
		menusInternos.put( f,  menuItem );
		menuVentanas.add( menuItem );	
		misSubventanas.add( f );
		f.setName( codVentana );
	}
	
	public void removeVentanaInterna( JInternalFrame f ) {
		if ( misSubventanas.contains( f ) ) {
			desktop.remove( f );
			JMenuItem mi = menusInternos.remove( f );
			menuVentanas.remove( mi );
			misSubventanas.remove( f );
		}
	}
	
	public void setAccionCierre( Runnable runnable ) {
		accionCierre = runnable;
	}
	
	public JPanel getPanelSuperior() {
		return pSuperior;
	}
	
	public JDesktopPane getJDesktopPane() {
		return desktop;
	}
	
	public void setMensaje( String mens ) {
		if (mens==null || mens.isEmpty()) mens = " ";
		lMensaje.setText( mens );
	}
	
	public void addMenuAccion( String textoMenu, ActionListener accion ) {
		JMenuItem menuItem = new JMenuItem( textoMenu );
		menuItem.setActionCommand( textoMenu );
		menuItem.addActionListener( accion );
		menuAcciones.add( menuItem );
	}
	
}
