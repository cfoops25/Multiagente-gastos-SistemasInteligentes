package agentes;

import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntology;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ontologia.Transaccion;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class AgenteInteligente extends Agent {
    
    private BeanOntology ontologia;
    
    // VARIABLES FINANCIERAS REALES
    private float totalIngresos = 0.0f; 
    private float totalGastado = 0.0f;
    
    private StringBuilder historialMovimientos = new StringBuilder();
    private boolean esCritico = false;
    private String alertaPredictiva = "";
    
    // RUTAS ABSOLUTAS DE LA FSM
    private final int IR_A_AHORRO = 0;
    private final int IR_A_PRECAUCION = 1;
    private final int IR_A_CRITICO = 2;

    // VARIABLES GLOBALES DE WEKA (PASO 4)
    private Instances datasetEstructura;
    private StringToWordVector filtroTexto;
    private Classifier clasificador;

    @Override
    protected void setup() {
        System.out.println("Agente Inteligencia " + getLocalName() + " iniciado.");
        //Registro ontología y el codec de comunicación JADE
        try {
            ontologia = new BeanOntology("OntologiaGastos");
            ontologia.add("ontologia");
            getContentManager().registerOntology(ontologia);
            getContentManager().registerLanguage(new SLCodec());
        } catch (Exception e) { e.printStackTrace(); }

        // INICIALIZACIÓN Y ENTRENAMIENTO DE Naive Bayes con Weka 
       
        try {
            String rutaProyecto = System.getProperty("user.dir");
            String rutaCompleta = rutaProyecto + java.io.File.separator + "gastos.arff";
            DataSource source = new DataSource(rutaCompleta);
            Instances datosEntrenamiento = source.getDataSet();

            datosEntrenamiento.setClassIndex(datosEntrenamiento.numAttributes() - 1);

 
            filtroTexto = new StringToWordVector();
            filtroTexto.setInputFormat(datosEntrenamiento);
            Instances datosFiltrados = Filter.useFilter(datosEntrenamiento, filtroTexto);


            clasificador = new NaiveBayes();
            clasificador.buildClassifier(datosFiltrados);

            this.datasetEstructura = datosEntrenamiento;
            System.out.println("[IA] Naive Bayes entrenado correctamente con 'gastos.arff'. Listo para clasificar.");

        } catch (Exception e) {
            System.out.println("[IA] Error crítico al inicializar Weka o cargar el dataset.");
            e.printStackTrace();
        }

        //Configuración de la Máquina de Estados Finitos (FSM)
        FSMBehaviour fsm = new FSMBehaviour(this);

        fsm.registerFirstState(new EstadoEvaluacion("AHORRO"), "AHORRO");
        fsm.registerState(new EstadoEvaluacion("PRECAUCION"), "PRECAUCION");
        fsm.registerState(new EstadoEvaluacion("CRITICO"), "CRITICO");

        // Todas las carreteras posibles registradas
        fsm.registerTransition("AHORRO", "AHORRO", IR_A_AHORRO);
        fsm.registerTransition("AHORRO", "PRECAUCION", IR_A_PRECAUCION);
        fsm.registerTransition("AHORRO", "CRITICO", IR_A_CRITICO);
        
        fsm.registerTransition("PRECAUCION", "AHORRO", IR_A_AHORRO);
        fsm.registerTransition("PRECAUCION", "PRECAUCION", IR_A_PRECAUCION);
        fsm.registerTransition("PRECAUCION", "CRITICO", IR_A_CRITICO);
        
        fsm.registerTransition("CRITICO", "AHORRO", IR_A_AHORRO);
        fsm.registerTransition("CRITICO", "PRECAUCION", IR_A_PRECAUCION);
        fsm.registerTransition("CRITICO", "CRITICO", IR_A_CRITICO);

        addBehaviour(fsm);

        // Registrarse en el DF como servicio de clasificación
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("clasificacion-gastos");
        sd.setName("servicio-clasificacion");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private class EstadoEvaluacion extends Behaviour {
        private String nombreEstado;
        private int eventoSalida = IR_A_AHORRO;
        private boolean msgProcesado = false;

        public EstadoEvaluacion(String nombre) { this.nombreEstado = nombre; }

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                try {
                    Transaccion t = (Transaccion) myAgent.getContentManager().extractContent(msg);

                    String conceptoUsuario = t.getCategoria().toLowerCase().trim();
                    // Inferencia en tiempo real pasando el filtro vectorial
                    Instance nuevaInstancia = new DenseInstance(datasetEstructura.numAttributes());
                    nuevaInstancia.setDataset(datasetEstructura);
                    nuevaInstancia.setValue(0, conceptoUsuario); 

                    filtroTexto.input(nuevaInstancia);
                    Instance instanciaFiltrada = filtroTexto.output();

                    double prediccionIndice = clasificador.classifyInstance(instanciaFiltrada);
                    String categoriaPredicha = datasetEstructura.classAttribute().value((int) prediccionIndice);

                    System.out.println("\n [IA Predicción] Concepto: '" + conceptoUsuario + "' -> Clasificado como: " + categoriaPredicha);
                    
                    t.setCategoria(categoriaPredicha);
                    String motivoRechazo = "";
                    boolean transaccionAprobada = false;

                    // LÓGICA DE NEGOCIO INTEGRADA CON LA PREDICCIÓN DE IA
                    if ("Ingreso".equalsIgnoreCase(categoriaPredicha)) {
                        // LÓGICA DE INGRESOS
                        System.out.println("[" + nombreEstado + "] Procesando INGRESO de " + t.getMonto() + "€");
                        totalIngresos += t.getMonto();
                        
                        historialMovimientos.append(conceptoUsuario).append(" (Dia ").append(t.getDiaDelMes()).append("),")
                                            .append(t.getMonto()).append(",")
                                            .append("INGRESO").append(",")
                                            .append(categoriaPredicha).append(";");
                        transaccionAprobada = true;

                    } 
                   
                    else {
                        // LÓGICA DE GASTOS (Ocio, Necesidad, Ahorro u Otros)
                        System.out.println("[" + nombreEstado + "] Procesando GASTO de " + t.getMonto() + "€ tipo [" + categoriaPredicha + "]");
                        
                        // Generación de alerta predictiva basada en el ritmo de gasto actual y los ingresos totales
                        if (t.getDiaDelMes() > 0 && totalIngresos > 0) {
                            float gastoMedioDiario = (totalGastado + t.getMonto()) / t.getDiaDelMes();
                            float gastoProyectado = gastoMedioDiario * 30;
                            if (gastoProyectado > totalIngresos) {
                                int diaQuiebra = (int) (totalIngresos / gastoMedioDiario);
                                alertaPredictiva = "Si sigues gastando así diariamente, te quedarás sin saldo el día " + diaQuiebra;
                                System.out.println("ALERTA PREDICTIVA: A este ritmo, te quedarás sin saldo el día " + diaQuiebra);
                                esCritico = true;
                            } else {
                                alertaPredictiva = "";
                            }
                        }

                        // Evaluar aprobación restrictiva inteligente usando los estados FSM
                        boolean aprobado = false;
                        if (nombreEstado.equals("AHORRO")) {
                            aprobado = true;
                        } else if (nombreEstado.equals("PRECAUCION")) {
                            if (categoriaPredicha.equalsIgnoreCase("Ocio") && t.getMonto() > 100 ||
                                categoriaPredicha.equalsIgnoreCase("Otros") && t.getMonto() > 100) {
                                motivoRechazo = "Gasto de Ocio excesivo (>100€) para Precaución.";
                                System.out.println(" Rechazado: " + motivoRechazo);
                            } else { aprobado = true; }
                        } else if (nombreEstado.equals("CRITICO")) {
                            if (categoriaPredicha.equalsIgnoreCase("Necesidad")) {
                                aprobado = true;
                            } else {
                                motivoRechazo = "Modo Crítico activo. Solo se permiten 'Necesidades'.";
                                System.out.println("Rechazado: " + motivoRechazo);
                            }
                        } 

                        if (aprobado) {
                            totalGastado += t.getMonto();
                            historialMovimientos.append(conceptoUsuario).append(" (Dia ").append(t.getDiaDelMes()).append("),")
                                                .append(t.getMonto()).append(",")
                                                .append("GASTO").append(",")
                                                .append(categoriaPredicha).append(";");
                            transaccionAprobada = true;
                        }
                    }
                    // Respuesta directa al emisor (Agente Percepción) con el resultado de la evaluación
                    ACLMessage respuesta = msg.createReply();
                    if (transaccionAprobada) {
                        respuesta.setPerformative(ACLMessage.INFORM);
                        respuesta.setContent("OK");
                    } else {
                        respuesta.setPerformative(ACLMessage.REFUSE);
                        respuesta.setContent(motivoRechazo);
                    }
                    myAgent.send(respuesta);

                    // Calcular dinámicamente la transición al próximo estado financiero
                    float porcentajeGastado = (totalIngresos > 0) ? (totalGastado / totalIngresos) : 1.0f;
                    
                    if (porcentajeGastado >= 0.8f) {
                        eventoSalida = IR_A_CRITICO;
                        esCritico = true;
                    } else if (porcentajeGastado >= 0.3f) {
                        eventoSalida = IR_A_PRECAUCION;
                        esCritico = false;
                    } else {
                        eventoSalida = IR_A_AHORRO;
                        esCritico = false;
                    }

                    // Empaquetar y enviar al Dashboard (Agente Visualizador)
                    float balance = totalIngresos - totalGastado;
                    String datosFormateados = "INGRESOS:" + totalIngresos + 
                                              "|GASTOS:" + totalGastado + 
                                              "|BALANCE:" + balance + 
                                              "|CRITICO:" + esCritico + 
                                              "|MOVIMIENTOS:" + historialMovimientos.toString() +
                                              "|RECHAZO:" + motivoRechazo + 
                                              "|PREDICCION:" + alertaPredictiva;;
                    
                    enviarAVisualizador(datosFormateados);
                    msgProcesado = true; 

                } catch (Exception e) { 
                    e.printStackTrace(); 
                }
            } else { 
                block();
             }
        }

        @Override
        public boolean done() { 
            return msgProcesado; 
        }

        @Override
        public int onEnd() { 
            msgProcesado = false; 
            return eventoSalida;
         }
    }

    public void enviarAVisualizador(String texto) {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("visualizacion-gastos"); 
            template.addServices(sd);
            
            DFAgentDescription[] result = DFService.search(this, template);
            
            if (result.length > 0) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(result[0].getName());
                msg.setContent(texto);
                msg.setConversationId("datos-financieros");
                send(msg);
                System.out.println("[DF] Datos financieros enviados dinámicamente al Agente Visualización.");
            } else {
                // Control preventivo por consola
                System.out.println("[ERROR DF] Agente Inteligente no encontró ningún servicio de 'visualizacion-gastos'.");
            }
        } catch (FIPAException e) { 
            e.printStackTrace(); 
        }
    }
    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}