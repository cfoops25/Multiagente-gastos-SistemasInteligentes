package agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.Scanner;

public class AgentePercepcion extends Agent {

    @Override
    protected void setup() {

        System.out.println(getLocalName() + " iniciado.");

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {

                Scanner scanner = new Scanner(System.in);

                System.out.println("Introduce concepto del gasto:");
                String concepto = scanner.nextLine();

                System.out.println("Introduce cantidad:");
                String cantidad = scanner.nextLine();

                System.out.println("Introduce categoría (COMIDA, OCIO, TRANSPORTE...):");
                String categoria = scanner.nextLine();

                System.out.println("¿Es extraordinario? (SI/NO):");
                String extraordinario = scanner.nextLine();

                String contenido = "GASTO;" + concepto + ";" + cantidad + ";" + categoria + ";" + extraordinario;

                ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);

                mensaje.addReceiver(new AID("inteligente", AID.ISLOCALNAME));
                mensaje.setContent(contenido);

                send(mensaje);

                System.out.println("Mensaje enviado: " + contenido);
            }
        });
    }
}