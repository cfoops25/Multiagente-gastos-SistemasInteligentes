package agentes;

import javax.swing.SwingUtilities;

import Comportamientos.PercepcionGUI;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntology;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ontologia.Transaccion;

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
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sdPropio = new ServiceDescription();
        sdPropio.setType("percepcion-gastos");
        sdPropio.setName("servicio-captura-datos");
        dfd.addServices(sdPropio);
        try {
            DFService.register(this, dfd);
            System.out.println("[DF] Agente Percepcion registrado correctamente.");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
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
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("clasificacion-gastos");
            template.addServices(sd);

            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length == 0) {
            System.out.println("[Percepcion] No se encontró el agente inteligente en el DF.");
                return;
            }
            msg.addReceiver(result[0].getName());
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
        try {
            DFService.deregister(this);
            System.out.println("[DF] Agente Percepcion desregistrado correctamente.");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        // ==========================================================

        if (ventanaGUI != null) {
            ventanaGUI.dispose();
        }
        System.out.println("Agente Percepcion finalizado.");
    }
    
}