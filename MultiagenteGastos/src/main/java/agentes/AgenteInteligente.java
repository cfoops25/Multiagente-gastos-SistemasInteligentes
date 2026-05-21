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
        
        try {
            ontologia = new BeanOntology("OntologiaGastos");
            ontologia.add("ontologia");
            getContentManager().registerOntology(ontologia);
            getContentManager().registerLanguage(new SLCodec());
        } catch (Exception e) { e.printStackTrace(); }

        // ==========================================
        // PASO 4: INICIALIZACIÓN Y ENTRENAMIENTO DE IA (WEKA)
        // ==========================================
        try {
            // 1. Cargar el dataset 'gastos.arff' de la raíz del proyecto
            DataSource source = new DataSource("gastos.arff");
            Instances datosEntrenamiento = source.getDataSet();
            
            // Indicar que el último atributo ('categoria') es la clase objetivo a predecir
            datosEntrenamiento.setClassIndex(datosEntrenamiento.numAttributes() - 1);

            // 2. Configurar el filtro para transformar la descripción de texto en atributos numéricos
            filtroTexto = new StringToWordVector();
            filtroTexto.setInputFormat(datosEntrenamiento);
            Instances datosFiltrados = Filter.useFilter(datosEntrenamiento, filtroTexto);

            // 3. Entrenar el clasificador matemático Naive Bayes con los datos procesados
            clasificador = new NaiveBayes();
            clasificador.buildClassifier(datosFiltrados);

            // Conservamos la estructura del dataset base vacía para construir predicciones futuras
            this.datasetEstructura = datosEntrenamiento;
            System.out.println("[IA] Naive Bayes entrenado correctamente con 'gastos.arff'. Listo para clasificar.");

        } catch (Exception e) {
            System.out.println("[IA] Error crítico al inicializar Weka o cargar el dataset.");
            e.printStackTrace();
        }
        // ==========================================

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
    }

    private class EstadoEvaluacion extends Behaviour {
        private String nombreEstado;
        private int eventoSalida = IR_A_AHORRO;
        private boolean msgProcesado = false;

        public EstadoEvaluacion(String nombre) { this.nombreEstado = nombre; }

        @Override
        public void action() {
            msgProcesado = false; 
            
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                try {
                    Transaccion t = (Transaccion) myAgent.getContentManager().extractContent(msg);
                    
                    // ==========================================
                    // PASO 5: INFERENCIA DE IA EN TIEMPO REAL
                    // ==========================================
                    String conceptoUsuario = t.getCategoria(); // El AgentePercepcion metió el texto libre aquí

                    // 1. Crear una instancia virtual con la estructura del dataset
                    Instance nuevaInstancia = new DenseInstance(2);
                    nuevaInstancia.setDataset(datasetEstructura);
                    nuevaInstancia.setValue(0, conceptoUsuario); // Atributo 0: 'concepto'

                    // 2. Pasar el filtro de conversión vectorial a la instancia
                    filtroTexto.input(nuevaInstancia);
                    Instance instanciaFiltrada = filtroTexto.output();

                    // 3. Ejecutar la clasificación predictiva
                    double prediccionIndice = clasificador.classifyInstance(instanciaFiltrada);
                    String categoriaPredicha = datasetEstructura.classAttribute().value((int) prediccionIndice);

                    System.out.println("\n [IA Predicción] Concepto: '" + conceptoUsuario + "' -> Clasificado como: " + categoriaPredicha);
                    
                    // Sobreescribimos el objeto Transaccion con la categoría real descubierta por Weka
                    t.setCategoria(categoriaPredicha);
                    String motivoRechazo = "";
                    // ==========================================

                    // LÓGICA DE NEGOCIO INTEGRADA CON LA PREDICCIÓN DE IA
                    if ("Ingreso".equalsIgnoreCase(categoriaPredicha)) {
                        // LÓGICA DE INGRESOS
                        System.out.println("[" + nombreEstado + "] Procesando INGRESO de " + t.getMonto() + "€");
                        totalIngresos += t.getMonto();
                        
                        // === CORREGIDO: Guardamos el concepto real introducido por el usuario ===
                        historialMovimientos.append(conceptoUsuario).append(" (Dia ").append(t.getDiaDelMes()).append("),")
                                            .append(t.getMonto()).append(",")
                                            .append("INGRESO").append(",")
                                            .append(categoriaPredicha).append(";");
                    } else {
                        // LÓGICA DE GASTOS (Ocio, Necesidad o Ahorro)
                        System.out.println("[" + nombreEstado + "] Procesando GASTO de " + t.getMonto() + "€ tipo [" + categoriaPredicha + "]");
                        
                        // 1. Alerta predictiva temporal
                        if (t.getDiaDelMes() > 0 && totalIngresos > 0) {
                            float gastoMedioDiario = (totalGastado + t.getMonto()) / t.getDiaDelMes();
                            float gastoProyectado = gastoMedioDiario * 30;
                            if (gastoProyectado > totalIngresos) {
                                int diaQuiebra = (int) (totalIngresos / gastoMedioDiario);
                                System.out.println("ALERTA PREDICTIVA: A este ritmo, te quedarás sin saldo el día " + diaQuiebra);
                                esCritico = true;
                            }
                        }
                        // 2. Evaluar aprobación restrictiva inteligente usando los estados FSM
                        boolean aprobado = false;
                        if (nombreEstado.equals("AHORRO")) {
                            aprobado = true;
                        } else if (nombreEstado.equals("PRECAUCION")) {
                            if (categoriaPredicha.equalsIgnoreCase("Ocio") && t.getMonto() > 100) {
                                motivoRechazo = "Gasto de Ocio excesivo (>100€) para Precaución.";
                                System.out.println(" Rechazado por la FSM: " + motivoRechazo);
                            } else { aprobado = true; }
                        } else if (nombreEstado.equals("CRITICO")) {
                            if (categoriaPredicha.equalsIgnoreCase("Necesidad")) {
                                aprobado = true;
                            } else {
                                motivoRechazo = "Modo Crítico activo. Solo se permiten 'Necesidades'.";
                                System.out.println("Rechazado por la FSM: " + motivoRechazo);
                            }
                        }

                        // 3. Sumar el dinero real si fue validado por la política
                        if (aprobado) {
                            totalGastado += t.getMonto();
                            
                            // === CORREGIDO: Guardamos el concepto real introducido por el usuario ===
                            historialMovimientos.append(conceptoUsuario).append(" (Dia ").append(t.getDiaDelMes()).append("),")
                                                .append(t.getMonto()).append(",")
                                                .append("GASTO").append(",")
                                                .append(categoriaPredicha).append(";");
                        }
                    }

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
                                              "|RECHAZO:" + motivoRechazo;
                    
                    enviarAVisualizador(datosFormateados);
                    msgProcesado = true; 

                } catch (Exception e) { e.printStackTrace(); }
            } else { block(); }
        }

        @Override
        public boolean done() { return msgProcesado; }

        @Override
        public int onEnd() { return eventoSalida; }
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
                send(msg);
            }
        } catch (FIPAException e) { e.printStackTrace(); }
    }
}