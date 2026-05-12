package agentes;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteVisualizacion extends Agent {

    @Override
    protected void setup() {

        System.out.println(getLocalName() + " iniciado.");

        addBehaviour(new CyclicBehaviour() {

            @Override
            public void action() {

                ACLMessage mensaje = blockingReceive();

                if (mensaje != null) {

                    System.out.println(
                            "VISUALIZACION RECIBE: "
                                    + mensaje.getContent()
                    );
                }
            }
        });
    }
}