package br.com.conde.indice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AS CINCO TABELAS que o medidor imprime e que os testes conferem numero por numero.
 *
 * <p>Tudo aqui e contado, nunca cronometrado: paginas lidas, paginas distintas e entradas
 * de indice. Os numeros sao os mesmos em qualquer maquina, e por isso podem estar escritos
 * no README e afirmados num teste.
 */
public final class Medidas {

    public static final int LINHAS = 100_000;
    public static final int LINHAS_POR_PAGINA = 100;
    public static final long SEMENTE = 20_261_012L;

    /** As seletividades do eixo principal, em fracao das linhas da tabela. */
    public static final double[] SELETIVIDADES =
            {0.0001, 0.001, 0.005, 0.01, 0.02, 0.05, 0.10, 0.50, 1.00};

    private Medidas() {}

    /* Montar um indice ordena todas as chaves: com varias tabelas de medida isso aparece. */
    private static final Map<Tabela, Indice> INDICES = new java.util.IdentityHashMap<>();

    private static Indice indiceDe(Tabela tabela) {
        return INDICES.computeIfAbsent(tabela, Indice::new);
    }

    private static final Map<String, Tabela> TABELAS = new LinkedHashMap<>();

    private static Tabela aleatoria(int linhas, int linhasPorPagina) {
        return TABELAS.computeIfAbsent("aleatoria " + linhas + " " + linhasPorPagina,
                chave -> Tabelas.aleatoria(linhas, linhasPorPagina, SEMENTE));
    }

    private static Tabela agrupada(int linhas, int linhasPorPagina) {
        return TABELAS.computeIfAbsent("agrupada " + linhas + " " + linhasPorPagina,
                chave -> Tabelas.agrupada(linhas, linhasPorPagina));
    }

    /** Roda uma via com um contador novo, do tamanho certo. */
    public static Contador medir(ViaDeAcesso via, Tabela tabela, Consulta consulta) {
        Contador contador = new Contador();
        contador.prepararPara(tabela.paginas());

        via.buscar(consulta, contador);

        return contador;
    }

    private static int linhasDe(double seletividade, int total) {
        return Math.max(1, (int) Math.round(seletividade * total));
    }

    // ───────────────────── 1. a correcao ─────────────────────

    public record LinhaCorrecao(String tabela, String via, int linhas, boolean certa,
            String problema) {}

    /** Toda via devolve as mesmas linhas, em toda forma de tabela e toda seletividade. */
    public static List<LinhaCorrecao> aCorrecao() {
        List<LinhaCorrecao> linhas = new ArrayList<>();

        int n = 20_000;
        int p = 64;

        Map<String, Tabela> formas = new LinkedHashMap<>();
        formas.put("agrupada", Tabelas.agrupada(n, p));
        formas.put("janela 1000", Tabelas.correlacionada(n, p, 1_000, SEMENTE));
        formas.put("aleatoria", Tabelas.aleatoria(n, p, SEMENTE));

        for (Map.Entry<String, Tabela> forma : formas.entrySet()) {
            Tabela tabela = forma.getValue();
            Indice indice = indiceDe(tabela);

            List<ViaDeAcesso> vias = new ArrayList<>(List.of(
                    new VarreduraCompleta(tabela),
                    new PorIndiceNaoAgrupado(tabela, indice),
                    new PorIndiceComOrdenacao(tabela, indice)));

            if (tabela.agrupada()) vias.add(new PorIndiceAgrupado(tabela, indice));

            for (double seletividade : new double[] {0.0005, 0.01, 0.2, 1.0}) {
                Consulta consulta = faixaNoMeio(n, linhasDe(seletividade, n));

                int[] certo = Juiz.esperado(tabela, consulta);

                for (ViaDeAcesso via : vias) {
                    Juiz.Veredito veredito = Juiz.julgar(via, tabela, consulta);

                    linhas.add(new LinhaCorrecao(forma.getKey(), via.nome(), certo.length,
                            veredito.certa(), veredito.problema()));
                }
            }
        }

        return linhas;
    }

    /**
     * Uma faixa de tantas linhas que NAO comeca no zero.
     *
     * <p>Comecar no zero esconderia um erro inteiro de via: quem errasse a descida no
     * indice e comecasse sempre na primeira entrada passaria em todos os testes.
     */
    public static Consulta faixaNoMeio(int total, int quantas) {
        int de = Math.max(0, (total - quantas) / 2);

        return new Consulta(de, de + quantas - 1);
    }

    // ───────────────────── 2. a seletividade ─────────────────────

    public record LinhaSeletividade(double seletividade, int linhas, String via, long paginas,
            long entradas, double vezesAVarredura) {}

    /** O eixo principal: quantas paginas cada via le conforme a consulta devolve mais. */
    public static List<LinhaSeletividade> aSeletividade() {
        List<LinhaSeletividade> saida = new ArrayList<>();

        Tabela espalhada = aleatoria(LINHAS, LINHAS_POR_PAGINA);
        Tabela emOrdem = agrupada(LINHAS, LINHAS_POR_PAGINA);

        long daVarredura = espalhada.paginas();

        for (double seletividade : SELETIVIDADES) {
            int quantas = linhasDe(seletividade, LINHAS);

            for (ViaDeAcesso via : List.of(
                    new VarreduraCompleta(espalhada),
                    new PorIndiceNaoAgrupado(espalhada, indiceDe(espalhada)),
                    new PorIndiceComOrdenacao(espalhada, indiceDe(espalhada)))) {

                Contador contador = medir(via, espalhada, faixaNoMeio(LINHAS, quantas));

                saida.add(new LinhaSeletividade(seletividade, quantas, via.nome(),
                        contador.paginasLidas(), contador.entradasDeIndice(),
                        (double) contador.paginasLidas() / daVarredura));
            }

            ViaDeAcesso agrupado = new PorIndiceAgrupado(emOrdem, indiceDe(emOrdem));

            Contador contador = medir(agrupado, emOrdem, faixaNoMeio(LINHAS, quantas));

            saida.add(new LinhaSeletividade(seletividade, quantas, agrupado.nome(),
                    contador.paginasLidas(), contador.entradasDeIndice(),
                    (double) contador.paginasLidas() / daVarredura));
        }

        return saida;
    }

