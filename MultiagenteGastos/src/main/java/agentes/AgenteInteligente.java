package agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;


public class AgenteInteligente extends Agent {

    private AID agenteVisualizacion;

    /**
     * Límetes máximos de gasto mensual por categoría en euros. Si el total de una categoría supera dicho límite se genera una alerta.
     */
    private static final double LIMITE_NECESIDADES = 1000.0;
    private static final double LIMITE_OCIO = 1000.0;
    private static final double LIMITE_AHORRO = 1000.0;

    /**
     * Acumuladores del mes actual según categoría
     */
    private double totalNecesidades = 0.0;
    private double totalOcio = 0.0;
    private double totalAhorro = 0.0;


    /**
     * Contador de gastos del mes
     */
    private int numGastos= 0;

    @Override
    protected void setup() {

        System.out.println(getLocalName() + " iniciado.");
        registrarEnDF();

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                agenteVisualizacion = new AID("visualizacion", AID.ISLOCALNAME);
                System.out.println(getLocalName() + " Dirección de agente de visualizacion guardada");
            }
        });

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action(){
                MessageTemplate filtro = MessageTemplate.MatchPerformative(ACLMessage.INFORM);

                System.out.println(getLocalName() + " Esperando el mensaje del agente de percepcion");
                ACLMessage mensaje = blockingReceive(filtro);

                if (mensaje != null) {
                    System.out.println(getLocalName() + " contenido del mensaje " + mensaje.getContent());

                    if (agenteVisualizacion != null) {
                        ACLMessage respuesta = new ACLMessage(ACLMessage.INFORM);
                        respuesta.addReceiver(agenteVisualizacion);
                        respuesta.setContent("Simulacion informe mensual");
                        send(respuesta);
                        System.out.println(getLocalName() + " menasje enviado a visualizacion");
                    }
                }
            }
        });
    }

    private void registrarEnDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("clasificador-gastos");
        sd.setName("Servicio-Analisis-Financiero");
        dfd.addServices(sd);

        try
        {
            DFService.register(this, dfd);
            System.out.println(getLocalName() + "Registrado del DF");
        }
        catch (FIPAException e )
        {
            e.printStackTrace();
        }
    }

    protected void takeDown() {
        try
        {
            DFService.deregister(this);
            System.out.println(getLocalName() + " Desregistrado del DF");
        }
        catch (FIPAException e )
        {
            e.printStackTrace();
        }
        System.out.println(getLocalName() + " finalizando");
    }

    /**
     * Añade el importe del gasto en la categoría correspondiente
     *
     * @param categoria
     * @param importe
     */
    private void añadirGasto(String categoria, double importe){
        switch (categoria){
            case "Necesidad": totalNecesidades += importe;
            case "Ocio": totalOcio += importe;
            case "Ahorro": totalAhorro += importe;

        }
    }

    private boolean detectarAlerta(){
        return totalNecesidades > LIMITE_NECESIDADES ||
                totalOcio > LIMITE_OCIO ||
                totalAhorro > LIMITE_AHORRO;
    }
}

