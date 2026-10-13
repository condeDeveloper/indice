package br.com.conde.indice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros do README, afirmados.
 *
 * <p>Um README e texto: ele nao quebra quando deixa de ser verdade. Cada afirmacao das
 * tabelas esta repetida aqui para quebrar.
 */
class MedidasTest {

    /* Cada tabela varre cem mil linhas varias vezes: calcular uma vez so e o que importa. */
    private static final List<Medidas.LinhaSeletividade> SELETIVIDADE = Medidas.aSeletividade();
    private static final List<Medidas.LinhaAgrupamento> AGRUPAMENTO = Medidas.oAgrupamento();
    private static final List<Medidas.LinhaOrdenacao> ORDENACAO = Medidas.aOrdenacao();
    private static final List<Medidas.LinhaPagina> PAGINA = Medidas.aPagina();

    // ───────────────────── 1. a correcao ─────────────────────

    @Test
    @DisplayName("toda via devolve as mesmas linhas, em toda forma de tabela")
    void todasConcordam() {
        List<Medidas.LinhaCorrecao> linhas = Medidas.aCorrecao();

        assertThat(linhas).hasSize(40);

        assertThat(linhas).allSatisfy(l ->
                assertThat(l.certa()).as(l.tabela() + " / " + l.via() + ": " + l.problema())
                        .isTrue());

        assertThat(linhas).filteredOn(l -> l.via().equals("indice agrupado"))
                .as("a via agrupada so existe na tabela agrupada")
                .hasSize(4)
                .allSatisfy(l -> assertThat(l.tabela()).isEqualTo("agrupada"));
    }

    // ───────────────────── 2. a seletividade ─────────────────────

    /**
     * O NUMERO PRINCIPAL: a um por cento o indice sem ordenar EMPATA com a varredura.
     *
     * <p>Com cem linhas por pagina, mil linhas devolvidas custam mil paginas, e a tabela
     * inteira tambem custa mil. Daquele ponto em diante o indice e pior do que nao ter
     * indice nenhum.
     */
    @Test
    @DisplayName("a um por cento o indice sem ordenar empata com a varredura, e depois perde")
    void aViradaDeUmPorCento() {
        assertThat(seletividade(0.01, "varredura completa").paginas()).isEqualTo(1_000);
        assertThat(seletividade(0.01, "indice sem ordenar").paginas()).isEqualTo(1_000);

        assertThat(seletividade(0.01, "indice sem ordenar").vezesAVarredura())
                .isCloseTo(1.0, within(0.001));

        assertThat(seletividade(0.02, "indice sem ordenar").vezesAVarredura())
                .as("a dois por cento ele ja le o dobro")
                .isCloseTo(2.0, within(0.001));

        assertThat(seletividade(0.10, "indice sem ordenar").vezesAVarredura())
                .as("e a dez por cento, dez vezes")
                .isCloseTo(10.0, within(0.001));

        assertThat(seletividade(1.0, "indice sem ordenar").paginas())
                .as("devolvendo tudo ele le cem paginas por pagina que existe")
                .isEqualTo(100_000);
    }

    @Test
    @DisplayName("a varredura completa e uma reta: mil paginas em toda seletividade")
    void aVarreduraEhUmaReta() {
        for (double seletividade : Medidas.SELETIVIDADES) {
            assertThat(seletividade(seletividade, "varredura completa").paginas())
                    .as("" + seletividade)
                    .isEqualTo(1_000);

            assertThat(seletividade(seletividade, "varredura completa").entradas())
                    .as("e ela nao le entrada de indice nenhuma")
                    .isZero();
        }
    }

    /**
     * O INDICE AGRUPADO E OUTRO ESPORTE, e e por isso que ele quase nao aparece no texto.
     *
     * <p>Ele le as linhas que casam divididas pelo tamanho da pagina, e le o indice uma vez
     * so. So que uma tabela tem no maximo um indice agrupado.
     */
    @Test
    @DisplayName("o indice agrupado le dezessete entradas, devolvendo dez linhas ou cem mil")
    void oAgrupadoNaoDependeDaSeletividade() {
        for (double seletividade : Medidas.SELETIVIDADES)
            assertThat(seletividade(seletividade, "indice agrupado").entradas())
                    .as("" + seletividade)
                    .isBetween(17L, 18L);

        assertThat(seletividade(1.0, "indice sem ordenar").entradas())
                .as("e a via sem ordenar le uma entrada por linha devolvida")
                .isEqualTo(100_017);

        assertThat(seletividade(0.01, "indice agrupado").paginas()).isEqualTo(11);
        assertThat(seletividade(0.10, "indice agrupado").paginas()).isEqualTo(101);

        assertThat(seletividade(1.0, "indice agrupado").paginas())
                .as("so devolvendo a tabela inteira ele empata com a varredura")
                .isEqualTo(1_000);
    }

