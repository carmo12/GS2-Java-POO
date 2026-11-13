import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * CLASSE PRINCIPAL - Territory Predictive Insights (recorte: Clima local).
 *
 * Pergunta que o sistema ajuda a responder:
 * "O córrego vai transbordar nas próximas horas? O que a coordenação pode fazer agora?"
 *
 * Cada leitura simulada representa 1 hora.
 */
public class SistemaMonitoramento {

    private static final String LINHA = "=".repeat(78);
    private static final String LINHA_FINA = "-".repeat(78);

    private final Scanner entrada = new Scanner(System.in);
    private final DadosTerritorio dados;
    private final EstacaoDeColeta estacao;
    private final SensorChuva sensorChuva;
    private final ArrayList<Sensor> sensores = new ArrayList<>();
    private final ArrayList<EstrategiaPrevisao> estrategias = new ArrayList<>();
    private final CentralAlertas central = new CentralAlertas();
    private int horasSimuladas = 0;
    private boolean fimDaEntrada = false;   // usuário fechou a entrada (Ctrl+D / Ctrl+Z)

    public SistemaMonitoramento() {
        // Território fictício: descrito por região e equipamentos, nunca por pessoas.
        dados = new DadosTerritorio("Bacia do Córrego Água Rasa - Zona Leste", 18500, 7, "defesa2026");

        String regiao = dados.getNomeRegiao();
        estacao = new EstacaoDeColeta("EST-01", "Estação Ponte Baixa", regiao);
        sensorChuva = new SensorChuva("PLV-01", regiao);
        SensorNivelRio sensorRio = new SensorNivelRio("RIO-01", regiao, sensorChuva);
        SensorUmidade sensorUmidade = new SensorUmidade("SOL-01", regiao, sensorChuva);

        // A ordem importa: a chuva é lida primeiro porque rio e solo reagem a ela.
        sensores.add(sensorChuva);
        sensores.add(sensorRio);
        sensores.add(sensorUmidade);
        for (Sensor s : sensores) {
            estacao.adicionarSensor(s);
        }

        estrategias.add(new PrevisaoMediaMovel(5));
        estrategias.add(new PrevisaoComparacao(3, 1.0));
    }

    public static void main(String[] args) {
        new SistemaMonitoramento().executar();
    }

    // =====================================================================
    // LOOP PRINCIPAL
    // =====================================================================

    public void executar() {
        System.out.println(LINHA);
        System.out.println("  TERRITORY PREDICTIVE INSIGHTS - CitizenTech | Recorte: CLIMA LOCAL");
        System.out.println("  Chuva intensa e risco de alagamento em pontos vulneráveis");
        System.out.println(LINHA);
        System.out.println("  Dica: o histórico começa vazio. Use a opção 2 para simular leituras");
        System.out.println("  (com menos de " + EstrategiaPrevisao.MINIMO_LEITURAS_CONFIAVEIS
                + " leituras o sistema fica em silêncio de propósito).");

        int opcao;
        do {
            exibirMenu();
            opcao = lerInteiro("  Escolha uma opção: ");
            System.out.println();
            switch (opcao) {
                case 1: verificarSensores(); break;
                case 2: simularProximasLeituras(); break;
                case 3: rodarPrevisoes(); break;
                case 4: gerenciarDadosTerritorio(); break;
                case 5: exibirStatusCompleto(); break;
                case 6: simular24Horas(); break;
                case 7: titulo("HISTÓRICO DE ALERTAS"); central.exibirHistorico(); break;
                case 8: manutencaoComponentes(); break;
                case 0: System.out.println("  Encerrando o monitoramento. Até logo!"); break;
                default: System.out.println("  Opção inválida. Digite um número de 0 a 8.");
            }
        } while (opcao != 0);
        entrada.close();
    }

    private void exibirMenu() {
        System.out.println();
        System.out.println(LINHA);
        System.out.printf("  MENU PRINCIPAL  | %s | leituras no histórico: %d%n",
                momentoAtual(), sensorChuva.getHistorico().size());
        System.out.println(LINHA);
        System.out.println("  1 - Verificar sensores (leitura agora + funcionamento + limite)");
        System.out.println("  2 - Simular próximas leituras");
        System.out.println("  3 - Rodar previsões (2 estratégias) e alertas");
        System.out.println("  4 - Gerenciar dados do território");
        System.out.println("  5 - Exibir status completo");
        System.out.println("  6 - Simulação de 24 horas (cenário de temporal)");
        System.out.println("  7 - Histórico de alertas");
        System.out.println("  8 - Manutenção: ativar/desativar componentes");
        System.out.println("  0 - Sair");
    }