    // ───────────────────── 3. o agrupamento ─────────────────────

    public record LinhaAgrupamento(String forma, int linhas, long semOrdenar, long ordenando,
            long distintas, double vezesAVarredura) {}

    /** A mesma consulta nas mesmas chaves, mudando so a ordem em que as linhas estao. */
    public static List<LinhaAgrupamento> oAgrupamento() {
        List<LinhaAgrupamento> saida = new ArrayList<>();

        Map<String, Tabela> formas = new LinkedHashMap<>();
        formas.put("agrupada", agrupada(LINHAS, LINHAS_POR_PAGINA));
        formas.put("janela 100", Tabelas.correlacionada(LINHAS, LINHAS_POR_PAGINA, 100, SEMENTE));
        formas.put("janela 1000", Tabelas.correlacionada(LINHAS, LINHAS_POR_PAGINA, 1_000, SEMENTE));
        formas.put("janela 10000", Tabelas.correlacionada(LINHAS, LINHAS_POR_PAGINA, 10_000, SEMENTE));
        formas.put("aleatoria", aleatoria(LINHAS, LINHAS_POR_PAGINA));

        int quantas = linhasDe(0.01, LINHAS);

        Consulta consulta = faixaNoMeio(LINHAS, quantas);

        for (Map.Entry<String, Tabela> forma : formas.entrySet()) {
            Tabela tabela = forma.getValue();
            Indice indice = indiceDe(tabela);

            Contador sem = medir(new PorIndiceNaoAgrupado(tabela, indice), tabela, consulta);
            Contador com = medir(new PorIndiceComOrdenacao(tabela, indice), tabela, consulta);

            saida.add(new LinhaAgrupamento(forma.getKey(), quantas, sem.paginasLidas(),
                    com.paginasLidas(), com.paginasDistintas(),
                    (double) com.paginasLidas() / tabela.paginas()));
        }

        return saida;
    }

    // ───────────────────── 4. a ordenacao ─────────────────────

    public record LinhaOrdenacao(double seletividade, int linhas, long semOrdenar, long ordenando,
            long repetidas, double fracaoDaTabela, long passos) {}

    /** O que ordenar os numeros de linha antes de ir busca-las economiza, e onde. */
    public static List<LinhaOrdenacao> aOrdenacao() {
        List<LinhaOrdenacao> saida = new ArrayList<>();

        Tabela tabela = aleatoria(LINHAS, LINHAS_POR_PAGINA);
        Indice indice = indiceDe(tabela);

        for (double seletividade : SELETIVIDADES) {
            int quantas = linhasDe(seletividade, LINHAS);

            Consulta consulta = faixaNoMeio(LINHAS, quantas);

            Contador sem = medir(new PorIndiceNaoAgrupado(tabela, indice), tabela, consulta);
            Contador com = medir(new PorIndiceComOrdenacao(tabela, indice), tabela, consulta);

            saida.add(new LinhaOrdenacao(seletividade, quantas, sem.paginasLidas(),
                    com.paginasLidas(), sem.paginasRepetidas(),
                    (double) com.paginasLidas() / tabela.paginas(), com.passosDeOrdenacao()));
        }

        return saida;
    }

    // ───────────────────── 5. a pagina ─────────────────────

    public record LinhaPagina(int linhas, int linhasPorPagina, int paginasDaTabela,
            int viradaEmLinhas, double viradaEmFracao, double umSobreAPagina,
            double fracaoLidaNaVirada) {}

    /** Onde a via sem ordenar passa da varredura, para varios tamanhos de pagina e tabela. */
    public static List<LinhaPagina> aPagina() {
        List<LinhaPagina> saida = new ArrayList<>();

        for (int linhas : new int[] {100_000, 400_000}) {
            for (int porPagina : new int[] {10, 25, 100, 400, 1_000}) {
                Tabela tabela = aleatoria(linhas, porPagina);
                Indice indice = indiceDe(tabela);

                int virada = viradaSemOrdenar(tabela, indice);

                Contador naVirada = medir(new PorIndiceComOrdenacao(tabela, indice), tabela,
                        faixaNoMeio(linhas, virada));

                saida.add(new LinhaPagina(linhas, porPagina, tabela.paginas(), virada,
                        (double) virada / linhas, 1.0 / porPagina,
                        (double) naVirada.paginasLidas() / tabela.paginas()));
            }
        }

        return saida;
    }

    /**
     * A menor consulta em que o indice sem ordenar le MAIS paginas que a varredura.
     *
     * <p>Busca binaria sobre o numero de linhas: a conta de paginas da via sem ordenar
     * cresce com o numero de linhas devolvidas, entao a resposta esta num ponto so.
     */
    public static int viradaSemOrdenar(Tabela tabela, Indice indice) {
        ViaDeAcesso via = new PorIndiceNaoAgrupado(tabela, indice);

        long teto = tabela.paginas();

        int baixo = 1;
        int alto = tabela.linhas();

        while (baixo < alto) {
            int meio = (baixo + alto) >>> 1;

            long paginas = medir(via, tabela, faixaNoMeio(tabela.linhas(), meio)).paginasLidas();

            if (paginas > teto) alto = meio;
            else baixo = meio + 1;
        }

        return baixo;
    }
}
