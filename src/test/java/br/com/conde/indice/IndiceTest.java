package br.com.conde.indice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** As pecas, uma a uma, antes de qualquer medida. */
class IndiceTest {

    // ───────────────────── o sorteio ─────────────────────

    @Nested
    @DisplayName("o sorteio da casa")
    class OSorteio {

        /** O mesmo gerador sem o deslocamento, para o teste poder mostrar a diferenca. */
        private static final class SemDeslocamento {
            private long estado;

            SemDeslocamento(long semente) {
                estado = semente;
            }

            int ate(int limite) {
                estado = estado * 6364136223846793005L + 1442695040888963407L;

                return Math.floorMod((int) estado, limite);
            }
        }

        /**
         * O DEFEITO DOS BITS DE BAIXO, no caso em que ele aparece de verdade.
         *
         * <p>Com limite dois, o gerador estragado alterna zero e um para sempre: o ultimo
         * bit de um gerador linear congruente tem periodo dois, e o resto por dois e
         * exatamente esse bit.
         *
         * <p>Neste repositorio isso nao e teoria: a tabela correlacionada de janela dois
         * pede ate(2) a cada par, e com o gerador estragado ela trocaria um par sim e um
         * par nao, sempre, o que nao e embaralhar nada.
         */
        @Test
        @DisplayName("sem o deslocamento, sortear entre dois alterna para sempre")
        void semODeslocamentoOSorteioAlterna() {
            SemDeslocamento estragado = new SemDeslocamento(Medidas.SEMENTE);

            int anterior = estragado.ate(2);
            int alternancias = 0;

            for (int i = 0; i < 1_000; i++) {
                int agora = estragado.ate(2);

                if (agora != anterior) alternancias++;

                anterior = agora;
            }

            assertThat(alternancias)
                    .as("mil sorteios, mil trocas: nao sobrou acaso nenhum")
                    .isEqualTo(1_000);

            Sorteio bom = new Sorteio(Medidas.SEMENTE);

            int anteriorBom = bom.ate(2);
            int alternanciasBoas = 0;

            for (int i = 0; i < 1_000; i++) {
                int agora = bom.ate(2);

                if (agora != anteriorBom) alternanciasBoas++;

                anteriorBom = agora;
            }

            assertThat(alternanciasBoas)
                    .as("com os bits de cima da perto da metade, que e o que acaso faz")
                    .isBetween(440, 560);
        }

        /**
         * E O QUE EU ACHAVA QUE QUEBRARIA E NAO QUEBRA.
         *
         * <p>Eu tinha escrito que os bits de baixo estragariam o embaralhamento da tabela.
         * Nao estragam de forma mensuravel: Fisher e Yates pede um resto por um limite
         * diferente a cada passo, e isso dissolve o padrao. Este teste existe para a frase
         * do javadoc nao voltar a ser a frase errada.
         */
        @Test
        @DisplayName("mas o embaralhamento de Fisher e Yates sobrevive aos bits de baixo")
        void oEmbaralhamentoSobrevive() {
            int n = 100_000;

            int[] chaves = new int[n];
            for (int i = 0; i < n; i++) chaves[i] = i;

            SemDeslocamento estragado = new SemDeslocamento(Medidas.SEMENTE);

            for (int i = n - 1; i > 0; i--) {
                int j = estragado.ate(i + 1);

                int guarda = chaves[i];
                chaves[i] = chaves[j];
                chaves[j] = guarda;
            }

            Tabela ruim = new Tabela(chaves, 100, false);
            Tabela boa = Tabelas.aleatoria(n, 100, Medidas.SEMENTE);

            Consulta consulta = Medidas.faixaNoMeio(n, 1_000);

            long comEstragado =
                    Medidas.medir(new PorIndiceComOrdenacao(ruim), ruim, consulta).paginasLidas();
            long comBom =
                    Medidas.medir(new PorIndiceComOrdenacao(boa), boa, consulta).paginasLidas();

            assertThat(comEstragado).isEqualTo(613);
            assertThat(comBom).isEqualTo(630);
        }

