package agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntology;
import javax.swing.SwingUtilities;
import ontologia.Transaccion;

// Importamos tu ventana gráfica
import Comportamientos.PercepcionGUI; 

public class AgentePercepcion extends Agent {

    private BeanOntology ontologia;
    private PercepcionGUI ventanaGUI;

    @Override
    protected void setup() {
        System.out.println("Agente Percepcion " + getLocalName() + " iniciado. Esperando botón del Dashboard...");
        
        try {
            ontologia = new BeanOntology("OntologiaGastos");
            ontologia.add("ontologia"); 
            getContentManager().registerOntology(ontologia);
            getContentManager().registerLanguage(new SLCodec());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 1. Creamos la ventana gráfica pero la dejamos OCULTA (invisible)
        SwingUtilities.invokeLater(() -> {
            ventanaGUI = new PercepcionGUI(this);
            ventanaGUI.setVisible(false);
        });

        // 2. Comportamiento cíclico para escuchar la orden del Dashboard
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                // Filtramos para escuchar solo peticiones (REQUEST)
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);
                
                if (msg != null && "ABRIR_VENTANA".equals(msg.getContent())) {
                    // Si el Dashboard pide abrir la ventana, la mostramos en el centro
                    SwingUtilities.invokeLater(() -> {
                        ventanaGUI.setLocationRelativeTo(null); 
                        ventanaGUI.setVisible(true); 
                    });
                } else {
                    block(); // Si no hay mensajes, el agente se duerme para no gastar CPU
                }
            }
        });
    }

    // Método público que llamará la ventana PercepcionGUI al darle a "Enviar"
    public void enviarDatos(String tipo, float monto, String concepto, int dia) {
        try {
            Transaccion t = new Transaccion();
            
            // Mantenemos tu lógica: Forzamos PENDIENTE para que la IA lo descubra
            t.setTipo("PENDIENTE"); 
            t.setMonto(monto);
            t.setCategoria(concepto); // Guardamos el texto libre temporalmente
            t.setDiaDelMes(dia);

            // Preparamos el mensaje
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID("inteligente", AID.ISLOCALNAME)); 
            msg.setOntology(ontologia.getName());
            msg.setLanguage(new SLCodec().getName());

            // Rellenamos el mensaje y lo enviamos
            getContentManager().fillContent(msg, t);
            send(msg);
            
            // Ocultamos la ventana emergente automáticamente tras enviar los datos
            SwingUtilities.invokeLater(() -> ventanaGUI.setVisible(false));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Al cerrar el agente, nos aseguramos de destruir la ventana para liberar memoria
    @Override
    protected void takeDown() {
        if (ventanaGUI != null) {
            ventanaGUI.dispose();
        }
        System.out.println("Agente Percepcion finalizado.");
    }
}