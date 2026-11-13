import java.util.List;

/**
 * INTERFACE - contrato que todo sensor do sistema precisa cumprir.
 * Não implementa nada: só diz QUAIS métodos as classes devem ter.
 */
public interface Sensor {

    // Tipos de sensor do recorte "Clima local" (constantes da interface)
    String TIPO_CHUVA = "Chuva";
    String TIPO_NIVEL_RIO = "Nível do Rio";
    String TIPO_UMIDADE = "Umidade do Solo";

    /** Faz uma nova leitura. Retorna Double.NaN se o sensor estiver desligado ou falhar. */
    double lerValor();

    /** true se o sensor está ligado, sem falha e com leitura dentro da faixa física possível. */
    boolean verificarFuncionamento();

    /** Tipo do sensor (ex.: "Chuva"). */
    String getTipo();

    /** Unidade da medida (ex.: "mm/h"). */
    String getUnidade();

    /** Cópia das últimas leituras válidas (mais antiga primeiro). */
    List<Double> getHistorico();

    /** Última leitura válida registrada. */
    double getUltimaLeitura();

    /** Detecção: a última leitura passou do limite informado? */
    boolean ultrapassouLimite(double limite);

    /** Registra uma leitura vinda de fora (ex.: recarregar histórico). */
    void registrarLeitura(double valor);

    /** Apaga o histórico (usado para recomeçar uma simulação). */
    void limparHistorico();
}
