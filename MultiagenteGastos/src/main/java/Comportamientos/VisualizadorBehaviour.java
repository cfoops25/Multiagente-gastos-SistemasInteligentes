package Comportamientos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class VisualizadorBehaviour extends CyclicBehaviour {

    // ── Colores ──────────────────────────────────────────────────────────────
    private static final Color BG_DARK    = new Color(24, 24, 32);
    private static final Color BG_CARD    = new Color(34, 34, 46);
    private static final Color BG_CARD2   = new Color(40, 40, 54);
    private static final Color GREEN      = new Color(72,  210, 150);
    private static final Color RED        = new Color(230,  80,  80);
    private static final Color BLUE       = new Color(90,  160, 240);
    private static final Color ORANGE     = new Color(240, 150,  60);
    private static final Color TEXT       = new Color(220, 220, 230);
    private static final Color SUBTEXT    = new Color(120, 120, 140);

    // ── Componentes dinámicos ────────────────────────────────────────────────
    private JLabel lblIngresos, lblGastos, lblBalance;
    private JLabel lblAlerta, lblPct, lblGastosTotal, lblPresupuesto;
    private JProgressBar barraPresupuesto;
    private JPanel barraIngresosPanel, barraGastosPanel;
    private JLabel lblBarraI, lblValBarraI, lblBarraG, lblValBarraG;
    private DefaultTableModel modeloTabla;
    private DefaultPieDataset datasetDona;
    private PiePlot piePlot;
    private JPanel panelLeyenda;
    private JFrame ventana;

    private JLabel lblHealthScore, lblHealthEstado;
    private JProgressBar barraHealth;

    private java.util.Map<String, Color> mapaColores = new java.util.HashMap<>();
    private int colorIndex = 0;
    public VisualizadorBehaviour(Agent agente) {
        super(agente);
        SwingUtilities.invokeLater(this::crearVentana);
    }

    // ── CAMBIOS ────────────────────────────────────────────────

    private StringBuilder historialMovimientos = new StringBuilder();
    private boolean esCritico = false;

    // ── JADE ─────────────────────────────────────────────────────────────────
    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchConversationId("datos-financieros")
        );
        ACLMessage msg = myAgent.receive(mt);
        
        if (msg != null) {
            String contenido = msg.getContent();
            if (contenido != null) {
                procesarMensaje(contenido);
            }
        } else {
            block(); 
        }
    }

    private void procesarMensaje(String contenido) {
        System.out.println("[DEBUG Dashboard] Mensaje recibido bruto: " + contenido);
        String ing = "0", gas = "0", bal = "0", rechazo = "";
        boolean critico = false;
        String[] movs = new String[0];
        for (String p : contenido.split("\\|")) {
            if (p.startsWith("INGRESOS:"))    ing    = p.replace("INGRESOS:", "");
            if (p.startsWith("GASTOS:"))      gas    = p.replace("GASTOS:", "");
            if (p.startsWith("BALANCE:"))     bal    = p.replace("BALANCE:", "");
            if (p.startsWith("CRITICO:"))     critico = p.replace("CRITICO:", "").equals("true");
            if (p.startsWith("RECHAZO:"))     rechazo = p.replace("RECHAZO:", "");
            if (p.startsWith("MOVIMIENTOS:")) {
                String m = p.replace("MOVIMIENTOS:", "");
                if (!m.isEmpty()) movs = m.split(";");
            }
        }
        final String fI = ing, fG = gas, fB = bal, fR = rechazo;
        final boolean fC = critico;
        final String[] fM = movs;
        SwingUtilities.invokeLater(() -> actualizar(fI, fG, fB, fC, fM, fR));
    }

    // ── VENTANA ───────────────────────────────────────────────────────────────
    private void crearVentana() {
        ventana = new JFrame("Personal Finance Dashboard");
        ventana.setSize(1280, 760);
        ventana.setMinimumSize(new Dimension(900, 600));
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ventana.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DARK);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        root.add(crearHeader(),   BorderLayout.NORTH);
        root.add(crearCuerpo(),   BorderLayout.CENTER);

        ventana.setContentPane(root);
        ventana.setVisible(true);
    }

    // ── HEADER ────────────────────────────────────────────────────────────────
    private JPanel crearHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_DARK);
        p.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel titulo = new JLabel("Personal Finance Dashboard");
        titulo.setFont(new Font("Arial", Font.BOLD, 24));
        titulo.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Mayo 2026");
        sub.setFont(new Font("Arial", Font.PLAIN, 13));
        sub.setForeground(SUBTEXT);

        JPanel izq = new JPanel(new GridLayout(2, 1, 0, 2));
        izq.setBackground(BG_DARK);
        izq.add(titulo);
        izq.add(sub);

        // Botón para añadir movimiento
        JButton btnPeriod = new JButton("  Añadir Movimiento");
        btnPeriod.setBackground(new Color(60, 100, 220));
        btnPeriod.setForeground(Color.WHITE);
        btnPeriod.setFont(new Font("Arial", Font.BOLD, 12));
        
        btnPeriod.setOpaque(true);            
        btnPeriod.setContentAreaFilled(true);    
        btnPeriod.setBorderPainted(true);      

        btnPeriod.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 100, 220), 1),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        btnPeriod.setFocusPainted(false);

        btnPeriod.addActionListener(e -> {
            try {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("ABRIR_VENTANA");

                // Creamos la plantilla con el tipo que registramos en AgentePercepcion
                jade.domain.FIPAAgentManagement.DFAgentDescription template = new jade.domain.FIPAAgentManagement.DFAgentDescription();
                jade.domain.FIPAAgentManagement.ServiceDescription sd = new jade.domain.FIPAAgentManagement.ServiceDescription();
                sd.setType("percepcion-gastos"); 
                template.addServices(sd);

                // Buscamos en las páginas amarillas utilizando el objeto 'myAgent' de este Behaviour
                jade.domain.FIPAAgentManagement.DFAgentDescription[] result = jade.domain.DFService.search(myAgent, template);

                if (result.length > 0) {
                    // Añadimos como receptor al AID encontrado de forma dinámica
                    msg.addReceiver(result[0].getName());
                    msg.setConversationId("abrir-interfaz");
                    myAgent.send(msg);
                    System.out.println("[DF] Dashboard solicitó abrir ventana al Agente Percepción encontrado.");
                } else {
                    System.out.println("[ERROR DF] Dashboard no encontró ningún agente con el servicio 'percepcion-gastos'.");
                }
            } catch (jade.domain.FIPAException fe) {
                fe.printStackTrace();
            }
        });

        lblAlerta = new JLabel("Esperando datos...");
        lblAlerta.setFont(new Font("Arial", Font.BOLD, 13));
        lblAlerta.setForeground(SUBTEXT);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        der.setBackground(BG_DARK);
        der.add(lblAlerta);
        der.add(btnPeriod);

        p.add(izq, BorderLayout.WEST);
        p.add(der, BorderLayout.EAST);
        return p;
    }


    // ── CUERPO ────────────────────────────────────────────────────────────────
   private JPanel crearCuerpo() {

    JPanel cuerpo = new JPanel();
    cuerpo.setBackground(BG_DARK);
    cuerpo.setLayout(new BorderLayout(0, 16));

    cuerpo.add(crearFilaKPI(), BorderLayout.NORTH);
    cuerpo.add(crearZonaCentral(), BorderLayout.CENTER);

    return cuerpo;
}
private JPanel crearZonaCentral() {

    JPanel panel = new JPanel(new BorderLayout(16, 0));
    panel.setBackground(BG_DARK);

    JPanel izquierda = new JPanel(new BorderLayout(0,16));
izquierda.setBackground(BG_DARK);

JPanel budget = crearPanelBudget();
budget.setPreferredSize(new Dimension(320, 420));

JPanel health = crearPanelHealth();
health.setPreferredSize(new Dimension(320, 180));

izquierda.add(budget, BorderLayout.CENTER);
izquierda.add(health, BorderLayout.SOUTH);

    panel.add(izquierda, BorderLayout.WEST);
    panel.add(crearPanelDerecho(), BorderLayout.CENTER);

    return panel;
}

    // ── FILA KPI ──────────────────────────────────────────────────────────────
    private JPanel crearFilaKPI() {
        JPanel fila = new JPanel(new GridLayout(1, 3, 16, 0));
        fila.setBackground(BG_DARK);

        lblIngresos = new JLabel("-");
        lblGastos   = new JLabel("-");
        lblBalance  = new JLabel("-");

        fila.add(crearKPI("Income", lblIngresos, GREEN,  "↑"));
        fila.add(crearKPI("Expenses", lblGastos, RED,    "↓"));
        fila.add(crearKPI("Net",    lblBalance,  GREEN,  "+"));
        return fila;
    }

    private JPanel crearKPI(String titulo, JLabel lblValor, Color color, String icono) {
        JPanel card = new JPanel(new BorderLayout(14, 0));
        card.setBackground(BG_CARD);
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        // Círculo con icono
        JPanel circulo = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, 36, 36);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 18));
                FontMetrics fm = g2.getFontMetrics();
                int x = (36 - fm.stringWidth(icono)) / 2;
                int y = (36 + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(icono, x, y);
            }
        };
        circulo.setPreferredSize(new Dimension(36, 36));
        circulo.setBackground(BG_CARD);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Arial", Font.PLAIN, 13));
        lblTitulo.setForeground(SUBTEXT);

        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValor.setForeground(color);

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 6));
        textos.setBackground(BG_CARD);
        textos.add(lblTitulo);
        textos.add(lblValor);

        card.add(circulo, BorderLayout.WEST);
        card.add(textos,  BorderLayout.CENTER);
        return card;
    }

    // ── FILA INFERIOR ─────────────────────────────────────────────────────────
    private JPanel crearFilaInferior() {
        JPanel fila = new JPanel(new GridLayout(1, 2, 16, 0));
        fila.setBackground(BG_DARK);
        fila.add(crearPanelBudget());
        fila.add(crearPanelDerecho());
        return fila;
    }
    private JPanel crearPanelHealth() {

    JPanel card = new JPanel();
    card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
    card.setBackground(BG_CARD);
    card.setBorder(new EmptyBorder(20, 20, 20, 20));

    JLabel titulo = bold("Financial Health", 15, Color.WHITE);
    titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
    JLabel sub = plain("Based on income vs expenses", 11, SUBTEXT);
    sub.setAlignmentX(Component.CENTER_ALIGNMENT);

    lblHealthScore = new JLabel("0/100");
    lblHealthScore.setFont(new Font("Segoe UI", Font.BOLD, 38));
    lblHealthScore.setForeground(GREEN);
    lblHealthScore.setAlignmentX(Component.CENTER_ALIGNMENT);

    lblHealthEstado = new JLabel("Unknown");
    lblHealthEstado.setFont(new Font("Segoe UI", Font.BOLD, 14));
    lblHealthEstado.setForeground(SUBTEXT);
    lblHealthEstado.setAlignmentX(Component.CENTER_ALIGNMENT);

    barraHealth = new JProgressBar(0, 100);
    barraHealth.setValue(0);
    barraHealth.setBorderPainted(false);
    barraHealth.setBackground(new Color(55,55,70));
    barraHealth.setForeground(GREEN);
    barraHealth.setAlignmentX(Component.CENTER_ALIGNMENT);
    barraHealth.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));

    card.add(titulo);
    card.add(Box.createVerticalStrut(4));
    card.add(sub);

    card.add(Box.createVerticalGlue());

    card.add(lblHealthScore);
    card.add(Box.createVerticalStrut(8));
    card.add(lblHealthEstado);
    card.add(Box.createVerticalStrut(20));
    card.add(barraHealth);

    card.add(Box.createVerticalGlue());

    return card;
}
    // Budget Status
    private JPanel crearPanelBudget() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel tit = bold("Budget Status", 15, Color.WHITE);
        JLabel sub = plain("Monthly Overview", 12, SUBTEXT);

        lblGastosTotal  = bold("...", 22, Color.WHITE);
        lblPresupuesto  = plain("of 2500.00 € budget", 12, SUBTEXT);

        barraPresupuesto = new JProgressBar(0, 100);
        barraPresupuesto.setValue(0);
        barraPresupuesto.setForeground(ORANGE);
        barraPresupuesto.setBackground(new Color(55, 55, 70));
        barraPresupuesto.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        barraPresupuesto.setBorderPainted(false);

        lblPct = bold("0% used", 12, ORANGE);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(55, 55, 70));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        // Income vs Expenses
        JLabel titIE = bold("Income vs Expenses", 13, Color.WHITE);
        titIE.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblBarraI    = plain("Income",   12, SUBTEXT);
        lblValBarraI = bold("-",         12, GREEN);
        lblBarraG    = plain("Expenses", 12, SUBTEXT);
        lblValBarraG = bold("-",         12, RED);

        barraIngresosPanel = new JPanel();
        barraIngresosPanel.setBackground(GREEN);
        barraIngresosPanel.setMinimumSize(new Dimension(0, 10));
        barraIngresosPanel.setMaximumSize(new Dimension(260, 10));
        barraIngresosPanel.setPreferredSize(new Dimension(300, 10));

        barraGastosPanel = new JPanel();
        barraGastosPanel.setBackground(RED);
        barraGastosPanel.setMinimumSize(new Dimension(0, 10));
        barraGastosPanel.setMaximumSize(new Dimension(260, 10));
        barraGastosPanel.setPreferredSize(new Dimension(200, 10));

        for (JComponent c : new JComponent[]{tit, sub, lblGastosTotal, lblPresupuesto,
                barraPresupuesto, lblPct, sep, titIE}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(c);
            card.add(Box.createVerticalStrut(6));
        }

        card.add(crearFilaBarra(lblBarraI, lblValBarraI, barraIngresosPanel));
        card.add(Box.createVerticalStrut(10));
        card.add(crearFilaBarra(lblBarraG, lblValBarraG, barraGastosPanel));
        card.add(Box.createVerticalGlue());
        card.setPreferredSize(new Dimension(320, 400));
        return card;
    }

  private JPanel crearFilaBarra(JLabel lblNombre, JLabel lblValor, JPanel barra) {

    JPanel contenedor = new JPanel();
    contenedor.setLayout(new BoxLayout(contenedor, BoxLayout.Y_AXIS));
    contenedor.setBackground(BG_CARD);
    contenedor.setAlignmentX(Component.LEFT_ALIGNMENT);

    JPanel fila = new JPanel(new BorderLayout());
    fila.setBackground(BG_CARD);
    fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

    fila.add(lblNombre, BorderLayout.WEST);
    fila.add(lblValor, BorderLayout.EAST);

    // contenedor de la barra alineado a la derecha
    JPanel barraWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    barraWrapper.setBackground(BG_CARD);
    barraWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));

    barra.setPreferredSize(new Dimension(200, 10));
    barra.setMinimumSize(new Dimension(0, 10));

    barraWrapper.add(barra);

    contenedor.add(fila);
    contenedor.add(Box.createVerticalStrut(4));
    contenedor.add(barraWrapper);

    return contenedor;
}
private final Color[] coloresCategorias = {
    new Color(80,190,130),
    new Color(230,130,60),
    new Color(90,150,240),
    new Color(180,120,255),
    new Color(255,90,140),
    new Color(70,200,200),
    new Color(255,200,80),
    new Color(120,220,120)
};
    // Panel derecho: dona + tabla
   private JPanel crearPanelDerecho() {

    JPanel p = new JPanel(new BorderLayout(16, 0));
    p.setBackground(BG_DARK);

    JPanel izquierda = crearPanelDona();
    izquierda.setPreferredSize(new Dimension(320, 400));

    p.add(izquierda, BorderLayout.WEST);
    p.add(crearPanelTabla(), BorderLayout.CENTER);

    return p;
}
    // Dona
    private JPanel crearPanelDona() {

    JPanel card = new JPanel(new BorderLayout(0, 12));
    card.setBackground(BG_CARD);
    card.setBorder(new EmptyBorder(16, 16, 16, 16));

    JLabel tit = bold("Category Spending", 14, Color.WHITE);
    JLabel sub = plain("Top Categories", 11, SUBTEXT);

    JPanel header = new JPanel(new GridLayout(2,1));
    header.setBackground(BG_CARD);
    header.add(tit);
    header.add(sub);

    datasetDona = new DefaultPieDataset();
    datasetDona.setValue("Sin datos", 1);

    JFreeChart chart = ChartFactory.createRingChart(
        "",
        datasetDona,
        false,
        true,
        true
    );
    chart.setBackgroundPaint(BG_CARD);

    piePlot = (PiePlot) chart.getPlot();

    piePlot.setBackgroundPaint(BG_CARD);
    piePlot.setOutlineVisible(false);
    piePlot.setShadowPaint(null);
    piePlot.setLabelGenerator(null);
    piePlot.setSectionPaint("Sin datos", new Color(70,70,85));

    ChartPanel cp = new ChartPanel(chart);
    cp.setBackground(BG_CARD);
    cp.setPreferredSize(new Dimension(280,280)); 

    panelLeyenda = new JPanel();
    panelLeyenda.setLayout(new BoxLayout(panelLeyenda, BoxLayout.Y_AXIS));
    panelLeyenda.setBackground(BG_CARD);

    card.add(header, BorderLayout.NORTH);
    card.add(cp, BorderLayout.CENTER);
    card.add(panelLeyenda, BorderLayout.SOUTH);

    return card;
}
    // Tabla
    private JPanel crearPanelTabla() {
    JPanel card = new JPanel(new BorderLayout(0, 8));
    card.setBackground(BG_CARD);
    card.setBorder(new EmptyBorder(16, 16, 16, 16));

    JLabel tit = bold("Transactions", 14, Color.WHITE);

    // === NUEVO ORDEN DE COLUMNAS ===
    String[] cols = {"Day", "Type", "Category", "Amount", "Concept"};
    modeloTabla = new DefaultTableModel(cols, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    JTable tabla = new JTable(modeloTabla);
    tabla.setBackground(BG_CARD2);
    tabla.setForeground(TEXT);
    tabla.setGridColor(new Color(50, 50, 65));
    tabla.getTableHeader().setBackground(new Color(44, 44, 58));
    tabla.getTableHeader().setForeground(SUBTEXT);
    tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
    tabla.setRowHeight(34);
    tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    tabla.setIntercellSpacing(new Dimension(0, 1));
    tabla.setShowGrid(false);

    tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    tabla.getColumnModel().getColumn(0).setPreferredWidth(50);  
    tabla.getColumnModel().getColumn(1).setPreferredWidth(100);  
    tabla.getColumnModel().getColumn(2).setPreferredWidth(100);  
    tabla.getColumnModel().getColumn(3).setPreferredWidth(100);   
    tabla.getColumnModel().getColumn(4).setPreferredWidth(270); 

    tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            
            // CORREGIDO: Ahora el tipo está en la columna 1 (antes estaba en la 2)
            String tipo = (String) t.getModel().getValueAt(row, 1);
            
            setBackground(row % 2 == 0 ? BG_CARD2 : new Color(44, 44, 60));
            setForeground("INGRESO".equals(tipo) ? GREEN : TEXT);
            setBorder(new EmptyBorder(0, 8, 0, 8));
            return this;
        }
    });

    JScrollPane scroll = new JScrollPane(tabla);
    scroll.setBackground(BG_CARD);
    scroll.getViewport().setBackground(BG_CARD2);
    scroll.setBorder(BorderFactory.createEmptyBorder());

    card.add(tit,    BorderLayout.NORTH);
    card.add(scroll, BorderLayout.CENTER);
    return card;
}
    // ── ACTUALIZAR ────────────────────────────────────────────────────────────
    private void actualizar(String ing, String gas, String bal,
                             boolean critico, String[] movs,
                             String alertaRechazo) {
       
                                
        double dI = Double.parseDouble(ing);
        double dG = Double.parseDouble(gas);
        double dB = Double.parseDouble(bal);

        lblIngresos.setText(fmt(dI) + " €");
        lblGastos.setText(fmt(dG) + " €");
        lblBalance.setText(fmt(dB) + " €");
        lblBalance.setForeground(dB >= 0 ? GREEN : RED);

        lblGastosTotal.setText(fmt(dG) + " €");
        lblPresupuesto.setText("of " + fmt(dI) + " € budget");

        int pct = dI > 0 ? (int) Math.min((dG / dI) * 100, 100) : 0;
        barraPresupuesto.setValue(pct);
        barraPresupuesto.setForeground(pct > 90 ? RED : pct > 70 ? ORANGE : GREEN);
        lblPct.setText(pct + "% used");
        lblPct.setForeground(pct > 90 ? RED : pct > 70 ? ORANGE : GREEN);

        lblValBarraI.setText(fmt(dI) + " €");
        lblValBarraG.setText(fmt(dG) + " €");
        double ahorroRatio = dB / dI;          // cuánto ahorras
        int score = (int)(ahorroRatio * 100);
        score += 50;
        score = Math.max(0, Math.min(100, score));
        String estado;
        Color color;

        if (score >= 80) {
            estado = "Excellent";
            color = GREEN;
        }
        else if (score >= 60) {
            estado = "Good";
            color = BLUE;
        }
        else if (score >= 40) {
            estado = "Average";
            color = ORANGE;
        }
        else {
            estado = "Critical";
            color = RED;
        }

        lblHealthScore.setText(score + "/100");
        lblHealthScore.setForeground(color);

        lblHealthEstado.setText(estado);
        lblHealthEstado.setForeground(color);

        barraHealth.setValue(score);
        barraHealth.setForeground(color);
        // Anchos proporcionales de las barras
        int max = 260;

        double total = dI + dG;

        int anchoIngresos = total > 0 ? (int)((dI / total) * max): 0;

        int anchoGastos = total > 0 ? (int)((dG / total) * max): 0;

        Dimension dimIngresos = new Dimension(anchoIngresos, 10);
        Dimension dimGastos = new Dimension(anchoGastos, 10);

        barraIngresosPanel.setPreferredSize(dimIngresos);
        barraIngresosPanel.setMaximumSize(dimIngresos);
        barraIngresosPanel.setMinimumSize(dimIngresos);

        barraGastosPanel.setPreferredSize(dimGastos);
        barraGastosPanel.setMaximumSize(dimGastos); 
        barraGastosPanel.setMinimumSize(dimGastos);

        barraIngresosPanel.revalidate();
        barraGastosPanel.revalidate();

        lblAlerta.setText(critico ? "⚠  Gastos críticos" : "✓  On track");
        lblAlerta.setForeground(critico ? RED : GREEN);

        // Tabla y dona
        modeloTabla.setRowCount(0);
        datasetDona.clear();

        panelLeyenda.removeAll();


for (String mov : movs) {

    if (mov.isEmpty()) continue;

    String[] c = mov.split(",");

    if (c.length < 4) continue;

    String textoAgente = c[0].trim(); 
            String cantidad    = c[1].trim();
            String tipo        = c[2].trim(); 
            String categoria   = c[3].trim();
            String dia         = "1"; // Por defecto

            // 2. Extraemos el día del bloque de texto: "Texto (Dia X)"
            if (textoAgente.contains("(Dia ") && textoAgente.contains(")")) {
                int indexDia = textoAgente.indexOf("(Dia ");
                dia = textoAgente.substring(indexDia + 5, textoAgente.indexOf(")", indexDia)).trim();
                // Dejamos en el concepto el texto limpio quitando el (Dia X)
                textoAgente = textoAgente.substring(0, indexDia).trim();
            }

            // 3. Creamos la fila con vuestro orden estricto:
            // Día | Tipo | Categoría | Cantidad | Concepto
            String[] filaReordenada = {
                dia, 
                tipo, 
                categoria, 
                cantidad + " €", 
                textoAgente // Muestra "Ingreso", "netflix", etc., según lo mande el agente
            };
            
            modeloTabla.addRow(filaReordenada);

            // Gráfico de la Dona
            if ("GASTO".equals(tipo)) {
                double v = Double.parseDouble(cantidad);
                double prev = datasetDona.getKeys().contains(categoria)
                    ? datasetDona.getValue(categoria).doubleValue()
                    : 0;
                datasetDona.setValue(categoria, prev + v);
            }
        }
/* ---------- colores dinámicos ---------- */

for (Object keyObj : datasetDona.getKeys()) {

    String categoria = keyObj.toString();

    if (categoria.equals("Sin datos")) continue;

    Color colorCategoria = getColorCategoria(categoria);

    piePlot.setSectionPaint(categoria, colorCategoria);
    // leyenda
    JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
    item.setBackground(BG_CARD);

    JPanel cuadrado = new JPanel();
    cuadrado.setBackground(colorCategoria  );
    cuadrado.setPreferredSize(new Dimension(12,12));

    JLabel nombre = plain(categoria, 12, TEXT);

    item.add(cuadrado);
    item.add(nombre);

    panelLeyenda.add(item);

}

panelLeyenda.revalidate();
panelLeyenda.repaint();

        ventana.revalidate();
        ventana.repaint();

        if (alertaRechazo != null && !alertaRechazo.trim().isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(ventana, alertaRechazo, "Movimiento Denegado", javax.swing.JOptionPane.WARNING_MESSAGE);
        }
    }

    private Color getColorCategoria(String cat) {
        if (mapaColores.containsKey(cat)) {
            return mapaColores.get(cat);
        }

        Color c = coloresCategorias[mapaColores.size() % coloresCategorias.length];
        mapaColores.put(cat, c);
        return c;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String fmt(double v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    private JLabel bold(String t, int size, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Arial", Font.BOLD, size));
        l.setForeground(c);
        return l;
    }

    private JLabel plain(String t, int size, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Arial", Font.PLAIN, size));
        l.setForeground(c);
        return l;
    }
}