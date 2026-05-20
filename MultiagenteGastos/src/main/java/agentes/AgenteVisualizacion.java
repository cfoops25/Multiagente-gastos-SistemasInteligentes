package agentes;

import Comportamientos.VisualizadorBehaviour;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class AgenteVisualizacion extends Agent {

    @Override
    protected void setup() {
        System.out.println("AgenteVisualizacion iniciado: " + getAID().getName());

        // Registrarse en el DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("visualizacion-gastos");
        sd.setName("servicio-visualizacion");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        addBehaviour(new VisualizadorBehaviour(this));
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        System.out.println("AgenteVisualizacion finalizado");
    }
}
