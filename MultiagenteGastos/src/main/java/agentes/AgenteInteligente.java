package agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.unsupervised.attribute.StringToWordVector;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;

import java.io.InputStream;


public class AgenteInteligente extends Agent {

    private AID agenteVisualizacion;

    // VARIABLES DE JENA -- LIMITES ONTOLÓGICOS
    private double limiteNecesidades;
    private double limiteOcio;
    private double limiteAhorro;

    // VARIABLES DE WEKA
    /**
     * Contenedor en memoria del dataset de entrenamiento (.arff).
     * Define el espacio de atributos (texto e importe) y actúa como molde estructural para las predicciones.
     */
    private Instances datasetEntrenamiento;
    /**
     * Clasificador híbrido de Weka.
     * Encapsula el filtro StringToWordVector para vectorizar el lenguaje natural y el algoritmo Naive Bayes para la predicción.
     */
    private FilteredClassifier clasificadorWeka;

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

        try {
            String rutaOntologia = "src/main/resources/data/finanzas.ttl";
            Model modeloOntologia =  ModelFactory.createDefaultModel(); // Cramos un RDF vacío
            InputStream in = FileManager.get().open(rutaOntologia);

            if(in == null){
                throw new IllegalArgumentException("Archivo de ontología no encontrado");
            }
            modeloOntologia.read(in, null, "TURTLE"); // Usamos TURTLE para indicar a JENA que usamos el el formato abreviado de web semántica

            String ns = "http://www.fi.upm.es/sistemas-inteligentes/finanzas#";
            Property propLimite = modeloOntologia.getProperty(ns + "limiteMensual"); // Apuntamos a la URI

            limiteNecesidades = modeloOntologia.getResource(ns + "Necesidad").getProperty(propLimite).getLiteral().getDouble();
            limiteOcio = modeloOntologia.getResource(ns + "Ocio").getProperty(propLimite).getLiteral().getDouble();
            limiteAhorro = modeloOntologia.getResource(ns + "Ahorro").getProperty(propLimite).getLiteral().getDouble();
        }catch (Exception e) {
            System.err.println("Error al cargar la ontología con Jena: " + e.getMessage());
            e.printStackTrace();
        }

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
    private void anadirGasto(String categoria, double importe){
        switch (categoria){
            case "Necesidad":
                totalNecesidades += importe;
                break;
            case "Ocio":
                totalOcio += importe;
                break;
            case "Ahorro":
                totalAhorro += importe;
                break;
            default:
                System.out.println("Categoría '" + categoria + "' no mapeable en los acumuladores internos.");
                break;

        }
    }

    private boolean detectarAlerta(){
        return totalNecesidades > limiteNecesidades ||
                totalOcio > limiteOcio ||
                totalAhorro > limiteAhorro;
    }
}