    // =====================================================================
    // 1 - SENSORES
    // =====================================================================

    private void verificarSensores() {
        titulo("VERIFICAÇÃO DOS SENSORES - " + momentoAtual());
        coletarUmaHora();
        for (Sensor sensor : sensores) {
            double limite = dados.getLimiarPara(sensor.getTipo());
            System.out.printf("  %-16s (%s)%n", sensor.getTipo(), idDe(sensor));
            if (!estaColetando(sensor)) {
                System.out.println("     Status ......: " + motivoSemColeta(sensor) + " - sem coleta.");
                System.out.println("     Leitura .....: indisponível (última registrada: "
                        + sensor.getUltimaLeitura() + " " + sensor.getUnidade() + ", pode estar desatualizada)");
                continue;
            }
            System.out.println("     Status ......: " + statusDe(sensor));
            if (!sensor.verificarFuncionamento()) {
                System.out.println("     Leitura .....: indisponível nesta hora (sensor com falha ou desligado)");
                continue;
            }
            System.out.printf("     Leitura .....: %.2f %s | limite: %.2f %s%n",
                    sensor.getUltimaLeitura(), sensor.getUnidade(), limite, sensor.getUnidade());
            if (sensor.ultrapassouLimite(limite)) {
                System.out.println("     >>> VALOR ACIMA DO LIMITE AGORA! Veja a opção 3 para a previsão.");
            } else {
                System.out.println("     Dentro do limite.");
            }
        }
        System.out.println(LINHA_FINA);
        System.out.println("  Estação: " + estacao.verificarStatus());
    }

    // =====================================================================
    // 2 - SIMULAÇÃO DE LEITURAS
    // =====================================================================

    private void simularProximasLeituras() {
        int quantidade = lerInteiroNaFaixa("  Quantas horas de leitura simular? (1 a 48): ", 1, 48);
        titulo("SIMULANDO " + quantidade + " LEITURA(S)");
        System.out.println("  Momento      |  Chuva (mm/h) | Nível rio (m) | Umidade solo (%)");
        for (int i = 0; i < quantidade; i++) {
            String momento = momentoAtual();
            coletarUmaHora();
            System.out.printf("  %-12s | %13s | %13s | %16s%n", momento,
                    formatarLeitura(sensores.get(0)), formatarLeitura(sensores.get(1)), formatarLeitura(sensores.get(2)));
        }

        // Verificação AUTOMÁTICA das previsões após as novas leituras
        System.out.println(LINHA_FINA);
        System.out.println("  Verificação automática das previsões:");
        List<Alerta> alertas = analisarPrevisoes(false);
        if (alertas.isEmpty()) {
            System.out.println("  Nenhum alerta: previsões abaixo de " + (int) (NivelAlerta.ATENCAO.getFracaoDoLimiar() * 100)
                    + "% dos limites (ou dados insuficientes - use a opção 3 para detalhes).");
        } else {
            for (Alerta a : alertas) {
                System.out.println(a.explicar());
                System.out.println();
            }
        }
    }

    // =====================================================================
    // 3 - PREVISÕES + ALERTAS
    // =====================================================================

    private void rodarPrevisoes() {
        titulo("PREVISÃO PARA A PRÓXIMA HORA - " + momentoAtual());
        List<Alerta> alertas = analisarPrevisoes(true);
        System.out.println(LINHA_FINA);
        if (alertas.isEmpty()) {
            System.out.println("  Nenhum alerta emitido nesta análise.");
        } else {
            System.out.println("  ALERTAS EMITIDOS (" + alertas.size() + "):");
            for (Alerta a : alertas) {
                System.out.println(a.explicar());
                System.out.println();
            }
            System.out.println("  Lembrete: o sistema SUGERE; quem decide é a coordenação, que conhece o território.");
        }
    }

