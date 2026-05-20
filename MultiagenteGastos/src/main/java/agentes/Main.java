package agentes;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class Main {
//comentario
    public static void main(String[] args) {

        Runtime runtime = Runtime.instance();

        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");

        AgentContainer container = runtime.createMainContainer(profile);

        try {

            AgentController percepcion =
                    container.createNewAgent(
                            "percepcion",
                            "agentes.AgentePercepcion",
                            null
                    );

            AgentController inteligente =
                    container.createNewAgent(
                            "inteligente",
                            "agentes.AgenteInteligente",
                            null
                    );

            AgentController visualizacion =
                    container.createNewAgent(
                            "visualizacion",
                            "agentes.AgenteVisualizacion",
                            null
                    );

            percepcion.start();
            inteligente.start();
            visualizacion.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}