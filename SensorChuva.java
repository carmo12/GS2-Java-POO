import java.util.ArrayList;
import java.util.List;

/**
 * Pluviômetro: mede a intensidade da chuva em mm/h.
 * É um ComponenteTerritorial (extends) e cumpre o contrato de Sensor (implements).
 *
 * Simulação: a chuva "lembra" da leitura anterior (não muda do nada) e de vez em
 * quando acontece uma pancada forte. No modo tempestade as pancadas são frequentes.
 */
public class SensorChuva extends ComponenteTerritorial implements Sensor {

    private static final int MAX_HISTORICO = 48;
    private static final double CHANCE_FALHA = 0.03;
    private static final double VALOR_MAXIMO = 150.0;

    private final ArrayList<Double> historico = new ArrayList<>();
    private double ultimaLeitura = 0.0;
    private boolean modoTempestade = false;

    public SensorChuva(String id, String regiao) {
        super(id, "Pluviômetro", regiao);
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

        double chancePancada = modoTempestade ? 0.55 : 0.05;
        double valor = ultimaLeitura * 0.5 + Math.random() * 3;
        if (Math.random() < chancePancada) {
            valor += 15 + Math.random() * 25;
        }
        valor = Math.max(0, Math.min(VALOR_MAXIMO, valor));
        registrarLeitura(arredondar(valor));
        return ultimaLeitura;
    }

    @Override
    public boolean verificarFuncionamento() {
        return STATUS_ATIVO.equals(getStatus()) && ultimaLeitura >= 0 && ultimaLeitura <= VALOR_MAXIMO;
    }

    @Override
    public String verificarStatus() {
        if (!estaLigado()) return "Desligado para manutenção - sem coleta.";
        if (!verificarFuncionamento()) return "FALHA na última leitura - verificar entupimento/conexão do pluviômetro.";
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
        ultimaLeitura = 0.0;
    }

    @Override public String getTipo() { return TIPO_CHUVA; }
    @Override public String getUnidade() { return "mm/h"; }
    @Override public List<Double> getHistorico() { return new ArrayList<>(historico); }
    @Override public double getUltimaLeitura() { return ultimaLeitura; }

    /** Específico do pluviômetro: usado pela simulação de 24h para criar um cenário de temporal. */
    public void setModoTempestade(boolean modoTempestade) { this.modoTempestade = modoTempestade; }

    private double arredondar(double v) { return Math.round(v * 10) / 10.0; }
}