    private static Medidas.LinhaSeletividade seletividade(double qual, String via) {
        return SELETIVIDADE.stream()
                .filter(l -> l.seletividade() == qual && l.via().equals(via))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 3. o agrupamento ─────────────────────

    /**
     * A MESMA CONSULTA, AS MESMAS CHAVES, SO A ORDEM DAS LINHAS MUDANDO.
     *
     * <p>Dez paginas contra seiscentas e trinta. Nada no indice mudou.
     */
    @Test
    @DisplayName("so a ordem das linhas muda, e a conta vai de 10 paginas a 630")
    void aOrdemDasLinhasEhTudo() {
        assertThat(agrupamento("agrupada").ordenando()).isEqualTo(10);
        assertThat(agrupamento("janela 100").ordenando()).isEqualTo(10);
        assertThat(agrupamento("janela 1000").ordenando()).isEqualTo(20);
        assertThat(agrupamento("janela 10000").ordenando()).isEqualTo(197);
        assertThat(agrupamento("aleatoria").ordenando()).isEqualTo(630);

        assertThat(AGRUPAMENTO).allSatisfy(l -> {
            assertThat(l.linhas()).as(l.forma()).isEqualTo(1_000);

            assertThat(l.semOrdenar())
                    .as(l.forma() + ": sem ordenar e sempre uma pagina por linha")
                    .isEqualTo(1_000);
        });
    }

    /**
     * UMA JANELA DO TAMANHO DA PAGINA JA E AGRUPAMENTO.
     *
     * <p>Se a linha da chave c esta sempre a menos de cem linhas de c, e a pagina tem cem
     * linhas, entao cada pagina guarda uma faixa quase contigua de chaves. A tabela nao
     * esta em ordem nenhuma, e para o indice tanto faz.
     */
    @Test
    @DisplayName("uma janela do tamanho da pagina custa o mesmo que a tabela agrupada")
    void aJanelaDoTamanhoDaPagina() {
        assertThat(agrupamento("janela 100").ordenando())
                .isEqualTo(agrupamento("agrupada").ordenando());

        assertThat(Tabelas.correlacionada(Medidas.LINHAS, Medidas.LINHAS_POR_PAGINA, 100,
                Medidas.SEMENTE).agrupada())
                .as("e ela nao esta agrupada: so esta correlacionada")
                .isFalse();

        assertThat(agrupamento("aleatoria").vezesAVarredura() / agrupamento("agrupada").vezesAVarredura())
                .as("entre os dois extremos sao sessenta e tres vezes")
                .isCloseTo(63.0, within(0.5));
    }

    private static Medidas.LinhaAgrupamento agrupamento(String forma) {
        return AGRUPAMENTO.stream()
                .filter(l -> l.forma().equals(forma))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 4. a ordenacao ─────────────────────

    /**
     * A CORRECAO: ordenar nao salva o indice. Salva so da humilhacao.
     *
     * <p>Na seletividade exata em que a via sem ordenar empata com a varredura, a via que
     * ordena ja le 63% da tabela. O ganho dela cai junto com a utilidade dele.
     */
    @Test
    @DisplayName("na virada, a via que ordena ja le 63% da tabela")
    void naViradaOrdenarJaNaoSalvaMuito() {
        assertThat(ordenacao(0.01).ordenando()).isEqualTo(630);
        assertThat(ordenacao(0.01).fracaoDaTabela()).isCloseTo(0.630, within(0.001));

        assertThat(ordenacao(0.05).ordenando())
                .as("a cinco por cento ela ja e uma varredura completa com passos a mais")
                .isEqualTo(997);

        assertThat(ordenacao(0.10).fracaoDaTabela()).isEqualTo(1.0);
    }

    /**
     * E ONDE O INDICE E BOM DE VERDADE, ORDENAR NAO FAZ NADA.
     *
     * <p>A um por dez mil, as dez linhas que casam estao em dez paginas diferentes: nao ha
     * releitura para evitar. Ordenar economiza uma pagina de dez.
     */
    @Test
    @DisplayName("na seletividade em que o indice e bom, ordenar economiza uma pagina de dez")
    void ondeOIndiceEhBomOrdenarNaoFazNada() {
        assertThat(ordenacao(0.0001).semOrdenar()).isEqualTo(10);
        assertThat(ordenacao(0.0001).ordenando()).isEqualTo(9);
        assertThat(ordenacao(0.0001).repetidas()).isEqualTo(1);

        assertThat(ordenacao(0.001).semOrdenar()).isEqualTo(100);
        assertThat(ordenacao(0.001).ordenando()).isEqualTo(93);
    }

    @Test
    @DisplayName("a via que ordena nunca le mais que a tabela inteira, por mais larga que seja")
    void ordenarTemTeto() {
        assertThat(ORDENACAO).allSatisfy(l -> {
            assertThat(l.ordenando()).as("" + l.seletividade()).isLessThanOrEqualTo(1_000);

            assertThat(l.fracaoDaTabela()).as("" + l.seletividade()).isLessThanOrEqualTo(1.0);

            assertThat(l.semOrdenar() - l.ordenando())
                    .as("" + l.seletividade())
                    .isEqualTo(l.repetidas());
        });

        assertThat(ordenacao(1.0).ordenando()).isEqualTo(1_000);
        assertThat(ordenacao(1.0).semOrdenar()).isEqualTo(100_000);
    }

    @Test
    @DisplayName("e ordenar custa processador, que e a moeda que ela troca por pagina")
    void ordenarCustaPassos() {
        assertThat(ordenacao(0.01).passos()).isEqualTo(10_000);
        assertThat(ordenacao(1.0).passos()).isEqualTo(1_700_000);
    }

    private static Medidas.LinhaOrdenacao ordenacao(double qual) {
        return ORDENACAO.stream()
                .filter(l -> l.seletividade() == qual)
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 5. a pagina ─────────────────────

    /**
     * A VIRADA E UM SOBRE O TAMANHO DA PAGINA, E NAO DEPENDE DO TAMANHO DA TABELA.
     *
     * <p>Eu esperava que o famoso "entre um e cinco por cento" dependesse do tamanho da
     * tabela. Nao depende de nada disso: depende so de quantas linhas cabem numa pagina.
     */
    @Test
    @DisplayName("a virada e um sobre linhas por pagina, com cem mil linhas ou com quatrocentas mil")
    void aViradaEhUmSobreAPagina() {
        assertThat(PAGINA).hasSize(10);

        assertThat(PAGINA).allSatisfy(l -> {
            assertThat(l.viradaEmLinhas())
                    .as(l.linhas() + " linhas, " + l.linhasPorPagina() + " por pagina")
                    .isEqualTo(l.paginasDaTabela() + 1);

            assertThat(l.viradaEmFracao())
                    .as(l.linhas() + " linhas, " + l.linhasPorPagina() + " por pagina")
                    .isCloseTo(l.umSobreAPagina(), within(0.00002));
        });

        for (int porPagina : new int[] {10, 25, 100, 400, 1_000})
            assertThat(pagina(100_000, porPagina).viradaEmFracao())
                    .as("quadruplicar a tabela nao mexe na fracao, com " + porPagina + " por pagina")
                    .isCloseTo(pagina(400_000, porPagina).viradaEmFracao(), within(0.00002));
    }

    /**
     * E NA VIRADA, SEJA QUAL FOR A PAGINA, A VIA QUE ORDENA LE DOIS TERCOS DA TABELA.
     *
     * <p>Sempre perto de 0,632, que e um menos um sobre e. Nao foi escolhido: cai sozinho
     * de jogar tantas linhas quanto ha paginas em tantas paginas quanto ha.
     */
    @Test
    @DisplayName("na virada a via que ordena le sempre perto de 63% da tabela, em todo tamanho de pagina")
    void naViradaSempreDoisTercos() {
        assertThat(PAGINA).allSatisfy(l ->
                assertThat(l.fracaoLidaNaVirada())
                        .as(l.linhas() + " linhas, " + l.linhasPorPagina() + " por pagina")
                        .isBetween(0.62, 0.66));

        assertThat(pagina(400_000, 100).fracaoLidaNaVirada()).isCloseTo(0.6363, within(0.0001));

        assertThat(1 - 1 / Math.E)
                .as("e isso nao e coincidencia: e um menos um sobre e")
                .isCloseTo(0.632, within(0.001));
    }

    private static Medidas.LinhaPagina pagina(int linhas, int porPagina) {
        return PAGINA.stream()
                .filter(l -> l.linhas() == linhas && l.linhasPorPagina() == porPagina)
                .findFirst()
                .orElseThrow();
    }
}
