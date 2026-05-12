package agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class AgentePercepcion extends Agent {

    @Override
    protected void setup() {

        System.out.println(getLocalName() + " iniciado.");

        addBehaviour(new OneShotBehaviour() {

            @Override
            public void action() {

                ACLMessage mensaje =
                        new ACLMessage(ACLMessage.INFORM);

                mensaje.addReceiver(new AID("inteligente", AID.ISLOCALNAME));

                mensaje.setContent("GASTO:100");

                send(mensaje);

                System.out.println("Mensaje enviado al agente inteligente.");
            }
        });
    }
}