    /**
     * Roda as 2 estratégias em cada sensor e compara AUTOMATICAMENTE com os limiares.
     * @param detalhado true imprime cada previsão e o seu significado
     * @return alertas gerados nesta análise
     */
    private List<Alerta> analisarPrevisoes(boolean detalhado) {
        List<Alerta> gerados = new ArrayList<>();
        String momento = momentoAtual();

        for (Sensor sensor : sensores) {
            List<Double> leituras = sensor.getHistorico();
            double limiar = dados.getLimiarPara(sensor.getTipo());
            String unidade = sensor.getUnidade();

            if (detalhado) {
                System.out.printf("%n  >> %s (%s) | %d leituras | limite %.2f %s%n",
                        sensor.getTipo(), idDe(sensor), leituras.size(), limiar, unidade);
            }

            // Sensor ou estação desligados: não há dado novo, então o sistema se cala.
            if (!estaColetando(sensor)) {
                if (detalhado) {
                    System.out.println("     [silêncio] " + motivoSemColeta(sensor)
                            + ": sem dados atuais, nenhuma previsão é feita.");
                }
                continue;
            }
            if (detalhado && !sensor.verificarFuncionamento()) {
                System.out.println("     [cuidado] A última leitura falhou: a previsão usa apenas as leituras válidas anteriores.");
            }

            NivelAlerta[] niveis = new NivelAlerta[estrategias.size()];
            for (int i = 0; i < estrategias.size(); i++) {
                EstrategiaPrevisao estrategia = estrategias.get(i);
                double previsto = estrategia.prever(leituras);

                if (Double.isNaN(previsto)) {
                    niveis[i] = null;
                    if (detalhado) {
                        System.out.printf("     %-24s: [silêncio] %d de %d leituras necessárias - previsão não seria confiável.%n",
                                estrategia.getNome(), leituras.size(), estrategia.getLeiturasNecessarias());
                    }
                    continue;
                }

                NivelAlerta nivel = NivelAlerta.classificar(previsto, limiar);
                niveis[i] = nivel;
                if (detalhado) {
                    System.out.printf("     %-24s: %.2f %s (%.0f%% do limite) -> %s%n",
                            estrategia.getNome(), previsto, unidade, previsto / limiar * 100, significado(nivel, sensor));
                    System.out.println("        cálculo: " + estrategia.explicarCalculo(leituras));
                }

                Alerta alerta = central.avaliar(sensor, idDe(sensor), estrategia, leituras, previsto, limiar, momento);
                if (alerta != null) {
                    gerados.add(alerta);
                }
            }

            if (detalhado) {
                System.out.println("     " + concordancia(niveis));
            }
        }
        return gerados;
    }

    private String significado(NivelAlerta nivel, Sensor sensor) {
        switch (nivel) {
            case NORMAL:  return "NORMAL: abaixo do limite, situação sob controle.";
            case ATENCAO: return "ATENÇÃO: se aproximando do limite de " + sensor.getTipo().toLowerCase() + ".";
            case ALERTA:  return "ALERTA: deve atingir o limite na próxima hora.";
            default:      return "CRÍTICO: deve passar bem do limite - risco alto de alagamento.";
        }
    }

    private String concordancia(NivelAlerta[] niveis) {
        if (niveis.length < 2 || niveis[0] == null || niveis[1] == null) {
            return "Concordância: não avaliada (falta previsão de alguma estratégia).";
        }
        if (niveis[0] == niveis[1]) {
            return "Concordância: as duas estratégias apontam " + niveis[0].getRotulo() + " - sinal mais confiável.";
        }
        return "Concordância: estratégias DIVERGEM (" + niveis[0].getRotulo() + " x " + niveis[1].getRotulo()
                + ") - tratar com cautela e observar a próxima leitura.";
    }

    // =====================================================================
    // 4 - DADOS DO TERRITÓRIO (encapsulamento)
    // =====================================================================

    private void gerenciarDadosTerritorio() {
        int opcao;
        do {
            titulo("DADOS DO TERRITÓRIO");
            dados.exibir();
            System.out.println(LINHA_FINA);
            System.out.println("  1 - Alterar nome da região");
            System.out.println("  2 - Alterar população");
            System.out.println("  3 - Alterar quantidade de pontos vulneráveis");
            System.out.println("  4 - Alterar limiar de chuva        (exige código de acesso)");
            System.out.println("  5 - Alterar limiar do nível do rio (exige código de acesso)");
            System.out.println("  6 - Alterar limiar de umidade      (exige código de acesso)");
            System.out.println("  7 - Trocar código de acesso");
            System.out.println("  0 - Voltar");
            opcao = lerInteiro("  Opção: ");
            switch (opcao) {
                case 1:
                    dados.setNomeRegiao(lerTexto("  Novo nome da região: "));
                    break;
                case 2:
                    dados.setPopulacao(lerInteiro("  Nova população: "));
                    break;
                case 3:
                    dados.setPontosVulneraveis(lerInteiro("  Nova quantidade de pontos vulneráveis: "));
                    break;
                case 4:
                    dados.setLimiarChuva(lerDouble("  Novo limiar de chuva (mm/h): "), lerTexto("  Código de acesso: "));
                    break;
                case 5:
                    dados.setLimiarNivelRio(lerDouble("  Novo limiar do nível do rio (m): "), lerTexto("  Código de acesso: "));
                    break;
                case 6:
                    dados.setLimiarUmidade(lerDouble("  Novo limiar de umidade do solo (%): "), lerTexto("  Código de acesso: "));
                    break;
                case 7:
                    dados.alterarCodigoAcesso(lerTexto("  Código atual: "), lerTexto("  Novo código: "));
                    break;
                case 0:
                    break;
                default:
                    System.out.println("  Opção inválida.");
            }
        } while (opcao != 0);
    }

