package utils.ventanas.ventanaJuego;
//A
import java.io.Serializable;

/** Clase de posición de tablero para juegos de tablero.
 * La representación del tablero es por filas y columnas,
 * empezando las filas en 0 arriba e incrementándose hacia abajo,
 * y las columnas en 0 a la izquierda, incrementándose hacia la derecha.
 * @author eguiluz
 */
public class CoordTablero implements Serializable {
	private static final long serialVersionUID = 1L;
	private int fila;
	private int columna;
	
	/** Crea una nueva coordenada de tablero (se permiten valores
	 * negativos)
	 * @param fila	Fila de la coordenada
	 * @param columna	Columna de la coordenada
	 */
	public CoordTablero( int fila, int columna ) {
		this.fila = fila;
		this.columna = columna;
	}
	
	/** Devuelve la fila de la coordenada
	 * @return	Fila 
	 */
	public int getFila() {
		return fila;
	}

	/** Cambia la fila de la coordenada
	 * @param fila	Fila de la coordenada (se permiten valores negativos)
	 */
	public void setFila(int fila) {
		this.fila = fila;
	}

	/** Devuelve la columna de la coordenada
	 * @return	Columna
	 */
	public int getColumna() {
		return columna;
	}

	/** Cambia la columna de la coordenada
	 * @param columna	Columna de la coordenada (se permiten valores negativos)
	 */
	public void setColumna(int columna) {
		this.columna = columna;
	}

	/** Cambia la fila y la columna de la coordenada
	 * @param fila	Fila de la coordenada (se permiten valores negativos)
	 * @param columna	Columna de la coordenada (se permiten valores negativos)
	 */
	public void setCoord(int fila, int columna) {
		this.fila = fila;
		this.columna = columna;
	}

	/** Devuelve la distancia entre dos coordenadas
	 * @param c2	Segunda coordenada de tablero
	 * @return	Distancia lineal, en unidades de tablero, entre this y c2
	 */
	public double distanciaCon( CoordTablero c2 ) {
		return 
			Math.sqrt( 
				Math.pow( Math.abs(c2.fila-fila), 2 ) +
				Math.pow( Math.abs(c2.columna-columna), 2 ) );
	}

	@Override
	protected Object clone() {
		return new CoordTablero( fila, columna );
	}

	@Override
	public boolean equals(Object coor2) {
		if (coor2 instanceof CoordTablero) {
			CoordTablero ct = (CoordTablero) coor2;
			return fila==ct.fila && columna==ct.columna;
		} else
			return false;
	}

	@Override
	public String toString() {
		return "(" + fila + "," + columna + ")";
	}

	/** Método de prueba.
	 * @param args	No se utiliza
	 */
	public static void main(String[] args) {
		CoordTablero c1 = new CoordTablero( 0, 7 );
		CoordTablero c2 = new CoordTablero( -2, -1 );
		System.out.println( "Una coordenada: " + c1 );
		System.out.println( "Otra coordenada: " + c2 );
		CoordTablero c3 = (CoordTablero) c2.clone();
		System.out.println( "Tercera -copia de la segunda-: " + c3 );
		System.out.println( "Segunda y tercera " + 
			(c2.equals(c3)?"":"no ") + "son iguales." );		
		c3.setCoord( 1, 5 );
		System.out.println( "Cambiando la coordenada de la tercera a (1,5)...");
		System.out.println( "Segunda coordenada: " + c2 );
		System.out.println( "Segunda y tercera " + 
			(c2.equals(c3)?"":"no ") + "son iguales." );		
	}

}
