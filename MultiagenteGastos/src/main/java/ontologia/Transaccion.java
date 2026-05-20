package ontologia;

import jade.content.Predicate;

public class Transaccion implements Predicate {
    private float monto;
    private String categoria; // "Necesidad", "Ocio", "Ahorro" A CAMBIAR
    private int diaDelMes;
    private String tipo;

    // Getters y Setters obligatorios para BeanOntology
    public float getMonto() { return monto; }
    public void setMonto(float monto) { this.monto = monto; }
    
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    
    public int getDiaDelMes() { return diaDelMes; }
    public void setDiaDelMes(int diaDelMes) { this.diaDelMes = diaDelMes; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}