        @Test
        @DisplayName("o sorteio da os mesmos numeros em qualquer maquina")
        void oSorteioEhFixo() {
            Sorteio sorteio = new Sorteio(1);

            assertThat(List.of(sorteio.proximo(), sorteio.proximo(), sorteio.proximo()))
                    .containsExactly(1817669548, -2107078989, -1510284903);
        }

        @Test
        @DisplayName("embaralhar devolve uma permutacao, sem perder nem repetir chave")
        void embaralharEhPermutacao() {
            int[] chaves = new int[10_000];
            for (int i = 0; i < chaves.length; i++) chaves[i] = i;

            new Sorteio(7).embaralhar(chaves);

            int[] copia = chaves.clone();
            java.util.Arrays.sort(copia);

            for (int i = 0; i < copia.length; i++) assertThat(copia[i]).isEqualTo(i);

            assertThat(chaves).as("e de fato mexeu").isNotEqualTo(copia);
        }

        @Test
        @DisplayName("limite zero ou negativo nao e sorteio, e reclama")
        void limiteInvalido() {
            assertThatThrownBy(() -> new Sorteio(1).ate(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positivo");
        }
    }

    // ───────────────────── a tabela ─────────────────────

    @Nested
    @DisplayName("a tabela e suas paginas")
    class ATabela {

        @Test
        @DisplayName("a pagina de uma linha e o numero dela dividido pelo tamanho da pagina")
        void aPaginaDeUmaLinha() {
            Tabela tabela = Tabelas.agrupada(10, 4);

            assertThat(tabela.paginas()).as("dez linhas de quatro em quatro dao tres paginas")
                    .isEqualTo(3);

            assertThat(tabela.paginaDa(0)).isZero();
            assertThat(tabela.paginaDa(3)).isZero();
            assertThat(tabela.paginaDa(4)).isEqualTo(1);
            assertThat(tabela.paginaDa(9)).isEqualTo(2);
        }

        @Test
        @DisplayName("a ultima pagina pode vir pela metade, e o fim dela e a ultima linha")
        void aUltimaPaginaEhParcial() {
            Tabela tabela = Tabelas.agrupada(10, 4);

            assertThat(tabela.primeiraLinhaDa(2)).isEqualTo(8);
            assertThat(tabela.ultimaLinhaDa(2))
                    .as("nove e nao onze: a pagina acaba onde a tabela acaba")
                    .isEqualTo(9);
        }

        @Test
        @DisplayName("uma tabela agrupada fora de ordem nao existe, e o construtor recusa")
        void agrupadaForaDeOrdem() {
            assertThatThrownBy(() -> new Tabela(new int[] {3, 1, 2}, 2, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ordem de chave");
        }

        @Test
        @DisplayName("agrupar e so ordenar as linhas, e as chaves continuam as mesmas")
        void agrupar() {
            Tabela tabela = Tabela.agrupando(new int[] {5, 1, 4, 1}, 2);

            assertThat(tabela.agrupada()).isTrue();
            assertThat(List.of(tabela.chaveDa(0), tabela.chaveDa(1), tabela.chaveDa(2),
                    tabela.chaveDa(3))).containsExactly(1, 1, 4, 5);
        }

        @Test
        @DisplayName("uma janela de uma linha so ja e a tabela agrupada")
        void janelaDeUm() {
            assertThat(Tabelas.correlacionada(100, 10, 1, 1).agrupada()).isTrue();
            assertThat(Tabelas.correlacionada(100, 10, 2, 1).agrupada()).isFalse();
        }

        @Test
        @DisplayName("a correlacionada nunca tira a linha da janela dela")
        void aCorrelacionadaRespeitaAJanela() {
            int janela = 50;

            Tabela tabela = Tabelas.correlacionada(10_000, 100, janela, Medidas.SEMENTE);

            for (int linha = 0; linha < tabela.linhas(); linha++)
                assertThat(Math.abs(tabela.chaveDa(linha) - linha))
                        .as("linha " + linha)
                        .isLessThan(janela);
        }
    }

    // ───────────────────── a consulta ─────────────────────

    @Nested
    @DisplayName("a consulta")
    class AConsulta {

        @Test
        @DisplayName("a faixa de zero a k menos um devolve exatamente k linhas")
        void faixaExata() {
            Consulta consulta = Consulta.comTantasLinhas(7);

            assertThat(consulta.de()).isZero();
            assertThat(consulta.ate()).isEqualTo(6);
            assertThat(consulta.linhasEsperadas()).isEqualTo(7);
        }

        @Test
        @DisplayName("uma faixa de tras para frente nao e faixa")
        void faixaInvertida() {
            assertThatThrownBy(() -> new Consulta(10, 3))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("faixa vazia");
        }

        @Test
        @DisplayName("a faixa das medidas nao comeca no zero, de proposito")
        void aFaixaFicaNoMeio() {
            Consulta consulta = Medidas.faixaNoMeio(100_000, 1_000);

            assertThat(consulta.de())
                    .as("quem errasse a descida no indice passaria se tudo comecasse em zero")
                    .isEqualTo(49_500);

            assertThat(consulta.linhasEsperadas()).isEqualTo(1_000);
        }
    }

    // ───────────────────── o contador ─────────────────────

    @Nested
    @DisplayName("o contador")
    class OContador {

        @Test
        @DisplayName("contar pagina sem preparar seria uma coluna zerada em silencio")
        void semPreparar() {
            assertThatThrownBy(() -> new Contador().lePagina(0))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("prepararPara");
        }

        @Test
        @DisplayName("uma pagina fora da tabela e um erro de via, e nao uma leitura")
        void paginaForaDaTabela() {
            Contador contador = new Contador();
            contador.prepararPara(3);

            assertThatThrownBy(() -> contador.lePagina(3))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fora da tabela");
        }

        @Test
        @DisplayName("pedir a mesma pagina de novo conta como lida e nao como distinta")
        void paginasRepetidas() {
            Contador contador = new Contador();
            contador.prepararPara(4);

            contador.lePagina(2);
            contador.lePagina(2);
            contador.lePagina(0);
            contador.lePagina(2);

            assertThat(contador.paginasLidas()).isEqualTo(4);
            assertThat(contador.paginasDistintas()).isEqualTo(2);
            assertThat(contador.paginasRepetidas()).isEqualTo(2);
        }

        @Test
        @DisplayName("ordenar um numero de linhas so custa n vezes log n, e abaixo de dois e zero")
        void oCustoDeOrdenar() {
            Contador contador = new Contador();

            contador.ordena(1);
            assertThat(contador.passosDeOrdenacao()).isZero();

            contador.ordena(1_000);
            assertThat(contador.passosDeOrdenacao()).isEqualTo(10_000);
        }
    }

    // ───────────────────── o indice ─────────────────────

    @Nested
    @DisplayName("o indice")
    class OIndice {

        private final Tabela comRepetidas = new Tabela(new int[] {5, 3, 5, 1, 3}, 2, false);

        @Test
        @DisplayName("as entradas saem em ordem de chave e desempatam pelo numero da linha")
        void aOrdemDasEntradas() {
            Indice indice = new Indice(comRepetidas);

            List<Integer> chaves = new ArrayList<>();
            List<Integer> linhas = new ArrayList<>();

            for (int i = 0; i < indice.entradas(); i++) {
                chaves.add(indice.chaveDaEntrada(i));
                linhas.add(indice.linhaDaEntrada(i));
            }

            assertThat(chaves).containsExactly(1, 3, 3, 5, 5);
            assertThat(linhas)
                    .as("as duas chaves 3 estao nas linhas 1 e 4, nessa ordem")
                    .containsExactly(3, 1, 4, 0, 2);
        }

        @Test
        @DisplayName("a faixa devolve as linhas na ordem da chave, nao na ordem da pagina")
        void aFaixaSaiNaOrdemDaChave() {
            Contador contador = new Contador();
            contador.prepararPara(comRepetidas.paginas());

            assertThat(new Indice(comRepetidas).faixa(new Consulta(3, 5), contador))
                    .containsExactly(1, 4, 0, 2);
        }

        @Test
        @DisplayName("a descida custa o logaritmo das entradas, e isso e o que o indice cobra")
        void aDescidaCustaOLogaritmo() {
            Tabela tabela = Tabelas.aleatoria(100_000, 100, Medidas.SEMENTE);

            Contador contador = new Contador();
            contador.prepararPara(tabela.paginas());

            new Indice(tabela).primeiraComChaveAPartirDe(50_000, contador);

            assertThat(contador.entradasDeIndice())
                    .as("cem mil entradas cabem em dezesseis comparacoes de busca binaria")
                    .isEqualTo(16);
        }

        @Test
        @DisplayName("uma chave acima de todas cai no fim, e a faixa sai vazia")
        void faixaAcimaDeTudo() {
            Contador contador = new Contador();
            contador.prepararPara(comRepetidas.paginas());

            assertThat(new Indice(comRepetidas).faixa(new Consulta(9, 20), contador)).isEmpty();
        }

        @Test
        @DisplayName("este indice guarda a chave nos bits de cima, e chave negativa nao cabe")
        void chaveNegativa() {
            assertThatThrownBy(() -> new Indice(new Tabela(new int[] {-1}, 1, false)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nao negativa");
        }
    }

    // ───────────────────── as vias ─────────────────────

    @Nested
    @DisplayName("as vias de acesso")
    class AsVias {

        private final Tabela espalhada = Tabelas.aleatoria(10_000, 100, Medidas.SEMENTE);
        private final Tabela emOrdem = Tabelas.agrupada(10_000, 100);

        @Test
        @DisplayName("a varredura le a tabela inteira e olha toda linha, qualquer que seja a consulta")
        void aVarreduraEhUmaReta() {
            for (int quantas : new int[] {1, 100, 10_000}) {
                Contador contador = Medidas.medir(new VarreduraCompleta(espalhada), espalhada,
                        Medidas.faixaNoMeio(10_000, quantas));

                assertThat(contador.paginasLidas()).as("" + quantas).isEqualTo(100);
                assertThat(contador.linhasExaminadas()).as("" + quantas).isEqualTo(10_000);
                assertThat(contador.linhasDevolvidas()).as("" + quantas).isEqualTo(quantas);
            }
        }

        @Test
        @DisplayName("sem ordenar, e uma pagina por linha devolvida, e nao por pagina distinta")
        void umaPaginaPorLinha() {
            Contador contador = Medidas.medir(new PorIndiceNaoAgrupado(espalhada), espalhada,
                    Medidas.faixaNoMeio(10_000, 500));

            assertThat(contador.paginasLidas()).isEqualTo(500);
            assertThat(contador.paginasDistintas()).isLessThan(500);
        }

        /**
         * AS DUAS VIAS DO INDICE TOCAM AS MESMAS PAGINAS. A diferenca e so quantas vezes.
         *
         * <p>Isso nao e obvio de fora e e o que explica a tabela da ordenacao inteira:
         * ordenar nao evita nenhuma pagina, evita RE-LEITURA.
         */
        @Test
        @DisplayName("ordenar nao evita pagina nenhuma, evita pedir de novo a mesma")
        void ordenarNaoEvitaPagina() {
            Consulta consulta = Medidas.faixaNoMeio(10_000, 500);

            Contador sem = Medidas.medir(new PorIndiceNaoAgrupado(espalhada), espalhada, consulta);
            Contador com = Medidas.medir(new PorIndiceComOrdenacao(espalhada), espalhada, consulta);

            assertThat(com.paginasDistintas())
                    .as("as mesmas paginas distintas")
                    .isEqualTo(sem.paginasDistintas());

            assertThat(com.paginasLidas())
                    .as("e la cada uma foi pedida uma vez so")
                    .isEqualTo(com.paginasDistintas());

            assertThat(sem.paginasRepetidas()).isPositive();
            assertThat(com.paginasRepetidas()).isZero();
        }

        @Test
        @DisplayName("o indice agrupado so desce uma vez, e depois anda na tabela")
        void oAgrupadoSoDesceUmaVez() {
            Contador contador = Medidas.medir(new PorIndiceAgrupado(emOrdem), emOrdem,
                    Medidas.faixaNoMeio(10_000, 5_000));

            assertThat(contador.linhasDevolvidas()).isEqualTo(5_000);

            assertThat(contador.entradasDeIndice())
                    .as("treze da descida mais a entrada que diz onde comecar")
                    .isEqualTo(14);

            assertThat(contador.paginasLidas())
                    .as("cinco mil linhas contiguas de cem em cem sao cinquenta e uma paginas")
                    .isEqualTo(51);
        }

        @Test
        @DisplayName("o indice agrupado nao existe sem tabela agrupada, e o construtor recusa")
        void oAgrupadoExigeTabelaAgrupada() {
            assertThatThrownBy(() -> new PorIndiceAgrupado(espalhada))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tabela agrupada");
        }

        @Test
        @DisplayName("consulta que nao casa com nada devolve nada, em todas as vias")
        void consultaVazia() {
            Consulta nada = new Consulta(500_000, 500_010);

            for (ViaDeAcesso via : List.of(new VarreduraCompleta(espalhada),
                    new PorIndiceNaoAgrupado(espalhada), new PorIndiceComOrdenacao(espalhada))) {

                Contador contador = new Contador();
                contador.prepararPara(espalhada.paginas());

                assertThat(via.buscar(nada, contador)).as(via.nome()).isEmpty();
            }

            Contador contador = new Contador();
            contador.prepararPara(emOrdem.paginas());

            assertThat(new PorIndiceAgrupado(emOrdem).buscar(nada, contador)).isEmpty();
        }
    }

    // ───────────────────── o juiz ─────────────────────

    @Nested
    @DisplayName("o juiz")
    class OJuiz {

        private final Tabela tabela = Tabelas.aleatoria(5_000, 50, Medidas.SEMENTE);

        /** Uma via que esquece a ultima linha: o erro mais facil de nao notar. */
        private ViaDeAcesso quebrada() {
            return new ViaDeAcesso() {
                @Override
                public String nome() {
                    return "esquece a ultima";
                }

                @Override
                public int[] buscar(Consulta consulta, Contador contador) {
                    int[] linhas = new PorIndiceComOrdenacao(tabela).buscar(consulta, contador);

                    return java.util.Arrays.copyOf(linhas, Math.max(0, linhas.length - 1));
                }
            };
        }

        @Test
        @DisplayName("o juiz pega uma via que devolve uma linha a menos")
        void pegaAViaQueEsquece() {
            Juiz.Veredito veredito =
                    Juiz.julgar(quebrada(), tabela, Medidas.faixaNoMeio(5_000, 300));

            assertThat(veredito.certa()).isFalse();
            assertThat(veredito.devolvidas()).isEqualTo(299);
            assertThat(veredito.esperadas()).isEqualTo(300);
            assertThat(veredito.problema()).contains("299").contains("300");
        }

        @Test
        @DisplayName("o juiz pega uma via que devolve a mesma linha duas vezes")
        void pegaAViaQueRepete() {
            ViaDeAcesso repetidora = new ViaDeAcesso() {
                @Override
                public String nome() {
                    return "repete a primeira";
                }

                @Override
                public int[] buscar(Consulta consulta, Contador contador) {
                    int[] linhas = new PorIndiceComOrdenacao(tabela).buscar(consulta, contador);

                    linhas[linhas.length - 1] = linhas[0];

                    return linhas;
                }
            };

            Juiz.Veredito veredito =
                    Juiz.julgar(repetidora, tabela, Medidas.faixaNoMeio(5_000, 300));

            assertThat(veredito.certa()).isFalse();
            assertThat(veredito.problema()).contains("duas vezes");
        }

        @Test
        @DisplayName("o juiz nao olha paginas: a via mais cara do mundo passa igual")
        void oJuizNaoOlhaDesempenho() {
            Consulta consulta = Medidas.faixaNoMeio(5_000, 2_500);

            Contador caro = Medidas.medir(new PorIndiceNaoAgrupado(tabela), tabela, consulta);
            Contador barato = Medidas.medir(new PorIndiceComOrdenacao(tabela), tabela, consulta);

            assertThat(caro.paginasLidas()).isGreaterThan(barato.paginasLidas() * 20);

            assertThat(Juiz.julgar(new PorIndiceNaoAgrupado(tabela), tabela, consulta).certa())
                    .isTrue();
        }
    }
}
