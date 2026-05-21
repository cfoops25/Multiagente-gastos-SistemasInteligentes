package Comportamientos;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.YearMonth;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import agentes.AgentePercepcion;

public class PercepcionGUI extends JFrame {

    private AgentePercepcion miAgente;

    private JTextField txtMonto;
    private JTextField txtConcepto;
    private JTextField txtDia;

    public PercepcionGUI(AgentePercepcion agente) {
        this.miAgente = agente;

        setTitle("Registrar Nuevo Movimiento");
        setSize(380, 240);
        setResizable(false);
        setAlwaysOnTop(true);

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        JPanel panelPrincipal = new JPanel();
        panelPrincipal.setLayout(new GridLayout(4, 2, 10, 15));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panelPrincipal.setBackground(new Color(240, 244, 248));

        // 1. Concepto
        panelPrincipal.add(new JLabel("Concepto:"));
        txtConcepto = new JTextField();
        panelPrincipal.add(txtConcepto);

        // 2. Cantidad
        panelPrincipal.add(new JLabel("Cantidad (€):"));
        txtMonto = new JTextField();
        panelPrincipal.add(txtMonto);

        // 3. Día
        panelPrincipal.add(new JLabel("Día del mes (1-30):"));
        txtDia = new JTextField();
        panelPrincipal.add(txtDia);

        // 4. Botón de envío
        panelPrincipal.add(new JLabel(""));
        JButton btnEnviar = new JButton("Añadir");
        btnEnviar.setBackground(new Color(60, 100, 220));
        btnEnviar.setForeground(Color.WHITE);
        btnEnviar.setFocusPainted(false);
        btnEnviar.setFont(new Font("Arial", Font.BOLD, 12));
        btnEnviar.setOpaque(true);
        btnEnviar.setContentAreaFilled(true);
        btnEnviar.setBorderPainted(true);

        btnEnviar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 100, 220), 1),
                BorderFactory.createEmptyBorder(6, 24, 6, 24)
        ));
        btnEnviar.addActionListener(e -> enviarDatosAgente());

        panelPrincipal.add(btnEnviar);

        setContentPane(panelPrincipal);
    }

    private void enviarDatosAgente() {
        try {
            // Recogemos los datos introducidos
            float monto = Float.parseFloat(txtMonto.getText().replace(",", "."));
            String concepto = txtConcepto.getText().trim();
            int dia = Integer.parseInt(txtDia.getText().trim());
            int diasDelMesActual = YearMonth.now().lengthOfMonth();

            if (concepto.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor, introduce un concepto.", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (dia < 1 || dia > diasDelMesActual) {
                JOptionPane.showMessageDialog(this, "El día debe estar entre 1 y " + diasDelMesActual + " para el mes actual.", "Día inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Enviamos los datos al agente de percepción. 
            // Pasamos "PENDIENTE" como tipo por defecto
            miAgente.enviarDatos("PENDIENTE", monto, concepto, dia);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Error: La cantidad y el día deben ser números válidos.", "Error de formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void registrarExito() {
        txtMonto.setText("");
        txtConcepto.setText("");
        txtDia.setText("");
        setVisible(false); // Ocultamos la ventana de forma segura
    }

    public void registrarDenegacion(String motivo) {
        JOptionPane.showMessageDialog(
                this,
                "El sistema financiero ha rechazado este movimiento:\n" + motivo,
                "Movimiento Denegado (FSM)",
                JOptionPane.WARNING_MESSAGE
        );
    }
}