    // =====================================================================
    // 5 - STATUS COMPLETO
    // =====================================================================

    private void exibirStatusCompleto() {
        titulo("STATUS COMPLETO - " + momentoAtual());
        System.out.println("  TERRITÓRIO");
        dados.exibir();

        System.out.println(LINHA_FINA);
        System.out.println("  COMPONENTES INSTALADOS");
        System.out.println("  " + estacao.resumo());
        System.out.println("     -> " + estacao.verificarStatus());
        for (Sensor s : sensores) {
            if (s instanceof ComponenteTerritorial) {
                ComponenteTerritorial c = (ComponenteTerritorial) s;
                System.out.println("  " + c.resumo());
                System.out.println("     -> " + c.verificarStatus());
            }
        }

        System.out.println(LINHA_FINA);
        System.out.println("  ÚLTIMAS LEITURAS (até 8 mais recentes)");
        for (Sensor s : sensores) {
            List<Double> h = s.getHistorico();
            List<Double> recentes = h.subList(Math.max(0, h.size() - 8), h.size());
            System.out.printf("  %-16s (%s): %s%n", s.getTipo(), s.getUnidade(), recentes.isEmpty() ? "sem leituras" : recentes);
        }

        System.out.println(LINHA_FINA);
        System.out.println("  ESTRATÉGIAS DE PREVISÃO");
        for (EstrategiaPrevisao e : estrategias) {
            System.out.println("  - " + e.descrever() + " | execuções: " + e.getExecucoes());
        }

        System.out.println(LINHA_FINA);
        System.out.printf("  ALERTAS EMITIDOS: %d (ATENÇÃO: %d | ALERTA: %d | CRÍTICO: %d)%n",
                central.getTotalAlertas(), central.contarPorNivel(NivelAlerta.ATENCAO),
                central.contarPorNivel(NivelAlerta.ALERTA), central.contarPorNivel(NivelAlerta.CRITICO));
    }

    // =====================================================================
    // 6 - BÔNUS: SIMULAÇÃO DE 24 HORAS
    // =====================================================================

    private void simular24Horas() {
        titulo("SIMULAÇÃO DE 24 HORAS - CENÁRIO DE TEMPORAL");
        System.out.println("  Cenário: dia seco pela manhã, frente de chuva forte entre 13h e 18h.");
        String resposta = lerTexto("  Começar com histórico zerado? (s/n): ");
        if (resposta.equalsIgnoreCase("s")) {
            for (Sensor s : sensores) {
                s.limparHistorico();
            }
            horasSimuladas = (horasSimuladas / 24 + 1) * 24;   // começa às 00h do próximo dia
            System.out.println("  Histórico zerado. Repare no silêncio das primeiras horas.");
        }

        System.out.println(LINHA_FINA);
        System.out.println("  Hora         | Chuva  | Rio   | Solo  | Maior alerta da hora");
        System.out.println(LINHA_FINA);

        String primeiraHoraAlerta = null;
        NivelAlerta piorNivelDoDia = NivelAlerta.NORMAL;

        for (int i = 0; i < 24; i++) {
            int horaDoDia = horasSimuladas % 24;
            sensorChuva.setModoTempestade(horaDoDia >= 13 && horaDoDia <= 18);
            String momento = momentoAtual();
            coletarUmaHora();

            List<Alerta> alertas = analisarPrevisoes(false);
            Alerta maior = null;
            for (Alerta a : alertas) {
                if (maior == null || a.getNivel().ordinal() > maior.getNivel().ordinal()) {
                    maior = a;
                }
            }

            String coluna;
            if (sensorChuva.getHistorico().size() < EstrategiaPrevisao.MINIMO_LEITURAS_CONFIAVEIS) {
                coluna = "[silêncio] poucos dados para prever";
            } else if (maior == null) {
                coluna = "[ OK  ] normal";
            } else {
                coluna = maior.resumoCurto();
                if (alertas.size() > 1) coluna += " (+" + (alertas.size() - 1) + ")";
                if (maior.getNivel().ordinal() >= NivelAlerta.ALERTA.ordinal() && primeiraHoraAlerta == null) {
                    primeiraHoraAlerta = momento;
                }
                if (maior.getNivel().ordinal() > piorNivelDoDia.ordinal()) {
                    piorNivelDoDia = maior.getNivel();
                }
            }
            System.out.printf("  %-12s | %6s | %5s | %5s | %s%n", momento,
                    formatarLeitura(sensores.get(0)), formatarLeitura(sensores.get(1)),
                    formatarLeitura(sensores.get(2)), coluna);
        }
        sensorChuva.setModoTempestade(false);

        System.out.println(LINHA_FINA);
        System.out.println("  RESUMO DO DIA");
        System.out.println("  Pior nível previsto: " + piorNivelDoDia.getRotulo());
        System.out.println("  Primeiro ALERTA/CRÍTICO: " + (primeiraHoraAlerta == null ? "nenhum" : primeiraHoraAlerta));
        System.out.println("  Ação sugerida para o pior nível: " + piorNivelDoDia.getAcaoSugerida());
        System.out.println("  (Detalhes de cada alerta na opção 7.)");
    }

