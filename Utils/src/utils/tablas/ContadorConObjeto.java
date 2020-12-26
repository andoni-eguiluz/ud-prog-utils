package utils.tablas;

public class ContadorConObjeto implements Comparable<ContadorConObjeto> {
	private int contador;
	private Object objeto;
	public ContadorConObjeto( int c, Object o ) { contador = c; objeto = o; }
	@Override
	public int compareTo(ContadorConObjeto arg0) {
		return arg0.contador-contador;  // ordena en descendente
	}
	public Object getObject() {
		return objeto;
	}
	public int getCont() {
		return contador;
	}
	public void inc() {
		contador++;
	}
	public void inc( int cantIncremento ) {
		contador += cantIncremento;
	}
	@Override
	public String toString() {
		return "{" + contador + "} " + objeto;
	}
	
}
