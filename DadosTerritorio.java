/**
 * ENCAPSULAMENTO - guarda os dados do território monitorado.
 *
 * - TODOS os atributos são private.
 * - Getters/setters validam cada alteração.
 * - Limiares de alerta só mudam com o código de acesso da coordenação.
 * - O código de acesso NÃO tem getter: nunca sai desta classe.
 * - Alteração inválida é rejeitada com mensagem clara e o valor anterior é mantido.
 *
 * O território é descrito por região e equipamentos, nunca por pessoas.
 */
public class DadosTerritorio {

    // Faixas aceitas para cada dado
    private static final int POPULACAO_MINIMA = 1;
    private static final int POPULACAO_MAXIMA = 15_000_000;
    private static final int PONTOS_VULNERAVEIS_MAXIMO = 500;
    private static final double LIMIAR_CHUVA_MIN = 5.0, LIMIAR_CHUVA_MAX = 150.0;     // mm/h
    private static final double LIMIAR_RIO_MIN = 0.5, LIMIAR_RIO_MAX = 8.0;           // m
    private static final double LIMIAR_UMIDADE_MIN = 40.0, LIMIAR_UMIDADE_MAX = 95.0; // %

    private String nomeRegiao;
    private int populacao;
    private int pontosVulneraveis;
    private String codigoAcesso;
    private double limiarChuva;
    private double limiarNivelRio;
    private double limiarUmidade;
    private int tentativasCodigoInvalido;

    public DadosTerritorio(String nomeRegiao, int populacao, int pontosVulneraveis, String codigoAcesso) {
        if (!regiaoValida(nomeRegiao) || !populacaoValida(populacao)
                || !pontosValidos(pontosVulneraveis) || !codigoValido(codigoAcesso)) {
            throw new IllegalArgumentException("Dados iniciais do território inválidos.");
        }
        this.nomeRegiao = nomeRegiao.trim();
        this.populacao = populacao;
        this.pontosVulneraveis = pontosVulneraveis;
        this.codigoAcesso = codigoAcesso;
        // Limiares padrão (referência para um córrego urbano pequeno; ajustáveis pela coordenação)
        this.limiarChuva = 30.0;
        this.limiarNivelRio = 3.0;
        this.limiarUmidade = 85.0;
    }

    // ===================== GETTERS =====================

    public String getNomeRegiao() { return nomeRegiao; }
    public int getPopulacao() { return populacao; }
    public int getPontosVulneraveis() { return pontosVulneraveis; }
    public double getLimiarChuva() { return limiarChuva; }
    public double getLimiarNivelRio() { return limiarNivelRio; }
    public double getLimiarUmidade() { return limiarUmidade; }
    public int getTentativasCodigoInvalido() { return tentativasCodigoInvalido; }
    // (não existe getCodigoAcesso de propósito)

    /** Devolve o limiar correspondente ao tipo do sensor. */
    public double getLimiarPara(String tipoSensor) {
        switch (tipoSensor) {
            case Sensor.TIPO_CHUVA: return limiarChuva;
            case Sensor.TIPO_NIVEL_RIO: return limiarNivelRio;
            case Sensor.TIPO_UMIDADE: return limiarUmidade;
            default: throw new IllegalArgumentException("Tipo de sensor desconhecido: " + tipoSensor);
        }
    }

    // ===================== SETTERS COM VALIDAÇÃO =====================

    public boolean setNomeRegiao(String novoNome) {
        if (!regiaoValida(novoNome)) {
            System.out.println("  [X] Região inválida: use de 3 a 60 caracteres, com pelo menos uma letra. "
                    + "Valor mantido: \"" + nomeRegiao + "\"");
            return false;
        }
        nomeRegiao = novoNome.trim();
        System.out.println("  [OK] Região atualizada para \"" + nomeRegiao + "\".");
        return true;
    }

    public boolean setPopulacao(int novaPopulacao) {
        if (!populacaoValida(novaPopulacao)) {
            System.out.printf("  [X] População inválida (%d): deve estar entre %d e %d. Valor mantido: %d%n",
                    novaPopulacao, POPULACAO_MINIMA, POPULACAO_MAXIMA, populacao);
            return false;
        }
        populacao = novaPopulacao;
        System.out.println("  [OK] População atualizada para " + populacao + " habitantes.");
        return true;
    }

    public boolean setPontosVulneraveis(int novosPontos) {
        if (!pontosValidos(novosPontos)) {
            System.out.printf("  [X] Quantidade de pontos vulneráveis inválida (%d): deve estar entre 0 e %d. Valor mantido: %d%n",
                    novosPontos, PONTOS_VULNERAVEIS_MAXIMO, pontosVulneraveis);
            return false;
        }
        pontosVulneraveis = novosPontos;
        System.out.println("  [OK] Pontos vulneráveis atualizados para " + pontosVulneraveis + ".");
        return true;
    }

