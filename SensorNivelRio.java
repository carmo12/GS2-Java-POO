import java.util.ArrayList;
import java.util.List;

/**
 * Régua linimétrica / sensor ultrassônico: mede o nível do córrego em metros.
 *
 * Simulação coerente: o nível sobe quando chove (usa a leitura do pluviômetro
 * da mesma região - composição) e baixa devagar quando a chuva para.
 */
public class SensorNivelRio extends ComponenteTerritorial implements Sensor {

    private static final int MAX_HISTORICO = 48;
    private static final double CHANCE_FALHA = 0.03;
    private static final double NIVEL_NORMAL = 1.5;
    private static final double NIVEL_MINIMO = 0.3;
    private static final double NIVEL_MAXIMO = 8.0;

    private final ArrayList<Double> historico = new ArrayList<>();
    private final SensorChuva chuvaDaRegiao;
    private double ultimaLeitura = NIVEL_NORMAL;

    public SensorNivelRio(String id, String regiao, SensorChuva chuvaDaRegiao) {
        super(id, "Régua do Córrego", regiao);
        this.chuvaDaRegiao = chuvaDaRegiao;
    }

    @Override
    public double lerValor() {
        if (!estaLigado()) {
            return Double.NaN;
        }
        if (Math.random() < CHANCE_FALHA) {
            setStatus(STATUS_FALHA);
            return Double.NaN;
        }
        setStatus(STATUS_ATIVO);

        double chuva = chuvaDaRegiao.getUltimaLeitura();
        double valor = ultimaLeitura
                + chuva * 0.015                            // chuva enche o córrego
                - (ultimaLeitura - NIVEL_NORMAL) * 0.12    // escoamento natural
                + (Math.random() - 0.5) * 0.1;             // ruído da medição
        valor = Math.max(NIVEL_MINIMO, Math.min(NIVEL_MAXIMO, valor));
        registrarLeitura(Math.round(valor * 100) / 100.0);
        return ultimaLeitura;
    }

    @Override
    public boolean verificarFuncionamento() {
        return STATUS_ATIVO.equals(getStatus()) && ultimaLeitura >= NIVEL_MINIMO && ultimaLeitura <= NIVEL_MAXIMO;
    }

    @Override
    public String verificarStatus() {
        if (!estaLigado()) return "Desligado para manutenção - sem coleta.";
        if (!verificarFuncionamento()) return "FALHA na última leitura - verificar sensor/lixo acumulado na régua.";
        return "Funcionando. Última leitura: " + ultimaLeitura + " " + getUnidade();
    }

    @Override
    public void registrarLeitura(double valor) {
        historico.add(valor);
        if (historico.size() > MAX_HISTORICO) {
            historico.remove(0);
        }
        ultimaLeitura = valor;
    }

    @Override
    public boolean ultrapassouLimite(double limite) {
        return !historico.isEmpty() && ultimaLeitura >= limite;
    }

    @Override
    public void limparHistorico() {
        historico.clear();
        ultimaLeitura = NIVEL_NORMAL;
    }

    @Override public String getTipo() { return TIPO_NIVEL_RIO; }
    @Override public String getUnidade() { return "m"; }
    @Override public List<Double> getHistorico() { return new ArrayList<>(historico); }
    @Override public double getUltimaLeitura() { return ultimaLeitura; }
}
