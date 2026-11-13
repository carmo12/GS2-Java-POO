/**
 * CLASSE ABSTRATA - molde de tudo o que é instalado fisicamente no território
 * (estações de coleta, sensores...). Não pode ser instanciada diretamente.
 *
 * Atributos comuns: id, nome, região e status.
 * Métodos concretos: ativar() e desativar().
 * Método abstrato: verificarStatus() - cada componente sabe dizer como está.
 */
public abstract class ComponenteTerritorial {

    public static final String STATUS_ATIVO = "ATIVO";
    public static final String STATUS_INATIVO = "INATIVO";
    public static final String STATUS_FALHA = "FALHA";

    private final String id;
    private final String nome;
    private final String regiao;
    private String status;

    public ComponenteTerritorial(String id, String nome, String regiao) {
        this.id = id;
        this.nome = nome;
        this.regiao = regiao;
        this.status = STATUS_ATIVO;
    }

    /** Método concreto: liga o componente (volta a coletar dados). */
    public void ativar() {
        this.status = STATUS_ATIVO;
        System.out.println("  >> " + nome + " (" + id + ") ATIVADO.");
    }

    /** Método concreto: desliga o componente (ex.: manutenção). */
    public void desativar() {
        this.status = STATUS_INATIVO;
        System.out.println("  >> " + nome + " (" + id + ") DESATIVADO para manutenção.");
    }

    /** Ligado = não foi desativado manualmente (pode estar ATIVO ou em FALHA). */
    public boolean estaLigado() {
        return !STATUS_INATIVO.equals(status);
    }

    /** Só as subclasses podem mudar o status (ex.: sensor detectou falha). */
    protected void setStatus(String novoStatus) {
        this.status = novoStatus;
    }

    /** MÉTODO ABSTRATO: cada subclasse DEVE dizer como verifica seu próprio status. */
    public abstract String verificarStatus();

    public String resumo() {
        return String.format("[%s] %-20s | região: %-26s | status: %s", id, nome, regiao, status);
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getRegiao() { return regiao; }
    public String getStatus() { return status; }
}