    public boolean setLimiarChuva(double novoLimiar, String codigo) {
        if (!autorizar(codigo, "chuva")) return false;
        if (!limiarNaFaixa(novoLimiar, LIMIAR_CHUVA_MIN, LIMIAR_CHUVA_MAX, "chuva", "mm/h", limiarChuva)) return false;
        limiarChuva = novoLimiar;
        System.out.println("  [OK] Limiar de chuva atualizado para " + limiarChuva + " mm/h.");
        return true;
    }

    public boolean setLimiarNivelRio(double novoLimiar, String codigo) {
        if (!autorizar(codigo, "nível do rio")) return false;
        if (!limiarNaFaixa(novoLimiar, LIMIAR_RIO_MIN, LIMIAR_RIO_MAX, "nível do rio", "m", limiarNivelRio)) return false;
        limiarNivelRio = novoLimiar;
        System.out.println("  [OK] Limiar de nível do rio atualizado para " + limiarNivelRio + " m.");
        return true;
    }

    public boolean setLimiarUmidade(double novoLimiar, String codigo) {
        if (!autorizar(codigo, "umidade do solo")) return false;
        if (!limiarNaFaixa(novoLimiar, LIMIAR_UMIDADE_MIN, LIMIAR_UMIDADE_MAX, "umidade do solo", "%", limiarUmidade)) return false;
        limiarUmidade = novoLimiar;
        System.out.println("  [OK] Limiar de umidade do solo atualizado para " + limiarUmidade + " %.");
        return true;
    }

    /** Troca o código de acesso: exige o código atual e um novo código válido. */
    public boolean alterarCodigoAcesso(String codigoAtual, String novoCodigo) {
        if (!autorizar(codigoAtual, "código de acesso")) return false;
        if (!codigoValido(novoCodigo)) {
            System.out.println("  [X] Novo código inválido: use de 4 a 20 caracteres, sem espaços. Código anterior mantido.");
            return false;
        }
        if (novoCodigo.equals(codigoAcesso)) {
            System.out.println("  [X] O novo código é igual ao atual. Código anterior mantido.");
            return false;
        }
        codigoAcesso = novoCodigo;
        System.out.println("  [OK] Código de acesso alterado com sucesso.");
        return true;
    }

    // ===================== VALIDAÇÕES PRIVADAS =====================

    private boolean autorizar(String codigoInformado, String oQueAltera) {
        if (codigoInformado == null || !codigoInformado.equals(codigoAcesso)) {
            tentativasCodigoInvalido++;
            System.out.println("  [X] Código de acesso incorreto: alteração de " + oQueAltera
                    + " NEGADA. Valor anterior mantido. (tentativas inválidas registradas: "
                    + tentativasCodigoInvalido + ")");
            return false;
        }
        return true;
    }

    private boolean limiarNaFaixa(double valor, double min, double max, String nome, String unidade, double atual) {
        if (Double.isNaN(valor) || valor < min || valor > max) {
            System.out.printf("  [X] Limiar de %s fora da faixa (%.2f %s): deve estar entre %.1f e %.1f %s. Valor mantido: %.2f %s%n",
                    nome, valor, unidade, min, max, unidade, atual, unidade);
            return false;
        }
        return true;
    }

    private static boolean regiaoValida(String nome) {
        if (nome == null) return false;
        String limpo = nome.trim();
        return limpo.length() >= 3 && limpo.length() <= 60 && limpo.matches(".*\\p{L}.*");
    }

    private static boolean populacaoValida(int p) {
        return p >= POPULACAO_MINIMA && p <= POPULACAO_MAXIMA;
    }

    private static boolean pontosValidos(int p) {
        return p >= 0 && p <= PONTOS_VULNERAVEIS_MAXIMO;
    }

    private static boolean codigoValido(String c) {
        return c != null && c.length() >= 4 && c.length() <= 20 && !c.contains(" ");
    }

    // ===================== EXIBIÇÃO =====================

    public void exibir() {
        System.out.println("  Região monitorada ......: " + nomeRegiao);
        System.out.println("  População estimada .....: " + populacao + " habitantes");
        System.out.println("  Pontos vulneráveis .....: " + pontosVulneraveis + " (vielas, travessias e áreas baixas mapeadas)");
        System.out.println("  Limiar de chuva ........: " + limiarChuva + " mm/h");
        System.out.println("  Limiar do nível do rio .: " + limiarNivelRio + " m");
        System.out.println("  Limiar de umidade solo .: " + limiarUmidade + " %");
        System.out.println("  Código de acesso .......: **** (protegido)");
    }
}
