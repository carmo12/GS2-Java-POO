import java.util.ArrayList;
import java.util.List;

/**
 * Classe CONCRETA que herda de ComponenteTerritorial (extends).
 * Representa a estação física que agrupa os sensores de uma região
 * e coleta as leituras de todos eles de uma vez.
 */
public class EstacaoDeColeta extends ComponenteTerritorial {

    private final ArrayList<Sensor> sensores = new ArrayList<>();

    public EstacaoDeColeta(String id, String nome, String regiao) {
        super(id, nome, regiao);
    }

    public void adicionarSensor(Sensor sensor) {
        sensores.add(sensor);
    }

    /** Lê todos os sensores. Se a estação estiver desligada, nada é coletado. */
    public int coletarLeituras() {
        if (!estaLigado()) {
            System.out.println("  Estação " + getNome() + " desligada: nenhuma leitura coletada.");
            return 0;
        }
        int leiturasValidas = 0;
        for (Sensor sensor : sensores) {
            if (!Double.isNaN(sensor.lerValor())) {
                leiturasValidas++;
            }
        }
        return leiturasValidas;
    }

    /** Implementação do método abstrato: a estação está boa se seus sensores estão bons. */
    @Override
    public String verificarStatus() {
        if (!estaLigado()) {
            return "Estação desligada - nenhum sensor está coletando.";
        }
        int funcionando = 0;
        for (Sensor sensor : sensores) {
            if (sensor.verificarFuncionamento()) {
                funcionando++;
            }
        }
        if (funcionando == sensores.size()) {
            setStatus(STATUS_ATIVO);
            return "Operando normalmente (" + funcionando + "/" + sensores.size() + " sensores OK).";
        }
        setStatus(STATUS_FALHA);
        return "Operando COM FALHAS (" + funcionando + "/" + sensores.size()
                + " sensores OK) - previsões dos sensores com falha ficam menos confiáveis.";
    }

    public List<Sensor> getSensores() {
        return new ArrayList<>(sensores);
    }
}