    // =====================================================================
    // 8 - MANUTENÇÃO (usa ativar/desativar da classe abstrata)
    // =====================================================================

    private void manutencaoComponentes() {
        titulo("MANUTENÇÃO DE COMPONENTES");
        List<ComponenteTerritorial> componentes = new ArrayList<>();
        componentes.add(estacao);
        for (Sensor s : sensores) {
            if (s instanceof ComponenteTerritorial) componentes.add((ComponenteTerritorial) s);
        }
        for (int i = 0; i < componentes.size(); i++) {
            System.out.println("  " + (i + 1) + " - " + componentes.get(i).resumo());
        }
        int escolha = lerInteiroNaFaixa("  Componente (0 para voltar): ", 0, componentes.size());
        if (escolha == 0) return;
        ComponenteTerritorial c = componentes.get(escolha - 1);
        if (c.estaLigado()) {
            c.desativar();
        } else {
            c.ativar();
        }
    }

    // =====================================================================
    // AUXILIARES
    // =====================================================================

    private void coletarUmaHora() {
        estacao.coletarLeituras();
        horasSimuladas++;
    }

    private String momentoAtual() {
        return String.format("Dia %d, %02dh", horasSimuladas / 24 + 1, horasSimuladas % 24);
    }

    /** O sensor só está coletando se ele E a estação que o agrupa estiverem ligados. */
    private boolean estaColetando(Sensor s) {
        if (!estacao.estaLigado()) return false;
        return !(s instanceof ComponenteTerritorial) || ((ComponenteTerritorial) s).estaLigado();
    }

    private String motivoSemColeta(Sensor s) {
        return estacao.estaLigado() ? "Sensor desligado para manutenção" : "Estação de coleta desligada";
    }

    private String formatarLeitura(Sensor s) {
        if (!estaColetando(s)) return "desl.";
        if (!s.verificarFuncionamento()) return "falha";
        return String.format("%.1f", s.getUltimaLeitura());
    }

    private String idDe(Sensor s) {
        return (s instanceof ComponenteTerritorial) ? ((ComponenteTerritorial) s).getId() : "?";
    }

    private String statusDe(Sensor s) {
        return (s instanceof ComponenteTerritorial) ? ((ComponenteTerritorial) s).verificarStatus() : "-";
    }

    private void titulo(String texto) {
        System.out.println(LINHA);
        System.out.println("  " + texto);
        System.out.println(LINHA);
    }

    private String lerTexto(String mensagem) {
        System.out.print(mensagem);
        if (!entrada.hasNextLine()) {
            return "";
        }
        return entrada.nextLine().trim();
    }

    private int lerInteiro(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            if (!entrada.hasNextLine()) {
                System.out.println();
                fimDaEntrada = true;
                return 0;   // fim da entrada: sai do programa com segurança
            }
            String texto = entrada.nextLine().trim();
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("  Entrada inválida: digite um número inteiro.");
            }
        }
    }

    private int lerInteiroNaFaixa(String mensagem, int min, int max) {
        while (true) {
            int valor = lerInteiro(mensagem);
            if (fimDaEntrada) return min;   // evita loop infinito se a entrada acabar
            if (valor >= min && valor <= max) return valor;
            System.out.println("  Valor fora da faixa: digite entre " + min + " e " + max + ".");
        }
    }

    private double lerDouble(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            if (!entrada.hasNextLine()) {
                return Double.NaN;
            }
            String texto = entrada.nextLine().trim().replace(',', '.');
            try {
                return Double.parseDouble(texto);
            } catch (NumberFormatException e) {
                System.out.println("  Entrada inválida: digite um número (ex.: 32,5).");
            }
        }
    }
}
