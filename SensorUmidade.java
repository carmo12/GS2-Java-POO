import java.util.ArrayList;
import java.util.List;

/**
 * Sensor de umidade do solo (%) instalado na encosta/área de risco.
 * Solo encharcado não absorve mais água: a chuva seguinte vira enxurrada.
 *
 * Simulação coerente: a umidade sobe com a chuva da região e seca devagar.
 */
public class SensorUmidade extends ComponenteTerritorial implements Sensor {

    private static final int MAX_HISTORICO = 48;
    private static final double CHANCE_FALHA = 0.03;
    private static final double UMIDADE_BASE = 55.0;
    private static final double UMIDADE_MINIMA = 20.0;
    private static final double UMIDADE_MAXIMA = 100.0;

    private final ArrayList<Double> historico = new ArrayList<>();
    private final SensorChuva chuvaDaRegiao;
    private double ultimaLeitura = UMIDADE_BASE;

    public SensorUmidade(String id, String regiao, SensorChuva chuvaDaRegiao) {
        super(id, "Sensor de Solo", regiao);
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
                + chuva * 0.15                               // chuva encharca o solo
                - (ultimaLeitura - UMIDADE_BASE) * 0.12      // solo seca aos poucos
                + (Math.random() - 0.5) * 2;                 // ruído
        valor = Math.max(UMIDADE_MINIMA, Math.min(UMIDADE_MAXIMA, valor));
        registrarLeitura(Math.round(valor * 10) / 10.0);
        return ultimaLeitura;
    }

    @Override
    public boolean verificarFuncionamento() {
        return STATUS_ATIVO.equals(getStatus()) && ultimaLeitura >= UMIDADE_MINIMA && ultimaLeitura <= UMIDADE_MAXIMA;
    }

    @Override
    public String verificarStatus() {
        if (!estaLigado()) return "Desligado para manutenção - sem coleta.";
        if (!verificarFuncionamento()) return "FALHA na última leitura - verificar sonda enterrada/bateria.";
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
        ultimaLeitura = UMIDADE_BASE;
    }

    @Override public String getTipo() { return TIPO_UMIDADE; }
    @Override public String getUnidade() { return "%"; }
    @Override public List<Double> getHistorico() { return new ArrayList<>(historico); }
    @Override public double getUltimaLeitura() { return ultimaLeitura; }
}
