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
}