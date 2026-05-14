package agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteInteligente extends Agent {

    // Prueba commit
    @Override
    protected void setup() {

        System.out.println(getLocalName() + " iniciado.");

        addBehaviour(new CyclicBehaviour() {

            @Override
            public void action() {

                ACLMessage mensaje = blockingReceive();

                if (mensaje != null) {

                    System.out.println(
                            "Mensaje recibido: "
                                    + mensaje.getContent()
                    );

                    ACLMessage respuesta =
                            new ACLMessage(ACLMessage.INFORM);

                    respuesta.addReceiver(
                            new AID("visualizacion", AID.ISLOCALNAME)
                    );

                    respuesta.setContent(
                            "Resumen generado correctamente."
                    );

                    send(respuesta);

                    System.out.println(
                            "Mensaje enviado a visualizacion."
                    );
                }
            }
        });
    }
}